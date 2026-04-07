// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "GymBroUI",
    platforms: [
        .iOS(.v17)
    ],
    products: [
        .library(
            name: "GymBroUI",
            targets: ["GymBroUI"]
        )
    ],
    dependencies: [
        .package(path: "../GymBroCore"),
        .package(path: "../GymBroKit")
    ],
    targets: [
        .target(
            name: "GymBroUI",
            dependencies: [
                .product(name: "GymBroCore", package: "GymBroCore"),
                .product(name: "GymBroKit", package: "GymBroKit")
            ]
        ),
        .testTarget(
            name: "GymBroUITests",
            dependencies: ["GymBroUI"]
        )
    ]
)
