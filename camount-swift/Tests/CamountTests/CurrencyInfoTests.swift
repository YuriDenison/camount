import XCTest
@testable import Camount

final class CurrencyInfoTests: XCTestCase {

  func testUSDHasPrefixDollar() {
    let info = CurrencyInfo.forCurrency("USD")
    XCTAssertEqual(info.maximumFractionDigits, 2)
    XCTAssertFalse(info.prefix.isEmpty, "USD should have a currency prefix (\"$\" or similar)")
  }

  func testJPYHasZeroFractionDigits() {
    let info = CurrencyInfo.forCurrency("JPY")
    XCTAssertEqual(info.maximumFractionDigits, 0)
  }

  func testUnknownCurrencyStillReturns() {
    let info = CurrencyInfo.forCurrency("ZZZ")
    XCTAssertNotNil(info.decimalSeparator)
  }

  func testSanitizeBidiStripsControlChars() {
    let input = "\u{200E}$\u{202A}"
    XCTAssertEqual(CurrencyInfo._sanitizeBidi(input), "$")
  }
}
