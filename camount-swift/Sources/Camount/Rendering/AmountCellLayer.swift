#if canImport(UIKit)
import UIKit
import QuartzCore

final class AmountCellLayer: CALayer {

    struct SymbolRender {
        let char: Character
        let image: CGImage
        let intrinsicSize: CGSize
        let baseline: CGFloat
        let font: UIFont
        let color: UIColor
    }

    var symbols: [SymbolRender] = []
    var appearances: [Double] = []
    var tintColor: UIColor = .black

    static let animationDuration: CFTimeInterval = 0.120
    static let timingFunction = CAMediaTimingFunction(name: .default)

    override func action(forKey key: String) -> CAAction? {
        // Suppress the implicit `contents` crossfade: we regenerate the backing
        // store on every appearance tween frame, and the default contents action
        // would fade the old bitmap into the new one — visible as a "ghost trail"
        // during horizontal translates.
        if key == "contents" { return NSNull() }
        return super.action(forKey: key)
    }

    override init() {
        super.init()
        contentsScale = UIScreen.main.scale
        needsDisplayOnBoundsChange = false
        contentsGravity = .resize
        actions = AmountCellLayer.noImplicitActions
    }

    override init(layer: Any) {
        super.init(layer: layer)
        if let source = layer as? AmountCellLayer {
            symbols = source.symbols
            appearances = source.appearances
            tintColor = source.tintColor
        }
        actions = AmountCellLayer.noImplicitActions
    }

    private static let noImplicitActions: [String: CAAction] = [
        "contents": NSNull(),
        "hidden": NSNull(),
        "onOrderIn": NSNull(),
        "onOrderOut": NSNull(),
    ]

    required init?(coder: NSCoder) { fatalError() }

    override func draw(in ctx: CGContext) {
        guard !symbols.isEmpty else { return }
        let size = bounds.size
        guard size.width > 0, size.height > 0 else { return }

        ctx.saveGState()
        ctx.translateBy(x: 0, y: size.height)
        ctx.scaleBy(x: 1, y: -1)

        let last = symbols.last!
        let scaleX = last.intrinsicSize.width > 0 ? size.width / last.intrinsicSize.width : 1
        let scaleY = last.intrinsicSize.height > 0 ? size.height / last.intrinsicSize.height : 1

        ctx.scaleBy(x: scaleX, y: scaleY)

        for (i, sym) in symbols.enumerated() {
            let level = CGFloat(appearances[i])
            if level <= 0 { continue }
            let selfScale = 0.6 + (1.0 - 0.6) * level
            ctx.saveGState()
            let pivotX = sym.intrinsicSize.width * 0.5
            let pivotY = sym.baseline
            ctx.translateBy(x: pivotX, y: pivotY)
            ctx.scaleBy(x: selfScale, y: selfScale)
            ctx.translateBy(x: -pivotX, y: -pivotY)
            ctx.setAlpha(level)
            ctx.draw(sym.image, in: CGRect(origin: .zero, size: sym.intrinsicSize))
            ctx.restoreGState()
        }

        ctx.restoreGState()
    }
}
#endif
