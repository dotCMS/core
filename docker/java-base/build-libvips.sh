#!/usr/bin/env bash
#
# build-libvips.sh - build libvips once, with the ImageMagick and OpenEXR
# delegates compiled out, and install it under a dedicated prefix.
#
# Why this exists
# ---------------
# libvips 8.15.1 is what Ubuntu 24.04 (noble) ships, and noble's libvips42t64 has
# hard dependencies on libmagickcore-6.q16-7t64 and libopenexr-3-1-30. Those two
# packages carry every ImageMagick/OpenEXR CVE in the report, and noble offers no
# fixed version of either, so no amount of apt upgrading clears them. dotCMS never
# invokes ImageMagick itself - the dependency exists only because libvips' magick
# delegate links it.
#
# Building libvips here with -Dmagick=disabled -Dopenexr=disabled removes the
# delegates (and therefore the packages) while keeping every other capability:
# AVIF/HEIC (libheif), PDF (poppler), SVG (librsvg), GIF (cgif), WebP, JXL,
# JPEG2000, TIFF, PNG, JPEG.
#
# This runs once in the java-base image; downstream images COPY the resulting
# prefix instead of rebuilding it (same pattern as tcnative).
#
# Outputs, all under ${VIPS_PREFIX}:
#   bin/vips, bin/vipsheader, ...        the CLI (kept for validation)
#   lib/libvips.so.42.*                  the library the FFM binding loads
#   lib/vips-modules-8.18/*.so           dynamic delegate modules
#   share/runtime-packages.txt           apt package manifest for the runtime image
#   share/build-info.txt                 provenance (version, checksum, options)
#   share/vips-config.txt                vips --vips-config at build time
#
# Usage:
#   build-libvips.sh
#   LIBVIPS_VERSION=8.18.6 VIPS_PREFIX=/usr/local/libvips build-libvips.sh
#
# The checksum is a hardcoded literal on purpose: integrity must not depend on
# re-fetching a checksum over the same channel as the tarball at build time.
#
# Required build tools (the image build installs these): curl, tar, xz (xz-utils),
# coreutils (sha256sum), meson, ninja-build, pkg-config, binutils, dpkg, libc-bin
# (ldconfig), plus the -dev packages for every delegate enabled below.

set -euo pipefail

LIBVIPS_VERSION="${LIBVIPS_VERSION:-8.18.6}"
LIBVIPS_SHA256="${LIBVIPS_SHA256:-3c41e1d5458081bfa4a5bc54e116c46259c75c6760a18027764555632b9dda3e}"
VIPS_PREFIX="${VIPS_PREFIX:-/usr/local/libvips}"
BUILD_DIR="${BUILD_DIR:-/tmp/libvips-build}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

LIBVIPS_URL="https://github.com/libvips/libvips/releases/download/v${LIBVIPS_VERSION}/vips-${LIBVIPS_VERSION}.tar.xz"
TARBALL="${BUILD_DIR}/vips-${LIBVIPS_VERSION}.tar.xz"
SRC_DIR="${BUILD_DIR}/vips-${LIBVIPS_VERSION}"
MESON_BUILD_DIR="${BUILD_DIR}/build"

# libheif loads its codec plugins with dlopen, so they never appear in ldd output
# and ELF discovery alone cannot find them. aomdec decodes AV1/AVIF, aomenc
# encodes it; both were present in the previous (stock) image's closure via
# libheif-plugin-aomenc. Listing both keeps AVIF decode+encode at parity.
LIBHEIF_PLUGIN_PACKAGES=(
	libheif-plugin-aomdec
	libheif-plugin-aomenc
)

log() { printf '\n=== %s\n' "$*"; }

# ---------------------------------------------------------------------------
# 0. Preflight. Fail early and explicitly rather than midway through a build.
#    (Bare ubuntu:24.04 has no xz-utils, which only shows up when tar runs.)
# ---------------------------------------------------------------------------
MISSING_TOOLS=()
for tool in curl tar xz sha256sum meson ninja pkg-config ldconfig dpkg; do
	command -v "$tool" >/dev/null 2>&1 || MISSING_TOOLS+=("$tool")
done
if [ "${#MISSING_TOOLS[@]}" -gt 0 ]; then
	echo "FATAL: missing build tools: ${MISSING_TOOLS[*]}" >&2
	echo "       (xz comes from xz-utils; meson/ninja from meson and ninja-build)" >&2
	exit 1
fi

# ---------------------------------------------------------------------------
# 1. Fetch and verify. Fail closed on any checksum mismatch.
# ---------------------------------------------------------------------------
log "fetch libvips ${LIBVIPS_VERSION}"
mkdir -p "$BUILD_DIR"
rm -rf "$SRC_DIR" "$MESON_BUILD_DIR"

if [ ! -f "$TARBALL" ]; then
	curl -fsSL --retry 3 -o "$TARBALL" "$LIBVIPS_URL"
fi

echo "${LIBVIPS_SHA256}  ${TARBALL}" | sha256sum -c - || {
	echo "FATAL: checksum mismatch for ${TARBALL}" >&2
	echo "       expected ${LIBVIPS_SHA256}" >&2
	echo "       got      $(sha256sum "$TARBALL" | cut -d' ' -f1)" >&2
	exit 1
}

tar xf "$TARBALL" -C "$BUILD_DIR"

# ---------------------------------------------------------------------------
# 2. Configure. Delegates we need are enabled EXPLICITLY so that a missing
#    development package fails the configure step instead of silently
#    producing a libvips with less capability than the old stock package.
# ---------------------------------------------------------------------------
log "configure libvips"

# -Dpng=disabled + -Dspng=enabled: libvips prefers libpng when it is present, but
# Ubuntu's stock libvips42t64 is built against libspng ("selected quantisation
# package: imagequant", "PNG load/save with libspng: true"). Pinning spng keeps
# PNG behaviour identical to the image we are replacing rather than silently
# switching PNG codecs as a side effect of this change.
meson setup "$MESON_BUILD_DIR" "$SRC_DIR" \
	--prefix="$VIPS_PREFIX" \
	--libdir=lib \
	--buildtype=release \
	-Dmagick=disabled \
	-Dopenexr=disabled \
	-Dpng=disabled \
	-Dspng=enabled \
	-Djpeg=enabled \
	-Dtiff=enabled \
	-Dwebp=enabled \
	-Dheif=enabled \
	-Djpeg-xl=enabled \
	-Dopenjpeg=enabled \
	-Dcgif=enabled \
	-Drsvg=enabled \
	-Dpoppler=enabled \
	-Dpdfium=disabled \
	-Dlcms=enabled \
	-Dexif=enabled \
	-Dfontconfig=enabled \
	-Dpangocairo=enabled \
	-Darchive=enabled \
	-Dfftw=enabled \
	-Dmatio=enabled \
	-Dopenslide=enabled \
	-Dcfitsio=enabled \
	-Dhighway=enabled \
	-Dorc=disabled \
	-Dimagequant=enabled \
	-Dquantizr=disabled \
	-Dnifti=disabled \
	-Draw=disabled \
	-Dmodules=enabled \
	-Dintrospection=disabled \
	-Ddocs=false \
	-Dcpp-docs=false \
	-Dexamples=false \
	-Dcplusplus=false

# ---------------------------------------------------------------------------
# 3. Build and install.
# ---------------------------------------------------------------------------
log "build libvips"
ninja -C "$MESON_BUILD_DIR"
ninja -C "$MESON_BUILD_DIR" install

# Register the prefix with the dynamic loader. Without this, nothing can resolve
# libvips.so.42 from a custom prefix: the dynamic modules and the CLI all fail
# with "not found", and the FFM binding would fail to load at runtime. This file
# is copied into the runtime images (which rerun ldconfig, since they build on a
# plain ubuntu base rather than inheriting this one).
echo "${VIPS_PREFIX}/lib" >/etc/ld.so.conf.d/libvips.conf
ldconfig

# Strip development files from the prefix. Downstream images copy this whole
# tree, so headers, man pages, pkg-config metadata and any static archive are
# dead weight in a runtime image. (The vips CLI is kept deliberately: the
# verifier and the release validation both use it.)
rm -rf "${VIPS_PREFIX}/include" "${VIPS_PREFIX}/share/man" "${VIPS_PREFIX}/lib/pkgconfig"
find "$VIPS_PREFIX" -name '*.a' -delete

# ---------------------------------------------------------------------------
# 4. Generate the runtime package manifest.
#
# The runtime image must not install Ubuntu's libvips42t64 (that is what drags
# magickcore and openexr in), so it cannot lean on apt to resolve libvips'
# dependencies. We derive the exact set from the resolved ELF dependencies of
# everything we built - the CLI, the library, and every dynamic module.
# ---------------------------------------------------------------------------
log "generate runtime package manifest"

MANIFEST="${VIPS_PREFIX}/share/runtime-packages.txt"
mkdir -p "$(dirname "$MANIFEST")"

declare -A PKG_SET=()
UNRESOLVED=()
UNOWNED=()

collect_libs() {
	local elf
	while IFS= read -r elf; do
		[ -n "$elf" ] || continue
		# ldd lines look like: "libfoo.so.1 => /usr/lib/.../libfoo.so.1 (0x...)"
		while IFS= read -r line; do
			local resolved
			resolved="$(printf '%s' "$line" | awk '{for(i=1;i<=NF;i++) if($i=="=>"){print $(i+1); exit}}')"
			[ -n "$resolved" ] || continue

			if [ "$resolved" = "not" ]; then
				UNRESOLVED+=("${elf##*/}: $(printf '%s' "$line" | tr -s ' ')")
				continue
			fi

			case "$resolved" in
			"$VIPS_PREFIX"/*) continue ;;
			esac

			local abs owner
			abs="$(readlink -f "$resolved" 2>/dev/null || printf '%s' "$resolved")"
			owner="$(dpkg -S "$abs" 2>/dev/null | head -1 | cut -d: -f1 || true)"
			if [ -z "$owner" ]; then
				UNOWNED+=("$abs")
				continue
			fi
			PKG_SET["$owner"]=1
		done < <(ldd "$elf" 2>/dev/null | grep '=>' || true)
	done < <(find "$VIPS_PREFIX" -type f \( -name '*.so' -o -name '*.so.*' \) -o -type f -path "${VIPS_PREFIX}/bin/*")
}

collect_libs

if [ "${#UNRESOLVED[@]}" -gt 0 ]; then
	echo "FATAL: unresolved ELF dependencies in the built prefix:" >&2
	printf '  %s\n' "${UNRESOLVED[@]}" >&2
	exit 1
fi
if [ "${#UNOWNED[@]}" -gt 0 ]; then
	echo "FATAL: external libraries with no owning dpkg package:" >&2
	printf '  %s\n' "${UNOWNED[@]}" >&2
	exit 1
fi

{
	printf '%s\n' "${!PKG_SET[@]}" "${LIBHEIF_PLUGIN_PACKAGES[@]}"
} | sort -u >"$MANIFEST"

# Also refuse to emit a manifest that would reintroduce the vulnerable packages:
# if any of these appear, the build linked a delegate we thought we disabled.
FORBIDDEN_IN_MANIFEST=(imagemagick imagemagick-6-common libmagickcore-6.q16-7t64 libopenexr-3-1-30 libvips42 libvips42t64)
for forbidden in "${FORBIDDEN_IN_MANIFEST[@]}"; do
	if grep -qx "$forbidden" "$MANIFEST"; then
		echo "FATAL: runtime manifest would install forbidden package: ${forbidden}" >&2
		exit 1
	fi
done

echo "runtime packages (${#PKG_SET[@]} discovered + ${#LIBHEIF_PLUGIN_PACKAGES[@]} explicit plugins):"
sed 's/^/  /' "$MANIFEST"

# ---------------------------------------------------------------------------
# 5. Record provenance. A source-built library is not covered by Ubuntu's
#    security updates, so what we built and how must be discoverable later.
# ---------------------------------------------------------------------------
log "record provenance"

"$VIPS_PREFIX/bin/vips" --vips-config >"${VIPS_PREFIX}/share/vips-config.txt" 2>&1 || true

{
	echo "libvips_version: ${LIBVIPS_VERSION}"
	echo "source_url: ${LIBVIPS_URL}"
	echo "source_sha256: ${LIBVIPS_SHA256}"
	echo "prefix: ${VIPS_PREFIX}"
	echo "built_at: $(date -u +%Y-%m-%dT%H:%M:%SZ)"
	echo "delegates_disabled: magick openexr pdfium"
	echo "delegate_notes: png backend pinned to spng to match the stock ubuntu libvips42 build"
} >"${VIPS_PREFIX}/share/build-info.txt"

# Ship the verifier inside the prefix so downstream images inherit it with the
# library and do not need a second COPY of the script.
install -m 0755 "${SCRIPT_DIR}/verify-libvips.sh" "${VIPS_PREFIX}/share/verify-libvips.sh"

# ---------------------------------------------------------------------------
# 6. Install the dlopened codec plugin packages and protect the runtime set.
#
# libheif loads its codec plugins with dlopen, so ELF discovery cannot see them
# (section 4 lists them explicitly for this reason) and they must be installed
# here as well or the acceptance gate below cannot actually exercise AVIF.
#
# apt-mark manual keeps the runtime packages alive when the image build purges
# the auto-installed build dependencies with apt autoremove.
# ---------------------------------------------------------------------------
log "install libheif codec plugins"
apt-get install -y --no-install-recommends "${LIBHEIF_PLUGIN_PACKAGES[@]}"

# shellcheck disable=SC2046 # intentional word splitting: the manifest is a package list
apt-mark manual $(cat "$MANIFEST") >/dev/null 2>&1 || true

# ---------------------------------------------------------------------------
# 7. Acceptance gate. Same script the runtime image and CI use, so the builder
#    cannot produce an artifact that would fail later.
# ---------------------------------------------------------------------------
log "verify"
VIPS_PREFIX="$VIPS_PREFIX" EXPECTED_VIPS_VERSION="$LIBVIPS_VERSION" \
	bash "${SCRIPT_DIR}/verify-libvips.sh"

# ---------------------------------------------------------------------------
# 8. Drop build inputs. Apt packages are purged by the image build, which owns
#    the layering; we just remove the tarball, source tree and build dir.
# ---------------------------------------------------------------------------
log "clean up build inputs"
rm -rf "$SRC_DIR" "$MESON_BUILD_DIR" "$TARBALL"

log "libvips ${LIBVIPS_VERSION} installed to ${VIPS_PREFIX}"
