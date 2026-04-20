#if canImport(UIKit)
import SwiftUI

public struct AmountText: View {
    private let amount: Money

    public init(_ amount: Money) { self.amount = amount }

    public var body: some View {
        Representable(amount: amount)
    }

    fileprivate struct Representable: UIViewRepresentable {
        let amount: Money
        @Environment(\.amountStyle) var style
        @Environment(\.amountShowSign) var showSign
        @Environment(\.amountFractionPolicy) var fractionPolicy
        @Environment(\.amountMaximumNotationDigits) var maxNotationDigits
        @Environment(\.amountAlignment) var alignment

        func makeUIView(context: Context) -> AmountHostView {
            let config = AmountConfig.forCurrency(amount.currencyCode, maximumNotationDigits: maxNotationDigits)
            let view = AmountHostView(style: style, mode: .levenshtein, config: config, editable: false)
            view.configure(style: style, config: config, alignment: alignment, currencyCode: amount.currencyCode)
            return view
        }

        func updateUIView(_ view: AmountHostView, context: Context) {
            let config = AmountConfig.forCurrency(amount.currencyCode, maximumNotationDigits: maxNotationDigits)
            view.configure(style: style, config: config, alignment: alignment, currencyCode: amount.currencyCode)
            let formatter = AmountFormatter(
                config: config,
                withCurrency: true,
                withGroupingSeparators: true,
                withFixedFractionLength: fractionPolicy == .fixed,
                withFixedZeroNotation: true
            )
            let base = formatter.format(amount)
            let rendered: String
            if amount.isZero {
                rendered = base
            } else if !amount.isPositive {
                rendered = "-" + base
            } else if showSign == .always {
                rendered = "+" + base
            } else {
                rendered = base
            }
            view.setText(rendered, positions: .empty)
        }
    }
}
#endif
