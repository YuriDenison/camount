import { describe, expect, it } from "vitest";
import "../../src/elements";
import type { CamountFieldElement, CamountFieldChangeEventDetail } from "../../src/elements/amountField";

describe("<camount-field>", () => {
  it("registers the element", () => {
    expect(customElements.get("camount-field")).toBeDefined();
  });

  it("fires camount-change on input", () => {
    const el = document.createElement("camount-field") as CamountFieldElement;
    el.setAttribute("currency", "USD");
    document.body.appendChild(el);
    const shadow = el.shadowRoot!;
    const hidden = shadow.querySelector<HTMLInputElement>("input.camount-hidden-input")!;
    let last: CamountFieldChangeEventDetail | null = null;
    el.addEventListener("camount-change", (e) => {
      last = (e as CustomEvent<CamountFieldChangeEventDetail>).detail;
    });
    hidden.value = "5";
    hidden.dispatchEvent(new Event("input"));
    expect(last).not.toBeNull();
    expect(last!.units).toBe(5n);
    expect(last!.currencyCode).toBe("USD");
    el.remove();
  });
});
