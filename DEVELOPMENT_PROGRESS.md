# RoohuX — Development Progress (v0.2.0)

**Read this first:** the authoring environment had no Gradle, Kotlin compiler, Android SDK, emulator or network.
**Nothing in this project has been compiled, installed or run by me.** What I did run: XML/YAML well-formedness checks
and a script that checks brace balance in all Kotlin files and that project-internal imports resolve. That is a weak check;
expect the first GitHub Actions run to surface some compile errors (send me the log and I'll fix them).
Where this document says "Written", it means exactly that: code exists, behaviour is unverified.

## Build
Push to GitHub → Actions → **Build debug APK** → artifact `RoohuX-debug-apk`. The workflow assembles the debug APK, verifies it
exists/unzips/has package `com.roohu.roohux`, uploads it (fails if missing), then runs unit tests (so a failing test can't hide the APK).
No Gradle wrapper jar is committed; the workflow installs Gradle 8.9. JDK 17, AGP 8.7.3, Kotlin 2.0.21, compileSdk/targetSdk 35, minSdk 29.

## Priority 1 — fixes made to the Phase 1–2 code (all by code review; none verified by running)
- HTTP 416: if the server says the file is exactly what we already have, publish it instead of restarting; otherwise discard the partial and retry clean.
- HTTP 200 to a Range request (server ignored it): partial discarded, download restarts from byte 0 (no appended/corrupt file).
- HTTP 206: `Content-Range` start must equal our offset, else restart.
- `If-Range` (ETag / Last-Modified) sent on resume so a changed remote file can't be spliced into a stale partial.
- Size integrity check before publishing (mismatch → INTEGRITY error, partial removed so Retry is clean).
- Duplicate prevention: the same URL can't be queued twice while queued/running/paused. Duplicate filenames are handled by MediaStore ("name (1).ext").
- Cookies are sent per-host via a read-only WebView cookie jar (correct across redirects; nothing stored or logged).
- Android 15 `dataSync` foreground-service timeout handled: downloads pause cleanly and can be resumed.
- Foreground-service start failures no longer crash the service; Android 13+ notification permission requested on first download.
- `configChanges` on the activity so rotation (fullscreen video/player) doesn't recreate the UI.

## Implemented (written, unverified)
| Area | What exists |
|---|---|
| Downloads | Queue, N simultaneous, progress/speed/ETA, pause/resume (Range only), retry with backoff, cancel, delete, open/share/locate, notifications, MediaStore `Downloads/RoohuX`, plain-language errors |
| Browser | Multiple tabs (kept alive across app sections), address bar with URL-vs-search detection (DuckDuckGo/Google/Bing), back/forward/reload/stop, progress bar, desktop/mobile mode per tab, bookmarks, history (+delete/clear), find in page, share/copy/open-externally, fullscreen HTML5 video, user-initiated popups as tabs (script popups blocked), safe external-intent handling, error screens (offline/DNS/TLS/server), TLS errors never bypassed, website download callbacks → download engine (with referer/UA/cookies) |
| Media detection | Observes request URLs by extension + inspects `<video>/<audio>/<source>` after load; dismissible "Download available" chip (top-right, away from video controls); sheet with name, type, size (read from the server via HEAD/ranged GET, "unknown" if not available — resolutions are never invented); Download and Play for direct files; HLS/DASH and blob streams clearly labelled as not downloadable; DRM (EME) pages flagged |
| Player | Media3 ExoPlayer in a MediaSessionService (background audio, notification, lock-screen/Bluetooth controls); PlayerView controls (seek, speed, audio track, subtitles); landscape/fullscreen toggle; HLS/DASH playback modules included; hologram visual for audio-only; error message on failure |
| Library | Completed audio/video downloads + files added via system picker; search, sort (date/name/size), filter (video/audio/favourites), favourites, play, share, delete (downloads) / remove (imported files, never deleted) with confirmation |
| UI | Midnight/violet/cyan theme (dark/light/system), glass cards with spring press, animated hologram (Canvas rings + particles), page transitions, animated progress, pulsing loading dots, **Reduce Motion** (setting + system animation scale) |
| Settings | Theme, Reduce Motion, search engine, Wi-Fi only, simultaneous downloads, clear history/cookies/cache, About |
| Build | Gradle KTS, manifest, adaptive icon, splash screen, GitHub Actions workflow, unit tests |

## Actually tested
- **Not compiled, not installed, not run.** No emulator/device. No download, browser or playback test has been performed.
- Run by me: XML + workflow YAML parse; brace-balance and internal-import scan of the Kotlin sources.
- Unit tests written (not executed): URL normalising/resolution, filename sanitising, Content-Disposition, Content-Range, media classification.

## Untested (everything runtime) — use the checklist
Downloads: small/large file, pause→resume byte continuity, no-Range server, 403/404, airplane-mode retry, redirect, odd filenames, `.m3u8`, HTML page, Wi-Fi-only, queue limit, kill-and-reopen.
Browser: load/search, tabs survive switching sections, back/forward, desktop mode, fullscreen video, a site download, popup behaviour, TLS error page, find, bookmarks/history.
Detection: MP4/MP3 page → chip and sheet, size shown, download works; HLS page → labelled unsupported; blob/DRM page.
Player: local MP4/MP3, remote MP4/MP3, HLS, seek, speed, background audio + notification, rotate.
Library: downloaded items appear, add from device, favourites, delete/remove.
UI: small/large phones, long filenames, Reduce Motion, TalkBack basics, Android 13/14/15.

## Known limitations
- Not implemented: segmented/parallel downloads, checksum verification, HLS/DASH **downloading** (Media3 offline), cloud providers (Google Drive/TeraBox), playlists, playback-position memory, brightness/volume/seek gestures, Picture-in-Picture, bookmark edit/import/export/search, per-site permissions UI, accent colours, parallax cards, configurable download folder, tab persistence across app restarts.
- Website file-upload dialogs, camera/microphone/location are not supported (denied).
- Media detection only sees what WebView exposes: `shouldInterceptRequest` observes requests but cannot see everything; media-element scan is best-effort and runs 3 times after page load.
- Download Library shows only RoohuX downloads plus picker-added files (no broad storage scan, by design: no storage permission).
- Finished downloads are copied from private storage to MediaStore (briefly needs space for both).
- Unknown-length (chunked) downloads can't be size-verified or paused.
- Playing remote media doesn't send cookies/referer, so hotlink-protected files may fail to play.
- Audio keeps playing in the background; video pauses when leaving the player.
- English-only strings. Cleartext HTTP allowed (spec asks for HTTP downloads); typed bare hosts are upgraded to HTTPS.
- `allowBackup` is off, so app data isn't restored on a new phone.
