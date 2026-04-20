import XCTest
@testable import Camount

final class AmountConfigTests: XCTestCase {

  private func euro(maxNotation: Int = 5) -> AmountConfig {
    AmountConfig(
      maximumNotationDigits: maxNotation,
      decimalSeparator: ",",
      groupingSeparator: " ",
      prefix: "",
      suffix: " €",
      groupingSize: 3,
      maximumFractionDigits: 2
    )
  }

  func testDigitPredicate() {
    let c = euro()
    for ch: Character in "0123456789" { XCTAssertTrue(c.isDigit(ch)) }
    for ch: Character in "abc,. €" { XCTAssertFalse(c.isDigit(ch)) }
  }

  func testZeroIsFirstDigit() {
    let c = euro()
    XCTAssertEqual(c.zero, "0")
    XCTAssertTrue(c.isZero("0"))
    XCTAssertFalse(c.isZero("1"))
  }

  func testInputSeparator() {
    let c = euro()
    XCTAssertTrue(c.isInputSeparator("."))
    XCTAssertTrue(c.isInputSeparator(","))
    XCTAssertFalse(c.isInputSeparator(" "))
  }

  func testDecimalSeparator() {
    let c = euro()
    XCTAssertTrue(c.isDecimalSeparator(","))
    XCTAssertFalse(c.isDecimalSeparator("."))
  }

  func testGroupingSeparator() {
    let c = euro()
    XCTAssertTrue(c.isGroupingSeparator(" "))
    XCTAssertFalse(c.isGroupingSeparator(","))
  }

  func testGroupingSeparatorFalseWhenGroupingSizeZero() {
    let c = AmountConfig(
      maximumNotationDigits: 5,
      decimalSeparator: ".",
      groupingSeparator: ",",
      prefix: "",
      suffix: "",
      groupingSize: 0,
      maximumFractionDigits: 0
    )
    XCTAssertFalse(c.isGroupingSeparator(","))
  }

  func testMaximumFormattedSymbols() {
    XCTAssertEqual(euro().maximumFormattedSymbols, 11)
  }
}
