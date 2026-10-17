# david.io privacy policy

david.io is a Slither-compatible game client for Android. It has no accounts, no sign-in, no advertising, no analytics and no server of its own that collects anything about you. This page says exactly what stays on your phone and what leaves it.

---

## What stays on your phone

- Your in-game name, skins, accessories, tag and arena background choices.
- Your settings, control layouts and on-screen button layouts.
- Your best score, kill count and other local statistics.
- Saved arenas and recently played arenas.
- Saved teams. A Team ID and Auth Key are encrypted with a key held in the Android Keystore and are never written to a backup file.

None of this is uploaded anywhere by david.io.

## What leaves your phone

Playing needs the network, and the services you reach can see your IP address.

- **Slither arenas.** When you press Play, the app connects to the arena you picked. That arena receives the name, skin and controls your snake needs to play, exactly as the original game does.
- **Arena list.** The app downloads the public arena list from slither.io (`https://slither.io/i80124.txt`) and measures how fast each arena answers.
- **Country flags.** Arena addresses (not your personal data) are sent to `ntl-slither.com` to look up the country of each arena, and flag pictures are loaded from `flagcdn.com`.
- **NTL tag network: OFF by default.** The code for it is still in the app, but it is switched off at build time, so the game does not open a connection to `ws.ntl-slither.com` and other players' NTL tags are not shown. A build that turns it on (`DVD_NTL_TAG_NETWORK=1`) sends your in-game name, the arena you are in, your snake's id and skin and your position about once a second over an unencrypted `ws://` connection to NTL's server; that build must say so here.
- **Team mode (NTL team).** If you add an NTL team, the app sends the Team ID, Auth Key, your name, score, position and arena to NTL's team service every few seconds, together with any team chat message you type.
- **Team mode (your own team server).** If you set a team server address and use a `DVD-` team, the same information goes to that server instead, and only to that server. Whoever runs it can read it, so only join teams on servers you trust.

Nothing else is sent. There is no crash upload, no usage statistics and no update check.

## Permissions

- **Internet and network state:** to play and to reach the services above.
- **Notifications and foreground service:** to keep a game running in the background and show its notification.
- **Vibration:** for haptic feedback, if you turn it on.

## Children

david.io does not ask for or collect personal information. Arena chat and team chat are written by other players, not by david.io.

## Third parties

Slither.io, NTL and their names and artwork belong to their owners. david.io is not affiliated with or endorsed by them.

## Changes

If this policy changes, the new version ships with the app and is listed in `CHANGES.txt`.
