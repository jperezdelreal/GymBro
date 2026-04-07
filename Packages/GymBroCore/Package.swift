// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "GymBroCore",
    platforms: [
        .iOS(.v17),
        .watchOS(.v10)
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
            dependencies: [],
            resources: [
                .process("Resources")
            ]
        ),
        .testTarget(
            name: "GymBroCoreTests",
            dependencies: ["GymBroCore"]
        )
    ]
)
