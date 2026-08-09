// swift-tools-version: 5.10

import PackageDescription

let package = Package(
    name: "AtomCore",
    platforms: [
        .iOS(.v17),
        .macOS(.v13),
    ],
    products: [
        .library(name: "AtomCore", targets: ["AtomCore"]),
        .executable(name: "AtomCoreChecks", targets: ["AtomCoreChecks"]),
    ],
    targets: [
        .target(name: "AtomCore"),
        .executableTarget(
            name: "AtomCoreChecks",
            dependencies: ["AtomCore"],
            path: "Checks/AtomCoreChecks"
        ),
        .testTarget(name: "AtomCoreTests", dependencies: ["AtomCore"]),
    ],
    swiftLanguageVersions: [.v5]
)
