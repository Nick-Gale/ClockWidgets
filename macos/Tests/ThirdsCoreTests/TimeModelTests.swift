import XCTest
@testable import ThirdsCore

/// Runs the canonical shared vectors (core/fixtures/vectors.json).
final class TimeModelTests: XCTestCase {
    struct Vector: Decodable {
        let t: String
        let period: String
        let quarters: Int
        let fullSections: Int
        let section: Int
        let fill: Double
    }
    struct Envelope: Decodable { let vectors: [Vector] }

    func testSharedVectorsAllMatch() throws {
        let here = URL(fileURLWithPath: #filePath)   // .../macos/Tests/ThirdsCoreTests/…
        let vectorsURL = here
            .deletingLastPathComponent()            // ThirdsCoreTests
            .deletingLastPathComponent()            // Tests
            .deletingLastPathComponent()            // macos
            .deletingLastPathComponent()            // repo root
            .appendingPathComponent("core/fixtures/vectors.json")
        let data = try Data(contentsOf: vectorsURL)
        let envelope = try JSONDecoder().decode(Envelope.self, from: data)
        XCTAssertEqual(envelope.vectors.count, 23)
        for v in envelope.vectors {
            let parts = v.t.split(separator: ":").map { Int($0)! }
            let s = thirdState(hour: parts[0], minute: parts[1])
            XCTAssertEqual(s.period, v.period, "period @\(v.t)")
            XCTAssertEqual(s.quarters, v.quarters, "quarters @\(v.t)")
            XCTAssertEqual(s.fullSections, v.fullSections, "full @\(v.t)")
            XCTAssertEqual(s.section, v.section, "section @\(v.t)")
            XCTAssertEqual(s.fill, v.fill, accuracy: 1e-9, "fill @\(v.t)")
        }
    }
}
