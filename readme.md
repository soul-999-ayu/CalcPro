# Calculator Vault (CalcPro) 🔒

A fully functional **Scientific Calculator** that doubles as a secure, AES-encrypted **Photo & Video Vault**.

Designed with **privacy-first architecture**, this app disguises itself as a standard Android calculator using **Material You (Dynamic Colors)**. It features advanced security measures like "Ghost Mode" (hidden from Recents) and screenshot prevention.

## ✨ Key Features

### 🛡️ Security & Privacy
* **Stealth Mode:** App appears as a fully working Calculator in the launcher and system settings.
* **Ghost Mode:** The Vault activity is excluded from the "Recent Apps" list. If the user minimizes the app or presses Home, the Vault self-destructs instantly, leaving only the Calculator visible in the background.
* **AES Encryption:** All hidden files are encrypted. They are renamed and moved to internal private storage, making them inaccessible to other apps or file managers.
* **Screenshot Block:** `FLAG_SECURE` is enabled to prevent screen recording or screenshots within the Vault.
* **Lifecycle Protection:** The app detects `onUserLeaveHint` to instantly hide sensitive content if the user attempts to switch apps.

### 🔢 Calculator Functionality
* **Scientific Operations:** Support for sin, cos, tan, log, ln, square root, power, and factorial.
* **History Tape:** Scrollable calculation history (saved locally).
* **Smart Logic:** Parenthesis handling and percentage logic.
* **Expandable UI:** Smooth animation to reveal scientific functions.

### 🖼️ Custom Media Gallery
* **Secure Viewer:** Built-in secure gallery (no external apps needed).
* **Smart Zoom:** Custom-built `ZoomImageView` engine supporting smooth pinch-to-zoom, double-tap, and fling gestures.
* **Pro Video Player:** Custom video player with overlay controls, seek bar, and landscape toggle.
* **Immersive Mode:** UI controls (Share/Delete/Unhide) fade away automatically for a distraction-free viewing experience.

### 🎨 UI/UX
* **Material You:** Fully supports Android 12+ Dynamic Colors. The UI adapts to the user's system wallpaper.
* **System Theme Support:** Automatically switches between Light and Dark modes.
* **Edge-to-Edge:** Layouts implemented with `fitsSystemWindows` for a modern look.

---

## 🚀 How to Use

1.  **Launch the App:** Open "Calculator". It functions exactly like a normal calculator.
2.  **Unlock the Vault:**
    * Enter your numeric password (Default is `1234`).
    * Press the **Equals (=)** button.
    * The calculator interface will slide away to reveal the hidden vault.
3.  **Change Password (Secret Menu):**
    * To access Settings, type `+0+0` on the calculator display.
    * Press **Equals (=)**.
    * The secret Settings page will open where you can update your password.
4.  **Hide Files:** Click the `+` button inside the vault to select photos or videos from your device.
5.  **Unhide:** Open a file in the vault and click the "Eye" icon to restore it to your Downloads folder.

---

## 🛠️ Technical Stack

* **Language:** Kotlin
* **Minimum SDK:** Android 10 (API 29)
* **Target SDK:** Android 14 (API 34)
* **Architecture:** MVVM (Model-View-ViewModel) pattern concepts.
* **Encryption:** AES (Advanced Encryption Standard).
* **Components:**
    * `androidx.appcompat` & `material` (Material Design 3).
    * `ConstraintLayout` & `GridLayout`.
    * `Coroutines` (for background encryption tasks).
    * `Coil` (for efficient image loading).
    * `ViewBinding`.

---

## 🔧 Installation

1.  Clone the repository:
    ```bash
    git clone https://github.com/soul-999-ayu/CalcPro.git
    ```
2.  Open the project in **Android Studio**.
3.  Sync Gradle files.
4.  Connect an Android device or start an emulator.
5.  Run the app (`Shift + F10`).

---

## 📝 License

This project is open-source and available under the [MIT License](LICENSE).

---

**Made with ❤️ by Ayu Kashyap**