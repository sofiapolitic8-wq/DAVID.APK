# david.io for Android

A simple Slither-compatible snake game for Android. A native C engine draws the
arena with Vulkan; Jetpack Compose draws the menus around it. There are no
accounts, no sign-in and no developer backend: everything is stored on the phone.

## What is in the app

- **Play** on any normal Slither arena (protocol 19), with an arena picker that
  lists the public servers and recent / saved custom arenas.
- **Play with AI**: offline play against local bots, and an NTL-compatible assist bot.
- **Skin Studio**: skins, accessories and arena backgrounds.
- **Controls**: joystick or arrow steering, boost button, on-screen buttons and a
  draggable HUD layout editor.
- **Settings**: display, food, performance, bot and assist options, themes.
- **Team mode**: compatible with NTL Team ID / Auth Keys (roster, chat, minimap
  dots), plus your own `DVD-` teams from `server/server.js`.

Not included: accounts, social, chat threads, trails, voice, notifications,
profiles, support, backups to an account, auto-updates. See `PRIVACY.md`.

## Your own team server

```text
node server/server.js            # port 8787, no dependencies
cloudflared tunnel --url http://localhost:8787   # friends need https
```

In the app: **Play -> Team -> Add a team -> Create your own team**. Paste the
server address, choose a name and player limit, press *Save server and generate
team*, then *Copy invite*. A friend pastes the invite (`ID key https://server`)
into the Team ID box and presses Connect. See `server/README.txt`.

## Building

Requirements: JDK 17, Android SDK 36, NDK `28.2.13676358`, CMake `3.22.1`,
`slangc` (Slang), and an ARM64 Android 8.0+ device with Vulkan.

1. **Textures.** The game art is not in this repository, and the engine will not
   start without it. Put it in `app/res/textures/`:
   `python3 tools/install-textures.py --from /path/to/your/textures`
   and check with `python3 tools/install-textures.py --check`.
2. **Shaders:** `python3 build.py 2`
3. **APK:** `cd android && ./gradlew :app:assembleDebug -PDVD_VERSION_CODE=1 -PDVD_VERSION_NAME=1.0.0`

GitHub Actions does all of this on every push (`.github/workflows/build-apk.yml`)
and uploads `davidio-debug-apk`. Debug APKs are enough to install and test.

The first native build compiles the whole engine and takes several minutes.
Release signing reads `release-signing/keystore.properties`, which is private
and must never be committed.

## Layout

| Path | Contents |
|---|---|
| `app/src/` | C engine: gameplay, Slither protocol, renderer glue, touch controls, JNI |
| `app/res/` | Fonts, shader sources and (you supply) textures |
| `android/` | Gradle project; Kotlin/Java in `android/app/src/main/java/io/david/mobile/` |
| `server/` | The david.io team server and the reference extension scripts |
| `android-sdl/`, `glfw/`, `thermite/` | Vendored SDL3, GLFW and the Vulkan framework |
| `tests/`, `tools/`, `scripts/` | Protocol tests and asset / build tools |

## License

GNU GPL v3; see `LICENSE`, `NOTICE` (credits and third-party licences) and
`CHANGES.txt` (what was modified). If you distribute an APK you must offer the
matching source.
