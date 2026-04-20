import Foundation

struct CurrencyInfo: Equatable {
    let decimalSeparator: Character
    let groupingSeparator: Character
    let prefix: String
    let suffix: String
    let groupingSize: Int
    let maximumFractionDigits: Int

    static func forCurrency(_ currencyCode: String) -> CurrencyInfo {
        let localeId = NSLocale.localeIdentifier(
            fromComponents: [NSLocale.Key.currencyCode.rawValue: currencyCode]
        )
        let formatter = NumberFormatter()
        formatter.numberStyle = .currency
        formatter.locale = Locale(identifier: localeId)
        formatter.currencyCode = currencyCode

        let decimal: Character = formatter.currencyDecimalSeparator?.first ?? "."
        let grouping: Character = formatter.currencyGroupingSeparator?.first ?? ","

        return CurrencyInfo(
            decimalSeparator: decimal,
            groupingSeparator: grouping,
            prefix: _sanitizeBidi(formatter.positivePrefix ?? ""),
            suffix: _sanitizeBidi(formatter.positiveSuffix ?? ""),
            groupingSize: max(0, formatter.groupingSize),
            maximumFractionDigits: formatter.maximumFractionDigits
        )
    }

    static func _sanitizeBidi(_ s: String) -> String {
        var out = ""
        out.reserveCapacity(s.count)
        for scalar in s.unicodeScalars {
            switch scalar.value {
            case 0x200E, 0x200F,
                0x202A, 0x202B, 0x202C, 0x202D, 0x202E,
                0x2066, 0x2067, 0x2068, 0x2069:
                continue
            default:
                out.unicodeScalars.append(scalar)
            }
        }
        return out
    }
}

extension AmountConfig {
    static func forCurrency(_ currencyCode: String, maximumNotationDigits: Int) -> AmountConfig {
        let info = CurrencyInfo.forCurrency(currencyCode)
        return AmountConfig(
            maximumNotationDigits: maximumNotationDigits,
            decimalSeparator: info.decimalSeparator,
            groupingSeparator: info.groupingSeparator,
            prefix: info.prefix,
            suffix: info.suffix,
            groupingSize: info.groupingSize,
            maximumFractionDigits: info.maximumFractionDigits
        )
    }
}
