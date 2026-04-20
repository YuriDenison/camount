#if canImport(UIKit)
import SwiftUI

private struct AmountStyleKey: EnvironmentKey {
    static let defaultValue: AmountStyle = .default
}

private struct ShowSignKey: EnvironmentKey {
    static let defaultValue: ShowSign = .ifNegative
}

private struct FractionPolicyKey: EnvironmentKey {
    static let defaultValue: FractionPolicy = .fixed
}

private struct MaximumNotationDigitsKey: EnvironmentKey {
    static let defaultValue: Int = 5
}

private struct AmountAlignmentKey: EnvironmentKey {
    static let defaultValue: AmountAlignment = .center
}

extension EnvironmentValues {
    var amountStyle: AmountStyle {
        get { self[AmountStyleKey.self] }
        set { self[AmountStyleKey.self] = newValue }
    }
    var amountShowSign: ShowSign {
        get { self[ShowSignKey.self] }
        set { self[ShowSignKey.self] = newValue }
    }
    var amountFractionPolicy: FractionPolicy {
        get { self[FractionPolicyKey.self] }
        set { self[FractionPolicyKey.self] = newValue }
    }
    var amountMaximumNotationDigits: Int {
        get { self[MaximumNotationDigitsKey.self] }
        set { self[MaximumNotationDigitsKey.self] = newValue }
    }
    var amountAlignment: AmountAlignment {
        get { self[AmountAlignmentKey.self] }
        set { self[AmountAlignmentKey.self] = newValue }
    }
}

extension View {
    public func amountStyle(_ style: AmountStyle) -> some View { environment(\.amountStyle, style) }
    public func showSign(_ value: ShowSign) -> some View { environment(\.amountShowSign, value) }
    public func fractionPolicy(_ value: FractionPolicy) -> some View { environment(\.amountFractionPolicy, value) }
    public func maximumNotationDigits(_ value: Int) -> some View { environment(\.amountMaximumNotationDigits, value) }
    public func amountAlignment(_ value: AmountAlignment) -> some View { environment(\.amountAlignment, value) }
}
#endif
