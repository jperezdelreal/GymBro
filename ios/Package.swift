// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "GymBro",
    platforms: [
        .iOS(.v17)
    ],
    products: [
        .executable(
            name: "GymBro",
            targets: ["GymBro"]
        )
    ],
    dependencies: [
        .package(path: "Packages/GymBroCore"),
        .package(path: "Packages/GymBroUI"),
        .package(path: "Packages/GymBroKit")
    ],
    targets: [
        .executableTarget(
            name: "GymBro",
            dependencies: [
                .product(name: "GymBroCore", package: "GymBroCore"),
                .product(name: "GymBroUI", package: "GymBroUI"),
                .product(name: "GymBroKit", package: "GymBroKit")
            ],
            path: "GymBro"
        )
    ]
)
