import Foundation

#if canImport(UIKit)
import UIKit
import QuartzCore

final class SymbolCell {

    static let maxStackSymbols = 3
    static let animationDurationMs = 120

    private(set) var currentChar: Character = "\0"
    private(set) var isVisible: Bool = false
    var isRunning: Bool {
        layer.animationKeys()?.isEmpty == false
    }
    var intrinsicWidth: CGFloat { layer.symbols.last?.intrinsicSize.width ?? 0 }
    var intrinsicHeight: CGFloat { layer.symbols.last?.intrinsicSize.height ?? 0 }
    var field: AmountFieldKind?

    let layer = AmountCellLayer()

    private let glyphCache: GlyphCache
    private var style: AmountStyle
    private var duration: TimeInterval

    init(glyphCache: GlyphCache, style: AmountStyle, durationMs: Int = SymbolCell.animationDurationMs) {
        self.glyphCache = glyphCache
        self.style = style
        self.duration = TimeInterval(durationMs) / 1000
        layer.tintColor = style.color
    }

    deinit {
        displayLink?.invalidate()
    }

    func replace(char: Character, field: AmountFieldKind?) {
        let effectiveStyle = style.effectiveTextStyle(for: field)
        if let last = layer.symbols.last,
           last.char == char,
           self.field == field,
           last.font == effectiveStyle.font,
           last.color.isEqual(effectiveStyle.color) {
            animateAppearance(index: layer.appearances.count - 1, to: 1)
        } else {
            self.field = field
            guard
                let (image, size) = glyphCache.image(
                    for: char,
                    font: effectiveStyle.font,
                    color: effectiveStyle.color
                )
            else { return }
            let baseline = effectiveStyle.font.ascender
            let render = AmountCellLayer.SymbolRender(
                char: char,
                image: image,
                intrinsicSize: size,
                baseline: baseline,
                font: effectiveStyle.font,
                color: effectiveStyle.color
            )
            layer.symbols.append(render)
            layer.appearances.append(0)
            while layer.symbols.count > SymbolCell.maxStackSymbols {
                layer.symbols.removeFirst()
                layer.appearances.removeFirst()
            }
            for i in 0..<layer.appearances.count - 1 {
                animateAppearance(index: i, to: 0)
            }
            animateAppearance(index: layer.appearances.count - 1, to: 1)
        }
        currentChar = char
        isVisible = true
    }

    func delete() {
        if let lastIndex = layer.appearances.indices.last {
            animateAppearance(index: lastIndex, to: 0)
        }
        isVisible = false
    }

    func setTargetBounds(left: CGFloat, top: CGFloat, width: CGFloat, height: CGFloat) {
        let newFrame = CGRect(x: left, y: top, width: width, height: height)
        let isInitial = layer.bounds.size == .zero
        CATransaction.begin()
        CATransaction.setDisableActions(true)
        if isInitial {
            layer.frame = newFrame
        } else {
            let fromPosition = layer.presentation()?.position ?? layer.position
            let fromSize = layer.presentation()?.bounds.size ?? layer.bounds.size
            layer.frame = newFrame
            if fromPosition != layer.position {
                let anim = CABasicAnimation(keyPath: "position")
                anim.duration = duration
                anim.timingFunction = AmountCellLayer.timingFunction
                anim.fromValue = fromPosition
                anim.toValue = layer.position
                layer.add(anim, forKey: "position")
            }
            if fromSize != layer.bounds.size {
                let sizeAnim = CABasicAnimation(keyPath: "bounds.size")
                sizeAnim.duration = duration
                sizeAnim.timingFunction = AmountCellLayer.timingFunction
                sizeAnim.fromValue = fromSize
                sizeAnim.toValue = layer.bounds.size
                layer.add(sizeAnim, forKey: "bounds.size")
            }
        }
        CATransaction.commit()
    }

    func updateStyle(_ style: AmountStyle) {
        self.style = style
        layer.tintColor = style.color
        rerenderSymbols()
        CATransaction.begin()
        CATransaction.setDisableActions(true)
        layer.setNeedsDisplay()
        layer.displayIfNeeded()
        CATransaction.commit()
    }

    private func rerenderSymbols() {
        guard !layer.symbols.isEmpty else { return }
        let effective = style.effectiveTextStyle(for: field)
        var updated: [AmountCellLayer.SymbolRender] = []
        updated.reserveCapacity(layer.symbols.count)
        for sym in layer.symbols {
            if sym.font == effective.font && sym.color.isEqual(effective.color) {
                updated.append(sym)
                continue
            }
            guard let (image, size) = glyphCache.image(
                for: sym.char, font: effective.font, color: effective.color
            ) else {
                updated.append(sym)
                continue
            }
            updated.append(AmountCellLayer.SymbolRender(
                char: sym.char,
                image: image,
                intrinsicSize: size,
                baseline: effective.font.ascender,
                font: effective.font,
                color: effective.color
            ))
        }
        layer.symbols = updated
    }

    private struct Tween {
        let from: Double
        let to: Double
        let start: CFTimeInterval
        let duration: CFTimeInterval
    }
    private var tweens: [Int: Tween] = [:]
    private var displayLink: CADisplayLink?

    private func animateAppearance(index: Int, to target: Double) {
        let current = layer.appearances[index]
        if duration <= 0 || current == target {
            tweens.removeValue(forKey: index)
            if current != target {
                layer.appearances[index] = target
                CATransaction.begin()
                CATransaction.setDisableActions(true)
                layer.setNeedsDisplay()
                layer.displayIfNeeded()
                CATransaction.commit()
            }
            stopIfIdle()
            return
        }
        tweens[index] = Tween(
            from: current,
            to: target,
            start: CACurrentMediaTime(),
            duration: duration
        )
        ensureDisplayLink()
    }

    private func ensureDisplayLink() {
        if displayLink != nil { return }
        let link = CADisplayLink(target: AnimationTicker(cell: self), selector: #selector(AnimationTicker.tick))
        link.add(to: .main, forMode: .common)
        displayLink = link
    }

    fileprivate func tickAnimations() {
        if tweens.isEmpty {
            stopIfIdle()
            return
        }
        let now = CACurrentMediaTime()
        var finished: [Int] = []
        for (index, tween) in tweens {
            guard index < layer.appearances.count else {
                finished.append(index)
                continue
            }
            let raw = (now - tween.start) / tween.duration
            let clamped = min(max(raw, 0), 1)
            let eased = easeInOut(clamped)
            layer.appearances[index] = tween.from + (tween.to - tween.from) * eased
            if clamped >= 1 {
                layer.appearances[index] = tween.to
                finished.append(index)
            }
        }
        for index in finished { tweens.removeValue(forKey: index) }
        CATransaction.begin()
        CATransaction.setDisableActions(true)
        layer.setNeedsDisplay()
        layer.displayIfNeeded()
        CATransaction.commit()
        stopIfIdle()
    }

    private func stopIfIdle() {
        guard tweens.isEmpty else { return }
        displayLink?.invalidate()
        displayLink = nil
    }

    private func easeInOut(_ t: Double) -> Double {
        // Compose's FastOutSlowInEasing is the Material cubic Bézier (0.4, 0, 0.2, 1).
        cubicBezier(x: t, p1x: 0.4, p1y: 0, p2x: 0.2, p2y: 1)
    }

    private func cubicBezier(x: Double, p1x: Double, p1y: Double, p2x: Double, p2y: Double) -> Double {
        if x <= 0 { return 0 }
        if x >= 1 { return 1 }
        // Solve for t such that bezierX(t) == x using Newton-Raphson with bisection fallback.
        var t = x
        for _ in 0..<8 {
            let bx = bezier(t: t, p1: p1x, p2: p2x)
            let dbx = bezierDerivative(t: t, p1: p1x, p2: p2x)
            if abs(dbx) < 1e-6 { break }
            let delta = (bx - x) / dbx
            t -= delta
            if abs(delta) < 1e-5 { break }
        }
        return bezier(t: t, p1: p1y, p2: p2y)
    }

    private func bezier(t: Double, p1: Double, p2: Double) -> Double {
        let u = 1 - t
        return 3 * u * u * t * p1 + 3 * u * t * t * p2 + t * t * t
    }

    private func bezierDerivative(t: Double, p1: Double, p2: Double) -> Double {
        let u = 1 - t
        return 3 * u * u * p1 + 6 * u * t * (p2 - p1) + 3 * t * t * (1 - p2)
    }
}

private final class AnimationTicker: NSObject {
    weak var cell: SymbolCell?
    init(cell: SymbolCell) { self.cell = cell }
    @objc func tick() {
        cell?.tickAnimations()
    }
}

#else

final class SymbolCell {
    private(set) var currentChar: Character = "\0"
    private(set) var isVisible: Bool = false
    var isRunning: Bool { false }

    init() {}

    func replace(char: Character, field: AmountFieldKind?) {
        currentChar = char
        isVisible = true
    }

    func delete() {
        isVisible = false
    }
}

#endif
