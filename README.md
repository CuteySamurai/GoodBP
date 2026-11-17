# Good BP (量血压)

Voice-first Android app for logging blood pressure and medicine for an elderly user who can't read.
Family caregiver sets it up and gets alerts. Mandarin first; English and Bahasa Indonesia slot in.

**Safety rules (from the brief):** logs, reminds, alerts. Never diagnoses, never advises on medicine.
Results use fixed rules against the doctor's range. Every reading is read back and confirmed (对/不对) before it's saved.

## Run it
- Android Studio (Ladybug or newer): open this folder, press Run. Any emulator or phone, Android 8.0+.
- Or: `./gradlew assembleDebug` (needs the Android SDK), APK at `app/build/outputs/apk/debug/`.
- GitHub Actions builds a debug APK on every push (`.github/workflows/build.yml`), downloadable from the run page,
  so you can sideload it on her phone without Android Studio.

## Map of the code (`app/src/main/java/com/goodbp/`)
| Area | Files | Status |
|---|---|---|
| Home (one big button) | `ui/home/HomeScreen.kt` | Working. Long-press top-right corner opens family setup. |
| Core loop | `ui/flow/ReadingFlow.kt` (state machine), `ui/flow/ReadingScreen.kt` | Working: photo → voice backup → read-back → result. |
| Fixed-rule result | `rules/BpRules.kt` | Working. Age default range is a placeholder; doctor's range overrides. |
| Photo logging | `capture/PhotoReader.kt`, `capture/MonitorTextParser.kt` | ML Kit on-device; `CloudReader` slot for fallback. |
| Voice logging | `speech/VoiceInput.kt`, `speech/MandarinNumberParser.kt` | Android SpeechRecognizer zh-CN; parses 一百二十 / 一二零 / 120. |
| Speaking | `speech/Speaker.kt` | Recorded clip if present, else TTS (slowed). |
| Reminders | `reminders/*` | Exact alarms + full-screen reminder that repeats; reschedules after reboot. |
| Oppo setup | `setup/OemSetupGuide.kt`, `setup/FamilySetupScreen.kt` | Opens battery + auto-launch pages; manual steps in Chinese. |
| Storage | `data/*` | Room (local). Firebase comes in the family-alerts step. |
| Family alerts | `family/FamilyAlerts.kt` | Interface + no-op stub. |
| Medicine | `meds/MedicineReminders.kt`, `data/Entities.kt` | Data model + scheduling stub. Records "confirmed by voice"/"photo taken", never "taken". |
| History / report | `ui/history/HistoryScreen.kt`, `report/DoctorReport.kt` | Plain list + basic PDF. Chart is next. |

## Languages
- `res/values/` = Mandarin (default). `values-en/`, `values-in/` = English, Bahasa Indonesia (partial).
- Add a language: new `AppLanguage` entry in `data/Settings.kt`, a `values-xx/strings.xml`, and its recorded clips.
- Recorded prompts go in `res/raw/` named `<string_key>_<lang>.ogg`, e.g. `say_result_low_zh.ogg`.

## Before trusting it (step 0 on her phone)
1. Speech test: does `MandarinNumberParser` get her three numbers right?
2. Photo test: does ML Kit read the Omron HEM-7120 screen in home lighting? If not, add a `CloudReader`.
3. Reminder test: with battery/auto-launch settings done, does an alarm fire with the phone idle overnight?

## To confirm with the family / doctor
- Her doctor's range and reminder times (`data/Settings.kt` has placeholders: 08:00, 13:00, 20:00).
- Wording of the high/low spoken instruction (`res/values/strings.xml`, `say_result_*`).
