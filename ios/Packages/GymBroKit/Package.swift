// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "GymBroKit",
    platforms: [
        .iOS(.v17)
    ],
    products: [
        .library(
            name: "GymBroKit",
            targets: ["GymBroKit"]
        )
    ],
    targets: [
        .target(
            name: "GymBroKit",
            dependencies: []
        ),
        .testTarget(
            name: "GymBroKitTests",
            dependencies: ["GymBroKit"]
        )
    ]
)
