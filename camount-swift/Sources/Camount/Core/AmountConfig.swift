import Foundation

struct AmountConfig: Equatable {
    let maximumNotationDigits: Int
    let decimalSeparator: Character
    let groupingSeparator: Character
    let prefix: String
    let suffix: String
    let groupingSize: Int
    let maximumFractionDigits: Int

    private static let digitChars: [Character] =
        ["0", "1", "2", "3", "4", "5", "6", "7", "8", "9"]

    var zero: Character { Self.digitChars[0] }

    var maximumFormattedSymbols: Int {
        let groupingSeparators =
            groupingSize == 0
            ? 0
            : (maximumNotationDigits - 1) / groupingSize
        return prefix.count + maximumNotationDigits + groupingSeparators + 1 + maximumFractionDigits + suffix.count
    }

    func isDigit(_ c: Character) -> Bool { Self.digitChars.contains(c) }
    func isZero(_ c: Character) -> Bool { zero == c }
    func isInputSeparator(_ c: Character) -> Bool { c == "." || c == "," }
    func isDecimalSeparator(_ c: Character) -> Bool { decimalSeparator == c }
    func isGroupingSeparator(_ c: Character) -> Bool {
        groupingSize > 0 && groupingSeparator == c
    }

    func digit(at index: Int) -> Character { Self.digitChars[index] }
}
