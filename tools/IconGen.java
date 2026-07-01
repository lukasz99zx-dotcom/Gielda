import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.GeneralPath;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Build-time only tool (runs on the host JVM, not on Android) that draws the
 * cGiełda launcher icon and writes it out at the standard mipmap densities.
 * Usage: java IconGen <outputResDir>
 */
public class IconGen {
    public static void main(String[] args) throws Exception {
        File resDir = new File(args[0]);
        int[] sizes = {48, 72, 96, 144, 192};
        String[] densities = {"mdpi", "hdpi", "xhdpi", "xxhdpi", "xxxhdpi"};
        for (int i = 0; i < sizes.length; i++) {
            File dir = new File(resDir, "mipmap-" + densities[i]);
            dir.mkdirs();
            BufferedImage img = draw(sizes[i]);
            ImageIO.write(img, "png", new File(dir, "ic_launcher.png"));
        }
    }

    static BufferedImage draw(int size) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        float corner = size * 0.22f;
        RoundRectangle2D bg = new RoundRectangle2D.Float(0, 0, size, size, corner, corner);
        GradientPaint gradient = new GradientPaint(
                0, 0, new Color(0x1E3A8A),
                size, size, new Color(0x2563EB));
        g.setPaint(gradient);
        g.fill(bg);

        // Bold "G" letterform, centered.
        g.setColor(Color.WHITE);
        Font font = new Font("SansSerif", Font.BOLD, Math.round(size * 0.58f));
        g.setFont(font);
        FontMetrics fm = g.getFontMetrics();
        String letter = "G";
        int textWidth = fm.stringWidth(letter);
        int textX = (size - textWidth) / 2;
        int textY = (size + fm.getAscent() - fm.getDescent()) / 2;
        g.drawString(letter, textX, textY + Math.round(size * 0.02f));

        // Small "stock up" badge (mini ascending bars) tucked in the bottom-right
        // corner so it reads clearly even at launcher size without cluttering the G.
        float badgeR = size * 0.235f;
        float badgeCx = size - badgeR * 0.92f;
        float badgeCy = size - badgeR * 0.92f;
        g.setColor(new Color(0x22C55E));
        g.fill(new java.awt.geom.Ellipse2D.Float(
                badgeCx - badgeR, badgeCy - badgeR, badgeR * 2, badgeR * 2));

        g.setColor(Color.WHITE);
        float barW = badgeR * 0.34f;
        float gap = badgeR * 0.16f;
        float[] barH = {badgeR * 0.55f, badgeR * 0.85f, badgeR * 1.15f};
        float startX = badgeCx - (barW * 3 + gap * 2) / 2f;
        float baseY = badgeCy + badgeR * 0.62f;
        for (int i = 0; i < 3; i++) {
            float x = startX + i * (barW + gap);
            float h = barH[i];
            g.fill(new RoundRectangle2D.Float(x, baseY - h, barW, h, barW * 0.3f, barW * 0.3f));
        }

        g.dispose();
        return img;
    }
}
