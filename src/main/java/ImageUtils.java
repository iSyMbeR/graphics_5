import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import org.apache.commons.io.FilenameUtils;

public class ImageUtils {

    public static BufferedImage readFromFile(File file) {
        BufferedImage image = null;

        String ext = FilenameUtils.getExtension(file.getAbsolutePath());
        if (ext.equals("jpg")) {
            try {

                image = ImageIO.read(new File(file.getAbsolutePath()));
            } catch (IOException exception) {
                exception.printStackTrace();
            }
        }
        return image;
    }
}
