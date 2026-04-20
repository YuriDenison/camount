#if canImport(UIKit)
import UIKit
import SwiftUI
import QuartzCore

final class AmountPainter {

    let rootLayer = CALayer()
    private let cellsContainer = CALayer()
    private var gradientLayer: CAGradientLayer?
    private var cells: [SymbolCell] = []
    private var cursor: CursorCell?

    private var style: AmountStyle
    private var config: AmountConfig
    private var mode: DiffMode
    private var alignment: AmountAlignment = .center

    private var containerSize: CGSize = .zero
    private var densityScale: CGFloat = 1
    private var lastRenderedText: String?
    private var lastRenderedPositions: AmountFieldPositions = .empty
    private var cursorPositionIndex: Int = -1

    private let glyphCache = GlyphCache()
    private lazy var diffCalculator: DiffCalculator = {
        makeDiffCalculator(mode: mode, config: config, newCell: { [unowned self] in self.newCell() })
    }()

    var intrinsicWidth: CGFloat = 0
    var intrinsicHeight: CGFloat = 0

    init(style: AmountStyle, mode: DiffMode, config: AmountConfig) {
        self.style = style
        self.config = config
        self.mode = mode
        rootLayer.masksToBounds = false
        cellsContainer.actions = ["position": NSNull(), "bounds": NSNull()]
        rootLayer.addSublayer(cellsContainer)
        if let cs = style.cursor {
            let c = CursorCell(style: cs)
            c.layer.zPosition = 1
            cursor = c
            rootLayer.addSublayer(c.layer)
        }
        applyGradient()
    }

    func updateStyle(style: AmountStyle, config: AmountConfig, alignment: AmountAlignment) {
        let configChanged = config != self.config
        let styleChanged = self.style != style
        let alignmentChanged = alignment != self.alignment
        let cursorChanged = self.style.cursor != style.cursor

        self.style = style
        self.alignment = alignment
        if configChanged { self.config = config }
        if cursorChanged {
            cursor?.layer.removeFromSuperlayer()
            cursor = style.cursor.map { cs in
                let c = CursorCell(style: cs)
                c.layer.zPosition = 1
                return c
            }
            if let c = cursor { rootLayer.addSublayer(c.layer) }
        }
        if configChanged || styleChanged {
            diffCalculator = makeDiffCalculator(
                mode: mode, config: self.config,
                newCell: { [unowned self] in self.newCell() }
            )
        }
        if styleChanged {
            for cell in cells { cell.updateStyle(style) }
            applyGradient()
        }
        if alignmentChanged { layout() }
    }

    private func applyGradient() {
        CATransaction.begin()
        CATransaction.setDisableActions(true)
        defer { CATransaction.commit() }
        guard let gradient = style.gradient else {
            gradientLayer?.removeFromSuperlayer()
            gradientLayer = nil
            cellsContainer.mask = nil
            if cellsContainer.superlayer !== rootLayer {
                rootLayer.insertSublayer(cellsContainer, at: 0)
            }
            return
        }
        let gl: CAGradientLayer = gradientLayer ?? {
            let g = CAGradientLayer()
            g.actions = ["position": NSNull(), "bounds": NSNull(), "colors": NSNull()]
            return g
        }()
        let stops = gradient.stops
        gl.colors = stops.map { UIColor($0.color).cgColor }
        gl.locations = stops.map { NSNumber(value: Float($0.location)) }
        gl.startPoint = CGPoint(x: 0, y: 0.5)
        gl.endPoint = CGPoint(x: 1, y: 0.5)
        if gl.superlayer !== rootLayer {
            rootLayer.insertSublayer(gl, at: 0)
        }
        cellsContainer.removeFromSuperlayer()
        gl.mask = cellsContainer
        gl.frame = CGRect(origin: .zero, size: containerSize)
        cellsContainer.frame = CGRect(origin: .zero, size: containerSize)
        gradientLayer = gl
    }

    func setText(_ text: String, positions: AmountFieldPositions) {
        if lastRenderedText == text && lastRenderedPositions == positions { return }
        lastRenderedText = text
        lastRenderedPositions = positions
        CATransaction.begin()
        CATransaction.setDisableActions(true)
        diffCalculator.diff(&cells, text: text, positions: positions)
        syncLayerTree()
        cursorPositionIndex = positions.cursorPosition
        calculateIntrinsic()
        layout()
        CATransaction.commit()
    }

    func setBounds(width: CGFloat, height: CGFloat) {
        let newSize = CGSize(width: width, height: height)
        guard containerSize != newSize else { return }
        containerSize = newSize
        let frame = CGRect(origin: .zero, size: newSize)
        CATransaction.begin()
        CATransaction.setDisableActions(true)
        cellsContainer.frame = frame
        gradientLayer?.frame = frame
        CATransaction.commit()
        layout()
    }

    func setCursorVisible(_ visible: Bool) {
        cursor?.setVisible(visible)
    }

    func setDensity(_ density: CGFloat) {
        guard density != densityScale else { return }
        densityScale = density
        glyphCache.setScreenScale(density)
        for cell in cells { cell.updateStyle(style) }
        layout()
    }

    private func newCell() -> SymbolCell {
        let cell = SymbolCell(glyphCache: glyphCache, style: style, durationMs: SymbolCell.animationDurationMs)
        cellsContainer.addSublayer(cell.layer)
        return cell
    }

    private func syncLayerTree() {
        let kept = Set(cells.map { ObjectIdentifier($0.layer) })
        if let sublayers = cellsContainer.sublayers {
            for sub in sublayers {
                guard sub is AmountCellLayer else { continue }
                if !kept.contains(ObjectIdentifier(sub)) {
                    sub.removeFromSuperlayer()
                }
            }
        }
        for cell in cells where cell.layer.superlayer !== cellsContainer {
            cellsContainer.addSublayer(cell.layer)
        }
    }

    private func calculateIntrinsic() {
        var w: CGFloat = 0
        var h: CGFloat = 0
        for cell in cells where cell.isVisible {
            w += cell.intrinsicWidth
            h = max(h, cell.intrinsicHeight)
        }
        intrinsicWidth = w
        intrinsicHeight = h
    }

    private func layout() {
        guard containerSize.width > 0, containerSize.height > 0 else { return }

        var visibleWidth: CGFloat = 0
        var visibleHeight: CGFloat = 0
        for cell in cells where cell.isVisible {
            visibleWidth += cell.intrinsicWidth
            visibleHeight = max(visibleHeight, cell.intrinsicHeight)
        }
        let cursorStyle = style.cursor
        if let cs = cursorStyle {
            let cursorW = cs.width
            let cursorH = visibleHeight * cs.heightFraction
            visibleWidth += cursorW
            visibleHeight = max(visibleHeight, cursorH)
        }

        let scale: CGFloat =
            (containerSize.width < visibleWidth && visibleWidth > 0)
            ? containerSize.width / visibleWidth
            : 1
        let scaledWidth = visibleWidth * scale
        let scaledHeight = visibleHeight * scale

        let top = (containerSize.height - scaledHeight) / 2
        var left: CGFloat = {
            switch alignment {
            case .start: return 0
            case .center: return (containerSize.width - scaledWidth) / 2
            case .end: return containerSize.width - scaledWidth
            }
        }()

        var cursorLeft = left
        var visibleIndex = 0

        for cell in cells where cell.isVisible {
            let w = cell.intrinsicWidth * scale
            let h = cell.intrinsicHeight * scale
            cell.setTargetBounds(left: left, top: top, width: w, height: h)
            left += w
            visibleIndex += 1
            if visibleIndex == cursorPositionIndex { cursorLeft = left }
        }

        if let c = cursor, let cs = cursorStyle {
            let cursorW = cs.width * scale
            let cursorH = visibleHeight * cs.heightFraction * scale
            let cursorTop = top + (scaledHeight - cursorH) / 2
            c.setTargetBounds(left: cursorLeft, top: cursorTop, width: cursorW, height: cursorH)
        }
    }

}
#endif
