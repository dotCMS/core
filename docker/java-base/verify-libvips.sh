#!/usr/bin/env bash
#
# verify-libvips.sh - acceptance checks for the source-built libvips used by the
# dotCMS runtime image.
#
# This is the native counterpart to VipsParityTest: it proves that the libvips we
# ship is present, complete, and free of the ImageMagick/OpenEXR delegates and
# packages called out in the CVE report. It is deliberately independent of Java so
# it can run in the java-base builder, in the runtime image, and on a CI runner.
#
# Usage:
#   verify-libvips.sh
#   VIPS_PREFIX=/opt/libvips verify-libvips.sh
#   EXPECTED_VIPS_VERSION=8.18.6 VIPS_PREFIX=/usr/local/libvips verify-libvips.sh
#
# Exit status is non-zero if any check fails.
#
# Rationale for the two "forbidden" checks: the reported CVEs live in
# libmagickcore-6.q16-7t64 and libopenexr-3-1-30, which reach the image only as
# hard dependencies of Ubuntu's libvips42t64. We build libvips ourselves with
# -Dmagick=disabled -Dopenexr=disabled, so these must be absent. Their presence
# means the build silently linked a delegate we did not want.

set -uo pipefail

VIPS_PREFIX="${VIPS_PREFIX:-/usr/local/libvips}"
VIPS_BIN="${VIPS_BIN:-${VIPS_PREFIX}/bin/vips}"
EXPECTED_VIPS_VERSION="${EXPECTED_VIPS_VERSION:-8.18.6}"

FAILURES=0
CHECKS=0

pass() { CHECKS=$((CHECKS + 1)); printf '  ok    %s\n' "$*"; }
fail() { CHECKS=$((CHECKS + 1)); FAILURES=$((FAILURES + 1)); printf '  FAIL  %s\n' "$*"; }
section() { printf '\n== %s\n' "$*"; }

TMPDIR_RUN="$(mktemp -d)"
cleanup() { rm -rf "$TMPDIR_RUN"; }
trap cleanup EXIT

echo "verify-libvips: prefix=${VIPS_PREFIX} bin=${VIPS_BIN} expect=${EXPECTED_VIPS_VERSION}"

# ---------------------------------------------------------------------------
# 1. The prefix and CLI are present and report the pinned version.
# ---------------------------------------------------------------------------
section "libvips install"

if [ -x "$VIPS_BIN" ]; then
	pass "vips CLI present: ${VIPS_BIN}"
else
	fail "vips CLI missing or not executable: ${VIPS_BIN}"
fi

ACTUAL_VERSION=""
if [ -x "$VIPS_BIN" ]; then
	ACTUAL_VERSION="$("$VIPS_BIN" --version 2>/dev/null | head -1 | tr -d '[:space:]' | sed 's/^vips-//')"
	if [ "$ACTUAL_VERSION" = "$EXPECTED_VIPS_VERSION" ]; then
		pass "version is ${ACTUAL_VERSION}"
	else
		fail "version is '${ACTUAL_VERSION}', expected '${EXPECTED_VIPS_VERSION}'"
	fi
fi

# The FFM binding loads libvips.so.42 by soname; a different soname breaks the
# Java side with no useful error at build time.
if compgen -G "${VIPS_PREFIX}/lib/libvips.so.42*" >/dev/null 2>&1; then
	pass "soname libvips.so.42 present (matches the vips-ffm FFM binding)"
else
	fail "no libvips.so.42* under ${VIPS_PREFIX}/lib (FFM binding would fail to load)"
fi

# ---------------------------------------------------------------------------
# 2. Forbidden packages. The reason this whole change exists.
# ---------------------------------------------------------------------------
section "forbidden packages"

FORBIDDEN_PACKAGES=(
	imagemagick
	imagemagick-6-common
	libmagickcore-6.q16-7t64
	libopenexr-3-1-30
	libvips42
	libvips42t64
)

if command -v dpkg-query >/dev/null 2>&1; then
	for pkg in "${FORBIDDEN_PACKAGES[@]}"; do
		if dpkg-query -W -f='${Status}' "$pkg" 2>/dev/null | grep -q 'install ok installed'; then
			fail "package installed: ${pkg}"
		else
			pass "package absent: ${pkg}"
		fi
	done
else
	printf '  skip  dpkg-query unavailable; package checks skipped (not a dpkg image?)\n'
fi

# ---------------------------------------------------------------------------
# 3. Forbidden libraries on disk. Catches a delegate pulled in by another path
#    even if the package it came from was not one we listed above.
# ---------------------------------------------------------------------------
section "forbidden shared libraries"

FORBIDDEN_LIB_GLOBS=('libMagickCore*' 'libMagickWand*' 'libOpenEXR*' 'libIlmThread*' 'libImath*')
for glob in "${FORBIDDEN_LIB_GLOBS[@]}"; do
	found="$(find /usr/lib /usr/lib64 /lib /lib64 -maxdepth 3 -name "$glob" 2>/dev/null | head -5)"
	if [ -n "$found" ]; then
		fail "forbidden library present (${glob}): $(echo "$found" | tr '\n' ' ')"
	else
		pass "no ${glob}"
	fi
done

# ---------------------------------------------------------------------------
# 4. Delegate configuration. Disabled delegates render as 'false' rather than
#    disappearing, so both directions are assertable.
# ---------------------------------------------------------------------------
section "delegate configuration (vips --vips-config)"

CONFIG_OUT=""
if [ -x "$VIPS_BIN" ]; then
	CONFIG_OUT="$("$VIPS_BIN" --vips-config 2>&1)"
fi

config_has_true() {
	printf '%s\n' "$CONFIG_OUT" | grep -Eq "^${1}.*: true"
}
config_has_any() {
	printf '%s\n' "$CONFIG_OUT" | grep -Eq "^${1}"
}

# Must be present-but-false, or absent entirely: never true.
if [ -z "$CONFIG_OUT" ]; then
	fail "could not read --vips-config output"
else
	if config_has_true 'Magick'; then
		fail "Magick delegate is enabled: $(printf '%s\n' "$CONFIG_OUT" | grep -E '^Magick' | tr '\n' '; ')"
	else
		pass "Magick delegate disabled"
	fi

	if config_has_true 'EXR load'; then
		fail "OpenEXR delegate is enabled: $(printf '%s\n' "$CONFIG_OUT" | grep -E '^EXR load' | tr '\n' '; ')"
	else
		pass "OpenEXR delegate disabled"
	fi

	# Capabilities dotCMS advertises and must keep.
	REQUIRED_DELEGATES=(
		'enable GIF load'
		'enable modules'
		'JPEG load/save'
		'PNG load/save'
		'TIFF load/save'
		'WebP load/save'
		'HEIC/AVIF load/save'
		'JXL load/save'
		'JPEG2000 load/save'
		'PDF load'
		'SVG load'
		'GIF save'
		'EXIF metadata support'
		'ICC profile support'
		'text rendering'
		'font file support'
	)
	for delegate in "${REQUIRED_DELEGATES[@]}"; do
		if config_has_true "$delegate"; then
			pass "delegate enabled: ${delegate}"
		elif config_has_any "$delegate"; then
			fail "delegate present but NOT enabled: $(printf '%s\n' "$CONFIG_OUT" | grep -E "^${delegate}" | head -1)"
		else
			fail "delegate missing from build entirely: ${delegate}"
		fi
	done
fi

# ---------------------------------------------------------------------------
# 5. No magick/EXR operations registered, and the ones we need are.
# ---------------------------------------------------------------------------
section "registered operations (vips -l)"

OPS_OUT=""
if [ -x "$VIPS_BIN" ]; then
	OPS_OUT="$("$VIPS_BIN" -l 2>&1)"
fi

if [ -z "$OPS_OUT" ]; then
	fail "could not list operations"
else
	for op in magickload magicksave exrload; do
		if printf '%s\n' "$OPS_OUT" | grep -qw "$op"; then
			fail "forbidden operation still registered: ${op}"
		else
			pass "operation absent: ${op}"
		fi
	done

	for op in heifload heifsave pdfload svgload jpegload pngload webpload tiffload jp2kload jxlload gifsave webpsave; do
		if printf '%s\n' "$OPS_OUT" | grep -qw "$op"; then
			pass "operation present: ${op}"
		else
			fail "operation missing: ${op}"
		fi
	done
fi

# ---------------------------------------------------------------------------
# 6. ELF hygiene: nothing links magick/EXR, and nothing is unresolved.
# ---------------------------------------------------------------------------
section "ELF linkage"

if [ -d "$VIPS_PREFIX" ] && command -v ldd >/dev/null 2>&1; then
	elf_libs="$(find "$VIPS_PREFIX" -type f \( -name '*.so' -o -name '*.so.*' \) 2>/dev/null)"
	if [ -z "$elf_libs" ]; then
		fail "no shared libraries found under ${VIPS_PREFIX}"
	else
		bad_link=0
		unresolved=0
		while IFS= read -r lib; do
			[ -n "$lib" ] || continue
			out="$(ldd "$lib" 2>/dev/null)"
			if printf '%s\n' "$out" | grep -qiE 'libMagick|libOpenEXR'; then
				fail "links a forbidden library: ${lib##*/} -> $(printf '%s\n' "$out" | grep -iE 'libMagick|libOpenEXR' | tr '\n' ' ')"
				bad_link=1
			fi
			if printf '%s\n' "$out" | grep -q 'not found'; then
				fail "unresolved dependency in ${lib##*/}: $(printf '%s\n' "$out" | grep 'not found' | tr '\n' ' ')"
				unresolved=1
			fi
		done <<<"$elf_libs"
		[ "$bad_link" -eq 0 ] && pass "no shared library links MagickCore or OpenEXR"
		[ "$unresolved" -eq 0 ] && pass "no unresolved dependencies"
	fi
else
	printf '  skip  no prefix or no ldd; ELF linkage checks skipped\n'
fi

# ---------------------------------------------------------------------------
# 7. Representative native operations. Loader presence is not proof a delegate
#    actually works, so encode/decode a real round trip for each risky format.
# ---------------------------------------------------------------------------
section "native operations"

if [ -x "$VIPS_BIN" ]; then
	SRC="${TMPDIR_RUN}/src.v"
	if "$VIPS_BIN" black "$SRC" 64 64 --bands 3 >/dev/null 2>&1; then
		pass "created test image"
	else
		fail "could not create test image"
	fi

	roundtrip() {
		local ext="$1" out="${TMPDIR_RUN}/rt.${1}" back="${TMPDIR_RUN}/back-${1}.png" err
		# Deliberately no saver options: the CLI rejects some spellings (--Q is not
		# a valid switch), and this check is about whether the delegate works at all,
		# not about tuning output quality.
		if ! err="$("$VIPS_BIN" copy "$SRC" "$out" 2>&1)"; then
			fail "${ext}: encode failed: $(printf '%s' "$err" | head -1)"
			return
		fi
		if ! err="$("$VIPS_BIN" copy "$out" "$back" 2>&1)"; then
			fail "${ext}: decode failed: $(printf '%s' "$err" | head -1)"
			return
		fi
		pass "${ext}: encode + decode round trip"
	}

	# AVIF is the highest-risk built-in-only capability (libheif + an AV1 encoder
	# plugin that is dlopened, so it must be installed separately).
	roundtrip avif
	roundtrip webp
	roundtrip jxl
	roundtrip png
	roundtrip jpg
	roundtrip tif
	roundtrip jp2
	roundtrip gif

	# SVG needs a real document; it exercises librsvg (no magick involved).
	cat >"${TMPDIR_RUN}/t.svg" <<-'SVG'
		<svg xmlns="http://www.w3.org/2000/svg" width="32" height="32"><rect width="32" height="32" fill="#ff0000"/></svg>
	SVG
	if "$VIPS_BIN" copy "${TMPDIR_RUN}/t.svg" "${TMPDIR_RUN}/svg.png" >/dev/null 2>&1; then
		pass "svg: librsvg load"
	else
		fail "svg: librsvg load failed"
	fi
fi

# ---------------------------------------------------------------------------
section "result"
if [ "$FAILURES" -eq 0 ]; then
	echo "verify-libvips: PASS (${CHECKS} checks)"
	exit 0
fi
echo "verify-libvips: FAIL (${FAILURES} of ${CHECKS} checks failed)"
exit 1
