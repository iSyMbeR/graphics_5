import java.awt.*;
import java.awt.image.BufferedImage;
import javax.swing.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ImageWrapper extends JPanel {
    private BufferedImage image;

    @Override
    public void paint(Graphics g) {
        Graphics2D graphics2D = (Graphics2D) g;
        CanvasUtils.drawImage(graphics2D, image);
    }
}
