# NH Tiles & Marble Room Designer (v3.5.0 Luxury Edition)
**Package:** `com.nh.tilesmarbledesigner` · **Branding:** NH Developer · **100% offline** (the manifest declares no INTERNET permission)

Kotlin · Jetpack Compose · Material 3 · MVVM + Repository · Room · DataStore · Photo Picker · Android PdfDocument · custom Canvas perspective renderer.

## Build & install
1. Install Android Studio (Koala or newer) with Android SDK 34 and JDK 17 (bundled).
2. `File > Open` and choose this folder. Let Gradle sync (needs internet **once** to download Gradle 8.7 and the dependencies; the app itself never uses the network).
3. If Android Studio asks to create/update the Gradle wrapper, accept. (The wrapper JAR could not be bundled; `gradle/wrapper/gradle-wrapper.properties` pins Gradle 8.7.)
4. Run on a device/emulator (API 26+), or `Build > Build APK(s)`.
   Command line (after the wrapper exists): `./gradlew assembleDebug` -> `app/build/outputs/apk/debug/`.
Release builds are signed with the debug key so they install directly; use your own keystore for Play Store.

## Architecture
| Package | Contents |
|---|---|
| `data` | `Models.kt` (project model + JSON), `Db.kt` (Room entities/DAOs/Repository), `SettingsStore.kt` (DataStore) |
| `engine` | `Calc` (tile/box/slab maths + validation), `Materials` (offline procedural textures), `PatternComposer` (straight/diagonal/brick/herringbone/grid/slab + grout), `RoomRenderer` + `Scene` (perspective mesh renderer, lighting, shadows), `CropEngine` (import, auto-detect, perspective warp), `Exporter` (PNG, PDF, share) |
| `ui` | Theme, components, `AppViewModel`, 18 screens in `Screens1-5.kt` |

## What is new in v3.5.0 (Next-Level Luxury Edition)
* **Floating Luxury Pill Navigation Dock (`LuxuryBottomDock`)**: Sculpted rounded 28 dp floating dock with multi-layered glow shadow, gradient surface (`cardTop` → `card` → `chip`), luminous gradient border, spring-animated active capsules (`dark` → `primary` → `subAccent`), top glowing spark bar (`vivid`), and highlighted center **Studio** tab.
* **Multi-Depth Shadow & Elevation System**: Upgraded `Modifier.softShadow(elevation, shape, glow)` with deeper chromatic ambient & spot shadows plus optional luminous glow for hero cards, 3D canvases, active chips, buttons, and navigation bars.
* **Sculpted Top Header Bar & Gradient Cards**: `ScreenFrame` features an elevated rounded header bar with a glowing jewel bar, gradient back button, and dual-tone split headline (`accent` + `subAccent`). `NHCard` uses a vertical sheen gradient (`cardTop` → `card`) with a multi-stop luminous gradient border and 11 dp shadow.
* **Multi-Color Eye-Catching Typography**: Distinct, high-contrast color roles across every screen — `accent` for primary titles, `subAccent` for contrasting secondary headlines/subtitles/tags, `metric` for vivid eye-catching numbers/areas/box counts, `vivid` for active highlights, and `ink` for crisp body text.
* **8 Luxury Palettes**: Upgraded all 7 themes (**Emerald & Amber**, **Royal Amethyst**, **Sapphire Lagoon**, **Saffron & Ruby**, **Velvet Rose & Gold**, **Platinum Indigo**, **Midnight Gold**) and added an 8th dark flagship theme: **Obsidian Neon** (`royal`).
* **Enhanced Layouts Across All 18 Screens**: Categorized Home Bento grid with a 200 dp interactive 3D Hero Showcase Card, Executive Summary banners in Calculation Results, and a quick Floor/Wall Studio switcher bar inside the Design Studio.
