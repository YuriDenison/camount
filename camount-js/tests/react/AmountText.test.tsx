import { describe, expect, it } from "vitest";
import * as React from "react";
import { createRoot } from "react-dom/client";
import { act } from "react";
import { AmountText } from "../../src/react";

describe("<AmountText /> (React)", () => {
  it("renders the custom element and mirrors the amount attribute", async () => {
    const container = document.createElement("div");
    document.body.appendChild(container);
    const root = createRoot(container);
    await act(async () => {
      root.render(<AmountText amount="42.50" currency="USD" />);
    });
    const ct = container.querySelector("camount-text");
    expect(ct).not.toBeNull();
    expect(ct!.getAttribute("amount")).toBe("42.50");
    expect(ct!.getAttribute("currency")).toBe("USD");
    await act(async () => {
      root.unmount();
    });
    container.remove();
  });
});
