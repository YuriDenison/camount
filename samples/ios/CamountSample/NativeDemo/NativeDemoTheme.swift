import SwiftUI
import UIKit

enum NativeDemoTheme {
  static let accent       = UIColor(red: 0x40/255.0, green: 0x49/255.0, blue: 0xFF/255.0, alpha: 1)
  static let accentAlt    = UIColor(red: 0xFF/255.0, green: 0x40/255.0, blue: 0x81/255.0, alpha: 1)
  static let ink          = UIColor(red: 0x0F/255.0, green: 0x10/255.0, blue: 0x24/255.0, alpha: 1)
  static let inkMuted     = UIColor(red: 0x5A/255.0, green: 0x5E/255.0, blue: 0x80/255.0, alpha: 1)
  static let canvas       = UIColor(red: 0xF7/255.0, green: 0xF7/255.0, blue: 0xFB/255.0, alpha: 1)
  static let fieldSurface = UIColor(red: 0xEE/255.0, green: 0xEF/255.0, blue: 0xF6/255.0, alpha: 1)
  static let placeholder  = UIColor(red: 0xB5/255.0, green: 0xB8/255.0, blue: 0xCC/255.0, alpha: 1)
  static let border       = UIColor(red: 0xE0/255.0, green: 0xE1/255.0, blue: 0xEC/255.0, alpha: 1)
  static let selectedRow  = UIColor(red: 0xEF/255.0, green: 0xF0/255.0, blue: 0xFF/255.0, alpha: 1)

  static var canvasColor: Color       { Color(canvas) }
  static var inkColor: Color          { Color(ink) }
  static var inkMutedColor: Color     { Color(inkMuted) }
  static var accentColor: Color       { Color(accent) }
  static var fieldSurfaceColor: Color { Color(fieldSurface) }
  static var borderColor: Color       { Color(border) }
  static var selectedRowColor: Color  { Color(selectedRow) }

  static let manropeFamilyName = "Manrope-Medium"

  static func manrope(_ size: CGFloat, weight: UIFont.Weight = .medium) -> UIFont {
    if let font = UIFont(name: manropeFamilyName, size: size) {
      switch weight {
      case .bold, .heavy, .black:
        let desc = font.fontDescriptor.withSymbolicTraits(.traitBold) ?? font.fontDescriptor
        return UIFont(descriptor: desc, size: size)
      default:
        return font
      }
    }
    return .systemFont(ofSize: size, weight: weight)
  }

  static func manropeFont(_ size: CGFloat, weight: Font.Weight = .medium) -> Font {
    if UIFont(name: manropeFamilyName, size: size) != nil {
      return .custom(manropeFamilyName, size: size).weight(weight)
    }
    return .system(size: size, weight: weight)
  }
}

let sampleCurrencies: [(code: String, name: String)] = [
  ("EUR", "Euro"),
  ("USD", "US Dollar"),
  ("GBP", "British Pound"),
  ("JPY", "Japanese Yen"),
  ("CHF", "Swiss Franc"),
  ("CAD", "Canadian Dollar"),
  ("AUD", "Australian Dollar"),
  ("SEK", "Swedish Krona"),
  ("NOK", "Norwegian Krone"),
  ("PLN", "Polish Zloty"),
  ("CNY", "Chinese Yuan"),
  ("INR", "Indian Rupee"),
]
