#if canImport(UIKit)
import UIKit
import SwiftUI

public struct AmountStyle: Equatable {
    public let font: UIFont
    public let color: UIColor
    public let gradient: Gradient?
    public let cursor: CursorStyle?
    public let zeroNotationColor: UIColor?
    public let fixedFractionColor: UIColor?

    public init(
        font: UIFont,
        color: UIColor = .label,
        gradient: Gradient? = nil,
        cursor: CursorStyle? = nil,
        zeroNotationColor: UIColor? = nil,
        fixedFractionColor: UIColor? = nil
    ) {
        self.font = font
        self.color = color
        self.gradient = gradient
        self.cursor = cursor
        self.zeroNotationColor = zeroNotationColor
        self.fixedFractionColor = fixedFractionColor
    }

    public static let `default` = AmountStyle(font: .systemFont(ofSize: 20))

    public static func == (lhs: AmountStyle, rhs: AmountStyle) -> Bool {
        fontsEqual(lhs.font, rhs.font)
            && colorsEqual(lhs.color, rhs.color)
            && lhs.gradient == rhs.gradient
            && lhs.cursor == rhs.cursor
            && colorsEqual(lhs.zeroNotationColor, rhs.zeroNotationColor)
            && colorsEqual(lhs.fixedFractionColor, rhs.fixedFractionColor)
    }

    struct Effective {
        let font: UIFont
        let color: UIColor
    }

    func effectiveTextStyle(for field: AmountFieldKind?) -> Effective {
        switch field {
        case .zeroNotation:
            return Effective(font: font, color: zeroNotationColor ?? color)
        case .fixedFraction:
            return Effective(font: font, color: fixedFractionColor ?? color)
        case .currencySuffix, .none:
            return Effective(font: font, color: color)
        }
    }
}

public struct CursorStyle: Equatable {
    public let color: UIColor
    public let width: CGFloat
    public let heightFraction: CGFloat

    public init(color: UIColor, width: CGFloat = 2, heightFraction: CGFloat = 1.0) {
        precondition((0.0...1.0).contains(heightFraction), "heightFraction must be in 0...1")
        self.color = color
        self.width = width
        self.heightFraction = heightFraction
    }

    public static func == (lhs: CursorStyle, rhs: CursorStyle) -> Bool {
        colorsEqual(lhs.color, rhs.color)
            && lhs.width == rhs.width
            && lhs.heightFraction == rhs.heightFraction
    }
}

private func fontsEqual(_ a: UIFont, _ b: UIFont) -> Bool {
    a === b || (a.fontName == b.fontName && a.pointSize == b.pointSize)
}

private func colorsEqual(_ a: UIColor?, _ b: UIColor?) -> Bool {
    switch (a, b) {
    case (nil, nil): return true
    case (let x?, let y?): return x.isEqual(y)
    default: return false
    }
}
#endif
