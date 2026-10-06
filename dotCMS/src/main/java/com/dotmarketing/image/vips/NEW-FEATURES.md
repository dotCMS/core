# New Capabilities Unlocked by the libvips Engine

Beyond reaching parity with the legacy filters, libvips opens up features the
pure-JVM stack can't practically deliver. Ordered by value-to-effort.

## 1. Eliminate the documented OOM risk (already structural)

The legacy `ImageFilterAPI` Javadoc warns that resizing "should only be done on
smaller images (say less than 2000px) as very large images can cause garbage
collections and OOM exceptions" — because the whole image is decompressed into
heap. libvips is **demand-driven and streaming**: `VImage.thumbnail` shrinks on
load and never materializes the full raster. This already ships in
`VipsResizeImageFilter`/`VipsSubSampleImageFilter` — the `IMAGE_MAX_PIXEL_SIZE`
clamp and the subsample pre-pass become unnecessary for the libvips path.
**Status: done. Next: drop the artificial clamps when the flag is on.**

## 2. Modern output formats: AVIF & JXL

`webpsave` already gives us WebP. libvips (with libheif/libjxl delegates) can also
emit **AVIF** and **JPEG-XL** — 20-50% smaller than WebP/JPEG at equal quality.
A `VipsAvifImageFilter` is ~15 lines (`writeToFile(".avif", Q, effort)`), mirroring
`VipsWebPImageFilter`. Lets editors serve next-gen formats via `filter=avif&avif_q=50`.
**Effort: trivial. Gate behind a delegate check + fallback.**

## 3. Content-aware smart crop (prototyped ✅)

`VipsSmartCropImageFilter` crops to an exact box centred on the most *salient*
region (libvips attention model: saliency + skin-tone + edge energy), not the
geometric centre. Auto-generates well-composed thumbnails of arbitrary content —
something AWT cannot do. `filter=smartcrop&smartcrop_w=400&smartcrop_h=400`.
**Status: shipped in this branch with a passing test.**

## 4. Perceptual placeholders: BlurHash / ThumbHash / dominant colour

libvips can cheaply compute a tiny representation of an image:
- **Dominant colour** — `image.resize(→1px)` then read the pixel; a one-line
  `getpoint` gives an instant theme/average colour for a contentlet.
- **BlurHash/ThumbHash** — downscale to ~32px, encode a ~30-byte string the front
  end expands into a blurred placeholder during lazy-load (the "Medium blur-up").
Expose as a derived field or a `?meta=blurhash` query on the image endpoint.
**Effort: low. High perceived-performance win for SDK/UVE consumers.**

## 5. Single-pass pipeline (kill intermediate files)

Today each filter in `filter=resize,crop,jpeg` writes a temp PNG that the next
filter re-decodes — N decode/encode round-trips and N temp files. libvips builds a
**lazy operation graph** and executes it once. A `VipsPipeline` that maps the whole
filter chain to chained `VImage` ops and writes a single output would cut CPU, IO
and temp churn dramatically for multi-filter URLs. The current per-filter classes
keep parity; the pipeline is the optimization on top.
**Effort: medium. Biggest throughput win.**

## 6. Better resampling & sharpening defaults

libvips lanczos3 + optional `sharpen` on downscale produces visibly crisper
thumbnails than the current triangle filter, at lower cost. A `resize_sharpen=true`
opt (or on by default for downscales) is a one-liner.

## 7. Animation-aware everything

The legacy engine has a bespoke `GifDecoder`/`AnimatedGifEncoder` and only the GIF
resize path preserves frames. libvips treats animation uniformly (`n=-1`), so
crop/rotate/format-convert can all preserve animation — and it can transcode
**animated GIF → animated WebP/AVIF** (often 10x smaller) in one call.

## 8. Colour management (ICC)

`icc_transform` lets us normalize to sRGB and honour embedded ICC profiles, so
wide-gamut camera images don't look washed out after processing — a correctness
win the AWT path doesn't address.

---

### Suggested sequencing
1. Land parity + flag (this branch).
2. AVIF output + smartcrop (both cheap, both already mostly here).
3. BlurHash/dominant-colour derived metadata (front-end visible win).
4. Single-pass `VipsPipeline` (throughput).
5. Animated WebP/AVIF transcode + ICC.
