#if canImport(UIKit)
import UIKit

final class AmountHostView: UIView, UITextFieldDelegate {

    private let painter: AmountPainter
    private var currentConfig: AmountConfig
    private var currentCurrencyCode: String = ""

    private var inputFormatter: AmountFormatter
    private var displayFormatter: AmountFormatter

    private let hiddenField: UITextField?

    var onMoneyChange: ((Money) -> Void)?
    private var lastParsedMoney: Money?

    init(style: AmountStyle, mode: DiffMode, config: AmountConfig, editable: Bool) {
        self.painter = AmountPainter(style: style, mode: mode, config: config)
        self.currentConfig = config
        self.inputFormatter = Self.makeInputFormatter(config: config)
        self.displayFormatter = Self.makeDisplayFormatter(config: config)
        self.hiddenField = editable ? UITextField() : nil
        super.init(frame: .zero)
        layer.addSublayer(painter.rootLayer)
        painter.setDensity(traitCollection.displayScale)
        if let f = hiddenField {
            f.frame = CGRect(x: -10_000, y: -10_000, width: 1, height: 1)
            f.tintColor = .clear
            f.keyboardType = .decimalPad
            f.textContentType = nil
            f.autocorrectionType = .no
            f.spellCheckingType = .no
            f.delegate = self
            f.addTarget(self, action: #selector(editingBegan), for: .editingDidBegin)
            f.addTarget(self, action: #selector(editingEnded), for: .editingDidEnd)
            addSubview(f)
            let tap = UITapGestureRecognizer(target: self, action: #selector(handleTap))
            addGestureRecognizer(tap)
        }
    }

    required init?(coder: NSCoder) { fatalError() }

    override func layoutSubviews() {
        super.layoutSubviews()
        painter.rootLayer.frame = bounds
        painter.setBounds(width: bounds.width, height: bounds.height)
    }

    @objc private func handleTap() {
        hiddenField?.becomeFirstResponder()
    }

    override func traitCollectionDidChange(_ previousTraitCollection: UITraitCollection?) {
        super.traitCollectionDidChange(previousTraitCollection)
        painter.setDensity(traitCollection.displayScale)
    }

    func configure(style: AmountStyle, config: AmountConfig, alignment: AmountAlignment, currencyCode: String) {
        if config != currentConfig {
            currentConfig = config
            inputFormatter = Self.makeInputFormatter(config: config)
            displayFormatter = Self.makeDisplayFormatter(config: config)
        }
        currentCurrencyCode = currencyCode
        painter.updateStyle(style: style, config: config, alignment: alignment)
    }

    func setText(_ text: String, positions: AmountFieldPositions = .empty) {
        painter.setText(text, positions: positions)
    }

    func setCursorVisible(_ visible: Bool) {
        painter.setCursorVisible(visible)
    }

    func applyExternalMoney(_ money: Money) {
        guard hiddenField != nil else { return }
        if let last = lastParsedMoney, last == money { return }
        let input = inputFormatter.format(money)
        hiddenField?.text = input
        let positions = currentInputPositions(for: input)
        pushFormattedDisplay(forInputText: input, positions: positions)
        lastParsedMoney = money
    }

    func textField(
        _ textField: UITextField,
        shouldChangeCharactersIn range: NSRange,
        replacementString string: String
    ) -> Bool {
        let current = (textField.text ?? "") as NSString
        let proposed = current.replacingCharacters(in: range, with: string)
        let cursor = range.location + string.count
        let sanitized = sanitizeInput(text: proposed, cursor: cursor, config: currentConfig)
        textField.text = sanitized.text
        if let pos = textField.position(from: textField.beginningOfDocument, offset: sanitized.cursor) {
            textField.selectedTextRange = textField.textRange(from: pos, to: pos)
        }
        let positions = currentInputPositions(for: sanitized.text)
        pushFormattedDisplay(forInputText: sanitized.text, positions: positions)
        let parsed = displayFormatter.parse(sanitized.text, currencyCode: currentCurrencyCode)
        if parsed != lastParsedMoney {
            lastParsedMoney = parsed
            onMoneyChange?(parsed)
        }
        return false
    }

    @objc private func editingBegan() { painter.setCursorVisible(true) }
    @objc private func editingEnded() { painter.setCursorVisible(false) }

    private func pushFormattedDisplay(forInputText input: String, positions: AmountFieldPositions) {
        let formatted = displayFormatter.format(
            source: input,
            start: input.count,
            end: input.count,
            text: input,
            textStart: input.count,
            textEnd: input.count
        )
        painter.setText(formatted, positions: displayFormatter.fieldPositions())
    }

    private func currentInputPositions(for input: String) -> AmountFieldPositions {
        _ = inputFormatter.format(
            source: input,
            start: input.count,
            end: input.count,
            text: input,
            textStart: input.count,
            textEnd: input.count
        )
        return inputFormatter.fieldPositions()
    }

    private static func makeInputFormatter(config: AmountConfig) -> AmountFormatter {
        AmountFormatter(
            config: config,
            withCurrency: false,
            withGroupingSeparators: false,
            withFixedFractionLength: false,
            withFixedZeroNotation: true
        )
    }

    private static func makeDisplayFormatter(config: AmountConfig) -> AmountFormatter {
        AmountFormatter(config: config)
    }
}
#endif
