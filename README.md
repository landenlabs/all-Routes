# all-route (EXPERIMENT - NOT DONE YET)
<br>18-Apr-2026
<br>API 36 AndroidX Java
<br>[Home website](https://landenlabs.com/android/index.html)

<img src="screens/landenlabs.webp" width="300" alt="Logo">

Android Route Recorder App

Demo app to experiment with recording and comparing routes to automatically
determine current route by matching location, time and day-of-week to previously 
recorded tracks/trips. 

## Terms

- Track = GPS path driven (car start/stop)
- Trip = collection of tracks with similar end points. 
Commute to work and commute home are part of same trip. 

## Build Setup

The map layer runs on **mapsdk-v2** (TWC's Kotlin wrapper around **Pangea 5.11**), replacing
the old WSI MapSDK v1 / Pangea 4.20 stack. See [dev-notes.md](dev-notes.md) for the full
migration write-up, dependency graph, and map init flow.

To build/run, you need:

- **JDK 21+** available to Gradle (the toolchain in the root `build.gradle` requests 21 even
  though the app still targets Java 17 bytecode - Pangea 5's artifacts ship class files `javac`
  older than 21 can't read).
- A `local.properties` in the repo root with:
  ```properties
  mapSDKKey="<your mapsdk-v2 API key>"
  sunApiKey=""
  sunProductSet=""
  ```
  (`route/build.gradle` reads these into `BuildConfig`/`resValue`s consumed by `MapViewer.initBeforeCreate()`.)
- `libs/mapsdkv2-<version>.aar` present - built from the sibling project
  `mapsdk-v2` (module `map-sdk`) and copied in manually; it isn't published to a resolvable repo.
- `minSdk` is **29** (bumped from 28) - required by the mapsdkv2 AAR's manifest.


## Screens

### Summary Page
<img src="screens/route-summary.png" width="50%" />


---


### Routes Page - List and manipulate recorded tracks/routes/trips
<img src="screens/route-routes.png" width="50%" />

---


### Record Page - record current GPS activity to create a track.
<img src="screens/route-records.png" width="50%" />

---


### Developer Page - app/sys internals. 
<img src="screens/route-dev.png" width="50%" />

---

### License

```
Copyright 2026 Dennis Lang (LanDen Labs)

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0
Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
See [LICENSE](LICENSE) for the full license text.
