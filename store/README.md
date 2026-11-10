# Publishing on Google Play and F-Droid

Everything the store listing needs, ready to paste or upload, and the Play Console steps.

| What | Where |
|---|---|
| App Bundle to upload | `androidApp/build/outputs/bundle/release/androidApp-release.aab` (build it as below) |
| Title, short and full description, English and Italian | `fastlane/metadata/android/{en-US,it-IT}/*.txt` |
| Icon 512×512, feature graphic 1024×500, 6 phone screenshots | `fastlane/metadata/android/{en-US,it-IT}/images/` |
| Privacy policy | https://www.michaelmaguire.ca/dialett-da-rogn/privacy/ (source: `dialett-da-rogn/privacy/index.html` in michaelmaguire-ca-website); also linked from the app's start screen. `PRIVACY.md` here is the same text |
| How the graphics were made | `store/artwork/`: `icon.html` and `feature.html` over the flag `arogno-flag.svg` (public domain: Gastone Cambin, *Armoriale dei Comuni Ticinesi*, 1953, via Wikimedia Commons) |

The fastlane layout lets the listing be uploaded with `fastlane supply` later; for now it's pasted by hand.

## Build the App Bundle

Signed with the upload key named in `~/.gradle/gradle.properties` (`dialett.release.storeFile`,
`dialett.release.storePassword`, `dialett.release.keyAlias`), never in this repository:

```bash
./gradlew :shared:testAndroidHostTest :androidApp:bundleRelease
```

Each upload to Play needs a higher `versionCode` in `androidApp/build.gradle.kts` (1 for the first).

## Play Console

1. **Create app:** name *Dialett da Rögn*, default language English (United States), App, Free.
2. **App signing:** let Google create the app signing key. The key in Bitwarden becomes the upload key.
3. **Store listing:** paste the texts, upload the icon, feature graphic and screenshots; add Italian (it-IT)
   as a translation with its own texts. Category: Education. Contact email: required.
4. **App content:**
   - Privacy policy: `https://www.michaelmaguire.ca/dialett-da-rogn/privacy/`
   - Ads: no ads
   - App access: all functionality available without special access
   - Content rating: questionnaire, category Reference; no violence, sexuality, gambling, user interaction
   - Target audience: 18 and over is simplest (a younger audience brings Families policy checks)
   - Data safety: no data collected, no data shared
   - Government app: no; financial features: none; health: none
5. **Closed testing** (required for personal accounts): create a track, upload the `.aab`, add testers
   (a Google Group is easiest for them: they join with one tap), choose the countries, and send them the
   opt-in link. At least 12 testers opted in for 14 days in a row; invite about 15. Testers need an Android
   phone and the Google account they use on it.
6. **After 14 days:** apply for production access (a short questionnaire about the test), then release
   the same bundle to production.

## F-Droid

F-Droid builds the app itself from this repository's public source, signs it with its own key, and takes
the listing from `fastlane/metadata/android/` (texts, icon, feature graphic, screenshots, and a changelog
per version in `changelogs/<versionCode>.txt`). What makes the build acceptable to it:

- every library is open source (AndroidX, Compose, Kotlin, JetBrains, Guava): no Google Play Services,
  Firebase, ads or tracking SDKs
- no encrypted dependency list in the APK (`dependenciesInfo` in `androidApp/build.gradle.kts`)
- a release build without a signing key comes out unsigned, as F-Droid wants it; checked with a clean
  build on JDK 17 with no `~/.gradle/gradle.properties` (2026-11-10)

To submit:

1. The repository must be public.
2. Tag the release that matches `versionCode` / `versionName`, and push the tag:

   ```bash
   git tag v1.0 && git push origin v1.0
   ```

3. On GitLab, fork https://gitlab.com/fdroid/fdroiddata, add `store/fdroid/ca.michaelmaguire.languages.dialett.rogn.yml`
   as `metadata/ca.michaelmaguire.languages.dialett.rogn.yml`, and open a merge request. Its pipeline
   builds the app; an F-Droid reviewer then checks it (often a few weeks).
4. Later versions: raise `versionCode` and `versionName`, add `changelogs/<versionCode>.txt`, tag
   `v<versionName>`. F-Droid finds new tags by itself (`UpdateCheckMode: Tags`).

The F-Droid and Play versions are signed with different keys (F-Droid's, Google's), so Android treats them
as one app it can't switch between: someone moving from one to the other uninstalls first. The app keeps no
data, so nothing is lost.
