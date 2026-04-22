import { describe, expect, it } from "vitest";
import { SymbolCell } from "../../src/rendering/symbolCell";
import { AnimationScope } from "../../src/rendering/tween";
import { GlyphMeasurer } from "../../src/rendering/measure";

function makeCell() {
  const host = document.createElement("span");
  document.body.appendChild(host);
  const scope = new AnimationScope(window);
  const measurer = new GlyphMeasurer(host);
  return { cell: new SymbolCell(document, scope, measurer), host };
}

describe("SymbolCell", () => {
  it("starts hidden", () => {
    const { cell, host } = makeCell();
    expect(cell.isVisible).toBe(false);
    expect(cell.currentChar).toBe("\0");
    expect(cell.element).toBeInstanceOf(HTMLElement);
    host.remove();
  });

  it("replace sets char, visibility, and DOM text", () => {
    const { cell, host } = makeCell();
    cell.replace("5", undefined);
    expect(cell.isVisible).toBe(true);
    expect(cell.currentChar).toBe("5");
    expect(cell.element.textContent).toContain("5");
    host.remove();
  });

  it("delete hides", () => {
    const { cell, host } = makeCell();
    cell.replace("5", undefined);
    cell.delete();
    expect(cell.isVisible).toBe(false);
    host.remove();
  });

  it("field updates data attribute when a hint color applies", () => {
    const { cell, host } = makeCell();
    cell.setStyleHints({ zeroNotationColor: "rgb(1, 2, 3)" });
    cell.replace("0", "zeroNotation");
    const glyph = cell.element.querySelector<HTMLElement>(".camount-glyph");
    expect(glyph).not.toBeNull();
    expect(glyph!.style.color).toBe("rgb(1, 2, 3)");
    host.remove();
  });
});
