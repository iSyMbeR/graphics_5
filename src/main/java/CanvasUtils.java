import java.awt.*;
import java.awt.image.BufferedImage;

public class CanvasUtils {
    public static void clearCanvas(Graphics graphics, int width, int height) {
        graphics.clearRect(0, 0, width, height);
    }

    public static void drawImage(Graphics2D graphics2D, BufferedImage image) {
        graphics2D.drawImage(image, 0, 0, null);
    }
}
