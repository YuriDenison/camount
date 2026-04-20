import Camount
import SwiftUI

struct ContentView: View {
  @State private var mode: AppMode = .native
  @State private var money = Money(units: 1234, nanos: 560_000_000, currencyCode: "EUR")

  var body: some View {
    VStack(spacing: 0) {
      Picker("Mode", selection: $mode) {
        ForEach(AppMode.allCases) { m in
          Text(m.label).tag(m)
        }
      }
      .pickerStyle(.segmented)
      .padding(.horizontal, 16)
      .padding(.top, 8)
      .padding(.bottom, 4)

      Divider()

      switch mode {
      case .native:
        NativeSampleScreen(money: $money)
      case .cmp:
        CmpDemo()
          .ignoresSafeArea(edges: [.horizontal, .bottom])
      }
    }
  }
}
