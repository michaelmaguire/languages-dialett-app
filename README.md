# Dialett da Rögn: the phone app

The dictionary, verb tables, grammar and expressions of
[languages-dialett](https://github.com/michaelmaguire/languages-dialett) (the dialect of Arogno, Ticino), for
a phone, offline, with one search over all of it: dialect, Italian or English, accents optional (*buta*
finds *bütà*), conjugated forms (*bütavum* finds *bütà*, imperfetto, *nüm*), and a few words of a saying
(*boca lüf*).

Kotlin Multiplatform with Compose Multiplatform. Android today; laid out so iOS can be added.

| Folder | What's there |
|---|---|
| `shared/` | Everything but the platform shell, in common Kotlin: the data model (`data/`), search (`search/`), every screen (`ui/`), and the data itself, `src/commonMain/composeResources/files/dialett.json` |
| `androidApp/` | The Android app: `MainActivity`, icon, manifest |
| `iosApp/` | Empty for now: [iosApp/README.md](iosApp/README.md) |
| `scripts/update-data.sh` | Refreshes the data from `../languages-dialett` |

## The data

`dialett.json` is made by `pipeline/step6_build_app_data.py` in languages-dialett, from the same models its
pages and PDFs are rendered from, so the app shows exactly what they do; corrections go there, never here.
After a build there (`pipeline/build_all.sh`), refresh the app's copy and commit it:

```bash
scripts/update-data.sh
```

## Build and run

Builds in the `android-dev` container (ansible-config, `doc/android-dev.md`). From this folder:

```bash
android-dev bash -c 'cd ~/code/michaelmaguire/languages-dialett-app && ./gradlew :shared:testAndroidHostTest :androidApp:assembleDebug'
```

Install on the emulator or the phone (USB debugging on):

```bash
android-dev adb install -r ~/code/michaelmaguire/languages-dialett-app/androidApp/build/outputs/apk/debug/androidApp-debug.apk
```

Or open the folder in Android Studio and run `androidApp`.

## Tests

`shared/src/commonTest` (accent folding) and `shared/src/androidHostTest` (the real data file: counts, and
searches whose answer is known: *buta* → *bütà*, *bütavum* → its verb, *casa* → *cà*).
