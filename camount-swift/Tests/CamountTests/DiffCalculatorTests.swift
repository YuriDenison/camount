import XCTest
@testable import Camount

final class DiffCalculatorTests: XCTestCase {

  private let usd = AmountConfig(
    maximumNotationDigits: 5,
    decimalSeparator: ".",
    groupingSeparator: ",",
    prefix: "$",
    suffix: "",
    groupingSize: 3,
    maximumFractionDigits: 2
  )

  private func newCell() -> () -> SymbolCell { { SymbolCell() } }

  private func visibleChars(_ cells: [SymbolCell]) -> String {
    cells.filter { $0.isVisible }.map { String($0.currentChar) }.joined()
  }

  func testEditInsertsIntoEmpty() {
    var cells: [SymbolCell] = []
    let diff = EditDiffCalculator(config: usd, newCell: newCell())
    diff.diff(&cells, text: "$42", positions: .empty)
    XCTAssertEqual(visibleChars(cells), "$42")
  }

  func testEditReplacesDigit() {
    var cells: [SymbolCell] = []
    let diff = EditDiffCalculator(config: usd, newCell: newCell())
    diff.diff(&cells, text: "$4", positions: .empty)
    diff.diff(&cells, text: "$5", positions: .empty)
    XCTAssertEqual(visibleChars(cells), "$5")
  }

  func testEditDeletesTrailing() {
    var cells: [SymbolCell] = []
    let diff = EditDiffCalculator(config: usd, newCell: newCell())
    diff.diff(&cells, text: "$42", positions: .empty)
    diff.diff(&cells, text: "$4", positions: .empty)
    XCTAssertEqual(visibleChars(cells), "$4")
  }

  func testLevenshteinInsertsFromEmpty() {
    var cells: [SymbolCell] = []
    let diff = LevenshteinDiffCalculator(config: usd, newCell: newCell())
    diff.diff(&cells, text: "$1.50", positions: .empty)
    XCTAssertEqual(visibleChars(cells), "$1.50")
  }

  func testLevenshteinReplacesInterior() {
    var cells: [SymbolCell] = []
    let diff = LevenshteinDiffCalculator(config: usd, newCell: newCell())
    diff.diff(&cells, text: "$1.50", positions: .empty)
    diff.diff(&cells, text: "$2.50", positions: .empty)
    XCTAssertEqual(visibleChars(cells), "$2.50")
  }

  func testLevenshteinShrinks() {
    var cells: [SymbolCell] = []
    let diff = LevenshteinDiffCalculator(config: usd, newCell: newCell())
    diff.diff(&cells, text: "$12.34", positions: .empty)
    diff.diff(&cells, text: "$1.23", positions: .empty)
    XCTAssertEqual(visibleChars(cells), "$1.23")
  }
}
