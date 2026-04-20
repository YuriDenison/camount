import SwiftUI

struct CurrencyPickerSheet: View {
  let selected: String
  let onSelect: (String) -> Void

  @Environment(\.dismiss) private var dismiss

  var body: some View {
    NavigationStack {
      ScrollView {
        VStack(alignment: .leading, spacing: 0) {
          Text("Choose currency")
            .font(NativeDemoTheme.manropeFont(18, weight: .semibold))
            .foregroundColor(NativeDemoTheme.inkColor)
            .padding(.horizontal, 24)
            .padding(.vertical, 8)

          ForEach(sampleCurrencies, id: \.code) { item in
            CurrencyRow(
              code: item.code,
              name: item.name,
              isSelected: item.code == selected,
              onTap: {
                onSelect(item.code)
                dismiss()
              }
            )
          }
        }
        .padding(.bottom, 24)
      }
      .background(Color.white)
    }
    .presentationDetents([.medium, .large])
    .presentationDragIndicator(.visible)
  }
}

private struct CurrencyRow: View {
  let code: String
  let name: String
  let isSelected: Bool
  let onTap: () -> Void

  var body: some View {
    Button(action: onTap) {
      HStack(spacing: 16) {
        Text(code)
          .font(NativeDemoTheme.manropeFont(15, weight: .bold))
          .foregroundColor(isSelected ? NativeDemoTheme.accentColor : NativeDemoTheme.inkColor)
          .frame(width: 48, alignment: .leading)
        Text(name)
          .font(NativeDemoTheme.manropeFont(15, weight: .medium))
          .foregroundColor(NativeDemoTheme.inkColor)
          .frame(maxWidth: .infinity, alignment: .leading)
        if isSelected {
          Text("✓")
            .font(NativeDemoTheme.manropeFont(18, weight: .bold))
            .foregroundColor(NativeDemoTheme.accentColor)
        }
      }
      .padding(.horizontal, 24)
      .padding(.vertical, 14)
      .background(isSelected ? NativeDemoTheme.selectedRowColor : Color.clear)
      .contentShape(Rectangle())
    }
    .buttonStyle(.plain)
  }
}
