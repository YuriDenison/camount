import Foundation

public struct Money: Equatable, Hashable, Comparable, Sendable {
  public let units: Int64
  public let nanos: Int32
  public let currencyCode: String

  public init(units: Int64, nanos: Int32, currencyCode: String) {
    self.units = units
    self.nanos = nanos
    self.currencyCode = currencyCode
  }

  public static func zero(_ currencyCode: String) -> Money {
    Money(units: 0, nanos: 0, currencyCode: currencyCode)
  }

  public var isZero: Bool { units == 0 && nanos == 0 }

  public var isPositive: Bool {
    units > 0 || (units == 0 && nanos > 0)
  }

  public static func < (lhs: Money, rhs: Money) -> Bool {
    if lhs.units != rhs.units { return lhs.units < rhs.units }
    return lhs.nanos < rhs.nanos
  }
}

extension Money {
  var absoluteUnits: Int64 { units < 0 ? -units : units }
  var absoluteNanos: Int32 { nanos < 0 ? -nanos : nanos }
}
