#if canImport(UIKit)
import SwiftUI

public struct AmountField: View {
    @Binding private var amount: Money

    public init(_ amount: Binding<Money>) { self._amount = amount }

    public var body: some View {
        Representable(amount: $amount)
    }

    fileprivate struct Representable: UIViewRepresentable {
        @Binding var amount: Money
        @Environment(\.amountStyle) var style
        @Environment(\.amountMaximumNotationDigits) var maxNotationDigits
        @Environment(\.amountAlignment) var alignment

        func makeUIView(context: Context) -> AmountHostView {
            let config = AmountConfig.forCurrency(amount.currencyCode, maximumNotationDigits: maxNotationDigits)
            let view = AmountHostView(style: style, mode: .edit, config: config, editable: true)
            view.configure(style: style, config: config, alignment: alignment, currencyCode: amount.currencyCode)
            view.onMoneyChange = { [binding = $amount] new in
                if binding.wrappedValue != new { binding.wrappedValue = new }
            }
            view.applyExternalMoney(amount)
            return view
        }

        func updateUIView(_ view: AmountHostView, context: Context) {
            let config = AmountConfig.forCurrency(amount.currencyCode, maximumNotationDigits: maxNotationDigits)
            view.configure(style: style, config: config, alignment: alignment, currencyCode: amount.currencyCode)
            view.applyExternalMoney(amount)
        }
    }
}
#endif
