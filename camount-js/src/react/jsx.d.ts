import type React from "react";

declare global {
  namespace JSX {
    interface IntrinsicElements {
      "camount-text": React.DetailedHTMLProps<
        React.HTMLAttributes<HTMLElement> & {
          amount?: string;
          currency?: string;
          "maximum-notation-digits"?: string;
          "show-sign"?: "auto" | "always";
          "fraction-policy"?: "fixed" | "compact";
          alignment?: "start" | "center" | "end";
        },
        HTMLElement
      >;
      "camount-field": React.DetailedHTMLProps<
        React.HTMLAttributes<HTMLElement> & {
          value?: string;
          currency?: string;
          "maximum-notation-digits"?: string;
          alignment?: "start" | "center" | "end";
        },
        HTMLElement
      >;
    }
  }
}

export {};
