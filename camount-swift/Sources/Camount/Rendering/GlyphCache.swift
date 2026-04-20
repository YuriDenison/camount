#if canImport(UIKit)
import UIKit
import CoreText

final class GlyphCache {
    private struct Key: Hashable {
        let char: Character
        let fontName: String
        let fontSize: CGFloat
        let colorRGBA: UInt64
    }

    private var storage: [Key: CGImage] = [:]
    private var screenScale: CGFloat = UIScreen.main.scale

    func setScreenScale(_ scale: CGFloat) {
        if scale != screenScale {
            screenScale = scale
            storage.removeAll(keepingCapacity: true)
        }
    }

    func image(for char: Character, font: UIFont, color: UIColor) -> (CGImage, CGSize)? {
        let key = Key(
            char: char,
            fontName: font.fontName,
            fontSize: font.pointSize,
            colorRGBA: packColor(color)
        )
        if let cached = storage[key], let size = imageSize(cached) {
            return (cached, size)
        }
        guard let (image, size) = render(char: char, font: font, color: color) else { return nil }
        storage[key] = image
        return (image, size)
    }

    private func render(char: Character, font: UIFont, color: UIColor) -> (CGImage, CGSize)? {
        let str = String(char)
        let attributes: [NSAttributedString.Key: Any] = [
            .font: font,
            .foregroundColor: color,
        ]
        let attrStr = NSAttributedString(string: str, attributes: attributes)
        let line = CTLineCreateWithAttributedString(attrStr)
        let advance = CGFloat(CTLineGetTypographicBounds(line, nil, nil, nil))

        let ascent = font.ascender
        let descent = -font.descender
        let lineHeight = ascent + descent
        let width = max(1, ceil(advance))
        let height = max(1, ceil(lineHeight))

        let pixelW = Int(width * screenScale)
        let pixelH = Int(height * screenScale)
        let colorSpace = CGColorSpaceCreateDeviceRGB()
        let bitmapInfo = CGImageAlphaInfo.premultipliedLast.rawValue
        guard
            let ctx = CGContext(
                data: nil,
                width: pixelW,
                height: pixelH,
                bitsPerComponent: 8,
                bytesPerRow: 0,
                space: colorSpace,
                bitmapInfo: bitmapInfo
            )
        else { return nil }
        ctx.scaleBy(x: screenScale, y: screenScale)
        ctx.textMatrix = .identity
        ctx.translateBy(x: 0, y: descent)
        CTLineDraw(line, ctx)
        guard let image = ctx.makeImage() else { return nil }
        return (image, CGSize(width: width, height: height))
    }

    private func imageSize(_ image: CGImage) -> CGSize? {
        CGSize(
            width: CGFloat(image.width) / screenScale,
            height: CGFloat(image.height) / screenScale)
    }

    private func packColor(_ color: UIColor) -> UInt64 {
        var r: CGFloat = 0
        var g: CGFloat = 0
        var b: CGFloat = 0
        var a: CGFloat = 0
        color.getRed(&r, green: &g, blue: &b, alpha: &a)
        func q(_ x: CGFloat) -> UInt64 { UInt64(max(0, min(1, x)) * 65535) }
        return (q(r) << 48) | (q(g) << 32) | (q(b) << 16) | q(a)
    }
}
#endif
