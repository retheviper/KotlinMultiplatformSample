// swift-tools-version: 6.0
import PackageDescription

let package = Package(
    name: "KMPs",
    platforms: [
        .macOS(.v14)
    ],
    products: [
        .executable(
            name: "KMPs",
            targets: ["KMPs"]
        )
    ],
    targets: [
        .executableTarget(
            name: "KMPs",
            path: "Sources/ChatMacNative",
            resources: [
                .process("Resources")
            ]
        ),
        .testTarget(
            name: "ChatMacNativeTests",
            dependencies: ["KMPs"],
            path: "Tests/ChatMacNativeTests"
        )
    ]
)
