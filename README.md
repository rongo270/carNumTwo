# 🚀 Space Chickens - Android Arcade Game

A classic "Chicken Invaders" style arcade shooter, built with Kotlin for Android.
This project features a custom grid-based game engine, real-time weapon upgrades, sensor-based controls (Tilt), and an advanced audio management system.


## 🎮 Key Features

### 🕹️ Gameplay
* **Dynamic Grid Engine:** Smooth movement logic on a custom-built grid system.
* **Weapon Upgrade System:** Collecting coins upgrades the spaceship's firepower in real-time (Double shot, Triple shot, etc.).
* **Visual Cooldown Mechanic:** Fire button features a "filling energy" animation (ClipDrawable) to manage fire rate strategy.
* **Linear Difficulty Scaling:** Game speed accelerates smoothly and linearly as the score increases, ensuring a balanced challenge.
* **Penalty System:** Losing a life results in a coin penalty and weapon downgrade.

### ⚙️ Settings & Customization
* **Dual Control Modes:** Toggle between on-screen **Touch Buttons** and **Accelerometer (Tilt)** controls.
* **Advanced Audio Mixer:** Separate volume sliders (0-100%) for Background Music and Sound Effects (SFX).
* **Grid Customization:** Players can adjust the grid size (rows/columns) via settings.
* **Localization:** Full support for English and Hebrew.

### 🎨 UI/UX Design
* **Glassmorphism Style:** Custom "Pause" and "Game Over" dialogs with semi-transparent backgrounds.
* **Animations:** Smooth button interactions and damage feedback.
* **Assets:** Support for both Vector Drawables and high-quality PNGs.

---


## 🛠️ Tech Stack

* **Language:** Kotlin
* **Architecture:** MVC (Model-View-Controller) adapted for a custom game loop.
* **UI Components:** Custom Views, Dialogs, SeekBars, ValueAnimator, ClipDrawable.
* **Data Storage:** SharedPreferences (Persisting settings, volume levels, and control preferences).
* **Hardware Integration:** `SensorManager` (Accelerometer/Tilt control).
* **Audio Engine:** `SoundPool` (Low latency SFX) + `MediaPlayer` (Background Music) with custom audio mixing logic.

---

## 🚀 Getting Started

1.  Clone the repository:
    ```bash
    git clone https://github.com/rongo270/carNumTwo.git
    ```
2.  Open the project in **Android Studio**.
3.  Wait for the Gradle sync to complete.
4.  Run on an emulator or a physical device (Physical device recommended for testing Tilt controls).

---

## 📂 Project Structure

* `core/`: Infrastructure classes (Audio Manager, Storage, Configuration, UI Base).
* `feature/game/engine/`: The core game logic (GameLoop, Controller, Collision Detection).
* `feature/game/model/`: Data models (GameState, Chicken, Bullet, Coin).
* `feature/game/render/`: Handles drawing the grid and sprites.
* `feature/menu/` & `settings/`: UI Activities for menus and configuration.

---

## 👨‍💻 Author

Built by RonGo
Created as an advanced Kotlin Android development course focusing on game logic, sensors, and custom UI.

---

## 📸 Screenshots

---

<img width="564" height="853" alt="image" src="https://github.com/user-attachments/assets/c33e418d-2d40-44f9-a300-08251d1c1115" />
<img width="550" height="913" alt="image" src="https://github.com/user-attachments/assets/2efa25c0-a707-4ddf-a2cb-a22058249f28" />
<img width="516" height="941" alt="image" src="https://github.com/user-attachments/assets/df1b5b0c-bc89-4c53-a957-cbba3572898b" />
<img width="560" height="972" alt="image" src="https://github.com/user-attachments/assets/6c7d8b2a-0f96-44ba-a933-f13ded20fbb4" />

