#!/usr/bin/env bash
#
# publish-to-s3/publish.sh
#
# Publishes build artifacts to the dotCMS BunnyCDN S3-compatible storage zone.
# This replaces the Artifactory deployments (repo.dotcms.com).
#
# Two sub-commands:
#
#   maven   Publish one version of every com/dotcms module found in a local
#           Maven repository (~/.m2/repository), preserving the
#           groupId/artifactId/version layout, and (re)generate the
#           maven-metadata.xml files Artifactory used to create.
#
#   file    Upload a single file to an explicit key. Used for artifacts that do
#           not live in a Maven repository (e.g. the starter zips).
#
# BunnyCDN convention: the S3 access key id IS the storage-zone name, so when
# MAVEN_S3_BUCKET is not set it defaults to MAVEN_BUNNY_RW_USERNAME.
#
# Environment (all optional, CLI flags take precedence):
#   MAVEN_BUNNY_RW_USERNAME    Bunny storage-zone name  / S3 access key id
#   MAVEN_BUNNY_RW_PASSWORD    Bunny storage-zone password / S3 secret key
#   MAVEN_S3_BUCKET            Bucket (storage zone). Defaults to the username.
#   MAVEN_S3_PREFIX            Key prefix. Default: libs-release
#   MAVEN_S3_ENDPOINT          Default: https://ny-s3.storage.bunnycdn.com
#   MAVEN_S3_REGION            Default: ny
#   MAVEN_S3_PUBLIC_URL        Public CDN base, used for printed download URLs.
#                              Default: https://dotcms-repo.b-cdn.net
#   MAVEN_REPO_DIR             Default: $HOME/.m2/repository
#   MAVEN_S3_EXCLUDE_EXT       Extra extensions to skip. Default: repositories,excludeext
#   MAVEN_S3_UPDATE_METADATA   Default: true
#   MAVEN_S3_CHECKSUMS         Default: true
#   MAVEN_S3_ALLOW_SNAPSHOTS   Default: false (shared snapshots are not consumed)
#
# Exit codes: 0 success, 1 error.

set -euo pipefail

S3_ENDPOINT="${MAVEN_S3_ENDPOINT:-https://ny-s3.storage.bunnycdn.com}"
S3_REGION="${MAVEN_S3_REGION:-ny}"
S3_PREFIX="${MAVEN_S3_PREFIX:-libs-release}"
S3_BUCKET="${MAVEN_S3_BUCKET:-${MAVEN_BUNNY_RW_USERNAME:-}}"
S3_PUBLIC_URL="${MAVEN_S3_PUBLIC_URL:-https://dotcms-repo.b-cdn.net}"
MAVEN_REPO_DIR="${MAVEN_REPO_DIR:-$HOME/.m2/repository}"
EXCLUDE_EXT="${MAVEN_S3_EXCLUDE_EXT:-repositories,excludeext}"
UPDATE_METADATA="${MAVEN_S3_UPDATE_METADATA:-true}"
CHECKSUMS="${MAVEN_S3_CHECKSUMS:-true}"
ALLOW_SNAPSHOTS="${MAVEN_S3_ALLOW_SNAPSHOTS:-false}"

# Resolve S3 credentials. Org secrets are exposed under the MAVEN_BUNNY_* names;
# the AWS CLI reads the standard AWS_* variables. These must be exported so the
# `aws` child process inherits them.
if [[ -z "${AWS_ACCESS_KEY_ID:-}" && -n "${MAVEN_BUNNY_RW_USERNAME:-}" ]]; then
  AWS_ACCESS_KEY_ID="$MAVEN_BUNNY_RW_USERNAME"
fi
if [[ -z "${AWS_SECRET_ACCESS_KEY:-}" && -n "${MAVEN_BUNNY_RW_PASSWORD:-}" ]]; then
  AWS_SECRET_ACCESS_KEY="$MAVEN_BUNNY_RW_PASSWORD"
fi
export AWS_ACCESS_KEY_ID AWS_SECRET_ACCESS_KEY
export AWS_DEFAULT_REGION="$S3_REGION"

log() { printf '::notice::%s\n' "$*"; }
warn() { printf '::warning::%s\n' "$*" >&2; }
die() { printf '::error::%s\n' "$*" >&2; exit 1; }

# Temp dirs are cleaned up once, on exit. A per-function RETURN trap would fire
# on every function return (including the aws_s3 helper) and delete the staging
# directory before it has been uploaded.
CLEANUP_DIRS=()
cleanup() {
  local d
  for d in ${CLEANUP_DIRS[@]+"${CLEANUP_DIRS[@]}"}; do
    [[ -n "$d" ]] && rm -rf "$d"
  done
}
trap cleanup EXIT

usage() {
  cat <<'EOF'
Usage:
  publish.sh maven --version <version> [options]
  publish.sh file  --source <path> --key <key> [options]

Common options:
  --bucket <name>       S3 bucket / storage zone      (default: $MAVEN_BUNNY_RW_USERNAME)
  --prefix <path>       Key prefix                    (default: libs-release)
  --endpoint <url>      S3 endpoint                   (default: https://ny-s3.storage.bunnycdn.com)
  --region <region>     S3 signing region             (default: ny)
  --dry-run             Print what would be uploaded; do not write
  --no-metadata         (maven) Do not (re)generate maven-metadata.xml
  --no-checksums        (maven) Do not upload .sha1/.md5 checksums
  --allow-snapshots     (maven) Publish a -SNAPSHOT version (default: refuse)

maven options:
  --version <version>   Version subtree to publish (required)
  --repo-dir <path>     Local Maven repository      (default: $HOME/.m2/repository)
  --modules <a,b>       Restrict to these artifactIds (default: every com/dotcms module
                        that has a directory for <version>)
  --exclude-ext <a,b>   Extra file extensions to skip (default: repositories,excludeext)

file options:
  --source <path>       File to upload (required)
  --key <key>           Destination key relative to the prefix (required)
EOF
}

# Thin wrapper so every call shares the endpoint, region and credentials.
aws_s3() {
  aws --endpoint-url "$S3_ENDPOINT" --region "$S3_REGION" s3 "$@"
}

require_credentials() {
  if [[ "${DRY_RUN:-false}" == "true" ]]; then
    # Dry-runs are useful standalone (no secrets), but aws still needs a bucket
    # to build a valid s3:// destination, so fall back to a placeholder.
    : "${S3_BUCKET:=dry-run-bucket}"
    return 0
  fi
  [[ -n "$S3_BUCKET" ]] || die "Bucket is empty. Set MAVEN_S3_BUCKET or MAVEN_BUNNY_RW_USERNAME."
  [[ -n "${AWS_ACCESS_KEY_ID:-}" ]] || die "Missing MAVEN_BUNNY_RW_USERNAME / AWS_ACCESS_KEY_ID."
  [[ -n "${AWS_SECRET_ACCESS_KEY:-}" ]] || die "Missing MAVEN_BUNNY_RW_PASSWORD / AWS_SECRET_ACCESS_KEY."
}

# Emits "key=value" lines to $GITHUB_OUTPUT when running inside Actions.
emit_output() {
  local name="$1" value="$2"
  if [[ -n "${GITHUB_OUTPUT:-}" ]]; then
    printf '%s=%s\n' "$name" "$value" >> "$GITHUB_OUTPUT"
  fi
}

# ---------------------------------------------------------------------------
# maven
# ---------------------------------------------------------------------------

# extra_args are bash-glob exclusions derived from --exclude-ext.
build_exclude_args() {
  EXCLUDE_ARGS=(
    --exclude "*.lastUpdated"
    --exclude "resolver-status.properties"
    --exclude "maven-metadata-local.xml"
    --exclude "*.sha1"
    --exclude "*.md5"
    --exclude "*.sha256"
  )
  if [[ -n "$EXCLUDE_EXT" ]]; then
    local ext
    IFS=',' read -ra _exts <<< "$EXCLUDE_EXT"
    for ext in "${_exts[@]}"; do
      [[ -n "$ext" ]] && EXCLUDE_ARGS+=(--exclude "*.$ext")
    done
  fi
}

# Uploads .sha1/.md5 checksums for the primary artifacts of one version dir.
upload_checksums() {
  local src_dir="$1" dest="$2" tmp rel
  tmp="$(mktemp -d)"
  CLEANUP_DIRS+=("$tmp")

  while IFS= read -r -d '' rel; do
    mkdir -p "$tmp/$(dirname "$rel")"
    sha1sum "$src_dir/$rel" | awk '{print $1}' > "$tmp/$rel.sha1"
    md5sum "$src_dir/$rel" | awk '{print $1}' > "$tmp/$rel.md5"
  done < <(cd "$src_dir" && find . -type f \
      \( -name '*.pom' -o -name '*.jar' -o -name '*.zip' \
         -o -name '*.war' -o -name '*.aar' -o -name '*.module' \) -print0)

  local args=(--recursive --no-progress)
  if [[ "${DRY_RUN:-false}" == "true" ]]; then
    args+=(--dryrun)
  fi
  aws_s3 cp "$tmp" "$dest" "${args[@]}"
}

# Rebuilds com/dotcms/<artifactId>/maven-metadata.xml from the versions that
# already exist in the bucket plus the version just uploaded. Best-effort: a
# failure here must not fail the publish, because the artifacts themselves are
# already in place.
update_artifact_metadata() {
  local artifact="$1" version="$2" ts
  ts="$(date -u +%Y%m%d%H%M%S)"
  local base="$S3_PREFIX/com/dotcms/$artifact"
  local tmp versions latest release v

  tmp="$(mktemp -d)"
  CLEANUP_DIRS+=("$tmp")

  # `aws s3 ls` prints directory entries as `PRE <name>/` and file entries as
  # `<date> <time> <size> <name>`. Only PRE lines are versions; using $2 on a
  # file line would pick up the timestamp and corrupt the version list (and
  # therefore <latest>/<release>) on every publish after the first.
  versions="$(
    aws_s3 ls "s3://$S3_BUCKET/$base/" 2>/dev/null \
      | awk '$1 == "PRE" {print $2}' | sed 's:/$::' || true
  )"
  versions="$(printf '%s\n%s\n' "$versions" "$version" | sed '/^$/d' | sort -u -V)"

  latest="$(printf '%s\n' "$versions" | tail -n1)"
  release="$(printf '%s\n' "$versions" | grep -v -- '-SNAPSHOT$' | tail -n1 || true)"
  [[ -n "$release" ]] || release="$latest"

  {
    printf '<metadata modelVersion="1.1.0">\n'
    printf '  <groupId>com.dotcms</groupId>\n'
    printf '  <artifactId>%s</artifactId>\n' "$artifact"
    printf '  <versioning>\n'
    printf '    <latest>%s</latest>\n' "$latest"
    printf '    <release>%s</release>\n' "$release"
    printf '    <versions>\n'
    while IFS= read -r v; do
      [[ -n "$v" ]] && printf '      <version>%s</version>\n' "$v"
    done <<< "$versions"
    printf '    </versions>\n'
    printf '    <lastUpdated>%s</lastUpdated>\n' "$ts"
    printf '  </versioning>\n'
    printf '</metadata>\n'
  } > "$tmp/maven-metadata.xml"

  # Artifactory also published checksums for the metadata. Maven's default
  # checksum policy is `warn`, but consumers configured with checksumPolicy=fail
  # require these.
  sha1sum "$tmp/maven-metadata.xml" | awk '{print $1}' > "$tmp/maven-metadata.xml.sha1"
  md5sum "$tmp/maven-metadata.xml" | awk '{print $1}' > "$tmp/maven-metadata.xml.md5"

  local xml_args=(--no-progress --content-type application/xml)
  local sum_args=(--no-progress --content-type text/plain)
  if [[ "${DRY_RUN:-false}" == "true" ]]; then
    xml_args+=(--dryrun)
    sum_args+=(--dryrun)
  fi
  aws_s3 cp "$tmp/maven-metadata.xml" "s3://$S3_BUCKET/$base/maven-metadata.xml" "${xml_args[@]}"
  aws_s3 cp "$tmp/maven-metadata.xml.sha1" "s3://$S3_BUCKET/$base/maven-metadata.xml.sha1" "${sum_args[@]}"
  aws_s3 cp "$tmp/maven-metadata.xml.md5" "s3://$S3_BUCKET/$base/maven-metadata.xml.md5" "${sum_args[@]}"
}

# Non-unique SNAPSHOT deployments would need a version-level maven-metadata.xml
# so Maven can resolve -SNAPSHOT coordinates. We do not publish snapshots to the
# shared repository (see cmd_maven), so this is intentionally not implemented.

cmd_maven() {
  local version="" modules="" dry_run="$DRY_RUN"
  while [[ $# -gt 0 ]]; do
    case "$1" in
      --version) version="${2:?}"; shift 2 ;;
      --repo-dir) MAVEN_REPO_DIR="${2:?}"; shift 2 ;;
      --modules) modules="${2:-}"; shift 2 ;;
      --exclude-ext) EXCLUDE_EXT="${2:-}"; shift 2 ;;
      --bucket) S3_BUCKET="${2:?}"; shift 2 ;;
      --prefix) S3_PREFIX="${2:?}"; shift 2 ;;
      --endpoint) S3_ENDPOINT="${2:?}"; shift 2 ;;
      --region) S3_REGION="${2:?}"; shift 2 ;;
      --dry-run) dry_run=true; shift ;;
      --no-metadata) UPDATE_METADATA=false; shift ;;
      --no-checksums) CHECKSUMS=false; shift ;;
      --allow-snapshots) ALLOW_SNAPSHOTS=true; shift ;;
      -h|--help) usage; exit 0 ;;
      *) die "Unknown maven option: $1" ;;
    esac
  done
  DRY_RUN="$dry_run"

  [[ -n "$version" ]] || die "--version is required"

  # No shared snapshots are consumed at dotCMS. Publishing one would also make
  # the artifact-level maven-metadata.xml <latest> a snapshot, breaking the
  # release lookup the CLI action performs. Refuse unless explicitly allowed.
  if [[ "$version" == *-SNAPSHOT && "$ALLOW_SNAPSHOTS" != "true" ]]; then
    warn "Skipping snapshot version '$version': shared snapshots are not published (pass --allow-snapshots to override)."
    return 0
  fi

  require_credentials

  local group_dir="$MAVEN_REPO_DIR/com/dotcms"
  [[ -d "$group_dir" ]] || die "No com/dotcms directory under $MAVEN_REPO_DIR"

  local artifacts=() artifact
  if [[ -n "$modules" ]]; then
    local wanted
    IFS=',' read -ra wanted <<< "$modules"
    for artifact in "${wanted[@]}"; do
      artifact="${artifact//[[:space:]]/}"
      [[ -n "$artifact" ]] || continue
      if [[ -d "$group_dir/$artifact/$version" ]]; then
        artifacts+=("$artifact")
      else
        warn "Module $artifact has no $version directory; skipping."
      fi
    done
  else
    shopt -s nullglob
    local dirs=("$group_dir"/*/"$version")
    shopt -u nullglob
    for dir in "${dirs[@]}"; do
      [[ -d "$dir" ]] && artifacts+=("$(basename "$(dirname "$dir")")")
    done
  fi

  if [[ ${#artifacts[@]} -eq 0 ]]; then
    # An empty publish is only legitimate for the intentionally-skipped
    # conditionals (e.g. java variants), so keep it non-fatal but loud.
    warn "No com/dotcms artifacts found for version '$version' under $MAVEN_REPO_DIR."
    return 0
  fi

  log "Publishing ${#artifacts[@]} artifact(s) for version '$version' to s3://$S3_BUCKET/$S3_PREFIX/com/dotcms/"
  build_exclude_args

  local dest upload_args
  for artifact in "${artifacts[@]}"; do
    local src="$group_dir/$artifact/$version"
    dest="s3://$S3_BUCKET/$S3_PREFIX/com/dotcms/$artifact/$version/"
    upload_args=(--recursive --no-progress "${EXCLUDE_ARGS[@]}")
    [[ "$DRY_RUN" == "true" ]] && upload_args+=(--dryrun)

    log "  $artifact:$version -> $dest"
    aws_s3 cp "$src" "$dest" "${upload_args[@]}"

    if [[ "$CHECKSUMS" == "true" ]]; then
      upload_checksums "$src" "$dest" || warn "Checksum upload failed for $artifact:$version."
    fi

    if [[ "$UPDATE_METADATA" == "true" ]]; then
      update_artifact_metadata "$artifact" "$version" \
        || warn "maven-metadata.xml update failed for $artifact."
    fi
  done

  log "Published version '$version'."
}

# ---------------------------------------------------------------------------
# file
# ---------------------------------------------------------------------------

cmd_file() {
  local source="" key="" dry_run="$DRY_RUN"
  while [[ $# -gt 0 ]]; do
    case "$1" in
      --source) source="${2:?}"; shift 2 ;;
      --key) key="${2:?}"; shift 2 ;;
      --bucket) S3_BUCKET="${2:?}"; shift 2 ;;
      --prefix) S3_PREFIX="${2:?}"; shift 2 ;;
      --endpoint) S3_ENDPOINT="${2:?}"; shift 2 ;;
      --region) S3_REGION="${2:?}"; shift 2 ;;
      --dry-run) dry_run=true; shift ;;
      -h|--help) usage; exit 0 ;;
      *) die "Unknown file option: $1" ;;
    esac
  done
  DRY_RUN="$dry_run"

  [[ -n "$source" ]] || die "--source is required"
  [[ -f "$source" ]] || die "Source file not found: $source"
  [[ -n "$key" ]] || die "--key is required"
  key="${key#/}"
  require_credentials

  local dest="s3://$S3_BUCKET/$S3_PREFIX/$key"
  local args=(--no-progress)
  [[ "$DRY_RUN" == "true" ]] && args+=(--dryrun)

  log "Uploading $source -> $dest"
  aws_s3 cp "$source" "$dest" "${args[@]}"

  local public_url="$S3_PUBLIC_URL/$S3_PREFIX/$key"
  log "Artifact URL: $public_url"
  emit_output "filename" "$(basename "$source")"
  emit_output "key" "$key"
  emit_output "url" "$public_url"
}

# ---------------------------------------------------------------------------

DRY_RUN="${MAVEN_S3_DRY_RUN:-false}"

cmd="${1:-}"
[[ $# -gt 0 ]] && shift || true

case "$cmd" in
  maven) cmd_maven "$@" ;;
  file) cmd_file "$@" ;;
  -h|--help|"") usage; exit 0 ;;
  *) usage >&2; die "Unknown command: $cmd" ;;
esac