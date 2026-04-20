import Foundation

struct FieldRange: Equatable {
    var beginIndex: Int
    var endIndex: Int

    init(beginIndex: Int = 0, endIndex: Int = 0) {
        self.beginIndex = beginIndex
        self.endIndex = endIndex
    }

    var length: Int { isValid ? endIndex - beginIndex : 0 }
    var isValid: Bool { beginIndex < endIndex && beginIndex >= 0 }

    mutating func clear() {
        beginIndex = 0
        endIndex = 0
    }

    mutating func offset(by value: Int) {
        guard isValid else { return }
        beginIndex += value
        endIndex += value
    }

    func contains(_ index: Int) -> Bool {
        index >= beginIndex && index < endIndex
    }
}

enum AmountFieldKind {
    case fixedFraction
    case zeroNotation
    case currencySuffix
}

struct AmountFieldPositions: Equatable {
    let cursorPosition: Int
    let fixedFraction: FieldRange
    let zeroNotation: FieldRange

    static let empty = AmountFieldPositions(
        cursorPosition: -1,
        fixedFraction: FieldRange(),
        zeroNotation: FieldRange()
    )
}
