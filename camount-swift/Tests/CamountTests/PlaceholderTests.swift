import XCTest
@testable import Camount

final class PlaceholderTests: XCTestCase {
  func testPackageBuilds() {
    XCTAssertEqual(String(describing: CamountPackageMarker.self), "CamountPackageMarker")
  }
}
