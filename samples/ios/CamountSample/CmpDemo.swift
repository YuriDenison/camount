import SampleShared
import SwiftUI
import UIKit

struct CmpDemo: UIViewControllerRepresentable {
  func makeUIViewController(context: Context) -> UIViewController {
    MainViewControllerKt.MainViewController()
  }

  func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
