# NoteDrop 📝

**Privacy-first Note Capture for Android 12+**

NoteDrop is a modern, beautiful note-taking app built with Material You design, featuring quick capture, voice recording, and seamless Obsidian integration.

![Android 12+](https://img.shields.io/badge/Android-12%2B-green.svg)
![Material You](https://img.shields.io/badge/Material-You-blue.svg)
![Kotlin](https://img.shields.io/badge/Kotlin-100%25-purple.svg)

---

## ✨ Features

### 🎨 **Material You Design**
- Dynamic colors that match your wallpaper
- Beautiful edge-to-edge experience
- Professional splash screen
- Dark/Light mode support

### ⚡ **Quick Capture**
- Capture notes in under 2 seconds
- Template system (Quick Capture, Daily Note, Meeting Note)
- Tag support for organization
- Title and content fields
- **Home Screen Widgets** - Quick capture from your home screen 🆕

### 🎤 **Voice Recording**
- Record voice notes with one tap
- Attach recordings to notes
- Pause/Resume recording support (Android 24+)
- Automatic transcription ready (future)

### 📁 **Obsidian Integration**
- Save notes directly to Obsidian vaults
- Markdown formatting with front-matter
- Daily notes support
- Tag synchronization
- Custom templates

### 🏠 **Home Screen**
- View all notes in one place
- "Today's Notes" quick section
- Powerful search functionality
- Filter by: All, Today, Voice, Tagged
- Delete notes with confirmation

### ⚙️ **Settings & Vaults**
- Multiple vault support
- Set default vault
- Provider configuration (Obsidian, Local, Notion*, Custom*)
- Vault management UI
- About section

*\*Future implementation*

---

## 🏗️ Architecture

NoteDrop follows **Clean Architecture** principles:

```
┌─────────────────────────────────────┐
│         Presentation Layer          │
│  (Compose UI + ViewModels + Nav)    │
├─────────────────────────────────────┤
│          Domain Layer               │
│  (Models + Repositories + UseCases) │
├─────────────────────────────────────┤
│           Data Layer                │
│   (Room DB + Providers + DAOs)      │
└─────────────────────────────────────┘
```

### Tech Stack

- **UI**: Jetpack Compose + Material 3
- **DI**: Hilt
- **Database**: Room
- **Async**: Kotlin Coroutines + Flow
- **Navigation**: Navigation Compose
- **Architecture**: MVVM + Clean Architecture
- **Widgets**: Glance API (ready for implementation)

---

## 📱 Screenshots

*Coming soon*

---

## 🚀 Getting Started

### Prerequisites

- Android Studio Hedgehog | 2023.1.1 or later
- Android device/emulator running Android 12 (API 31) or higher
- Kotlin 2.0.21

### Build & Run

1. **Clone the repository**
   ```bash
   git clone https://github.com/yourusername/notedrop.git
   cd notedrop
   ```

2. **Open in Android Studio**
   - Open Android Studio
   - Select "Open an Existing Project"
   - Navigate to the NoteDrop directory

3. **Sync Gradle**
   - Wait for Gradle to sync
   - All dependencies will be downloaded automatically

4. **Run the app**
   - Connect an Android 12+ device or start an emulator
   - Click Run ▶️

---

## 📖 User Guide

### First Launch

1. **Create a Vault**
   - Open Settings (gear icon)
   - Tap "+ New Vault"
   - Enter vault name
   - Select provider (Obsidian recommended)
   - Enter vault path (e.g., `/storage/emulated/0/Documents/ObsidianVault`)
   - Check "Set as default"
   - Tap "Create"

### Quick Capture

1. **From Home Screen**
   - Tap the floating "+" button
   - Enter your note content
   - Optionally add title and tags
   - Tap ✓ to save

2. **With Templates**
   - On Quick Capture screen
   - Select a template (Quick Capture, Daily Note, Meeting)
   - Template expands with variables filled
   - Edit and save

3. **With Voice Recording**
   - On Quick Capture screen
   - Tap the microphone FAB
   - Record your message
   - Tap stop when done
   - Voice recording attaches to note
   - Save the note

### Viewing Notes

- **All Notes**: Default view shows all notes
- **Today's Notes**: Highlighted section at top
- **Search**: Use search bar to find notes
- **Filter**: Tap filter chips (All, Today, Voice, Tagged)
- **Delete**: Tap delete icon on note card

### Obsidian Integration

Your notes are automatically saved to your Obsidian vault as Markdown files:

```markdown
---
created: 2025-11-23T10:30:00Z
updated: 2025-11-23T10:30:00Z
tags:
  - important
  - work
---

# Meeting Notes

Discussion points from today's meeting...

#important #work
```

---

## 🎯 Permissions

NoteDrop requires the following permissions:

- **RECORD_AUDIO**: For voice note recording
- **READ/WRITE_EXTERNAL_STORAGE** (Android 12 only): For Obsidian vault access

All permissions are requested at runtime when needed.

---

## 🔐 Privacy & Security

### Privacy-First Design

- ✅ **Local-First**: All data stored on your device
- ✅ **No Analytics**: Zero tracking or telemetry
- ✅ **No Cloud Required**: Works 100% offline
- ✅ **Your Data**: Full control of your notes
- ✅ **Open Source**: Transparent codebase

### Data Storage

- Notes: Room SQLite database (local)
- Voice recordings: App private storage
- Obsidian sync: Direct file writes (your vault)

### GDPR Compliant

- No personal data collection
- No third-party services
- Complete data portability
- Full delete capability

---

## 🗺️ Roadmap

### Phase 1: MVP (Completed ✅)
- [x] Material You theming
- [x] Quick Capture UI
- [x] Voice recording
- [x] Obsidian provider
- [x] Settings & Vault management
- [x] Home screen with notes display
- [x] Search & filtering

### Phase 2: Enhanced Features (Next)
- [x] Home screen widget (Glance) ✅
- [ ] Voice transcription (Whisper model)
- [ ] Note editing screen
- [ ] Rich text editor
- [ ] Image attachments
- [ ] Export/Import functionality

### Phase 3: Multi-Platform (Future)
- [ ] Wear OS companion app
- [ ] Android TV version
- [ ] Android Auto integration
- [ ] Multi-device sync (P2P)

### Phase 4: Advanced Features
- [ ] Notion provider
- [ ] Custom providers SDK
- [ ] Advanced search
- [ ] Note linking
- [ ] Graph view
- [ ] Encryption option

---

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the project
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

---

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

---

## 🙏 Acknowledgments

- **Obsidian**: Inspiration for local-first knowledge management
- **Material You**: Beautiful adaptive design system
- **Jetpack Compose**: Modern Android UI toolkit
- **Android Community**: Endless learning resources

---

## 📞 Support

- **Issues**: [GitHub Issues](https://github.com/yourusername/notedrop/issues)
- **Discussions**: [GitHub Discussions](https://github.com/yourusername/notedrop/discussions)
- **Documentation**: See [PROJECT_STRUCTURE.md](PROJECT_STRUCTURE.md)

---

## 🌟 Show Your Support

If you like NoteDrop, please:
- ⭐ Star this repository
- 🐛 Report bugs
- 💡 Suggest features
- 📣 Share with friends

---

**Built with ❤️ for the privacy-conscious note-taker**

*NoteDrop - Your thoughts, your device, always private.*
