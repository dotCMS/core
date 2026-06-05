# libvips Image Engine (vips-ffm)

A feature-flagged, drop-in alternative to the legacy pure-JVM image filters
(`com.dotmarketing.image.filter`), backed by [libvips](https://www.libvips.org/)
through the [vips-ffm](https://github.com/lopcode/vips-ffm) Panama FFM bindings.

The legacy engine is untouched and remains the default. This engine is selected
**per request** and falls back automatically when libvips can't do the job.

## Enabling

| Property | Default | Effect |
|----------|---------|--------|
| `IMAGE_API_USE_LIBVIPS` | `false` | Turn the libvips engine on for the image exporter chain. |
| `IMAGE_API_LIBVIPS_FALLBACK` | `true` | If a libvips op fails (corrupt image, missing delegate), run the legacy filter instead of erroring. |

### Operation-cache tuning (optional)

libvips keeps a global cache of operation *results*. dotCMS already caches finished
renditions on disk (`dotGenerated`) and serves mostly unique transforms, so the
in-process cache rarely hits — the libvips defaults are kept unless you override one
of these (applied once at engine init). This is **not** a per-operation memory cap;
a single op's buffers are bounded by the image and freed when its arena closes.

| Property | Default | Effect |
|----------|---------|--------|
| `IMAGE_LIBVIPS_CACHE_DISABLED` | `false` | Turn the operation cache off entirely (lowest, most predictable memory). |
| `IMAGE_LIBVIPS_CACHE_MAX_MEM` | libvips default (~100 MB) | Max cache memory in **bytes** (e.g. `268435456` = 256 MB). |
| `IMAGE_LIBVIPS_CACHE_MAX` | libvips default | Max number of cached operations. |
| `IMAGE_LIBVIPS_CACHE_MAX_FILES` | libvips default | Max cached open files. |

> Raising the cache rarely helps this workload (low hit rate) and uses more RAM;
> for a busy image server, `IMAGE_LIBVIPS_CACHE_DISABLED=true` is often the better
> knob. Remember the `DOT_` env prefix: `DOT_IMAGE_LIBVIPS_CACHE_MAX_MEM`, etc.

The engine only activates when the flag is on **and** native libvips is loadable
(`VipsManager.isEnabled()`); otherwise the legacy engine is used transparently.

> **Setting the flag via environment variable:** dotCMS `Config` only reads env
> overrides with the `DOT_` prefix (and `.`/`-` become `_`). So the flag is
> **`DOT_IMAGE_API_USE_LIBVIPS=true`**, not `IMAGE_API_USE_LIBVIPS=true`. A
> plain (unprefixed) env var is silently ignored and the engine stays on legacy —
> in which case the libvips-only filters (`avif`, `smartcrop`) no-op while the
> shared filters still work, which is the tell-tale symptom. The fallback override
> is `DOT_IMAGE_API_LIBVIPS_FALLBACK`.

## Native dependency

Unlike the legacy stack (pure-JVM: TwelveMonkeys, PDFBox, Batik, webp-imageio),
this engine needs native libraries on the host:

- **Linux/CI**: `apt install libvips42` — found on the standard library path; no
  extra JVM args needed beyond `--enable-native-access=ALL-UNNAMED`.
- **macOS (local dev)**: `brew install vips`. SIP strips `DYLD_*` from forked
  JVMs, so point the loader straight at the dylibs:
  ```
  -Dvipsffm.libpath.args="-Dvipsffm.libpath.vips.override=/opt/homebrew/lib/libvips.42.dylib \
    -Dvipsffm.libpath.glib.override=/opt/homebrew/lib/libglib-2.0.0.dylib \
    -Dvipsffm.libpath.gobject.override=/opt/homebrew/lib/libgobject-2.0.0.dylib"
  ```

Format coverage (PDF, SVG, AVIF/HEIC, animated GIF) depends on the **delegates
compiled into the host libvips** — poppler/pdfium, librsvg, libheif, cgif. Verify
the target container's build; `IMAGE_API_LIBVIPS_FALLBACK` covers any gap at runtime.

> **AVIF encode** needs libheif built with an AV1 encoder. On Ubuntu that's the
> `libheif-plugin-aomenc` package — a *Recommends* that `--no-install-recommends`
> drops, so it must be listed explicitly (it is, in the runtime Dockerfile). Without
> it, `heifsave` reports "Unsupported compression". AVIF/HEIC *decode* works with
> stock `libheif1`.

Requires **JDK 23+** (FFM); dotCMS runs on Java 25.

## Parameter contract

Identical to the legacy engine. Every filter subclasses the legacy `ImageFilter`,
reusing its cache-file naming, `overwrite()` short-circuit and prefix parsing, so
URLs like `…/filter=resize,jpeg/resize_w/800/jpeg_q/70` work unchanged. The only
difference is the pixels (libvips lanczos3 vs jhlabs/TwelveMonkeys) — so cached
renditions regenerate once when the flag flips.

## Filter coverage

| Filter | Class | libvips op |
|--------|-------|-----------|
| crop | `VipsCropImageFilter` | `extract_area` (+ ported focal-point math) |
| resize / scale | `VipsResizeImageFilter` / `VipsScaleImageFilter` | `thumbnail` (shared `ResizeCalc`) |
| subsample | `VipsSubSampleImageFilter` | streaming `thumbnail` |
| thumbnail | `VipsThumbnailImageFilter` | `thumbnail` + `flatten` + `gravity` |
| rotate | `VipsRotateImageFilter` | `rotate` |
| flip | `VipsFlipImageFilter` | `flip` |
| gamma | `VipsGammaImageFilter` | `gamma` |
| grayscale | `VipsGrayscaleImageFilter` | `colourspace(B_W)` |
| hsb | `VipsHsbImageFilter` | `sRGB2HSV` + `linear` + `HSV2sRGB` (approx) |
| exposure | `VipsExposureImageFilter` | `linear` gain (approx) |
| jpeg | `VipsJpegImageFilter` | `jpegsave` (Q + interlace) |
| png | `VipsPngImageFilter` | `pngsave` |
| webp | `VipsWebPImageFilter` | `webpsave` (Q + lossless) |
| gif | `VipsGifImageFilter` | `gifsave` (animated, `n=-1`) |
| pdf | `VipsPdfImageFilter` | `pdfload` (page + dpi) |
| **smartcrop** | `VipsSmartCropImageFilter` | `smartcrop` — **new, no legacy equivalent** |

## Architecture

- `VipsManager` — native init/availability probe (memoized), `Vips.run` arena
  wrapper, null-safe metadata reads. All work for one filter runs inside a single
  arena so no native `VImage` escapes its lifetime.
- `VipsImageFilter` — base class: legacy cache machinery + `transform(in,out,params)`
  pixel hook + per-op legacy fallback.
- `VipsImageFilterApiImpl` — implements `ImageFilterAPI`; `resolveFilters` returns
  the libvips filter classes for the same canonical keys.
- `ImageFilterExporter` — the dispatch seam; picks the engine via the flag.

See `NEW-FEATURES.md` for capabilities this engine unlocks beyond parity.
