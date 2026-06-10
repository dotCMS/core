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
in-process cache rarely hits and mainly pins memory — therefore it is **disabled by
default**. Re-enable it (and tune it) with these, applied once at engine init. This is
**not** a per-operation memory cap; a single op's buffers are bounded by the image and
freed when its arena closes.

| Property | Default | Effect |
|----------|---------|--------|
| `IMAGE_LIBVIPS_CACHE_DISABLED` | `true` | Operation cache off (lowest, most predictable memory). Set `false` to re-enable libvips' default cache. |
| `IMAGE_LIBVIPS_CACHE_MAX_MEM` | libvips default (~100 MB) | Max cache memory in **bytes** (e.g. `268435456` = 256 MB). Only applies when not disabled. |
| `IMAGE_LIBVIPS_CACHE_MAX` | libvips default | Max number of cached operations. |
| `IMAGE_LIBVIPS_CACHE_MAX_FILES` | libvips default | Max cached open files. |

> Remember the `DOT_` env prefix: `DOT_IMAGE_LIBVIPS_CACHE_DISABLED=false`,
> `DOT_IMAGE_LIBVIPS_CACHE_MAX_MEM=268435456`, etc.

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
| **avif** | `VipsAvifImageFilter` | `heifsave` (AV1) — **new, no legacy equivalent** |

## New libvips-only filters — usage

These two have **no legacy equivalent**, so they only work when the engine is enabled
(`DOT_IMAGE_API_USE_LIBVIPS=true`); on the legacy engine the filter key is unknown and
silently dropped.

### smartcrop — content-aware crop

Crops to an exact box centred on the most salient region (not the geometric centre).

| Param | Type | Default | Meaning |
|-------|------|---------|---------|
| `smartcrop_w` | int | source width | Target width (clamped to source) |
| `smartcrop_h` | int | source height | Target height (clamped to source) |
| `smartcrop_mode` | enum | `attention` | `attention` (saliency) \| `entropy` (busiest region) \| `centre` |

```
# explicit filter syntax
/contentAsset/image/<id>/asset/filter/smartcrop/smartcrop_w/400/smartcrop_h/400/smartcrop_mode/attention

# ShortyServlet shorthand: crop tokens + /smart
/dA/<id>/400cw/400ch/smart
```

Requesting a box larger than the source returns the largest valid crop (no error).

### avif — AVIF (AV1) output

| Param | Type | Default | Meaning |
|-------|------|---------|---------|
| `avif_q` | int (0-100) | 50 | Quality |
| `avif_lossless` | present | off | Lossless encode |
| `avif_effort` | int (0-9) | 4 | Encoder effort/speed tradeoff |

```
# explicit filter syntax
/contentAsset/image/<id>/asset/filter/avif/avif_q/50

# ShortyServlet shorthand (mirrors /webp, /jpeg); honours an explicit /(\d+)q
/dA/<id>/1024w/avif
/dA/<id>/1024w/50q/avif

# chain: resize then AVIF
/contentAsset/image/<id>/asset/filter/resize,avif/resize_w/1024/avif_q/50
```

> AVIF `Q` is **not** comparable to WebP/JPEG `Q` — at low Q it is much smaller than WebP,
> at high Q it can be larger. Around Q≈40–50 it is a clear win. Requires the libheif AV1
> encoder (`libheif-plugin-aomenc`) in the host libvips build.

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
