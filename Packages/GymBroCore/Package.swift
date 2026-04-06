// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "GymBroCore",
    platforms: [
        .iOS(.v17)
    ],
    products: [
        .library(
            name: "GymBroCore",
            targets: ["GymBroCore"]
        )
    ],
    targets: [
        .target(
            name: "GymBroCore",
            dependencies: []
        ),
        .testTarget(
            name: "GymBroCoreTests",
            dependencies: ["GymBroCore"]
        )
    ]
)
