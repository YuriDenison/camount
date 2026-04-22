import * as React from "react";
import "../elements";
import type {} from "./jsx";

export interface AmountTextProps {
  amount: number | string | bigint;
  currency: string;
  maximumNotationDigits?: number;
  showSign?: "auto" | "always";
  fractionPolicy?: "fixed" | "compact";
  alignment?: "start" | "center" | "end";
  className?: string;
  style?: React.CSSProperties;
}

export const AmountText = React.forwardRef<HTMLElement, AmountTextProps>(
  (props, ref) => {
    const {
      amount,
      currency,
      maximumNotationDigits,
      showSign,
      fractionPolicy,
      alignment,
      className,
      style,
    } = props;
    return (
      <camount-text
        ref={ref}
        amount={String(amount)}
        currency={currency}
        {...(maximumNotationDigits !== undefined
          ? { "maximum-notation-digits": String(maximumNotationDigits) }
          : null)}
        {...(showSign !== undefined ? { "show-sign": showSign } : null)}
        {...(fractionPolicy !== undefined ? { "fraction-policy": fractionPolicy } : null)}
        {...(alignment !== undefined ? { alignment } : null)}
        className={className}
        style={style}
      />
    );
  },
);
AmountText.displayName = "AmountText";
