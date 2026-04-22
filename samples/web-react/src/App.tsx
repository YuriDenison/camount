import * as React from "react";
import { AmountText, AmountField } from "@yuridenison/camount/react";
import type { CamountFieldChangeEventDetail } from "@yuridenison/camount/react";
import { SAMPLE_CURRENCIES } from "./currencies";

interface Money {
  units: bigint;
  nanos: number;
  currencyCode: string;
}

function moneyToString(m: Money): string {
  const neg = m.units < 0n || m.nanos < 0;
  const u = m.units < 0n ? -m.units : m.units;
  const n = m.nanos < 0 ? -m.nanos : m.nanos;
  const frac = n.toString().padStart(9, "0").replace(/0+$/, "");
  const suffix = frac.length === 0 ? "" : `.${frac}`;
  return `${neg ? "-" : ""}${u.toString()}${suffix}`;
}

function randomMoney(currencyCode: string): Money {
  return {
    units: BigInt(Math.floor(Math.random() * 99_999)),
    nanos: Math.floor(Math.random() * 1_000_000_000),
    currencyCode,
  };
}

export function App(): React.ReactElement {
  const [money, setMoney] = React.useState<Money>({
    units: 1234n,
    nanos: 560_000_000,
    currencyCode: "EUR",
  });
  const [pickerOpen, setPickerOpen] = React.useState(false);

  const onFieldChange = React.useCallback((d: CamountFieldChangeEventDetail) => {
    setMoney((prev) => {
      if (prev.units === d.units && prev.nanos === d.nanos && prev.currencyCode === d.currencyCode) {
        return prev;
      }
      return { units: d.units, nanos: d.nanos, currencyCode: d.currencyCode };
    });
  }, []);

  const amountString = moneyToString(money);

  return (
    <div className="camount-app">
      <div className="stack">
        <header>
          <h1>Camount</h1>
          <p className="subtitle">
            Every widget below shares the same Money — change any, watch the rest animate in sync.
          </p>
        </header>

        <div className="card controls">
          <button className="currency-dropdown" onClick={() => setPickerOpen(true)}>
            <span className="code">{money.currencyCode}</span>
            <span className="name">
              {SAMPLE_CURRENCIES.find((c) => c.code === money.currencyCode)?.name ?? money.currencyCode}
            </span>
            <span className="chev">▾</span>
          </button>
          <div className="btn-row">
            <button
              className="btn"
              onClick={() => setMoney(randomMoney(money.currencyCode))}
            >
              Shuffle
            </button>
            <button
              className="btn primary"
              onClick={() =>
                setMoney((m) => ({ ...m, units: m.units + 1n }))
              }
            >
              +1
            </button>
            <button
              className="btn outline"
              onClick={() =>
                setMoney((m) => ({ units: 0n, nanos: 0, currencyCode: m.currencyCode }))
              }
            >
              Reset
            </button>
          </div>
        </div>

        <section className="card">
          <div className="section-head">
            <span className="title">AmountText</span>
            <span className="subtitle">React (camount-js)</span>
          </div>
          <div className="text-big">
            <AmountText amount={amountString} currency={money.currencyCode} />
          </div>
          <div className="btn-row" style={{ marginTop: 10 }}>
            <div className="labeled-amount text-mid">
              <span className="label">Always signed</span>
              <AmountText amount={amountString} currency={money.currencyCode} showSign="always" />
            </div>
            <div className="labeled-amount text-muted">
              <span className="label">No trailing zeros</span>
              <AmountText amount={amountString} currency={money.currencyCode} fractionPolicy="compact" />
            </div>
          </div>
        </section>

        <section className="card">
          <div className="section-head">
            <span className="title">AmountField</span>
            <span className="subtitle">React (camount-js)</span>
          </div>
          <div style={{ display: "flex", flexDirection: "column", gap: 12 }}>
            <div className="field-box field-default">
              <AmountField value={amountString} currency={money.currencyCode} onChange={onFieldChange} />
            </div>
            <div className="field-box field-gradient">
              <AmountField value={amountString} currency={money.currencyCode} onChange={onFieldChange} />
            </div>
            <div className="field-box field-compact">
              <AmountField value={amountString} currency={money.currencyCode} onChange={onFieldChange} />
            </div>
          </div>
        </section>
      </div>

      {pickerOpen && (
        <div className="picker-backdrop" onClick={() => setPickerOpen(false)}>
          <div className="picker-sheet" onClick={(e) => e.stopPropagation()}>
            <h2>Choose currency</h2>
            {SAMPLE_CURRENCIES.map((c) => {
              const selected = c.code === money.currencyCode;
              return (
                <button
                  key={c.code}
                  className={`picker-row${selected ? " selected" : ""}`}
                  onClick={() => {
                    setMoney((m) => ({ ...m, currencyCode: c.code }));
                    setPickerOpen(false);
                  }}
                >
                  <span className="code">{c.code}</span>
                  <span className="name">{c.name}</span>
                  {selected && <span className="check">✓</span>}
                </button>
              );
            })}
          </div>
        </div>
      )}
    </div>
  );
}
