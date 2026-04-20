import XCTest
@testable import Camount

@MainActor
final class AmountFormatterTests: XCTestCase {

  // MARK: - Test configs

  private let euroConfig = AmountConfig(
    maximumNotationDigits: 5,
    decimalSeparator: ",",
    groupingSeparator: " ",
    prefix: "",
    suffix: " €",
    groupingSize: 3,
    maximumFractionDigits: 2
  )

  private let usdConfig = AmountConfig(
    maximumNotationDigits: 5,
    decimalSeparator: ".",
    groupingSeparator: ",",
    prefix: "$",
    suffix: "",
    groupingSize: 3,
    maximumFractionDigits: 2
  )

  private let yenConfig = AmountConfig(
    maximumNotationDigits: 5,
    decimalSeparator: ".",
    groupingSeparator: ",",
    prefix: "¥",
    suffix: "",
    groupingSize: 3,
    maximumFractionDigits: 0
  )

  // MARK: - format(Money) — positive cases

  func testFormatSmallInteger() {
    let f = AmountFormatter(config: usdConfig)
    let result = String(f.format(Money(units: 42, nanos: 0, currencyCode: "USD")))
    XCTAssertEqual(result, "$42")
  }

  func testFormatWithFraction() {
    let f = AmountFormatter(config: usdConfig)
    let result = String(f.format(Money(units: 1, nanos: 500_000_000, currencyCode: "USD")))
    XCTAssertEqual(result, "$1.50")
  }

  func testFormatSuffixCurrency() {
    let f = AmountFormatter(config: euroConfig)
    let result = String(f.format(Money(units: 1234, nanos: 560_000_000, currencyCode: "EUR")))
    XCTAssertEqual(result, "1234,56 €")
  }

  func testFormatZeroFractionCurrency() {
    let f = AmountFormatter(config: yenConfig)
    let result = String(f.format(Money(units: 12345, nanos: 0, currencyCode: "JPY")))
    XCTAssertEqual(result, "¥12345")
  }

  func testFormatNegativeIsUnsignedHere() {
    let f = AmountFormatter(config: usdConfig)
    let result = String(f.format(Money(units: -42, nanos: 0, currencyCode: "USD")))
    XCTAssertEqual(result, "$42")
  }

  func testFormatNanosPaddedToNine() {
    let f = AmountFormatter(config: usdConfig)
    let result = String(f.format(Money(units: 1, nanos: 50_000_000, currencyCode: "USD")))
    XCTAssertEqual(result, "$1.05")
  }

  func testFormatOverflowNotationTruncated() {
    let f = AmountFormatter(config: usdConfig)
    let result = String(f.format(Money(units: 1_234_567, nanos: 0, currencyCode: "USD")))
    XCTAssertEqual(result, "$12345")
  }

  func testFormatZeroNanosAndFixedFractionTrue() {
    let f = AmountFormatter(config: usdConfig, withFixedFractionLength: true)
    let result = String(f.format(Money(units: 10, nanos: 0, currencyCode: "USD")))
    XCTAssertEqual(result, "$10")
  }

  // MARK: - parse

  func testParseUnitsOnly() {
    let f = AmountFormatter(config: usdConfig)
    XCTAssertEqual(f.parse("42", currencyCode: "USD"),
                   Money(units: 42, nanos: 0, currencyCode: "USD"))
  }

  func testParseWithFraction() {
    let f = AmountFormatter(config: usdConfig)
    XCTAssertEqual(f.parse("1.5", currencyCode: "USD"),
                   Money(units: 1, nanos: 500_000_000, currencyCode: "USD"))
  }

  func testParseNegative() {
    let f = AmountFormatter(config: usdConfig)
    XCTAssertEqual(f.parse("-2", currencyCode: "USD"),
                   Money(units: -2, nanos: 0, currencyCode: "USD"))
  }

  func testParseIgnoresGroupingAndCurrency() {
    let f = AmountFormatter(config: euroConfig)
    XCTAssertEqual(f.parse("1 234,56 €", currencyCode: "EUR"),
                   Money(units: 1234, nanos: 560_000_000, currencyCode: "EUR"))
  }

  func testParseEmpty() {
    let f = AmountFormatter(config: usdConfig)
    XCTAssertEqual(f.parse("", currencyCode: "USD"),
                   Money(units: 0, nanos: 0, currencyCode: "USD"))
  }

  // MARK: - format(source, start, end, text, textStart, textEnd) input flow

  func testFormatInputFlowSimpleDigit() {
    let f = AmountFormatter(
      config: usdConfig,
      withCurrency: false,
      withGroupingSeparators: false,
      withFixedFractionLength: false,
      withFixedZeroNotation: true
    )
    let result = String(f.format(source: "", start: 0, end: 0, text: "5", textStart: 0, textEnd: 1))
    XCTAssertEqual(result, "5")
  }

  func testFormatInputFlowDecimalSeparator() {
    let f = AmountFormatter(
      config: usdConfig,
      withCurrency: false,
      withGroupingSeparators: false,
      withFixedFractionLength: false,
      withFixedZeroNotation: true
    )
    let result = String(f.format(source: "5", start: 1, end: 1, text: ".", textStart: 0, textEnd: 1))
    XCTAssertTrue(result.hasPrefix("5.") || result.hasPrefix("5,"),
                  "Expected '5.' or '5,' got '\(result)'")
  }
}
