# Developer Notes

Technical companion to [README.md](README.md). Covers the map SDK migration, the
module/dependency graph, the map init/lifecycle flow, and gotchas discovered while
getting it running end-to-end on a device.

## Status

The map layer was migrated from **WSI MapSDK v1** (`com.wsi.mapsdk.*`, itself wrapping an
older Pangea/Mapbox generation) to **mapsdk-v2** (`com.weather.mapsdk.*`), TWC's Kotlin
wrapper around **Pangea 5.11** / Mapbox Maps v11. A prior, incomplete attempt had added a
direct `com.weather.pangea.android:pangeamapbox:4.20.0` dependency without porting the
Java call sites - that intermediate state is gone; the app now goes straight from the old
SDK to mapsdk-v2.

mapsdk-v2 lives in a **sibling project**, `../auto/mapsdk-v2` (module `map-sdk`), and is
consumed here as a flat AAR (`libs/mapsdkv2-<version>.aar`) copied in manually - it is not
published to any resolvable Maven repo, so there's no way for Gradle to pull it
automatically. If the AAR needs rebuilding: `mapsdk-v2/` → `./gradlew :map-sdk:assembleDebug`
→ copy `map-sdk/build/outputs/aar/*.aar` into `libs/` here and update
`dependencies.gradle`'s `mapSdkV2` version.

## Module / dependency graph

```
all-Routes (root)
 ├─ route/                          application module (landenlabs.routes / landenlabs_dev.routes)
 │   ├─ depends on :lib_data, :lib_gpx (test only)
 │   ├─ libs/mapsdkv2-<ver>.aar      -- flat file dep, no transitive metadata
 │   ├─ com.weather.pangea.kotlin:pangea-mapbox-android:5.11.1   (Pangea 5 / Mapbox v11 core)
 │   ├─ com.google.firebase:*        (analytics/crashlytics/perf, via firebase-bom)
 │   ├─ androidx.navigation, androidx.security, play-services-location, app-update
 │   └─ (desugar_jdk_libs 2.1.5, required by Pangea 5 / mapsdk-v2's AAR metadata)
 │
 ├─ lib_data/                        library module (landenlabs.lib_data)
 │   ├─ libs/wxdata-<ver>.aar        -- flat file dep, TWC weather-data client (com.wsi.wxdata)
 │   ├─ com.squareup.okhttp3:{okhttp,logging-interceptor}:5.3.2   (synced with Pangea's own okhttp version)
 │   ├─ com.squareup.retrofit2:{retrofit,converter-gson}
 │   ├─ io.reactivex.rxjava2:*        (legacy adapter path, still used by some retrofit calls)
 │   └─ io.reactivex.rxjava3:* + com.squareup.retrofit2:adapter-rxjava3
 │        (required at runtime by wxdata 2.26+, which builds its own Retrofit client
 │         internally using RxJava3CallAdapterFactory - see "Gotchas" below)
 │
 └─ lib_gpx/                         library module, GPX file support (test dependency of route)
```

Route's map classes (`landenlabs.routes.map.*`) depend only on `com.weather.mapsdk.*`
(mapsdk-v2) and `com.weather.mapsdk.props.*` types - no more direct `com.weather.pangea.*`
imports anywhere in `route/` except transitively through mapsdk-v2 itself.

### Why the JDK 21 toolchain / desugar bump

- `route/build.gradle`'s `coreLibraryDesugaring` needed bumping to `desugar_jdk_libs:2.1.5`
  (from 2.0.4) - AGP's `checkAarMetadata` task enforces a minimum desugar version declared
  by the mapsdk-v2 AAR and by several `pangea-*-android` artifacts.
- The root `build.gradle`'s Java toolchain was bumped to `JavaLanguageVersion.of(21)`.
  `sourceCompatibility`/`targetCompatibility` stay at 17 (we still emit Java 17 bytecode),
  but `javac` itself must be JDK 21+ to *read* some of Pangea 5's dependency class files
  (class file major version 65 = Java 21; a JDK 17 `javac` throws
  `bad class file ... wrong version 65.0, should be 61.0`).

## Map init / lifecycle flow

`MapViewer` (`route/src/main/java/landenlabs/routes/map/MapViewer.java`) extends
`com.weather.mapsdk.TWCMapView` directly (composition wasn't an option - `TWCMapView` is a
`FrameLayout` subclass meant to be used as the map widget itself). It re-exposes a
WSI-v1-flavored API (`OnWSIMapViewChangedCallback`, `setCamera`, `setCameraBounds`,
`setRasterLayer`, `showRadar`/`showDDI`/`showWind`, `moveMarker`/`removeMarker`) on top of the
new SDK so the three page fragments needed only moderate rewiring rather than a full rewrite.

```
App startup / Fragment.onCreateView()
  └─ authorizeMap()                          [Page{Weather,Routes,Recorder}Frag]
       └─ MapViewer.initBeforeCreate(context)          (idempotent, guarded by sApiKeyRegistered)
            └─ TWCMapSDKInitializer.Companion.get(context).register(MapSdkOptions)
                 (key comes from R.string.mapSDKKey, i.e. local.properties["mapSDKKey"])

Fragment.onResume()
  └─ initMap(binding.mapViewer)              [fragment-local method, NOT MapViewer.startInit]
       └─ mapView.startInit()                          (idempotent, guarded by startedInit)
            └─ TWCMapView.initMap(callback)             [inherited from mapsdk-v2]
                 └─ TWCMapSDK.authenticateSDK(apiKey, ...)      -- network call:
                      GET https://config.media.weather.com/api/v1/features?key=...&type=MobileMapSDK
                      (async, on IO dispatcher; this is where a bad/placeholder key
                       surfaces as "MapSDK Authentication Failed: 400")
                 └─ onAuthSuccess → prepareMap() → MapboxViewport.create(...)
                      └─ callback.onMapReady(Result(mapView, errorMsg))   [async, main thread]
                           └─ MapViewer's lambda (in startInit()):
                                if ready && authorized && doReadyOnce:
                                  setAttribution(...)
                                  post { initTWCMap(); executeChangedCallbacks(MAP_STATE_READY) }
                                     └─ initTWCMap(): clearOverlays/clearRasters,
                                                      restoreMapViewState() (camera + default raster),
                                                      setOverlayLayers(true) (Severe alert + Lightning)
                                     └─ executeChangedCallbacks(READY)
                                          └─ fragment.onMapReady(mapViewer, MAP_STATE_READY)
                                               (fragment sets its own raster/timeline/camera/markers here)
```

Two other events flow through the **same** `OnWSIMapViewChangedCallback.onMapReady(mapViewer, why)`
method, via `TWCMapSDKEventCallbacks.onLoading(...)` wired in `MapViewer.commonInit()`:

- `MAP_STATE_START_LOADING` / `MAP_STATE_COMPLETED_LOADING` - fired from mapsdk-v2's
  `subscribeRenderFrameStarted` / `subscribeRenderFrameFinished` hooks (TWCMapView.kt).
  **These fire on every rendered frame**, not just on initial load - a naming holdover from
  the WSI v1 API where the equivalent signal really did mean "raster tile data started/finished
  loading." See the "per-frame feedback loop" gotcha below.
- `MAP_STATE_TIMES_CHANGED` - fired from the raster animator's `frameChanged` flow (only
  relevant while a looping raster animation is actually playing).

## Raster / overlay name mapping (old WSI → mapsdk-v2)

mapsdk-v2's `addRaster(id)` / `addOverlay(id)` take plain strings matched against
`TWCMapSDKRasterLayerType.valueOf(id)` / `TWCMapSDKAlertLayerType.valueOf(id)` /
`TWCMapSDKFeatureLayerType.valueOf(id)` / `TWCMapSDKTrafficLayerType.valueOf(id)` - i.e. exact
enum names, not the old WSI catalog strings. Mapping applied during migration:

| Old WSI name              | New mapsdk-v2 id       | Type    |
|----------------------------|-------------------------|---------|
| `RadarWithModel`/`RadarSmooth` | `Radar`             | Raster  |
| `RoadWeather`              | `RoadWeather`           | Raster  (unchanged) |
| `NoRaster`                 | *(call `clearRasters()` instead of addRaster)* | - |
| `LIGHTNINGGLOBAL`          | `Lightning`             | Feature |
| `SDK_WATCHWARNING_SEVERE_GLOBAL` | `Severe`          | Alert   |
| `Windstream` (animated wind streamlines) | *(no equivalent yet)* | - |

The `Windstream` overlay has no vector/streamline equivalent in mapsdk-v2 today;
`MapViewer.showWind()` substitutes the static `WindSpeed` **raster** layer as the closest
available approximation. If mapsdk-v2 ever adds a streamline layer type, revisit this.

Full enum catalogs live in mapsdk-v2's `com.weather.mapsdk.enums.*`
(`TWCMapSDKRasterLayerType`, `TWCMapSDKFeatureLayerType`, `TWCMapSDKAlertLayerType`,
`TWCMapSDKTrafficLayerType`) - check there before inventing a new id string.

## Class overview

- **`MapViewer`** (`map/MapViewer.java`) - the map widget itself (`extends TWCMapView`).
  Owns: API key registration, ready/loading callback fan-out (`OnWSIMapViewChangedCallback`),
  camera persistence (`SharedPreferences`, keyed by view tag), pin-marker bookkeeping
  (`pinMarkers`, keyed by drawable resource id → `"pin<resId>"` label), active-raster
  bookkeeping (single `activeRasterId`, since mapsdk-v2's `addLayer` stacks layers rather
  than replacing them - `setRasterLayer()` does `clearRasters()` then `addRaster()` to
  emulate "one active raster" semantics).
- **`MapMarkers`** (`map/MapMarkers.java`) - named-marker helper (`START_MARKER`/`POS_MARKER`/
  `END_MARKER` + arbitrary keys) built on `TWCMapView.addImageMarker`/`removeImageMarker`.
  Every marker is rebuilt from its drawable resource each call (no persistent `Icon` object
  like the old SDK had); size is derived from the drawable's intrinsic size × a scale factor.
- **`MapTracks`** (`map/MapTracks.java`) - draws recorded-track polylines plus optional debug
  overlays (grid lines, bounding box, grid cells) via `addMarkerPolyline`/`addMarkerPolygon`,
  keyed by string id. Track polylines persist across calls (keyed by `Track.getKey()`, so
  multiple selected tracks coexist); debug overlays are fully cleared and rebuilt every call.
- **`RouteSettings.LineStyle`** (`data/RouteSettings.java`) - replaces the old
  `com.weather.pangea.model.overlay.StrokeStyle`/`StrokeStyleBuilder` (Pangea 4.x-only types).
  Holds color/opacity/width/dash-pattern as plain fields and exposes `argbColor()` to bake
  opacity into the alpha channel, because `addMarkerPolyline`/`addMarkerPolygon` take a plain
  packed ARGB `int`, not a style object.

## Gotchas / lessons learned

- **`isReady()` guards are load-bearing, not decorative.** The old WSI SDK tolerated calls
  made before the map finished initializing; mapsdk-v2's `TWCMapView` does not - several of
  its methods (`getFrameMilli`, `setCenter`, `addRaster`, ...) touch a `lateinit var
  mapboxViewport` directly and throw `UninitializedPropertyAccessException` if called too
  early. Every `MapViewer` method that touches SDK state now checks `isReady()` first and
  no-ops/returns a safe default if not. **Any new code added to a fragment's `onMapReady`
  handler, or any new `MapViewer` method, must do the same** - don't assume the map is ready
  just because a menu or button exists.
- **`onMapReady(mapViewer, why)` fires for `why` values other than `MAP_STATE_READY` -
  every rendered frame, in fact.** `refresh()`/`refreshUi()` calls in the fragments' `onMapReady`
  overrides must stay **inside** the `if (why == MAP_STATE_READY)` block. Getting this wrong
  caused a real bug: `refresh()` outside the guard → calls an animated `setCamera()` → which
  itself produces render frames → which re-fires `onMapReady` → which calls `refresh()` again,
  forever. This pegged the main thread at 100% CPU and made the UI completely unresponsive
  (no crash, just a silent lockup) until traced with a temporary stack-trace log in
  `GpsUtils.getCurrentLocation()`.
- **`TWCMapView.clearMarkers()` is `final` in Kotlin** (no `open` modifier) and clears
  markers **and** polylines **and** polygons together - it can't be overridden and it isn't
  scoped to "just pins" like the old WSI SDK's marker layer was. `MapViewer` exposes
  `clearPinMarkers()` instead (a new name, not an override) which only clears the
  `pinMarkers` bookkeeping map via `removeImageMarker`, leaving track/route polylines alone.
  Several other SDK methods are `final` too (no `open`) - check before assuming you can
  override rather than wrap.
- **Groovy `build.gradle` vs Kotlin `build.gradle.kts` syntax.** Snippets copied from
  mapsdk-v2's `build.gradle.kts` (constructor calls like `Properties()`, `FileInputStream(...)`)
  need `new` in Groovy (`new Properties()`, `new FileInputStream(...)`); Groovy fails with
  `Could not find method Properties() for arguments []` otherwise. Map/subscript access
  (`localProperties["key"]`) and the `as String` cast both work unchanged in Groovy.
- **`wxdata` 2.26+ needs RxJava 3 at runtime**, not just RxJava 2. It builds its own Retrofit
  client internally using `retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory`, which isn't
  referenced by any of *our* code, so javac never complains about a missing symbol at compile
  time - it only shows up as a `NoClassDefFoundError` at runtime, inside `WxData.initStuff()`.
  Fixed by adding `io.reactivex.rxjava3:{rxjava,rxandroid}` and
  `com.squareup.retrofit2:adapter-rxjava3` to `lib_data/build.gradle` (kept alongside the
  existing RxJava 2 deps, which other code paths still use).
- **`minSdk` is 29, not 28.** The mapsdkv2 AAR's manifest declares `minSdkVersion 29`; AGP's
  manifest merger hard-fails the build otherwise (`tools:overrideLibrary` can force it, but
  "may lead to runtime failures" per AGP's own warning - not attempted). This drops Android 9
  (API 28) device support project-wide; flag if that's a real constraint.
- **The API key path matters.** `MapViewer.initBeforeCreate()` originally used
  `getString1x(context, "app_name")` (the old WSI-era obfuscated-resource lookup) - since no
  `app_name_d1`/`app_name_x1` resource exists in this checkout, that silently fell back to
  returning the literal string `"app_name"` as the "key," which the mapsdk-v2 auth endpoint
  correctly rejected with HTTP 400 (logged as `MapSDK Authentication Failed: 400`, tag
  `MapSDKv2`). It now reads `R.string.mapSDKKey` (see `route/build.gradle`'s `resValue` block,
  sourced from `local.properties`). The `getString1x` import in `MapViewer.java` is currently
  unused dead weight from that switch - safe to remove whenever someone's next in that file.
- **Two installed packages can look identical.** A stale build under `com.landenlabs_dev.routes`
  (with the `com.` prefix, version 1.2.6, from May 2025 - predates a package rename and still
  runs the old WSI MapSDK v1 code) may still be on test devices alongside the current
  `landenlabs_dev.routes`. If you see a crash mentioning `com.wsi.mapsdk.*` or a `com.landenlabs.routes.*`
  package name, you launched the wrong icon - check with `adb shell pm list packages | grep routes`
  and `adb shell dumpsys package <pkg> | grep lastUpdateTime`.

## Useful adb one-liners used while debugging this migration

```sh
# Per-thread CPU - spot a pegged main thread (a "spin loop" with no crash)
adb shell top -b -n 1 -H -p "$(adb shell pidof landenlabs_dev.routes)"

# Confirm which build is actually installed/running
adb shell pm list packages | grep routes
adb shell dumpsys package <package> | grep -E "versionName|lastUpdateTime"

# Watch for the auth-rejection signature
adb logcat | grep -iE "MapSDK Authentication|MobileMapSDK"
```
