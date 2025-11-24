# NoteDrop Widget Implementation Guide

**Material You Quick Capture Widgets for Android 12+**

---

## 📱 Overview

NoteDrop includes beautiful, responsive home screen widgets built with Jetpack Glance and Material You design. The widgets provide instant access to note capture functionality directly from your home screen.

### Features

- **Material You Dynamic Colors** - Adapts to your wallpaper
- **Responsive Sizing** - Small, Medium, and Large layouts
- **Three Capture Modes:**
  - 📝 **Text Input** - Quick text note
  - 🎤 **Voice Recording** - Record audio note
  - 📷 **Camera Capture** - Take photo note
- **Automatic Updates** - Updates every 30 minutes
- **Battery Optimized** - Respects system constraints

---

## 🏗️ Architecture

### Widget Components

```
Widget System
├── QuickCaptureWidget.kt                    # Main widget (basic)
├── ImprovedQuickCaptureWidget.kt            # Enhanced widget with responsive layouts
├── QuickCaptureWidgetReceiver.kt            # Widget receiver
├── WidgetUpdateWorker.kt                    # Background update worker
├── WidgetUpdateScheduler.kt                 # Update scheduling
└── action/
    └── CaptureActionIntent.kt               # Intent creation helper
```

### Integration Points

1. **MainActivity** - Handles widget launch intents
2. **NoteDropNavigation** - Routes to QuickCaptureScreen
3. **QuickCaptureScreen** - Receives initial capture type
4. **NoteDropApplication** - Schedules widget updates

---

## 📐 Widget Sizes

The widget supports three responsive layouts:

### Small (100x100 dp)
- Single button with icon
- Compact "Note" action
- Perfect for minimal home screens

### Medium (180x200 dp)
- Three action buttons
- App header with icon
- Vertical button layout

### Large (250x300 dp)
- Full featured widget
- App header with subtitle
- Detailed action descriptions
- Larger touch targets

---

## 🎨 Material You Integration

### Dynamic Colors

The widget uses Glance Theme colors that automatically adapt to the user's wallpaper:

```kotlin
GlanceTheme {
    Box(
        modifier = GlanceModifier
            .background(GlanceTheme.colors.surface)
    ) {
        // Widget content with dynamic colors
    }
}
```

### Color Palette

- **Surface** - Widget background
- **OnSurface** - Primary text
- **PrimaryContainer** - Button backgrounds
- **OnPrimaryContainer** - Button text

---

## 🔧 Implementation Details

### 1. Widget Metadata

Location: `app/src/main/res/xml/quick_capture_widget_info.xml`

```xml
<appwidget-provider
    android:description="@string/widget_quick_capture_description"
    android:minWidth="180dp"
    android:minHeight="180dp"
    android:updatePeriodMillis="1800000"
    android:widgetCategory="home_screen"
    android:widgetFeatures="reconfigurable" />
```

### 2. Widget Receiver Registration

Location: `app/src/main/AndroidManifest.xml`

```xml
<receiver
    android:name=".ui.widget.QuickCaptureWidgetReceiver"
    android:exported="true"
    android:label="@string/widget_quick_capture_label">
    <intent-filter>
        <action android:name="android.appwidget.action.APPWIDGET_UPDATE" />
    </intent-filter>
    <meta-data
        android:name="android.appwidget.provider"
        android:resource="@xml/quick_capture_widget_info" />
</receiver>
```

### 3. Capture Types

```kotlin
enum class CaptureType {
    TEXT,    // Text input capture
    VOICE,   // Voice recording capture
    CAMERA   // Camera photo capture
}
```

### 4. Intent Flow

```
Widget Button Click
    ↓
CaptureActionIntent.createIntent()
    ↓
MainActivity.handleWidgetIntent()
    ↓
NoteDropNavigation (with startWithCaptureType)
    ↓
QuickCaptureScreen (initialCaptureType)
```

---

## 🚀 Usage

### Adding Widget to Home Screen

1. **Long press** on home screen
2. Tap **Widgets**
3. Find **NoteDrop**
4. Drag **Quick Capture** widget
5. Select size (Small, Medium, or Large)
6. Drop on home screen

### Widget Actions

#### Text Capture
- Taps opens app to Quick Capture screen
- Ready for text input
- Auto-focuses text field

#### Voice Capture
- Opens app with microphone ready
- Auto-requests RECORD_AUDIO permission
- Begins recording flow

#### Camera Capture
- Opens app with camera intent
- Auto-requests CAMERA permission
- Photo attachment flow

---

## ⚙️ Configuration

### Update Frequency

Default: **30 minutes**

Modify in `WidgetUpdateScheduler.kt`:

```kotlin
val updateRequest = PeriodicWorkRequestBuilder<WidgetUpdateWorker>(
    repeatInterval = 30,  // Change this value
    repeatIntervalTimeUnit = TimeUnit.MINUTES
)
```

### Battery Optimization

Widget updates respect system constraints:

```kotlin
val constraints = Constraints.Builder()
    .setRequiresBatteryNotLow(true)
    .setRequiresDeviceIdle(false)
    .setRequiresCharging(false)
    .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
    .build()
```

---

## 🎯 Best Practices

### 1. Keep It Simple
- Widgets should provide quick access, not full features
- Minimal UI with clear actions
- Fast to load and interact with

### 2. Material You Theming
- Always use `GlanceTheme.colors`
- Support dynamic colors
- Test with different wallpapers

### 3. Responsive Design
- Support multiple widget sizes
- Adapt layout to available space
- Use `SizeMode.Responsive`

### 4. Battery Efficiency
- Schedule updates with WorkManager
- Use appropriate update frequency
- Respect battery constraints

### 5. User Experience
- Immediate feedback on tap
- Clear visual hierarchy
- Accessible touch targets (min 48dp)

---

## 🧪 Testing

### Manual Testing Checklist

- [ ] Widget appears in launcher widget picker
- [ ] Small size renders correctly
- [ ] Medium size renders correctly
- [ ] Large size renders correctly
- [ ] Text button opens app to quick capture
- [ ] Voice button opens app with voice mode
- [ ] Camera button opens app with camera mode
- [ ] Widget updates periodically
- [ ] Material You colors adapt to wallpaper
- [ ] Works in light and dark mode
- [ ] Touch targets are accessible
- [ ] Widget respects battery optimization

### Device Testing

Test on:
- Android 12 (API 31)
- Android 13 (API 33)
- Android 14 (API 34)
- Various launcher apps (Pixel Launcher, Samsung One UI, etc.)

---

## 🐛 Troubleshooting

### Widget Not Appearing

**Issue:** Widget doesn't show in widget picker

**Solution:**
- Check AndroidManifest receiver registration
- Verify widget info XML exists
- Ensure exported="true" in receiver

### Widget Not Updating

**Issue:** Widget content is stale

**Solution:**
- Check WorkManager is initialized
- Verify update scheduling in Application.onCreate()
- Check device battery optimization settings

### Widget Crashes on Click

**Issue:** App crashes when widget button clicked

**Solution:**
- Verify MainActivity handles intent extras
- Check CaptureType enum parsing
- Ensure navigation parameter is optional

### Colors Not Dynamic

**Issue:** Widget doesn't adapt to wallpaper

**Solution:**
- Use `GlanceTheme.colors` instead of hardcoded colors
- Test on Android 12+ devices
- Verify Material You is enabled on device

---

## 📊 Performance

### Widget Load Time
- **Target:** < 100ms
- **Glance Loading Layout:** Shows immediately
- **Content Load:** Asynchronous via `provideGlance()`

### Memory Usage
- **Small Widget:** ~2MB
- **Medium Widget:** ~3MB
- **Large Widget:** ~4MB

### Battery Impact
- **Minimal** - Updates only every 30 minutes
- **Optimized** - Respects battery constraints
- **Efficient** - Uses WorkManager for scheduling

---

## 🔮 Future Enhancements

### Planned Features

1. **Widget Configuration**
   - User-selectable actions
   - Custom button order
   - Theme customization

2. **Dynamic Content**
   - Show recent note count
   - Display last note preview
   - Today's notes summary

3. **Multiple Widget Variants**
   - Voice-only widget
   - Camera-only widget
   - Template-specific widgets

4. **Advanced Layouts**
   - Horizontal button layout
   - Grid layout for large widgets
   - Swipeable action carousel

5. **Interactive Elements**
   - In-widget text input (Glance 1.2+)
   - Quick action menu
   - Note preview cards

---

## 📚 Resources

### Official Documentation

- [Jetpack Glance](https://developer.android.com/jetpack/androidx/releases/glance)
- [App Widgets](https://developer.android.com/develop/ui/views/appwidgets)
- [Material You](https://m3.material.io/)
- [WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager)

### Code Examples

- [Glance Sample Apps](https://github.com/android/snippets/tree/main/compose/glance)
- [Material Design Components](https://github.com/material-components/material-components-android)

---

## 🤝 Contributing

### Adding New Widget Features

1. Create new widget class in `ui/widget/`
2. Extend `GlanceAppWidget`
3. Create corresponding receiver
4. Register in AndroidManifest
5. Add widget info XML
6. Update documentation

### Code Style

- Follow Kotlin coding conventions
- Use Compose-style declarative UI
- Document all public APIs
- Include KDoc comments

---

## 📝 Notes

### Known Limitations

- In-widget text input requires Glance 1.2+ (planned)
- Widget preview images are placeholders (need screenshots)
- Camera capture flow not fully implemented yet
- Voice transcription integration pending

### Compatibility

- **Minimum SDK:** 31 (Android 12)
- **Target SDK:** 35
- **Glance Version:** 1.1.1
- **WorkManager Version:** 2.9.0

---

## ✅ Checklist for Production

Before releasing widgets:

- [ ] Test on all supported Android versions
- [ ] Create proper widget preview screenshots
- [ ] Test with different launchers
- [ ] Verify all three capture modes work
- [ ] Ensure proper permission handling
- [ ] Test in low battery mode
- [ ] Verify Material You theming
- [ ] Update app screenshots in Play Store
- [ ] Document widget features in app listing
- [ ] Test widget resize behavior
- [ ] Verify accessibility with TalkBack

---

**Built with ❤️ using Jetpack Glance and Material You**

*Last updated: 2025-11-24*
