#if canImport(UIKit)
import UIKit
import QuartzCore

final class CursorCell {

    static let blinkDurationMs = 530
    static let appearDurationMs = 500

    let layer = CALayer()
    private var style: CursorStyle
    private var durationMs: Int = SymbolCell.animationDurationMs
    private var blinkTimer: DispatchSourceTimer?
    private var cursorVisible = false

    init(style: CursorStyle) {
        self.style = style
        layer.backgroundColor = style.color.cgColor
        layer.cornerRadius = style.width / 2
        layer.opacity = 0
    }

    func setDurationMs(_ value: Int) {
        durationMs = value
    }

    func setTargetBounds(left: CGFloat, top: CGFloat, width: CGFloat, height: CGFloat) {
        let newFrame = CGRect(x: left, y: top, width: width, height: height)
        let isInitial = layer.bounds.size == .zero
        if isInitial {
            CATransaction.begin()
            CATransaction.setDisableActions(true)
            layer.frame = newFrame
            layer.cornerRadius = style.width / 2
            CATransaction.commit()
        } else {
            animate("position", from: layer.presentation()?.position ?? layer.position)
            animate("bounds.size", from: layer.presentation()?.bounds.size ?? layer.bounds.size)
            layer.frame = newFrame
        }
    }

    func setVisible(_ visible: Bool) {
        guard cursorVisible != visible else { return }
        cursorVisible = visible
        blinkTimer?.cancel()
        blinkTimer = nil

        if visible {
            animateOpacity(to: 1, duration: Self.appearDurationMs)
            let timer = DispatchSource.makeTimerSource(queue: .main)
            var on = true
            timer.schedule(
                deadline: .now() + .milliseconds(Self.blinkDurationMs),
                repeating: .milliseconds(Self.blinkDurationMs)
            )
            timer.setEventHandler { [weak self] in
                guard let self = self else { return }
                on.toggle()
                self.animateOpacity(to: on ? 1 : 0, duration: Self.appearDurationMs)
            }
            timer.resume()
            blinkTimer = timer
        } else {
            animateOpacity(to: 0, duration: Self.appearDurationMs)
        }
    }

    private func animateOpacity(to target: Float, duration ms: Int) {
        let anim = CABasicAnimation(keyPath: "opacity")
        anim.duration = TimeInterval(ms) / 1000
        anim.timingFunction = AmountCellLayer.timingFunction
        anim.fromValue = layer.presentation()?.opacity ?? layer.opacity
        anim.toValue = target
        layer.opacity = target
        layer.add(anim, forKey: "opacity")
    }

    private func animate(_ key: String, from value: Any?) {
        let anim = CABasicAnimation(keyPath: key)
        anim.duration = TimeInterval(durationMs) / 1000
        anim.timingFunction = AmountCellLayer.timingFunction
        anim.fromValue = value
        layer.add(anim, forKey: key)
    }

    deinit {
        blinkTimer?.cancel()
    }
}
#endif
