# Parkinson's Disease Diagnostic App

An Android app that helps clinicians screen for Parkinson's disease by analyzing a patient's **handwriting and voice** with **on-device machine learning**. Built as the practical implementation of a master's thesis.

![Platform](https://img.shields.io/badge/platform-Android%207.0%2B-3DDC84?logo=android&logoColor=white)
![Language](https://img.shields.io/badge/language-Kotlin-7F52FF?logo=kotlin&logoColor=white)
![License](https://img.shields.io/badge/license-MIT-blue)

> ⚠️ Research/thesis prototype — not a certified medical device. Results are indicative and not a substitute for clinical diagnosis.

## The problem

There's no definitive biological test for Parkinson's disease — diagnosis relies on subjective clinical rating scales, and studies report up to 25% of patients being initially misdiagnosed, even by specialists. Research shows that subtle, measurable changes in **handwriting** (micrographia, tremor, reduced pen pressure) and **voice** (reduced volume, monotone pitch, imprecise articulation) appear early in the disease and can be captured non-invasively with everyday hardware — a tablet with a stylus, or a microphone.

Most existing research tools only look at one of these modalities and rely on a server to run the classification. This app combines both into a single tool that runs entirely offline on the device, wrapped in a workflow a clinician can actually use day to day: manage patients, run standardized measurement tasks, and get an instant result.

## Architecture

<img width="611" height="461" alt="Architecture: clinician uses the Android app, which runs on-device ML inference and syncs with a Nextcloud server over WebDAV" src="https://github.com/user-attachments/assets/3ea34b48-7265-4ffb-a1df-ed5290a11446" />

- **Android app** — handles data capture, feature extraction, and classification, all on-device. Built with Kotlin following the **MVVM** pattern (Activities/Fragments → ViewModels → Repositories), so measurement state survives things like screen rotation.
- **ML models** — two independent classifiers exported to **ONNX** and run locally via **ONNX Runtime**. No network call, no latency, no health data leaves the device during inference.
- **Remote storage** — a **Nextcloud** server (over WebDAV) acts as the multi-device patient database and backup. Sync runs asynchronously in the background, so the app works fully offline and catches up automatically once a connection is available.

## Tech stack

- **Kotlin**, MVVM architecture, Android Activities/Fragments + XML layouts
- **ONNX Runtime** for on-device model inference
- **TarsosDSP** for real-time audio signal processing (pitch/F0 detection)
- **Nextcloud / WebDAV** for remote storage and sync
- **Python + scikit-learn**, exported via `skl2onnx`, for offline model training

## What was implemented

**Handwriting capture & analysis** — Captures raw pen input through Android's `MotionEvent` API (position, timestamp, surface contact, pressure, azimuth, altitude) while visualizing the stroke live on screen. Recordings are exported in a `.svc` format compatible with a public handwriting research dataset. On-device, a pipeline of **72 features** is computed from each recording — velocity/acceleration/jerk statistics, direction-change counts, segmented pressure analysis, stroke and pen-orientation statistics — implemented to exactly mirror the Python pipeline used to train the model, so inference sees the same inputs as training.

**Voice capture & analysis** — Records uncompressed 16 kHz / 16-bit mono audio directly from the microphone. Extracts **12 acoustic features** (fundamental frequency statistics, jitter, shimmer, harmonics-to-noise ratio) via frame-based signal analysis using the YIN pitch-detection algorithm.

**On-device classification** — Two Random Forest models (trained in Python, exported to ONNX) run locally to classify each completed task as Healthy / Parkinson's with a confidence score. The app also supports uploading a **custom ONNX model** per modality, as long as it matches the documented input feature format — useful for experimenting with new models without touching the app's source code.

**Patient & protocol management** — Create/search patient records, assign predefined or custom measurement protocols (sequences of handwriting/voice tasks), track completion per task, and review results and history — all backed by local storage with automatic background sync to the server.

**Offline-first design** — Every step of running a measurement and getting a result works with no network connection; only loading or creating a patient record requires connectivity, and pending data syncs automatically once it's back.

## Screenshots

<img width="2560" height="1600" alt="screenshot_patient_list" src="https://github.com/user-attachments/assets/d4fb63e9-6c37-4590-b561-8f6f2811fb8d" />
<img width="2560" height="1600" alt="screenshot_voice_task" src="https://github.com/user-attachments/assets/e4fd24e3-2611-4911-92d7-2a06cf242c5a" />
<img width="2560" height="1600" alt="screenshot_evaluation_result2" src="https://github.com/user-attachments/assets/da2469b6-9184-4ccf-a5d1-11e1f245ed04" />
<img width="2560" height="1600" alt="screenshot_protocol_manager" src="https://github.com/user-attachments/assets/a8d25554-b815-45b6-9ff0-947d62a01296" />


## License

[MIT](LICENSE)
