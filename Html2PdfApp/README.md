# Html2PdfApp

Android app that converts HTML to a high-quality PDF on-device and lets you share/save instantly. Optional server-side converter included.

Features
- Paste or open HTML file via system picker
- Faithful rendering via WebView, selectable text PDF
- Single-tap Share; Save via system document picker
- Offline on-device conversion; optional Puppeteer server
- FileProvider-secured sharing; input size limits

Build (Android Studio)
- Open `Html2PdfApp/` in Android Studio Iguana+/latest
- Build > Make Project, then Run or Build > Generate Signed APK

Build (CLI)
- If you have system Gradle 8.7+: `gradle :app:assembleDebug`
- Or use the wrapper if present: `./gradlew :app:assembleDebug`

Usage
1. Paste HTML or tap Open HTML to select a local file
2. Preview renders below
3. Tap Share to generate+share, Save to export via system picker, or Convert to open a PDF viewer

Limits & privacy
- Max HTML: 1MB (~200k chars). PDFs are generated in app cache and shared via `FileProvider` with temporary read permission.
- No data leaves the device unless you use the optional server or share a file.

Optional server
- See `../html2pdf-server/` for a hardened Puppeteer service providing `/convert` and temporary download links.

Tests
- UI: basic Espresso test for control presence
- Unit: simple limit check

Samples
- `app/src/main/assets/samples/sample1.html`