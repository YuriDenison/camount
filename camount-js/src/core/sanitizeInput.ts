import type { AmountConfig, SanitizedInput } from "./types";
import { configIsDigit, configIsInputSeparator } from "./types";

export function sanitizeInput(
  text: string,
  cursor: number,
  config: AmountConfig,
): SanitizedInput {
  const src = [...text];
  let builder = "";
  let separatorSeen = false;
  let integerDigits = 0;
  let fractionDigits = 0;
  const originalCursor = Math.max(0, Math.min(cursor, src.length));
  let mappedCursor = 0;

  for (let i = 0; i < src.length; i++) {
    const c = src[i]!;
    let kept = false;
    if (configIsDigit(config, c)) {
      const underLimit = separatorSeen
        ? fractionDigits < config.maximumFractionDigits
        : integerDigits < config.maximumNotationDigits;
      if (underLimit) {
        builder += c;
        if (separatorSeen) fractionDigits += 1;
        else integerDigits += 1;
        kept = true;
      }
    } else if (
      configIsInputSeparator(config, c) &&
      !separatorSeen &&
      config.maximumFractionDigits > 0
    ) {
      separatorSeen = true;
      builder += config.decimalSeparator;
      kept = true;
    }
    if (kept && i < originalCursor) mappedCursor += 1;
  }

  return { text: builder, cursor: Math.min(mappedCursor, builder.length) };
}
