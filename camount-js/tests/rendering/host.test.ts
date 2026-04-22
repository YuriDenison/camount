import { describe, expect, it } from "vitest";
import { AmountHost } from "../../src/rendering/host";

describe("AmountHost (text mode)", () => {
  it("renders a money value into the shadow DOM", () => {
    const hostEl = document.createElement("div");
    hostEl.style.width = "400px";
    hostEl.style.height = "60px";
    const shadow = hostEl.attachShadow({ mode: "open" });
    document.body.appendChild(hostEl);
    const host = new AmountHost(shadow, { currencyCode: "USD" });
    host.setAmount({ units: 42n, nanos: 0, currencyCode: "USD" });
    const glyphs = shadow.querySelectorAll(".camount-glyph");
    const text = Array.from(glyphs).map((e) => e.textContent).join("");
    expect(text).toContain("42");
    host.dispose();
    hostEl.remove();
  });

  it("updates on second setAmount", () => {
    const hostEl = document.createElement("div");
    hostEl.style.width = "400px";
    hostEl.style.height = "60px";
    const shadow = hostEl.attachShadow({ mode: "open" });
    document.body.appendChild(hostEl);
    const host = new AmountHost(shadow, { currencyCode: "USD" });
    host.setAmount({ units: 1n, nanos: 500_000_000, currencyCode: "USD" });
    host.setAmount({ units: 2n, nanos: 500_000_000, currencyCode: "USD" });
    const glyphs = shadow.querySelectorAll(".camount-glyph");
    const text = Array.from(glyphs).map((e) => e.textContent).join("");
    expect(text).toContain("2");
    expect(text).toContain("5");
    host.dispose();
    hostEl.remove();
  });
});
