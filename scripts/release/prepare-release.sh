#!/usr/bin/env bash

set -euo pipefail

usage() {
  cat <<'EOF'
Usage: ./scripts/release/prepare-release.sh [release-version]

Examples:
  ./scripts/release/prepare-release.sh
  ./scripts/release/prepare-release.sh 0.1.0

When no release version is provided, the script derives it from the current
project version by removing any pre-release or build suffix. For example,
0.1.0-RC2 becomes 0.1.0.

It updates project versions, commits the changes, and creates an annotated tag.
It does not push the commit or tag.
EOF
}

if [[ "${1:-}" == "--help" || "${1:-}" == "-h" ]]; then
  usage
  exit 0
fi

if [[ $# -gt 1 ]]; then
  usage >&2
  exit 1
fi

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo_root="$(cd "$script_dir/../.." && pwd)"

cd "$repo_root"

if ! git rev-parse --is-inside-work-tree >/dev/null 2>&1; then
  echo "This script must be run inside a git work tree." >&2
  exit 1
fi

if [[ -n "$(git status --porcelain)" ]]; then
  echo "Working tree is not clean. Commit or stash your changes before preparing a release." >&2
  git status --short >&2
  exit 1
fi

current_version="$(
  sed -n '/<\/parent>/,$p' pom.xml \
    | sed -n 's/.*<version>[[:space:]]*\([^<[:space:]]\{1,\}[^<]*\)[[:space:]]*<\/version>.*/\1/p' \
    | head -n 1
)"

if [[ -z "$current_version" ]]; then
  echo "Unable to read the current project version from pom.xml." >&2
  exit 1
fi

if [[ $# -eq 1 ]]; then
  release_version="$1"
else
  release_version="$(printf '%s' "$current_version" | sed -E 's/[-+].*$//')"
fi

release_version_pattern='^[0-9]+\.[0-9]+\.[0-9]+$'

if [[ ! "$release_version" =~ $release_version_pattern ]]; then
  echo "Invalid release version: $release_version" >&2
  echo "Expected a stable semantic version such as 0.1.0." >&2
  exit 1
fi

tag_name="v$release_version"

if git rev-parse -q --verify "refs/tags/$tag_name" >/dev/null; then
  echo "Tag already exists: $tag_name" >&2
  exit 1
fi

"$script_dir/set-version.sh" "$release_version"

if git diff --quiet; then
  echo "No version changes detected. Current version may already be $release_version." >&2
  exit 1
fi

git add -u
git commit -m "chore(release): prepare $tag_name"
git tag -a "$tag_name" -m "Release $tag_name"

echo "Prepared release $tag_name."
echo "Remember to push the commit and tag when ready:"
echo "  git push origin HEAD $tag_name"
