#!/usr/bin/env bash

set -euo pipefail

usage() {
  cat <<'EOF'
Usage: ./scripts/release/prepare-release-v1.sh <original-branch> <new-branch> <release-version>

Example:
  ./scripts/release/prepare-release-v1.sh main release/0.1.0 0.1.0

Creates and switches to a new branch based on an existing local branch, updates
all project versions, commits the changes, and creates an annotated release tag.
It does not push the branch or tag.
EOF
}

if [[ "${1:-}" == "--help" || "${1:-}" == "-h" ]]; then
  usage
  exit 0
fi

if [[ $# -ne 3 ]]; then
  usage >&2
  exit 1
fi

original_branch="$1"
new_branch="$2"
release_version="$3"

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

if ! git check-ref-format --branch "$original_branch" >/dev/null 2>&1; then
  echo "Invalid original branch name: $original_branch" >&2
  exit 1
fi

if ! git show-ref --verify --quiet "refs/heads/$original_branch"; then
  echo "Original local branch does not exist: $original_branch" >&2
  exit 1
fi

if ! git check-ref-format --branch "$new_branch" >/dev/null 2>&1; then
  echo "Invalid new branch name: $new_branch" >&2
  exit 1
fi

if git show-ref --verify --quiet "refs/heads/$new_branch"; then
  echo "Branch already exists: $new_branch" >&2
  exit 1
fi

numeric_identifier='(0|[1-9][0-9]*)'
release_version_pattern="^$numeric_identifier\.$numeric_identifier\.$numeric_identifier$"

if [[ ! "$release_version" =~ $release_version_pattern ]]; then
  echo "Invalid release version: $release_version" >&2
  echo "Expected a stable semantic version such as 0.1.0." >&2
  echo "Numeric identifiers must not contain leading zeroes." >&2
  exit 1
fi

tag_name="v$release_version"

if git rev-parse -q --verify "refs/tags/$tag_name" >/dev/null; then
  echo "Tag already exists: $tag_name" >&2
  exit 1
fi

git switch -c "$new_branch" "$original_branch"

"$script_dir/set-version.sh" "$release_version"

./mvnw -f prism-explorer/pom.xml versions:set \
  -DnewVersion="$release_version" \
  -DgenerateBackupPoms=false

if git diff --quiet; then
  echo "No version changes detected. Project versions may already be $release_version." >&2
  exit 1
fi

git add -u
git commit -m "chore(release): prepare $tag_name"
git tag -a "$tag_name" -m "Release $tag_name"

echo "Prepared release $tag_name."
echo "  Source branch: $original_branch"
echo "  New branch:    $new_branch"
echo "  Version:       $release_version"
echo "  Commit:        chore(release): prepare $tag_name"
echo "  Tag:           $tag_name"
echo "The branch and tag have not been pushed. Push them manually when ready:"
echo "  git push <remote> $new_branch $tag_name"
