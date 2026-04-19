import XCTest
@testable import Camount

final class MoneyTests: XCTestCase {

  func testIsZero() {
    XCTAssertTrue(Money(units: 0, nanos: 0, currencyCode: "USD").isZero)
    XCTAssertFalse(Money(units: 1, nanos: 0, currencyCode: "USD").isZero)
    XCTAssertFalse(Money(units: 0, nanos: 1, currencyCode: "USD").isZero)
  }

  func testIsPositive() {
    XCTAssertTrue(Money(units: 1, nanos: 0, currencyCode: "USD").isPositive)
    XCTAssertTrue(Money(units: 0, nanos: 1, currencyCode: "USD").isPositive)
    XCTAssertFalse(Money(units: 0, nanos: 0, currencyCode: "USD").isPositive)
    XCTAssertFalse(Money(units: -1, nanos: 0, currencyCode: "USD").isPositive)
    XCTAssertFalse(Money(units: 0, nanos: -1, currencyCode: "USD").isPositive)
  }

  func testZeroFactory() {
    XCTAssertEqual(Money.zero("EUR"), Money(units: 0, nanos: 0, currencyCode: "EUR"))
  }

  func testCompare() {
    let a = Money(units: 1, nanos: 0, currencyCode: "USD")
    let b = Money(units: 1, nanos: 500_000_000, currencyCode: "USD")
    let c = Money(units: 2, nanos: 0, currencyCode: "USD")
    XCTAssertLessThan(a, b)
    XCTAssertLessThan(b, c)
    XCTAssertEqual(a, Money(units: 1, nanos: 0, currencyCode: "USD"))
  }

  func testHashable() {
    let set: Set<Money> = [
      Money(units: 1, nanos: 0, currencyCode: "USD"),
      Money(units: 1, nanos: 0, currencyCode: "USD"),
    ]
    XCTAssertEqual(set.count, 1)
  }
}
