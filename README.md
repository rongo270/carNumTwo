# 🚀 Space Chickens - Android Arcade Game

A classic "Chicken Invaders" style arcade shooter, built with Kotlin for Android.
This project features a custom grid-based game engine, real-time weapon upgrades, sensor-based controls (Tilt), location-based high scores, and an advanced audio management system.


## 🎮 Key Features

### 🕹️ Gameplay
* **Dynamic Grid Engine:** Smooth movement logic on a custom-built grid system.
* **Weapon Upgrade System:** Collecting coins upgrades the spaceship's firepower in real-time (Double shot, Triple shot, etc.).
* **Visual Cooldown Mechanic:** Fire button features a "filling energy" animation (ClipDrawable) to manage fire rate strategy.
* **Linear Difficulty Scaling:** Game speed accelerates smoothly and linearly as the score increases, ensuring a balanced challenge.
* **Penalty System:** Losing a life results in a coin penalty and weapon downgrade.

### 🌍 Location & High Scores
* **Global Leaderboard:** Tracks the top 10 high scores locally.
* **Location Integration:** Players can save their high scores with their real-world GPS location.
* **Interactive Map:** View high scores on an integrated Google Map, showing exactly where each commander achieved their victory.
* **Map Picker:** Option to manually select a location on the map when saving a new high score.

### ⚙️ Settings & Customization
* **Dual Control Modes:** Toggle between on-screen **Touch Buttons** and **Accelerometer (Tilt)** controls.
* **Advanced Audio Mixer:** Separate volume sliders (0-100%) for Background Music and Sound Effects (SFX).
* **Grid Customization:** Players can adjust the grid size (rows/columns) via settings.
* **Localization:** Full support for English and Hebrew.

### 🎨 UI/UX Design
* **Glassmorphism Style:** Custom "Pause", "Game Over", and "High Score" dialogs with semi-transparent backgrounds.
* **Animations:** Smooth button interactions, cooldown effects, and damage feedback.
* **Assets:** Support for both Vector Drawables and high-quality PNGs.

---

## 🛠️ Tech Stack

* **Language:** Kotlin
* **Architecture:** MVC (Model-View-Controller) adapted for a custom game loop.
* **UI Components:** Custom Views, Dialogs, SeekBars, ValueAnimator, ClipDrawable, Fragments.
* **Data Storage:**
    * `SharedPreferences`: Persisting settings, volume levels, and control preferences.
    * `Gson`: Serializing and storing the top 10 high scores list.
* **Hardware Integration:**
    * `SensorManager`: Accelerometer/Tilt control.
    * `LocationServices (FusedLocationProviderClient)`: Retrieving precise GPS coordinates.
* **Google Maps SDK:** Integrating interactive maps for the high score display and location picker.
* **Audio Engine:** `SoundPool` (Low latency SFX) + `MediaPlayer` (Background Music) with custom audio mixing logic.

---

## 🚀 Getting Started

1.  Clone the repository:
    ```bash
    git clone [https://github.com/rongo270/carNumTwo.git](https://github.com/rongo270/carNumTwo.git)
    ```
2.  Open the project in **Android Studio**.
3.  **Important:** Add your Google Maps API Key to `local.properties`:
    ```properties
    MAPS_API_KEY=your_api_key_here
    ```
4.  Wait for the Gradle sync to complete.
5.  Run on an emulator or a physical device (Physical device recommended for testing Tilt controls).

---

## 📂 Project Structure

* `core/`: Infrastructure classes (Audio Manager, Storage, Configuration, UI Base).
* `feature/game/engine/`: The core game logic (GameLoop, Controller, Collision Detection).
* `feature/game/model/`: Data models (GameState, Chicken, Bullet, Coin).
* `feature/game/render/`: Handles drawing the grid and sprites.
* `feature/score/`: High score logic, list adapter, and Google Maps integration fragments.
* `feature/menu/` & `settings/`: UI Activities for menus and configuration.

---

## 👨‍💻 Author

Built by RonGo
Created as an advanced Kotlin Android development course focusing on game logic, sensors, location services, and custom UI.

---

## 📸 Screenshots

<p float="left">
  <img src="https://github.com/user-attachments/assets/c33e418d-2d40-44f9-a300-08251d1c1115" width="200" />
  <img src="https://github.com/user-attachments/assets/2efa25c0-a707-4ddf-a2cb-a22058249f28" width="200" />
  <img src="https://github.com/user-attachments/assets/df1b5b0c-bc89-4c53-a957-cbba3572898b" width="200" />
  <img src="https://github.com/user-attachments/assets/6c7d8b2a-0f96-44ba-a933-f13ded20fbb4" width="200" />
  <img width="720" height="1640" alt="43" src="https://github.com/user-attachments/assets/8a34e3db-130e-447b-b5d8-0f6755da1d05" />
</p>
