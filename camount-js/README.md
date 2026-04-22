# @yuridenison/camount

Animated currency/amount widget for the web: per-character stack animations, field-aware styling, framework-agnostic Web Components, optional React wrapper. TypeScript port of [Camount](https://github.com/yuridenison/camount) — shares tag `v0.9.1` with the Kotlin Multiplatform and Swift Package releases.

## Install

```shell
npm install @yuridenison/camount
```

React is an optional peer dependency. If you want the React wrapper, install it alongside:

```shell
npm install react react-dom
```

## Usage

### Web Components (no framework)

Import the package once to register `<camount-text>` and `<camount-field>`:

```ts
import "@yuridenison/camount";
```

```html
<camount-text amount="1234.56" currency="USD"></camount-text>

<camount-field value="0" currency="USD"></camount-field>
<script>
  document.querySelector("camount-field").addEventListener("camount-change", (e) => {
    console.log(e.detail); // { amount, units, nanos, currencyCode }
  });
</script>
```

Attributes:

- `<camount-text>`: `amount`, `currency`, `maximum-notation-digits`, `show-sign` (`auto`|`always`), `fraction-policy` (`fixed`|`compact`), `alignment` (`start`|`center`|`end`).
- `<camount-field>`: `value`, `currency`, `maximum-notation-digits`, `alignment`. Dispatches `camount-change` as a bubbling, composed `CustomEvent`.

### React

```tsx
import { AmountText, AmountField } from "@yuridenison/camount/react";

export function Wallet() {
  return (
    <>
      <AmountText amount="1234.56" currency="USD" />
      <AmountField
        value="0"
        currency="USD"
        onChange={(detail) => console.log(detail)}
      />
    </>
  );
}
```

## Development

```shell
npm install
npm run typecheck
npm test
npm run build
```

## License

Apache-2.0
