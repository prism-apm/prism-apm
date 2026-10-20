#!/usr/bin/env bash

set -euo pipefail

usage() {
  cat <<'EOF'
Usage: ./scripts/release/set-version.sh <version>

Example:
  ./scripts/release/set-version.sh 0.2.0-SNAPSHOT

Updates the main Maven reactor and the prism-apm-ui npm package.
Maven SNAPSHOT versions are mapped to npm development versions; for example,
0.2.0-SNAPSHOT becomes 0.2.0-dev in prism-apm-ui.
The prism-explorer project is not changed.
It does not build, commit, tag, or push anything.
EOF
}

if [[ "${1:-}" == "--help" || "${1:-}" == "-h" ]]; then
  usage
  exit 0
fi

if [[ $# -ne 1 ]]; then
  usage >&2
  exit 1
fi

version="$1"
numeric_identifier='(0|[1-9][0-9]*)'
non_numeric_identifier='[0-9A-Za-z-]*[A-Za-z-][0-9A-Za-z-]*'
prerelease_identifier="($numeric_identifier|$non_numeric_identifier)"
semver_pattern="^$numeric_identifier\.$numeric_identifier\.$numeric_identifier(-$prerelease_identifier(\.$prerelease_identifier)*)?(\+[0-9A-Za-z-]+(\.[0-9A-Za-z-]+)*)?$"

if [[ ! "$version" =~ $semver_pattern ]]; then
  echo "Invalid version: $version" >&2
  echo "Expected a semantic version such as 0.1.0-RC1." >&2
  echo "Numeric identifiers must not contain leading zeroes." >&2
  exit 1
fi

ui_version="$version"
if [[ "$version" == *-SNAPSHOT ]]; then
  ui_version="${version%-SNAPSHOT}-dev"
fi

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo_root="$(cd "$script_dir/../.." && pwd)"

if [[ ! -x "$repo_root/mvnw" ]]; then
  echo "Maven wrapper is missing or not executable: $repo_root/mvnw" >&2
  exit 1
fi

if ! command -v npm >/dev/null 2>&1; then
  echo "npm is required to update prism-apm-ui/package.json." >&2
  exit 1
fi

cd "$repo_root"

./mvnw versions:set \
  -DnewVersion="$version" \
  -DprocessAllModules=true \
  -DgenerateBackupPoms=false

(
  cd prism-apm-ui
  npm version "$ui_version" --no-git-tag-version
)

echo "Maven project versions updated to $version."
echo "prism-apm-ui version updated to $ui_version."
