#!/bin/sh
set -eu
export LC_ALL=C
export LANG=C

PACKAGING_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
PROJECT_ROOT=$(CDPATH= cd -- "$PACKAGING_DIR/.." && pwd)
VERSION=$(tr -d '[:space:]' < "$PACKAGING_DIR/VERSION")
OUTPUT_DIR=${1:-"$PROJECT_ROOT/outputs"}

if [ "$(uname -m)" != "x86_64" ]; then
  echo "This release script currently produces the Intel macOS Homebrew artifact only." >&2
  echo "Build and test a separate arm64 runtime before publishing Apple Silicon support." >&2
  exit 1
fi

case "$OUTPUT_DIR" in
  /*) ;;
  *) OUTPUT_DIR="$PROJECT_ROOT/$OUTPUT_DIR" ;;
esac

if [ -z "${JAVA_HOME:-}" ]; then
  JAVA_HOME=$(/usr/libexec/java_home -v 21)
  export JAVA_HOME
fi

mkdir -p "$PROJECT_ROOT/work" "$OUTPUT_DIR"
STAGING_DIR=$(mktemp -d "$PROJECT_ROOT/work/homebrew-release.XXXXXX")
cleanup() { rm -rf "$STAGING_DIR"; }
trap cleanup EXIT INT TERM

"$PROJECT_ROOT/mvnw" -q clean verify

install -m 0644 "$PROJECT_ROOT/devdoctor-cli/target/devdoctor.jar" "$STAGING_DIR/devdoctor.jar"
install -m 0644 "$PROJECT_ROOT/devdoctor-agent-extension/target/devdoctor-agent.jar" "$STAGING_DIR/devdoctor-agent.jar"
install -m 0644 "$PROJECT_ROOT/devdoctor-agent-extension/target/devdoctor-agent-extension-$VERSION-SNAPSHOT.jar" "$STAGING_DIR/devdoctor-control-agent.jar"
install -m 0644 "$PROJECT_ROOT/README.md" "$STAGING_DIR/README.md"
install -m 0644 "$PROJECT_ROOT/LICENSE" "$STAGING_DIR/LICENSE"

MODULES=$("$JAVA_HOME/bin/jdeps" --multi-release 21 --ignore-missing-deps --print-module-deps "$STAGING_DIR/devdoctor.jar")
"$JAVA_HOME/bin/jlink" \
  --module-path "$JAVA_HOME/jmods" \
  --add-modules "$MODULES,jdk.attach,jdk.jcmd,jdk.jfr,jdk.crypto.ec" \
  --strip-debug \
  --no-header-files \
  --no-man-pages \
  --compress=zip-9 \
  --output "$STAGING_DIR/runtime"

find "$STAGING_DIR" -exec touch -t 202608170000 {} +
find "$STAGING_DIR" -type l -exec touch -h -t 202608170000 {} +

ARCHIVE="$OUTPUT_DIR/devdoctor-$VERSION-macos-x86_64.tar.gz"
ARCHIVE_TMP="$ARCHIVE.tmp"
TAR_TMP="$OUTPUT_DIR/devdoctor-$VERSION-macos-x86_64.tar.tmp"
(cd "$STAGING_DIR" && find LICENSE README.md devdoctor.jar devdoctor-agent.jar devdoctor-control-agent.jar runtime -print | LC_ALL=C sort > archive-files.txt)
COPYFILE_DISABLE=1 tar -cf "$TAR_TMP" --no-recursion -C "$STAGING_DIR" -T "$STAGING_DIR/archive-files.txt"
gzip -n -9 < "$TAR_TMP" > "$ARCHIVE_TMP"
rm -f "$TAR_TMP"
mv "$ARCHIVE_TMP" "$ARCHIVE"

SHA256=$(shasum -a 256 "$ARCHIVE" | awk '{print $1}')
echo "$SHA256  $(basename "$ARCHIVE")" > "$OUTPUT_DIR/SHA256SUMS"
if [ -n "${DEVDOCTOR_RELEASE_URL_BASE:-}" ]; then
  ARCHIVE_URL="${DEVDOCTOR_RELEASE_URL_BASE%/}/$(basename "$ARCHIVE")"
else
  ARCHIVE_URL="file://$ARCHIVE"
fi
HOMEPAGE_URL=${DEVDOCTOR_HOMEPAGE_URL:-"file://$PROJECT_ROOT/README.md"}

sed \
  -e "s|@VERSION@|$VERSION|g" \
  -e "s|@URL@|$ARCHIVE_URL|g" \
  -e "s|@HOMEPAGE@|$HOMEPAGE_URL|g" \
  -e "s|@SHA256@|$SHA256|g" \
  "$PACKAGING_DIR/homebrew/devdoctor.rb.in" > "$OUTPUT_DIR/devdoctor.rb"

echo "Release archive: $ARCHIVE"
echo "Homebrew formula: $OUTPUT_DIR/devdoctor.rb"
echo "Checksum manifest: $OUTPUT_DIR/SHA256SUMS"
echo "SHA-256: $SHA256"
