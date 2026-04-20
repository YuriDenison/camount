import Foundation

enum AppMode: String, CaseIterable, Identifiable {
  case native
  case cmp

  var id: String { rawValue }

  var label: String {
    switch self {
    case .native: return "Native"
    case .cmp:    return "CMP"
    }
  }
}
