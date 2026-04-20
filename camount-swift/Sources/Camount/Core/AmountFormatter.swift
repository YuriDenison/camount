import Foundation

/// Stateful, single-threaded formatter. Not safe for concurrent use.
/// Scratch state (`notation`, `fraction`, `resultBuffer`, positions)
/// is reused across calls to avoid per-format allocation; each `format`
/// variant resets it at entry. Callers must pair `format(...)` with the
/// subsequent `fieldPositions()` read before invoking format again.
@MainActor
final class AmountFormatter {

    private let config: AmountConfig
    private let withCurrency: Bool
    private let withGroupingSeparators: Bool
    private let withFixedFractionLength: Bool
    private let withFixedZeroNotation: Bool

    private var fixedFractionPosition = FieldRange()
    private var zeroNotationPosition = FieldRange()
    private var currencySuffixPosition = FieldRange()
    private var cursorPosition = 0

    private var notation = ""
    private var separatorFound = false
    private var duplicateSeparator = false
    private var fraction = ""
    private var resultBuffer = ""

    init(
        config: AmountConfig,
        withCurrency: Bool = true,
        withGroupingSeparators: Bool = true,
        withFixedFractionLength: Bool = true,
        withFixedZeroNotation: Bool = true
    ) {
        self.config = config
        self.withCurrency = withCurrency
        self.withGroupingSeparators = withGroupingSeparators
        self.withFixedFractionLength = withFixedFractionLength
        self.withFixedZeroNotation = withFixedZeroNotation
    }

    // MARK: - Public API

    func format(_ money: Money) -> String {
        reset()

        let unitsStr = String(money.absoluteUnits)
        let units =
            unitsStr.count <= config.maximumNotationDigits
            ? unitsStr
            : String(unitsStr.prefix(config.maximumNotationDigits))
        resultBuffer.append(units)

        if config.maximumFractionDigits > 0 && money.nanos != 0 {
            let nanosPadded =
                String(
                    repeating: "0",
                    count: max(0, 9 - String(money.absoluteNanos).count))
                + String(money.absoluteNanos)
            let nanos =
                nanosPadded.count <= config.maximumFractionDigits
                ? nanosPadded
                : String(nanosPadded.prefix(config.maximumFractionDigits))

            if withFixedFractionLength || nanos.contains(where: { $0 != config.zero }) {
                resultBuffer.append(config.decimalSeparator)
                resultBuffer.append(nanos)
            }
        }

        appendCurrency()
        return resultBuffer
    }

    func format(
        source: String,
        start: Int,
        end: Int,
        text: String,
        textStart: Int,
        textEnd: Int
    ) -> String {
        reset()

        _ = appendRange(source, from: 0, upTo: start, withInputSeparator: false)
        let afterChangeCount = appendRange(text, from: textStart, upTo: textEnd, withInputSeparator: true)
        _ = appendRange(source, from: end, upTo: source.count, withInputSeparator: false)

        if duplicateSeparator {
            cursorPosition = end
            return source
        }
        let out = buildResult()
        cursorPosition = findSelection(in: out, selection: afterChangeCount)
        return out
    }

    var lastCursorPosition: Int { cursorPosition }

    func fieldPositions() -> AmountFieldPositions {
        AmountFieldPositions(
            cursorPosition: cursorPosition,
            fixedFraction: FieldRange(
                beginIndex: fixedFractionPosition.beginIndex,
                endIndex: fixedFractionPosition.endIndex
            ),
            zeroNotation: FieldRange(
                beginIndex: zeroNotationPosition.beginIndex,
                endIndex: zeroNotationPosition.endIndex
            )
        )
    }

    func parse(_ raw: String, currencyCode: String) -> Money {
        var negative = false
        var separator = false
        var hasDigits = false
        var integer = ""
        var fractionDigits = ""

        for c in raw {
            if c == "-" && !hasDigits {
                negative = true
            } else if config.isInputSeparator(c) {
                if !separator { separator = true }
            } else if config.isDigit(c) {
                hasDigits = true
                if separator { fractionDigits.append(c) } else { integer.append(c) }
            }
        }

        if !hasDigits { return Money(units: 0, nanos: 0, currencyCode: currencyCode) }

        let units = Int64(integer.isEmpty ? "0" : integer) ?? 0
        let nanoDigits =
            (fractionDigits
            + String(repeating: "0", count: max(0, 9 - fractionDigits.count))).prefix(9)
        let nanos = Int32(nanoDigits) ?? 0

        let signedUnits = negative ? -units : units
        let signedNanos = negative ? -nanos : nanos
        return Money(units: signedUnits, nanos: signedNanos, currencyCode: currencyCode)
    }

    // MARK: - Internals

    private func reset() {
        notation.removeAll(keepingCapacity: true)
        separatorFound = false
        duplicateSeparator = false
        fraction.removeAll(keepingCapacity: true)
        resultBuffer.removeAll(keepingCapacity: true)
        fixedFractionPosition.clear()
        zeroNotationPosition.clear()
        currencySuffixPosition.clear()
        cursorPosition = 0
    }

    private func appendRange(
        _ source: String,
        from start: Int,
        upTo end: Int,
        withInputSeparator: Bool
    ) -> Int {
        var count = 0
        let chars = Array(source)
        let clampedStart = max(0, min(start, chars.count))
        let clampedEnd = max(clampedStart, min(end, chars.count))
        var i = clampedStart
        while i < clampedEnd {
            if duplicateSeparator { break }
            let c = chars[i]
            if withInputSeparator && config.isInputSeparator(c) {
                count += ensureSeparator()
            } else if !withInputSeparator && config.isDecimalSeparator(c) {
                count += ensureSeparator()
            } else if config.isDigit(c) {
                count += appendDigit(c)
            }
            i += 1
        }
        return count
    }

    private func ensureSeparator() -> Int {
        if separatorFound {
            duplicateSeparator = true
            return 0
        }
        separatorFound = true
        return 1
    }

    private func appendDigit(_ c: Character) -> Int {
        if separatorFound {
            if fraction.count < config.maximumFractionDigits {
                fraction.append(c)
                return 1
            }
        } else {
            if notation.count < config.maximumNotationDigits {
                if notation.count == 1 && config.isZero(notation.first!) {
                    if !config.isZero(c) {
                        notation.removeFirst()
                        notation.append(c)
                    }
                } else {
                    notation.append(c)
                    return 1
                }
            }
        }
        return 0
    }

    private func buildResult() -> String {
        appendNotation()
        appendFraction()
        appendCurrency()
        return resultBuffer
    }

    private func appendNotation() {
        if !notation.isEmpty {
            resultBuffer.append(notation)
            if withGroupingSeparators {
                let groupLength = config.groupingSize
                if groupLength > 0 {
                    let notationLength = notation.count
                    if notationLength > groupLength {
                        var offset = notationLength - groupLength
                        while offset >= 1 {
                            let insertIndex = resultBuffer.index(resultBuffer.startIndex, offsetBy: offset)
                            resultBuffer.insert(config.groupingSeparator, at: insertIndex)
                            offset -= groupLength
                        }
                    }
                }
            }
        } else if withFixedZeroNotation {
            savePosition(&zeroNotationPosition) {
                resultBuffer.append(config.zero)
            }
        }
    }

    private func appendFraction() {
        guard separatorFound else { return }
        guard config.maximumFractionDigits > 0 else { return }

        zeroNotationPosition.clear()

        if resultBuffer.isEmpty { resultBuffer.append(config.zero) }

        resultBuffer.append(config.decimalSeparator)
        resultBuffer.append(fraction)

        if withFixedFractionLength {
            savePosition(&fixedFractionPosition) {
                let pad = config.maximumFractionDigits - fraction.count
                if pad > 0 {
                    resultBuffer.append(String(repeating: String(config.zero), count: pad))
                }
            }
        }
    }

    private func appendCurrency() {
        guard withCurrency else { return }

        if !config.prefix.isEmpty {
            resultBuffer = config.prefix + resultBuffer
            fixedFractionPosition.offset(by: config.prefix.count)
            zeroNotationPosition.offset(by: config.prefix.count)
        }

        if !config.suffix.isEmpty {
            savePosition(&currencySuffixPosition) {
                resultBuffer.append(config.suffix)
            }
        }
    }

    private func findSelection(in text: String, selection: Int) -> Int {
        var count = selection
        let trailingRegions = [
            currencySuffixPosition,
            fixedFractionPosition,
            zeroNotationPosition,
        ].filter { $0.isValid }
        var index = trailingRegions.map { $0.beginIndex }.min() ?? text.count
        let chars = Array(text)
        while index > 0 && count > 0 {
            let c = chars[index - 1]
            if config.isDecimalSeparator(c) || config.isDigit(c) {
                count -= 1
            }
            index -= 1
        }
        return index
    }

    private func savePosition(_ position: inout FieldRange, _ block: () -> Void) {
        position.beginIndex = resultBuffer.count
        block()
        position.endIndex = resultBuffer.count
    }
}
