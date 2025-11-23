package app.notedrop.android.data.voice

import android.content.Context
import android.media.MediaPlayer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for playing back voice recordings.
 */
@Singleton
class VoicePlayer @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var mediaPlayer: MediaPlayer? = null

    private val _playbackState = MutableStateFlow<PlaybackState>(PlaybackState.Idle)
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val _currentPosition = MutableStateFlow(0)
    val currentPosition: StateFlow<Int> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0)
    val duration: StateFlow<Int> = _duration.asStateFlow()

    /**
     * Start playing a recording.
     */
    fun play(filePath: String): Result<Unit> {
        return try {
            // Stop any existing playback
            stop()

            mediaPlayer = MediaPlayer().apply {
                setDataSource(filePath)
                prepare()
                start()

                setOnCompletionListener {
                    _playbackState.value = PlaybackState.Completed
                }

                _duration.value = duration
            }

            _playbackState.value = PlaybackState.Playing(filePath)
            Result.success(Unit)
        } catch (e: Exception) {
            _playbackState.value = PlaybackState.Error(e.message ?: "Unknown error")
            Result.failure(e)
        }
    }

    /**
     * Pause playback.
     */
    fun pause(): Result<Unit> {
        return try {
            mediaPlayer?.pause()
            _playbackState.value = PlaybackState.Paused(
                (playbackState.value as? PlaybackState.Playing)?.filePath ?: ""
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Resume playback.
     */
    fun resume(): Result<Unit> {
        return try {
            mediaPlayer?.start()
            _playbackState.value = PlaybackState.Playing(
                (playbackState.value as? PlaybackState.Paused)?.filePath ?: ""
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Stop playback.
     */
    fun stop(): Result<Unit> {
        return try {
            mediaPlayer?.apply {
                stop()
                release()
            }
            mediaPlayer = null
            _playbackState.value = PlaybackState.Idle
            _currentPosition.value = 0
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Seek to position (in milliseconds).
     */
    fun seekTo(position: Int): Result<Unit> {
        return try {
            mediaPlayer?.seekTo(position)
            _currentPosition.value = position
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get current playback position.
     */
    fun getCurrentPosition(): Int {
        return mediaPlayer?.currentPosition ?: 0
    }

    /**
     * Get total duration.
     */
    fun getDuration(): Int {
        return mediaPlayer?.duration ?: 0
    }

    /**
     * Clean up resources.
     */
    fun release() {
        mediaPlayer?.release()
        mediaPlayer = null
        _playbackState.value = PlaybackState.Idle
    }
}

/**
 * State of the voice player.
 */
sealed class PlaybackState {
    object Idle : PlaybackState()
    data class Playing(val filePath: String) : PlaybackState()
    data class Paused(val filePath: String) : PlaybackState()
    object Completed : PlaybackState()
    data class Error(val message: String) : PlaybackState()
}
