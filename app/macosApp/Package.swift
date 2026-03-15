// swift-tools-version: 6.0
import PackageDescription

let package = Package(
    name: "ChatMacNative",
    platforms: [
        .macOS(.v14)
    ],
    products: [
        .executable(
            name: "ChatMacNative",
            targets: ["ChatMacNative"]
        )
    ],
    targets: [
        .executableTarget(
            name: "ChatMacNative",
            path: "Sources/ChatMacNative",
            resources: [
                .process("Resources")
            ]
        ),
        .testTarget(
            name: "ChatMacNativeTests",
            dependencies: ["ChatMacNative"],
            path: "Tests/ChatMacNativeTests"
        )
    ]
)
