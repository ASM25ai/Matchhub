# MatchHub Android v3.0 (prototype)

## Features
- Today screen: Premier League, La Liga, MLS and UEFA Champions League fixture cards, status and live scores. Filters: all, live and favorite teams.
- Team favorites and kickoff reminders from v2. Schedule synchronization uses Android JobScheduler and non-exact alarms. Requires notification permission on Android 13+.
- Match detail with score, status, and best-effort match events and statistics from ESPN JSON endpoints. Coverage depends on fixture.
- Browse Footy100 or Yalla Shoot from match detail; **does not resolve direct match stream URLs**.
- Fullscreen web video with a manual PiP button on Android 8+, if an in-app playable video is available. Third-party restrictions may prevent playback or PiP.

## Build debug APK (GitHub)
1. Upload **contents** of this project folder (including `.github`) to a GitHub repository.
2. Open Actions > Build Android APK > Run workflow.
3. Download `MatchHub-debug-apk` from run artifacts, unzip and install `app-debug.apk`.
4. Build requires internet; source has **not** been compiled or tested on a device in this environment.

## Known limitations
- Uses ESPN's **unofficial, undocumented** endpoints; subject to breakage/rate limits and missing stats. Not affiliated with ESPN.
- Scoreboard is refreshed on demand, not continuously. Reminder times may be delayed by OS power management.
- Stream sites can change, block WebViews or links, redirect externally, and may contain unauthorized broadcasts. No guarantee of English audio. Do not enter credentials or payment information into untrusted sites.
- Debug APK is intended for personal testing; signing/security hardening, accessibility, device testing and a licensed stream-provider integration are recommended before release.
