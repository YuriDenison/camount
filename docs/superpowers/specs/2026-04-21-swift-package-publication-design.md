# Swift Package Publication — Design

**Date:** 2026-04-21
**Status:** Draft
**Target version:** `0.9.1` (shared with the Maven Central release tag `v0.9.1`)

## Goal

Make `camount-swift` installable via Swift Package Manager directly from the
monorepo, using the shared `v0.9.1` tag. No separate Swift repo, no separate
release step — SwiftPM resolves the package from the same git tag that triggers
the Maven Central publish workflow.

Consumers will install it with:

```swift
// In their Package.swift
.package(url: "https://github.com/yuridenison/camount.git", from: "0.9.1")

// and in the dependent target:
.product(name: "Camount", package: "camount")
```

Or via Xcode: **File → Add Package Dependencies →
`https://github.com/yuridenison/camount` → Exact `0.9.1`**.

## Non-Goals

- Moving Swift sources or tests out of `camount-swift/`. Only the manifest moves.
- Any change to Swift source files (`Sources/Camount/*.swift`) or test files
  (`Tests/CamountTests/*.swift`).
- Any change to the Maven Central publish workflow — the shared `v0.9.1` tag
  already covers Swift because SwiftPM resolves tags directly from GitHub; no
  separate upload action is required.
- Publishing to Swift Package Index — that's a one-time manual submission,
  documented as a post-implementation step.
- Precompiled XCFramework / `binaryTarget` distribution. Source distribution is
  sufficient for a small pure-Swift library.

## Coordinates

| Aspect | Value |
|---|---|
| Repo URL | `https://github.com/yuridenison/camount.git` |
| SwiftPM package name | `Camount` (unchanged from current manifest) |
| SwiftPM product name | `Camount` |
| SwiftPM target/module name | `Camount` (consumers write `import Camount`) |
| Version tag | `v0.9.1` (shared with Kotlin libs) |
| Minimum platform | iOS 16 |
| swift-tools-version | `5.9` |

## Approach

SwiftPM discovers a package manifest at the repo root (`/Package.swift`). The
current manifest lives at `camount-swift/Package.swift`, which is not
discoverable by `git+https` consumers — SwiftPM does not support sub-directory
package URLs.

We move **only the manifest** to the repo root, keeping all Swift sources and
tests in place under `camount-swift/`. The new root manifest rebases its
`path:` arguments from `Sources/Camount` to `camount-swift/Sources/Camount`
(and the equivalent for tests). This is the minimum-churn layout that keeps
the Kotlin monorepo structure intact while making the Swift package resolvable
from the repo URL.

Releases piggy-back on the existing `v0.9.1` tag — SwiftPM reads tags directly
from GitHub, so no additional CI or upload is required.

## File Changes

### 1. New `Package.swift` at repo root

Create `/Package.swift` (at project root, alongside `build.gradle.kts` and
`settings.gradle.kts`):

```swift
// swift-tools-version: 5.9
import PackageDescription

let package = Package(
  name: "Camount",
  platforms: [
    .iOS(.v16),
  ],
  products: [
    .library(name: "Camount", targets: ["Camount"]),
  ],
  targets: [
    .target(
      name: "Camount",
      path: "camount-swift/Sources/Camount"
    ),
    .testTarget(
      name: "CamountTests",
      dependencies: ["Camount"],
      path: "camount-swift/Tests/CamountTests"
    ),
  ]
)
```

This is character-for-character identical to the existing
`camount-swift/Package.swift` except for the two `path:` values.

### 2. Delete `camount-swift/Package.swift`

Only one manifest should exist in the repo. The rule SwiftPM applies is "the
nearest `Package.swift` to the repo root wins", but dual manifests are
confusing for humans reading the repo. Remove `camount-swift/Package.swift`.

### 3. Update `README.md`

The existing Swift section says:

> Or integrate as a Swift package by pointing at `./camount-swift`
> (`swift-tools-version:5.9`, iOS 16+).

Replace with a SwiftPM install snippet that shows the real distribution URL:

```markdown
### Swift Package Manager

Add Camount as a dependency in your `Package.swift`:

```swift
.package(url: "https://github.com/yuridenison/camount.git", from: "0.9.1")
```

Then list it in your target's dependencies:

```swift
.product(name: "Camount", package: "camount")
```

Or via Xcode: **File → Add Package Dependencies →
`https://github.com/yuridenison/camount`**, pinned to exact version `0.9.1`.

Swift and Compose-Multiplatform releases share the same tag (`v0.9.1`).
```

Also update the `swift test` snippet — sources still live under
`camount-swift/`, but the manifest is now at the root, so the command becomes:

```shell
swift test
```

(from repo root, not from `camount-swift/`).

### 4. Update `samples/ios/CamountSample.xcodeproj/project.pbxproj`

The sample consumes the Swift package as a local SwiftPM reference. The
pbxproj contains three occurrences that must be updated from
`../../camount-swift` to `../..` (repo root):

- Line ~124: `CA000500 /* XCLocalSwiftPackageReference "../../camount-swift" */`
- Line ~400: `CA000500 /* XCLocalSwiftPackageReference "../../camount-swift" */ = {`
- Line ~402: `relativePath = ../../camount-swift;`

The `CA000500` object ID and surrounding structure do not change — only the
path string.

### 5. New `.github/workflows/swift.yml`

CI for the Swift side, running `swift test` on PRs and pushes to `main` when
Swift-relevant files change:

```yaml
name: Swift CI

on:
  push:
    branches: [main]
    paths:
      - 'Package.swift'
      - 'camount-swift/**'
      - '.github/workflows/swift.yml'
  pull_request:
    paths:
      - 'Package.swift'
      - 'camount-swift/**'
      - '.github/workflows/swift.yml'
  workflow_dispatch: {}

jobs:
  test:
    runs-on: macos-latest
    steps:
      - uses: actions/checkout@v4
      - name: swift test
        run: swift test
```

Uses the Xcode-bundled Swift toolchain on `macos-latest` (Swift 5.9+ is
available on all supported GitHub macOS runners). No Xcode selection needed
for a pure-Swift package with no UIKit target.

## Verification

Implementation is complete when all of the following hold:

1. `swift package resolve` (run from repo root) succeeds.
2. `swift build` succeeds.
3. `swift test` succeeds and all existing tests in `Tests/CamountTests` pass.
4. The iOS sample app (`samples/ios`) still opens in Xcode and builds without
   unresolved package references.
5. `.github/workflows/swift.yml` passes basic YAML sanity (`publishToMavenCentral`
   check is inapplicable — the analog is presence of `swift test` and the
   workflow name).
6. No `camount-swift/Package.swift` remains; a single `Package.swift` exists at
   repo root.

## Risks and Open Questions

- **iOS sample breakage.** The `samples/ios` Xcode project currently consumes
  the Swift package via a relative-path local package reference. Moving the
  manifest changes that relative path. Mitigation: Step 1 of the implementation
  plan inspects and updates the project file before running Xcode.
- **SwiftPM cache on consumer side.** Consumers who previously pinned against
  the old (unresolvable) path will see resolution errors if they try to resolve
  without updating. This is a non-issue for new consumers; existing internal
  consumers (just the iOS sample) are fixed as part of this change.
- **Swift toolchain version on CI.** `macos-latest` pins to the newest macOS
  image; Xcode versions bundled there include Swift 6.x as of 2026. Our
  `swift-tools-version: 5.9` manifest remains compatible — Swift 6 compilers
  happily build 5.9-tools packages.
- **Tag reuse for both Kotlin and Swift.** Single `v0.9.1` tag releases both.
  If one platform ever needs to ship a point release (e.g. a Swift-only
  `0.9.2` bugfix), the escape hatch is a Swift-only tag like `swift-0.9.2` —
  SwiftPM accepts arbitrary semver-parseable tags and the Maven workflow's
  `v*` filter won't match. Out of scope until it's actually needed.

## Post-implementation manual steps

Out of scope for the automated work; listed for tracking:

1. Cut and push tag `v0.9.1`. This triggers the existing Maven Central publish
   workflow **and** makes the Swift package resolvable at that version.
2. In a scratch Swift package, verify resolution:
   ```shell
   mkdir /tmp/camount-smoke && cd /tmp/camount-smoke
   swift package init --type executable
   # edit Package.swift to add the .package(url:...) dep
   swift package resolve
   ```
3. Submit the repo to [Swift Package Index](https://swiftpackageindex.com/add-a-package)
   at `https://github.com/yuridenison/camount`. Index picks up the `Package.swift`
   automatically on its next crawl.
