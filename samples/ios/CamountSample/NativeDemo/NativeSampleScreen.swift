import Camount
import SwiftUI
import UIKit

struct NativeSampleScreen: View {
  @Binding var money: Money

  @State private var pickerOpen = false

  var body: some View {
    ScrollView {
      VStack(alignment: .leading, spacing: 20) {
        header

        controlsCard

        amountTextSection

        amountFieldSection

        Spacer(minLength: 8).frame(height: 8)
      }
      .padding(.horizontal, 20)
      .padding(.top, 16)
      .padding(.bottom, 24)
    }
    .background(NativeDemoTheme.canvasColor.ignoresSafeArea())
    .sheet(isPresented: $pickerOpen) {
      CurrencyPickerSheet(
        selected: money.currencyCode,
        onSelect: { newCode in
          money = Money(units: money.units, nanos: money.nanos, currencyCode: newCode)
        }
      )
    }
  }

  private var header: some View {
    VStack(alignment: .leading, spacing: 4) {
      Text("Camount")
        .font(NativeDemoTheme.manropeFont(34, weight: .bold))
        .foregroundColor(NativeDemoTheme.inkColor)
        .tracking(-0.5)
      Text("Every widget below shares the same Money — change any, watch the rest animate in sync.")
        .font(NativeDemoTheme.manropeFont(14, weight: .medium))
        .foregroundColor(NativeDemoTheme.inkMutedColor)
    }
  }

  private var controlsCard: some View {
    ElevatedCard {
      VStack(spacing: 14) {
        currencyDropdown
        HStack(spacing: 10) {
          actionButton(label: "Shuffle") {
            money = randomMoney(currencyCode: money.currencyCode)
          }
          .frame(maxWidth: .infinity)

          plusOneButton {
            money = Money(units: money.units + 1, nanos: money.nanos, currencyCode: money.currencyCode)
          }
          .frame(maxWidth: .infinity)

          resetButton {
            money = Money.zero(money.currencyCode)
          }
          .frame(maxWidth: .infinity)
        }
      }
      .padding(16)
    }
  }

  private var currencyDropdown: some View {
    let name = sampleCurrencies.first { $0.code == money.currencyCode }?.name ?? money.currencyCode
    return Button(action: { pickerOpen = true }) {
      HStack(spacing: 12) {
        Text(money.currencyCode)
          .font(NativeDemoTheme.manropeFont(15, weight: .bold))
          .foregroundColor(NativeDemoTheme.inkColor)
        Text(name)
          .font(NativeDemoTheme.manropeFont(14, weight: .medium))
          .foregroundColor(NativeDemoTheme.inkMutedColor)
          .frame(maxWidth: .infinity, alignment: .leading)
        Text("▾")
          .font(NativeDemoTheme.manropeFont(16, weight: .medium))
          .foregroundColor(NativeDemoTheme.inkMutedColor)
      }
      .padding(.horizontal, 16)
      .frame(height: 56)
      .background(Color.white)
      .overlay(
        RoundedRectangle(cornerRadius: 14)
          .stroke(NativeDemoTheme.borderColor, lineWidth: 1)
      )
      .clipShape(RoundedRectangle(cornerRadius: 14))
    }
    .buttonStyle(.plain)
  }

  private var amountTextSection: some View {
    SectionCard(title: "AmountText", subtitle: "SwiftUI (camount-swift)") {
      VStack(alignment: .leading, spacing: 10) {
        AmountText(money)
          .amountStyle(AmountStyle(
            font: NativeDemoTheme.manrope(44, weight: .bold),
            color: NativeDemoTheme.ink
          ))
          .frame(maxWidth: .infinity, minHeight: 60, maxHeight: 60, alignment: .leading)

        HStack(spacing: 12) {
          LabeledAmount(label: "Always signed") {
            AmountText(money)
              .amountStyle(AmountStyle(
                font: NativeDemoTheme.manrope(22, weight: .medium),
                color: NativeDemoTheme.ink
              ))
              .showSign(.always)
              .frame(maxWidth: .infinity, minHeight: 32, maxHeight: 32, alignment: .leading)
          }

          LabeledAmount(label: "No trailing zeros") {
            AmountText(money)
              .amountStyle(AmountStyle(
                font: NativeDemoTheme.manrope(18, weight: .regular),
                color: NativeDemoTheme.inkMuted
              ))
              .fractionPolicy(.ignoreZero)
              .frame(maxWidth: .infinity, minHeight: 32, maxHeight: 32, alignment: .leading)
          }
        }
      }
    }
  }

  private var amountFieldSection: some View {
    SectionCard(title: "AmountField", subtitle: "SwiftUI (camount-swift)") {
      let defaultStyle = AmountStyle(
        font: NativeDemoTheme.manrope(36, weight: .semibold),
        color: NativeDemoTheme.ink,
        cursor: CursorStyle(color: NativeDemoTheme.accent),
        zeroNotationColor: NativeDemoTheme.placeholder,
        fixedFractionColor: NativeDemoTheme.placeholder
      )
      let gradientStyle = AmountStyle(
        font: NativeDemoTheme.manrope(44, weight: .bold),
        color: NativeDemoTheme.ink,
        gradient: Gradient(colors: [Color(NativeDemoTheme.accent), Color(NativeDemoTheme.accentAlt)]),
        cursor: CursorStyle(color: NativeDemoTheme.accentAlt)
      )
      let compactStyle = AmountStyle(
        font: NativeDemoTheme.manrope(20, weight: .medium),
        color: NativeDemoTheme.ink,
        cursor: CursorStyle(color: NativeDemoTheme.ink)
      )

      VStack(spacing: 12) {
        FieldBox(height: 64) {
          AmountField($money)
            .amountStyle(defaultStyle)
            .frame(maxWidth: .infinity, minHeight: 56, maxHeight: 56)
        }
        FieldBox(height: 76) {
          AmountField($money)
            .amountStyle(gradientStyle)
            .frame(maxWidth: .infinity, minHeight: 64, maxHeight: 64)
        }
        FieldBox(height: 48) {
          AmountField($money)
            .amountStyle(compactStyle)
            .frame(maxWidth: .infinity, minHeight: 32, maxHeight: 32)
        }
      }
    }
  }

  private func actionButton(label: String, action: @escaping () -> Void) -> some View {
    Button(action: action) {
      Text(label)
        .font(NativeDemoTheme.manropeFont(14, weight: .medium))
        .foregroundColor(NativeDemoTheme.inkColor)
        .frame(maxWidth: .infinity, minHeight: 44)
        .background(NativeDemoTheme.fieldSurfaceColor)
        .clipShape(RoundedRectangle(cornerRadius: 14))
    }
    .buttonStyle(.plain)
  }

  private func plusOneButton(action: @escaping () -> Void) -> some View {
    Button(action: action) {
      Text("+1")
        .font(NativeDemoTheme.manropeFont(15, weight: .semibold))
        .foregroundColor(.white)
        .frame(maxWidth: .infinity, minHeight: 44)
        .background(NativeDemoTheme.accentColor)
        .clipShape(RoundedRectangle(cornerRadius: 14))
    }
    .buttonStyle(.plain)
  }

  private func resetButton(action: @escaping () -> Void) -> some View {
    Button(action: action) {
      Text("Reset")
        .font(NativeDemoTheme.manropeFont(14, weight: .medium))
        .foregroundColor(NativeDemoTheme.inkColor)
        .frame(maxWidth: .infinity, minHeight: 44)
        .background(Color.white)
        .overlay(
          RoundedRectangle(cornerRadius: 14)
            .stroke(NativeDemoTheme.borderColor, lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 14))
    }
    .buttonStyle(.plain)
  }

  private func randomMoney(currencyCode: String) -> Money {
    Money(
      units: Int64.random(in: 0..<99_999),
      nanos: Int32.random(in: 0..<1_000_000_000),
      currencyCode: currencyCode
    )
  }
}

private struct ElevatedCard<Content: View>: View {
  @ViewBuilder let content: () -> Content
  var body: some View {
    content()
      .frame(maxWidth: .infinity, alignment: .leading)
      .background(Color.white)
      .clipShape(RoundedRectangle(cornerRadius: 20))
      .shadow(color: Color.black.opacity(0.04), radius: 8, x: 0, y: 2)
  }
}

private struct SectionCard<Content: View>: View {
  let title: String
  let subtitle: String
  @ViewBuilder let content: () -> Content
  var body: some View {
    ElevatedCard {
      VStack(alignment: .leading, spacing: 14) {
        HStack(alignment: .bottom, spacing: 8) {
          Text(title)
            .font(NativeDemoTheme.manropeFont(18, weight: .semibold))
            .foregroundColor(NativeDemoTheme.inkColor)
          Text(subtitle)
            .font(NativeDemoTheme.manropeFont(12, weight: .medium))
            .foregroundColor(NativeDemoTheme.inkMutedColor)
            .padding(.bottom, 2)
        }
        content()
      }
      .padding(18)
    }
  }
}

private struct LabeledAmount<Content: View>: View {
  let label: String
  @ViewBuilder let content: () -> Content
  var body: some View {
    VStack(alignment: .leading, spacing: 4) {
      Text(label.uppercased())
        .font(NativeDemoTheme.manropeFont(10, weight: .semibold))
        .foregroundColor(NativeDemoTheme.inkMutedColor)
        .tracking(0.8)
      content()
    }
    .frame(maxWidth: .infinity, alignment: .leading)
  }
}

private struct FieldBox<Content: View>: View {
  let height: CGFloat
  @ViewBuilder let content: () -> Content
  var body: some View {
    content()
      .padding(.horizontal, 16)
      .padding(.vertical, 4)
      .frame(maxWidth: .infinity, minHeight: height, maxHeight: height, alignment: .leading)
      .background(NativeDemoTheme.fieldSurfaceColor)
      .clipShape(RoundedRectangle(cornerRadius: 14))
  }
}
