# Development

This project follows a **mainline development + release branch** model:

```text
feature/*
    │
    │ Development
    │ Testing
    │ Code Review
    ▼
   main
    │
    ├─────────────────────→ release/v1.3.x
    │                           │
    │                           ├── RC
    │                           ├── Release testing
    │                           ├── v1.3.0
    │                           ├── Bug fixes
    │                           ├── v1.3.1
    │                           └── v1.3.2
    │
    ▼
Continue development for 1.4
```

The core principle is:

> `main` should contain completed and tested changes that are acceptable for inclusion in the next release.

When a version enters the release stabilization phase, a release branch is created from `main`.

## Branch Model

The project uses the following branch types:

```text
main             Primary development branch

feature/*        New features
fix/*            Bug fixes
refactor/*       Refactoring
docs/*           Documentation
test/*           Test-related changes
release/*        Release stabilization and maintenance
```

Normal development follows:

```text
feature/*
    │
    ▼
   main
    │
    ▼
release/vX.Y.x
```

Release branches are not used as general feature integration branches.

---

## `main`

`main` is the primary development branch.

Normal development eventually goes into `main`, including:

* New features
* Bug fixes
* Refactoring
* Documentation changes
* Test changes
* Dependency updates

`main` should remain in a releasable state as much as possible.

A change should normally be merged into `main` only when:

1. The implementation is complete.
2. Automated tests pass.
3. Code review is complete.
4. Existing functionality is not broken.
5. The change is acceptable for inclusion in the next release.

---

## Feature Branches

Each feature should normally be developed on a short-lived branch:

```text
feature/<name>
```

Examples:

```text
feature/add-login
feature/add-opentelemetry-support
feature/add-sharding
```

Typical workflow:

```text
main
 │
 └── feature/add-a
          │
          ├── development
          ├── unit tests
          ├── integration tests
          ├── CI
          └── code review
                 │
                 ▼
                main
```

Create a branch from the latest `main`:

```bash
git switch main
git pull

git switch -c feature/add-a
```

After development and testing, create a Pull Request and merge it into `main`.

---

## Fix Branches

Bug fixes should normally use:

```text
fix/<name>
```

Examples:

```text
fix/login-timeout
fix/null-pointer-exception
fix/database-connection-leak
```

For bugs found during normal development:

```text
main
 │
 └── fix/xxx
        │
        ▼
       main
```

For bugs affecting an already released version, use the maintenance and Backport process described later.

---

## Other Branch Types

Other commonly used branch prefixes include:

```text
refactor/*
docs/*
test/*
```

Examples:

```text
refactor/opentelemetry-config
docs/update-readme
test/add-integration-tests
```

---

## Commit Message Convention

Use Conventional Commits where practical.

Common types include:

```text
feat:       New feature
fix:        Bug fix
refactor:   Code refactoring
docs:       Documentation
test:       Tests
build:      Build system or dependency changes
ci:         CI configuration
chore:      Maintenance work
```

Examples:

```text
feat: add OpenTelemetry tracing support

fix: handle null configuration values

refactor: simplify database routing logic

docs: add release workflow documentation

test: add integration tests for authentication
```

---

## Rules for Merging into `main`

Before merging a Pull Request into `main`:

```text
Implementation complete
        │
        ▼
Automated tests pass
        │
        ▼
Code review passes
        │
        ▼
No known regression
        │
        ▼
Safe for next release
        │
        ▼
       main
```

The decision about whether a feature is ready for release should normally happen **before it enters `main`**.

The release branch should not be used as a place to arbitrarily select finished and unfinished features.

---

## Incomplete or Experimental Features

Large features sometimes need to be merged before they are ready for normal production use.

Possible mechanisms include:

```text
Feature Flag
Experimental status
Disabled by default
Internal-only implementation
```

For example:

```properties
feature.c.enabled=false
```

This means that Feature C exists in the codebase but is disabled by default.

Example:

```text
main
├── Feature A   enabled
├── Feature B   enabled
├── Feature C   disabled
└── Feature D   enabled
```

After the release branch is created:

```text
release/v1.3.x
├── Feature A   enabled
├── Feature B   enabled
├── Feature C   disabled
└── Feature D   enabled
```

Therefore:

```text
Code included in release
        ≠
Feature enabled for users
```

---

# Testing

Testing is performed at two different stages:

```text
Feature testing
      ↓
Merge into main
      ↓
Release testing
      ↓
Stable release
```

The two stages may run some of the same automated tests, but they answer different questions.

## Feature-Level Testing

Before a feature enters `main`:

```text
feature/add-a
      │
      ├── unit tests
      ├── feature integration tests
      ├── CI
      ├── static checks
      └── code review
             │
             ▼
            main
```

The main question is:

> Is this individual change ready to enter `main`?

Typical checks include:

* Unit tests
* Feature-related integration tests
* Static analysis
* Build verification
* Code review
* Basic regression testing

---

## Release-Level Testing

After a release branch is created, the entire release is tested again:

```text
release/v1.3.x
       │
       ├── RC build
       ├── full regression tests
       ├── integration tests
       ├── deployment tests
       ├── compatibility tests
       ├── upgrade tests
       └── production-like validation
              │
              ▼
           v1.3.0
```

The main question is:

> Is the complete release ready for production?

Even if individual features pass:

```text
Feature A ✅
Feature B ✅
Feature C ✅
```

The combined result may still contain problems:

```text
A + B + C ❌
```

For this reason, the same unit and integration tests may run both during Pull Request validation and during release validation.

---

## Public and Private Testing

The public repository should run all tests that can safely be open-sourced.

For example:

```text
Build
Unit tests
Public integration tests
Static analysis
Code style checks
Public CI checks
```

Internal tests that cannot be published should remain in the private environment.

A release should pass both:

```text
Public repository tests
        +
Private/internal tests
        =
Release validation
```

The exact commit that will be tagged should be tested:

```text
Release commit
      │
      ├── Public CI ✅
      ├── Private tests ✅
      │
      ▼
   Create tag
```

Do not change the code between final validation and tagging without retesting.

---

# Release

## Creating a Release Branch

When the project is ready to prepare version `1.3.0`, create a release branch from `main`.

Recommended name:

```text
release/v1.3.x
```

The `.x` represents the entire `1.3` maintenance line:

```text
v1.3.0
v1.3.1
v1.3.2
...
```

Example:

```bash
git switch main
git pull

git switch -c release/v1.3.x
```

The branch relationship becomes:

```text
                       release/v1.3.x
                      /
main ────────────────●
                      \
                       main continues toward 1.4
```

After branching:

```text
main
    → next minor version development

release/v1.3.x
    → release stabilization
    → RC
    → v1.3.0
    → patch maintenance
```

---

## Version Numbers

For a Maven-based Java project, `main` may use a development version such as:

```text
1.3.0-SNAPSHOT
```

Before branching:

```text
main
└── 1.3.0-SNAPSHOT
```

After branching:

```text
release/v1.3.x
└── prepare 1.3.0

main
└── 1.4.0-SNAPSHOT
```

This clearly separates release stabilization from future development.

---

## Release Candidate

Before a stable release, one or more Release Candidates may be created:

```text
release/v1.3.x
       │
       ├── v1.3.0-RC1
       ├── testing
       ├── bug fixes
       ├── v1.3.0-RC2
       ├── testing
       └── v1.3.0
```

Once the RC phase begins, the release branch should be considered frozen.

Normally only the following changes should be accepted:

* Critical bug fixes
* Release blockers
* Documentation fixes
* Build fixes
* Release configuration fixes

New features should normally wait for the next release.

---

## Stable Release and Tags

After final release testing passes:

```text
release/v1.3.x
       │
       ▼
   final release commit
       │
       ▼
     tag v1.3.0
```

Example:

```bash
git tag v1.3.0
git push <remote> v1.3.0
```

The tag must point to the exact commit that passed final validation:

```text
Tested commit
     │
     └── v1.3.0
```

Do not do this:

```text
Tested commit
     │
     └── additional changes
               │
               └── v1.3.0
```

If additional changes are made, the appropriate release tests should be rerun.

---

## Release Branch Rules

Release branches are for:

```text
Release stabilization
Patch maintenance
Release-related fixes
```

Normally allowed:

* Bug fixes
* Release blockers
* Documentation fixes
* Version updates
* Build fixes
* Release configuration changes

Normally avoid:

* New features
* Large refactoring
* Unrelated cleanup
* Breaking API changes
* Unrelated experimental work

---

# Maintenance

## Patch Releases

A release branch maintains an entire release line:

```text
release/v1.3.x
       │
       ├── v1.3.0
       ├── fix A
       ├── v1.3.1
       ├── fix B
       └── v1.3.2
```

Patch releases should normally contain bug fixes only.

For example:

```text
v1.3.0 → initial stable release
v1.3.1 → bug fixes
v1.3.2 → bug fixes
```

Avoid adding new public features or incompatible API changes in a patch release.

---

## Bug Fixes After a Release

Assume:

```text
release/v1.3.x
       │
       └── v1.3.0

main
       └── development toward 1.4
```

A bug is found in `v1.3.0`, and the same bug exists in `main`.

The default process is:

```text
Create fix branch from main
        │
        ▼
Fix and test
        │
        ▼
Merge into main
        │
        ▼
Backport
        │
        ▼
release/v1.3.x
        │
        ▼
Release testing
        │
        ▼
      v1.3.1
```

Graphically:

```text
main
A──B──C──D──F
             ↑
           bug fix
             │
             │ backport
             ▼
release/v1.3.x
A──B──C──────F'
    │         │
 v1.3.0    v1.3.1
```

The fix enters `main` first so future releases contain it automatically.

The complete release branch should normally **not** be merged back into `main`.

---

## Backport

A Backport means applying a fix from a newer development branch to an older maintained release branch.

Direction:

```text
newer
main
 │
 │ backport
 ▼
release/v1.3.x
older
```

At the Git level, it is often equivalent to:

```bash
git switch release/v1.3.x
git cherry-pick <commit>
```

It may also be implemented using a Backport Pull Request:

```text
PR #100
fix: xxx
    │
    ▼
   main
    │
    │ Backport
    ▼
PR #105
[1.3.x backport] fix: xxx
    │
    ▼
release/v1.3.x
```

Backport changes must still pass:

* Code review
* CI
* Tests

Because the older codebase may behave differently.

---

## Forward Merge / Forward Port

Some projects use the opposite direction when a bug affects several maintained versions.

For example:

```text
release/v1.2.x
release/v1.3.x
main
```

The bug may first be fixed in the oldest affected branch:

```text
release/v1.2.x
       │
       └── fix
            │
            ▼
release/v1.3.x
            │
            ▼
           main
```

This is a Forward Merge or Forward Port.

Direction:

```text
Backport:
newer → older

Forward Port:
older → newer
```

For this project, the normal default is:

```text
Fix main first
      ↓
Backport when necessary
      ↓
Supported release branches
```

---

## Multiple Supported Releases

The project may eventually maintain several versions:

```text
main
release/v1.4.x
release/v1.3.x
```

Their responsibilities are:

```text
main
    → future development

release/v1.4.x
    → 1.4 maintenance

release/v1.3.x
    → 1.3 maintenance
```

If a bug affects all supported versions:

```text
                    fix
                     │
                     ▼
main ────────────────F────────────>

                     │
                     ├── backport
                     ▼
release/v1.4.x ──────F'───────────>

                     │
                     └── backport
                         ▼
release/v1.3.x ──────F''──────────>
```

Only supported release lines need to receive the Backport.

---

# Repository and Distribution

## GitHub and Gitee

The project may be published to both GitHub and Gitee.

The important branches and release tags should remain consistent:

```text
GitHub
├── main
├── release/v1.3.x
└── v1.3.0

Gitee
├── main
├── release/v1.3.x
└── v1.3.0
```

Do not independently develop conflicting changes on the same logical branch in both repositories.

A branch should have one authoritative history and then be synchronized to the other public remote.

---

## Release Tags

Stable versions are represented by Git tags:

```text
v1.3.0
v1.3.1
v1.3.2
```

The release branch represents the maintenance line:

```text
release/v1.3.x
```

The tags represent specific immutable releases:

```text
release/v1.3.x
       │
       ├── v1.3.0
       ├── v1.3.1
       └── v1.3.2
```

---

## Docker Images and Artifacts

Docker images and other release artifacts should be built from the tested release commit or release tag.

Recommended process:

```text
Release commit
      │
      ├── Release tests ✅
      │
      ▼
   v1.3.0 tag
      │
      ▼
Build artifacts
      │
      ├── Docker image
      ├── JAR
      └── Other distribution packages
```

This ensures that released artifacts correspond exactly to the source code identified by the release tag.

---

# Reference

## Full Development Lifecycle

```text
Issue / Requirement
        │
        ▼
feature/add-a
        │
        ├── Development
        ├── Unit tests
        ├── Integration tests
        ├── CI
        └── Code Review
               │
               ▼
              main
               │
               ├── Feature A
               ├── Feature B
               ├── Feature C (disabled)
               └── Fix D
               │
               ▼
       release/v1.3.x
               │
               ├── RC1
               ├── Release testing
               ├── Release fixes
               ├── RC2 if necessary
               └── Final validation
                       │
                       ▼
                    v1.3.0
                       │
                       ├── Bug discovered
                       │
                       │
main ── fix bug ───────┘
        │
        │ Backport
        ▼
release/v1.3.x
        │
        ├── Testing
        └── v1.3.1
```

Meanwhile:

```text
main
 │
 └── continues toward 1.4.0-SNAPSHOT
```

---

## Key Principles

1. Develop features on short-lived Feature branches.
2. Merge completed and tested changes into `main`.
3. Keep `main` releasable.
4. Use Feature Flags for unfinished features that must be merged early.
5. Create Release branches from `main`.
6. Use Release branches for stabilization and maintenance, not normal Feature integration.
7. Test individual changes before merging into `main`.
8. Test the complete Release again before publishing it.
9. Use tags to identify actual released versions.
10. Use `release/vX.Y.x` to maintain a release line.
11. Fix bugs in `main` first by default.
12. Backport fixes to supported older releases when necessary.
13. Do not normally merge the entire Release branch back into `main`.
14. Run public and private tests against the actual Release Candidate commit.
15. Tag only the exact commit that passed final release validation.

---

## Example

Suppose the project is preparing `1.3.0`.

Development:

```text
feature/add-a ──┐
                │
feature/add-b ──┼──→ main
                │
feature/add-c ──┘
```

Feature C is not yet ready for normal use but needs to be merged early:

```properties
feature.c.enabled=false
```

`main` becomes:

```text
main
├── A
├── B
└── C (disabled)
```

When the release is ready:

```text
main
 │
 ├────────────────→ release/v1.3.x
 │                       │
 │                       ├── v1.3.0-RC1
 │                       ├── Release testing
 │                       └── v1.3.0
 │
 └── continue toward 1.4.0-SNAPSHOT
```

Later, a bug is found in `v1.3.0`:

```text
fix/xxx
   │
   ▼
 main
   │
   │ Backport
   ▼
release/v1.3.x
   │
   ├── Testing
   └── v1.3.1
```

This keeps development, release stabilization, and maintenance clearly separated.

---

# Concrete Procedure for Creating a Release Branch

This section uses the preparation of `0.1.0` as an example to describe the concrete procedure for creating a Release maintenance branch from `main`, preparing an RC, publishing a stable release, and updating the development version on `main`.

The commands below assume the following names and versions:

```text
GitHub remote: github
Current development version: 0.1.0-SNAPSHOT
Release maintenance branch: release/v0.1.x
First RC branch: release/v0.1.0-RC1
Stable Release branch: release/v0.1.0
Stable Release tag: v0.1.0
Next development version: 0.2.0-SNAPSHOT
```

If the local GitHub remote is not named `github`, replace `github` in the commands with the actual remote name. Use the following command to check it:

```bash
git remote -v
```

## Pre-release Checks

Before starting, verify that the working tree is clean and update the local `main` branch to the latest state on GitHub:

```bash
git status --short

git fetch github --prune
git switch main
git pull --ff-only github main
```

If `git status --short` produces any output, commit or stash those changes before creating the Release branch. Do not create a Release branch with uncommitted changes in the working tree.

## Create the Release Maintenance Branch

Create the branch that will maintain the entire `0.1` release line from the latest `main`:

```bash
git switch -c release/v0.1.x
git push -u github release/v0.1.x
```

The `release/v0.1.x` branch maintains `0.1.0`, `0.1.1`, `0.1.2`, and all other releases in the `0.1` line. During each stable release, the release script also creates a version-specific branch and tag:

```text
release/v0.1.x
       │
       ├── release/v0.1.0 ── tag v0.1.0
       ├── release/v0.1.1 ── tag v0.1.1
       └── release/v0.1.2 ── tag v0.1.2
```

The version-specific branch preserves the branch state for that stable release. The tag is the exact, immutable identifier of the released version. Subsequent Patch fixes still start from `release/v0.1.x`.

## Update the Development Version on `main`

After the Release maintenance branch has been created, `main` can move to development of the next Minor version. Update the version on a separate branch and merge it into `main` through a Pull Request:

```bash
git switch main
git switch -c chore/upgrade-version-to-0.2.0

./scripts/release/set-version.sh 0.2.0-SNAPSHOT

git diff --check
git add -u
git commit -m "chore: bump version to 0.2.0-SNAPSHOT"
git push -u github chore/upgrade-version-to-0.2.0
```

The Maven development-version suffix must be uppercase `SNAPSHOT`. The script maps the Maven version `0.2.0-SNAPSHOT` to `0.2.0-dev` for `prism-apm-ui`.

After pushing the branch, open a Pull Request. Merge it into `main` only after CI and Code Review pass.

This step can run in parallel with RC preparation and testing on the `release/v0.1.x` line.

## Create an RC Branch

Create the RC branch from the Release maintenance branch. Use a hyphen (`-`) rather than an underscore (`_`) in the branch name:

```bash
git switch release/v0.1.x
git pull --ff-only github release/v0.1.x
git switch -c release/v0.1.0-RC1

./scripts/release/set-version.sh 0.1.0-RC1

git diff --check
git add -u
git commit -m "chore(release): prepare 0.1.0-RC1"
git push -u github release/v0.1.0-RC1
```

Run the complete Release test suite against this RC branch. If a problem is found, fix it on the RC branch and repeat the tests.

If another RC is required, create its branch from the commit that contains the fixes from the previous RC, then update the version:

```bash
git switch release/v0.1.0-RC1
git switch -c release/v0.1.0-RC2

./scripts/release/set-version.sh 0.1.0-RC2

git diff --check
git add -u
git commit -m "chore(release): prepare 0.1.0-RC2"
git push -u github release/v0.1.0-RC2
```

The current Docker image publishing workflow accepts only stable tags in the `vMAJOR.MINOR.PATCH` format. Pushing an RC branch therefore does not publish stable images to Docker Hub or Alibaba Cloud ACR.

## Confirm the Final RC

Assume that `release/v0.1.0-RC1` is the RC that passed validation. Before publishing the stable release, update the local branch and verify again that the working tree is clean:

```bash
git fetch github --prune
git switch release/v0.1.0-RC1
git pull --ff-only github release/v0.1.0-RC1
git status --short
```

`git status --short` should produce no output. Also verify that the following branch and tag names are not already in use:

```bash
git branch --all --list 'release/v0.1.0' 'remotes/github/release/v0.1.0'
git tag --list v0.1.0
```

These two commands should also produce no output. Then complete the final round of Release validation against this exact RC commit.

## Create the Stable Release Branch, Commit, and Tag

Use `prepare-release-v1.sh` to create the stable Release branch from the validated RC branch:

```bash
./scripts/release/prepare-release-v1.sh \
  release/v0.1.0-RC1 \
  release/v0.1.0 \
  0.1.0
```

The script performs the following operations:

```text
1. Creates release/v0.1.0 from release/v0.1.0-RC1 and switches to it.
2. Updates the project version to 0.1.0.
3. Creates the commit: chore(release): prepare v0.1.0.
4. Creates the annotated tag: v0.1.0.
```

The source branch must exist locally, and neither the target branch nor the tag may already exist. The script does not build, test, or push anything.

After the script finishes, verify that the version, commit, and tag are correct, then run Release validation again against the final Release commit:

```bash
git status --short
git show --stat --decorate HEAD
git show v0.1.0
```

`git status --short` should produce no output, and `release/v0.1.0` and `v0.1.0` should identify the same Release commit.

## Update the Release Maintenance Branch

After the stable Release commit passes validation, fast-forward `release/v0.1.x` to that commit. This ensures that subsequent Patch releases continue from the version that was actually published:

```bash
git switch release/v0.1.x
git pull --ff-only github release/v0.1.x
git merge --ff-only release/v0.1.0
```

If the fast-forward is not possible, the Release maintenance branch and the final RC have diverged. Do not overwrite history. First merge the missing changes through a Pull Request, complete Code Review, CI, and Release validation, and only then continue the release.

## Push to GitHub

Push the Release maintenance branch, stable Release branch, and tag only after final validation succeeds. Push the tag last so that it triggers the stable-release workflow:

```bash
git push -u github release/v0.1.0
git push github release/v0.1.x
git push github v0.1.0
```

After the `v0.1.0` tag is pushed, the Docker image publishing workflow in GitHub Actions first runs the tests, then builds the images and pushes them to Docker Hub and Alibaba Cloud ACR.

If testing or publishing fails, do not move or overwrite a tag that has already been published. Fix the problem, repeat validation, and create a new version appropriate to the nature of the issue.

## Synchronize to Gitee

After the GitHub release and validation complete, synchronize the same Release branches and tag to Gitee. Do not create an independent history on Gitee:

```bash
git push gitee release/v0.1.x
git push gitee release/v0.1.0
git push gitee v0.1.0
```

After synchronization, confirm that both public repositories contain the same references:

```bash
git ls-remote github refs/heads/release/v0.1.x refs/heads/release/v0.1.0 refs/tags/v0.1.0
git ls-remote gitee refs/heads/release/v0.1.x refs/heads/release/v0.1.0 refs/tags/v0.1.0
```
