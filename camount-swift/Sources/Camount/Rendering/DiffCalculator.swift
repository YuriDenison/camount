import Foundation

enum DiffMode {
    case edit
    case levenshtein
}

protocol DiffCalculator {
    func diff(
        _ cells: inout [SymbolCell],
        text: String,
        positions: AmountFieldPositions
    )
}

func makeDiffCalculator(
    mode: DiffMode,
    config: AmountConfig,
    newCell: @escaping () -> SymbolCell
) -> DiffCalculator {
    switch mode {
    case .edit: return EditDiffCalculator(config: config, newCell: newCell)
    case .levenshtein: return LevenshteinDiffCalculator(config: config, newCell: newCell)
    }
}

private func fieldAt(_ positions: AmountFieldPositions, index: Int) -> AmountFieldKind? {
    if positions.zeroNotation.contains(index) { return .zeroNotation }
    if positions.fixedFraction.contains(index) { return .fixedFraction }
    return nil
}

final class EditDiffCalculator: DiffCalculator {
    private let config: AmountConfig
    private let newCell: () -> SymbolCell

    init(config: AmountConfig, newCell: @escaping () -> SymbolCell) {
        self.config = config
        self.newCell = newCell
    }

    func diff(
        _ cells: inout [SymbolCell],
        text: String,
        positions: AmountFieldPositions
    ) {
        var result: [SymbolCell] = []
        result.reserveCapacity(config.maximumFormattedSymbols)
        let chars = Array(text)
        var textIndex = 0

        var cellIndex = 0
        while cellIndex < cells.count {
            let curCell = cells[cellIndex]
            if curCell.isVisible || curCell.isRunning {
                var retry = true
                while retry {
                    let cell: SymbolCell
                    if textIndex >= chars.count {
                        curCell.delete()
                        cell = curCell
                        retry = false
                    } else {
                        let s1 = curCell.currentChar
                        let s2 = chars[textIndex]
                        let field = fieldAt(positions, index: textIndex)
                        if s1 == s2 {
                            curCell.replace(char: s2, field: field)
                            textIndex += 1
                            cell = curCell
                            retry = false
                        } else if config.isGroupingSeparator(s1) || config.isDecimalSeparator(s1) {
                            curCell.delete()
                            cell = curCell
                            retry = false
                        } else if config.isGroupingSeparator(s2) || config.isDecimalSeparator(s2) {
                            let fresh = newCell()
                            fresh.replace(char: s2, field: field)
                            textIndex += 1
                            result.append(fresh)
                            retry = true
                            continue
                        } else {
                            let s1IsDigit = config.isDigit(s1)
                            let s2IsDigit = config.isDigit(s2)
                            if s1IsDigit {
                                if s2IsDigit {
                                    curCell.replace(char: s2, field: field)
                                    textIndex += 1
                                    cell = curCell
                                    retry = false
                                } else {
                                    curCell.delete()
                                    cell = curCell
                                    retry = false
                                }
                            } else {
                                if s2IsDigit {
                                    let fresh = newCell()
                                    fresh.replace(char: s2, field: field)
                                    textIndex += 1
                                    result.append(fresh)
                                    retry = true
                                    continue
                                } else {
                                    curCell.replace(char: s2, field: field)
                                    textIndex += 1
                                    cell = curCell
                                    retry = false
                                }
                            }
                        }
                    }
                    result.append(cell)
                }
            }
            cellIndex += 1
        }

        let restCount = min(chars.count, config.maximumFormattedSymbols)
        while textIndex < restCount {
            let s = chars[textIndex]
            let field = fieldAt(positions, index: textIndex)
            textIndex += 1
            let cell = newCell()
            cell.replace(char: s, field: field)
            result.append(cell)
        }

        cells = result
    }
}

final class LevenshteinDiffCalculator: DiffCalculator {
    private let config: AmountConfig
    private let newCell: () -> SymbolCell

    init(config: AmountConfig, newCell: @escaping () -> SymbolCell) {
        self.config = config
        self.newCell = newCell
    }

    func diff(
        _ cells: inout [SymbolCell],
        text: String,
        positions: AmountFieldPositions
    ) {
        var working = cells
        let chars = Array(text)
        let matrix = levenshteinMatrix(cells: working, text: chars)
        applyDiff(cells: &working, text: chars, matrix: matrix, positions: positions)
        cells = working
    }

    private func levenshteinMatrix(cells: [SymbolCell], text: [Character]) -> [[Int]] {
        let xLength = cells.count
        let yLength = text.count
        var dp = Array(repeating: Array(repeating: 0, count: yLength + 1), count: xLength + 1)
        for i in 0...xLength {
            for j in 0...yLength {
                if i == 0 {
                    dp[i][j] = j
                } else if j == 0 {
                    dp[i][j] = i
                } else {
                    let c1 = cells[i - 1].currentChar
                    let c2 = text[j - 1]
                    let replaceCost = dp[i - 1][j - 1] + (c1 == c2 ? 0 : 1)
                    let insertCost = dp[i][j - 1] + 1
                    let deleteCost = dp[i - 1][j] + 1
                    dp[i][j] = min(replaceCost, min(insertCost, deleteCost))
                }
            }
        }
        return dp
    }

    private func applyDiff(
        cells: inout [SymbolCell],
        text: [Character],
        matrix: [[Int]],
        positions: AmountFieldPositions
    ) {
        var i = cells.count
        var j = text.count
        while i >= 0 && j >= 0 {
            if i == 0 && j == 0 { break }
            if i == 0 && j > 0 {
                j -= 1
                insert(into: &cells, at: i, char: text[j], field: fieldAt(positions, index: j))
            } else if i > 0 && j == 0 {
                i -= 1
                cells[i].delete()
            } else {
                let replaceCost = matrix[i - 1][j - 1]
                let deleteCost = matrix[i - 1][j]
                let insertCost = matrix[i][j - 1]

                let from = cells[i - 1].currentChar
                let to = text[j - 1]
                let minCost = min(replaceCost, min(insertCost, deleteCost))

                if minCost == replaceCost {
                    if isSeparator(from) && !isSeparator(to) {
                        i -= 1
                        cells[i].delete()
                    } else if isSeparator(to) && !isSeparator(from) {
                        j -= 1
                        insert(into: &cells, at: i, char: to, field: fieldAt(positions, index: j))
                    } else {
                        i -= 1
                        j -= 1
                        cells[i].replace(char: to, field: fieldAt(positions, index: j))
                    }
                } else if minCost == insertCost {
                    j -= 1
                    insert(into: &cells, at: i, char: to, field: fieldAt(positions, index: j))
                } else {
                    i -= 1
                    cells[i].delete()
                }
            }
        }
    }

    private func insert(into cells: inout [SymbolCell], at index: Int, char: Character, field: AmountFieldKind?) {
        let cell = newCell()
        cell.replace(char: char, field: field)
        cells.insert(cell, at: index)
    }

    private func isSeparator(_ c: Character) -> Bool {
        config.isDecimalSeparator(c) || config.isGroupingSeparator(c)
    }
}
