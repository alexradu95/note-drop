package app.notedrop.android.ui.widget.camera

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import app.notedrop.android.domain.model.Note
import app.notedrop.android.domain.repository.NoteRepository
import app.notedrop.android.domain.repository.VaultRepository
import app.notedrop.android.domain.sync.ProviderFactory
import app.notedrop.android.ui.theme.NoteDropTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.inject.Inject

/**
 * Transparent activity for instant camera capture
 *
 * Features:
 * - Minimal/transparent UI
 * - Instant photo capture
 * - Auto-close after capture
 * - Permission handling
 */
@AndroidEntryPoint
class TransparentCameraActivity : ComponentActivity() {

    companion object {
        private const val TAG = "TransparentCameraActivity"
    }

    @Inject
    lateinit var noteRepository: NoteRepository

    @Inject
    lateinit var vaultRepository: VaultRepository

    @Inject
    lateinit var providerFactory: ProviderFactory

    private lateinit var cameraExecutor: ExecutorService
    private var imageCapture: ImageCapture? = null

    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startCamera()
        } else {
            Toast.makeText(this, "Camera permission required", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        cameraExecutor = Executors.newSingleThreadExecutor()

        setContent {
            NoteDropTheme {
                var isCapturing by remember { mutableStateOf(false) }

                // Semi-transparent background
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (isCapturing) {
                        CircularProgressIndicator(color = Color.White)
                    } else {
                        // Camera preview (small/hidden)
                        AndroidView(
                            factory = { context ->
                                PreviewView(context)
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                LaunchedEffect(Unit) {
                    checkCameraPermissionAndStart()
                }
            }
        }
    }

    private fun checkCameraPermissionAndStart() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                startCamera()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            // Build preview
            val preview = Preview.Builder().build()

            // Build image capture
            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            // Select back camera
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageCapture
                )

                // Capture photo immediately after camera is ready
                capturePhoto()

            } catch (e: Exception) {
                Log.e(TAG, "Camera binding failed", e)
                finish()
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun capturePhoto() {
        val imageCapture = imageCapture ?: run {
            finish()
            return
        }

        // Create output file
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val photoFile = File(cacheDir, "photo_$timestamp.jpg")

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    Log.d(TAG, "Photo saved: ${photoFile.absolutePath}")
                    Toast.makeText(
                        this@TransparentCameraActivity,
                        "Photo captured!",
                        Toast.LENGTH_SHORT
                    ).show()

                    // Save photo to note repository
                    coroutineScope.launch {
                        savePhotoNote(photoFile)
                    }

                    // Close activity
                    finish()
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed", exception)
                    Toast.makeText(
                        this@TransparentCameraActivity,
                        "Capture failed",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }
            }
        )
    }

    /**
     * Save photo to vault and create note
     */
    private suspend fun savePhotoNote(tempFile: File) {
        try {
            // Get default vault
            val vault = vaultRepository.getDefaultVault()
            if (vault == null) {
                Log.w(TAG, "No default vault configured, photo not saved")
                tempFile.delete()
                return
            }

            // Copy photo file to vault's attachments folder
            val photoFileName = tempFile.name
            val photoRelativePath = "attachments/$photoFileName"

            // Get vault root and create attachments folder
            val vaultUri = android.net.Uri.parse((vault.providerConfig as? app.notedrop.android.domain.model.ProviderConfig.ObsidianConfig)?.vaultPath)
            val vaultRoot = DocumentFile.fromTreeUri(this, vaultUri)

            if (vaultRoot != null && vaultRoot.exists()) {
                // Find or create attachments folder
                val attachmentsFolder = vaultRoot.findFile("attachments") ?: vaultRoot.createDirectory("attachments")

                if (attachmentsFolder != null) {
                    // Create photo file in vault
                    val photoFile = attachmentsFolder.createFile("image/jpeg", photoFileName)

                    if (photoFile != null) {
                        // Copy temp file to vault
                        contentResolver.openOutputStream(photoFile.uri)?.use { outputStream ->
                            tempFile.inputStream().use { inputStream ->
                                inputStream.copyTo(outputStream)
                            }
                        }

                        Log.d(TAG, "Photo file saved to vault: $photoRelativePath")

                        // Create note with photo reference
                        val note = Note(
                            content = "![[${photoRelativePath}]]\n\nPhoto captured from widget",
                            title = "Photo Note",
                            vaultId = vault.id,
                            tags = listOf("photo", "camera"),
                            createdAt = Instant.now(),
                            updatedAt = Instant.now()
                        )

                        val result = noteRepository.createNote(note)

                        result.onSuccess { savedNote ->
                            Log.d(TAG, "Photo note saved to database: ${savedNote.id}")

                            // Sync to provider
                            val noteProvider = providerFactory.getProvider(vault.providerType)
                            if (noteProvider.isAvailable(vault)) {
                                val providerResult = noteProvider.saveNote(savedNote, vault)
                                providerResult.onSuccess { filePath ->
                                    Log.d(TAG, "Photo note synced to provider: $filePath")
                                    noteRepository.updateNote(savedNote.copy(
                                        filePath = filePath,
                                        isSynced = true
                                    ))
                                }.onFailure { providerError ->
                                    Log.e(TAG, "Failed to sync photo note to provider", providerError)
                                }
                            }
                        }.onFailure { error ->
                            Log.e(TAG, "Failed to save photo note to database", error)
                        }
                    }
                }
            }

            // Clean up temp file
            tempFile.delete()

        } catch (e: Exception) {
            Log.e(TAG, "Failed to save photo note", e)
            tempFile.delete()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}
