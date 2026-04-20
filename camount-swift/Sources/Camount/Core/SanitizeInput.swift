import Foundation

struct SanitizedInput {
    let text: String
    let cursor: Int
}

func sanitizeInput(text: String, cursor: Int, config: AmountConfig) -> SanitizedInput {
    let src = Array(text)
    var builder = ""
    var separatorSeen = false
    var integerDigits = 0
    var fractionDigits = 0
    let originalCursor = max(0, min(cursor, src.count))
    var mappedCursor = 0

    for i in 0..<src.count {
        let c = src[i]
        var kept = false
        if config.isDigit(c) {
            let underLimit =
                separatorSeen
                ? fractionDigits < config.maximumFractionDigits
                : integerDigits < config.maximumNotationDigits
            if underLimit {
                builder.append(c)
                if separatorSeen { fractionDigits += 1 } else { integerDigits += 1 }
                kept = true
            }
        } else if config.isInputSeparator(c) && !separatorSeen && config.maximumFractionDigits > 0 {
            separatorSeen = true
            builder.append(config.decimalSeparator)
            kept = true
        }
        if kept && i < originalCursor { mappedCursor += 1 }
    }

    return SanitizedInput(text: builder, cursor: min(mappedCursor, builder.count))
}
