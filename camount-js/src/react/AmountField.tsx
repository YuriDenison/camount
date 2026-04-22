import * as React from "react";
import "../elements";
import type {} from "./jsx";
import type { CamountFieldChangeEventDetail, CamountFieldElement } from "../elements/amountField";

export interface AmountFieldProps {
  value: number | string | bigint;
  currency: string;
  onChange?: (detail: CamountFieldChangeEventDetail) => void;
  maximumNotationDigits?: number;
  alignment?: "start" | "center" | "end";
  className?: string;
  style?: React.CSSProperties;
}

export const AmountField = React.forwardRef<HTMLElement, AmountFieldProps>(
  (props, ref) => {
    const {
      value,
      currency,
      onChange,
      maximumNotationDigits,
      alignment,
      className,
      style,
    } = props;
    const innerRef = React.useRef<HTMLElement | null>(null);

    React.useEffect(() => {
      const el = innerRef.current as CamountFieldElement | null;
      if (!el || !onChange) return;
      const handler = (e: Event) =>
        onChange((e as CustomEvent<CamountFieldChangeEventDetail>).detail);
      el.addEventListener("camount-change", handler);
      return () => {
        el.removeEventListener("camount-change", handler);
      };
    }, [onChange]);

    const setRefs = React.useCallback(
      (el: HTMLElement | null) => {
        innerRef.current = el;
        if (typeof ref === "function") ref(el);
        else if (ref) (ref as React.MutableRefObject<HTMLElement | null>).current = el;
      },
      [ref],
    );

    return (
      <camount-field
        ref={setRefs}
        value={String(value)}
        currency={currency}
        {...(maximumNotationDigits !== undefined
          ? { "maximum-notation-digits": String(maximumNotationDigits) }
          : null)}
        {...(alignment !== undefined ? { alignment } : null)}
        className={className}
        style={style}
      />
    );
  },
);
AmountField.displayName = "AmountField";
