import XCTest
@testable import Camount

final class SanitizeInputTests: XCTestCase {

  private let usd = AmountConfig(
    maximumNotationDigits: 5,
    decimalSeparator: ".",
    groupingSeparator: ",",
    prefix: "",
    suffix: "",
    groupingSize: 3,
    maximumFractionDigits: 2
  )

  func testStripsLetters() {
    let result = sanitizeInput(text: "1a2b3", cursor: 5, config: usd)
    XCTAssertEqual(result.text, "123")
    XCTAssertEqual(result.cursor, 3)
  }

  func testEnforcesMaxNotation() {
    let result = sanitizeInput(text: "123456", cursor: 6, config: usd)
    XCTAssertEqual(result.text, "12345")
    XCTAssertEqual(result.cursor, 5)
  }

  func testEnforcesMaxFraction() {
    let result = sanitizeInput(text: "1.234", cursor: 5, config: usd)
    XCTAssertEqual(result.text, "1.23")
    XCTAssertEqual(result.cursor, 4)
  }

  func testOnlyOneSeparator() {
    let result = sanitizeInput(text: "1.2.3", cursor: 5, config: usd)
    XCTAssertEqual(result.text, "1.23")
    XCTAssertEqual(result.cursor, 4)
  }

  func testCommaMappedToDecimalSeparator() {
    let result = sanitizeInput(text: "1,2", cursor: 3, config: usd)
    XCTAssertEqual(result.text, "1.2")
  }
}
