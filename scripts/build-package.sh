#!/usr/bin/env bash
# Builds a self-contained native package for the Arch Hub (bundles its own
# JRE via jpackage — no separate Java install needed to run it). Produces a
# portable app-image on every OS, plus a platform installer when the
# underlying packaging tool is available (dpkg-deb on Linux, or a .dmg on
# macOS). Mirrors scripts/build-exe.ps1 (the Windows equivalent).
#
# Usage:
#   scripts/build-package.sh [--clean] [--skip-smoke]
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DIST_DIR="$REPO_ROOT/dist-desktop"
APP_NAME="ArchHub"
SKIP_SMOKE=0

for arg in "$@"; do
  case "$arg" in
    --clean) rm -rf "$REPO_ROOT/target" "$DIST_DIR" ;;
    --skip-smoke) SKIP_SMOKE=1 ;;
    *) echo "Unknown argument: $arg" >&2; exit 1 ;;
  esac
done

cd "$REPO_ROOT"

echo "==> Resolving version"
VERSION="$(mvn -q -DforceStdout help:evaluate -Dexpression=project.version)"
echo "    version: $VERSION"

echo "==> Running tests and building the jar"
mvn -q clean package
JAR="$REPO_ROOT/target/arch-hub-${VERSION}.jar"
[[ -f "$JAR" ]] || { echo "Jar not found: $JAR" >&2; exit 1; }

rm -rf "$DIST_DIR"
mkdir -p "$DIST_DIR"

echo "==> Building the portable app-image"
jpackage \
  --input "$REPO_ROOT/target" \
  --main-jar "arch-hub-${VERSION}.jar" \
  --name "$APP_NAME" \
  --app-version "$VERSION" \
  --type app-image \
  --dest "$DIST_DIR" \
  --java-options "-Djava.awt.headless=true"

APP_IMAGE_DIR="$DIST_DIR/$APP_NAME"
LAUNCHER="$APP_IMAGE_DIR/bin/$APP_NAME"
[[ -x "$LAUNCHER" ]] || { echo "Launcher not found: $LAUNCHER" >&2; exit 1; }

if [[ "$SKIP_SMOKE" -eq 0 ]]; then
  echo "==> Smoke test (--version)"
  "$LAUNCHER" --version
fi

case "$(uname -s)" in
  Darwin) PKG_TYPE="dmg" ;;
  Linux)  PKG_TYPE="deb" ;;
  *) PKG_TYPE="" ;;
esac

if [[ -n "$PKG_TYPE" ]]; then
  echo "==> Building the installer ($PKG_TYPE) from the app-image"
  if jpackage \
      --app-image "$APP_IMAGE_DIR" \
      --name "$APP_NAME" \
      --app-version "$VERSION" \
      --type "$PKG_TYPE" \
      --dest "$DIST_DIR" 2>/tmp/jpackage-installer.log; then
    INSTALLER="$(find "$DIST_DIR" -maxdepth 1 -name "*.${PKG_TYPE}" | head -n1)"
    if [[ -n "$INSTALLER" ]]; then
      SHA256="$(sha256sum "$INSTALLER" | cut -d' ' -f1)"
      echo "$SHA256  $(basename "$INSTALLER")" > "$INSTALLER.sha256"
      echo "    installer: $INSTALLER"
      echo "    sha256:    $SHA256"
    fi
  else
    echo "    (installer step skipped: no $PKG_TYPE packaging tool found — dpkg-deb/rpmbuild)"
    tail -n 5 /tmp/jpackage-installer.log || true
  fi
fi

echo ""
echo "Done. Contents of $DIST_DIR:"
ls -la "$DIST_DIR"
