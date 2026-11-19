// swift-tools-version:5.9
import PackageDescription

let package = Package(
    name: "Thirds",
    platforms: [.macOS(.v14)],
    targets: [
        .executableTarget(name: "Thirds", dependencies: ["ThirdsCore"]),
        .target(name: "ThirdsCore"),
        .testTarget(name: "ThirdsCoreTests", dependencies: ["ThirdsCore"]),
    ]
)
