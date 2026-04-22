const CSS = `
:host {
  display: inline-block;
  position: relative;
  line-height: 1;
  font: inherit;
  color: inherit;
  white-space: nowrap;
  min-height: 1em;
}
.camount-root {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  overflow: visible;
}
.camount-sizer {
  display: block;
  visibility: hidden;
  white-space: pre;
  pointer-events: none;
  line-height: 1;
}
.camount-cell {
  position: absolute;
  top: 0;
  left: 0;
  transform-origin: 0 0;
  pointer-events: none;
  will-change: transform;
}
.camount-glyph {
  position: absolute;
  top: 0;
  left: 0;
  transform-origin: center center;
  line-height: 1;
  white-space: pre;
  pointer-events: none;
  will-change: transform, opacity;
}
.camount-glyph[data-gradient="true"] {
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
  background-image: var(--camount-gradient, none);
  background-size: var(--camount-gradient-size, 100% 100%);
  background-position: var(--camount-gradient-pos, 0 0);
  background-repeat: no-repeat;
  background-attachment: scroll;
}
.camount-cursor {
  position: absolute;
  top: 0;
  left: 0;
  transform-origin: 0 0;
  background-color: var(--camount-cursor-color, currentColor);
  border-radius: 1px;
  opacity: 0;
  pointer-events: none;
  will-change: transform, opacity;
}
.camount-hidden-input {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  opacity: 0;
  cursor: text;
  background: transparent;
  border: 0;
  padding: 0;
  margin: 0;
  color: transparent;
  caret-color: transparent;
  font: inherit;
}
.camount-measure {
  position: absolute;
  top: 0;
  left: 0;
  visibility: hidden;
  pointer-events: none;
  white-space: pre;
  line-height: 1;
}
`;

let sharedSheet: CSSStyleSheet | null = null;

export function camountStyleSheet(): CSSStyleSheet {
  if (sharedSheet) return sharedSheet;
  const sheet = new CSSStyleSheet();
  sheet.replaceSync(CSS);
  sharedSheet = sheet;
  return sheet;
}

export const CAMOUNT_CSS = CSS;
