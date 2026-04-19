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
      path: "Sources/Camount"
    ),
    .testTarget(
      name: "CamountTests",
      dependencies: ["Camount"],
      path: "Tests/CamountTests"
    ),
  ]
)
