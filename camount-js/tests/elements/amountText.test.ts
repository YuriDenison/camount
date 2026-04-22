import { describe, expect, it } from "vitest";
import "../../src/elements";

describe("<camount-text>", () => {
  it("registers the element", () => {
    expect(customElements.get("camount-text")).toBeDefined();
  });

  it("renders a currency amount", () => {
    const el = document.createElement("camount-text");
    el.style.width = "400px";
    el.style.height = "60px";
    el.setAttribute("amount", "42.50");
    el.setAttribute("currency", "USD");
    document.body.appendChild(el);
    const shadow = el.shadowRoot!;
    const text = Array.from(shadow.querySelectorAll(".camount-glyph"))
      .map((e) => e.textContent)
      .join("");
    expect(text).toContain("42");
    expect(text).toContain("50");
    el.remove();
  });

  it("updates when amount attribute changes", () => {
    const el = document.createElement("camount-text");
    el.style.width = "400px";
    el.style.height = "60px";
    el.setAttribute("amount", "1");
    el.setAttribute("currency", "USD");
    document.body.appendChild(el);
    el.setAttribute("amount", "2");
    const shadow = el.shadowRoot!;
    const text = Array.from(shadow.querySelectorAll(".camount-glyph"))
      .map((e) => e.textContent)
      .join("");
    expect(text).toContain("2");
    el.remove();
  });
});
