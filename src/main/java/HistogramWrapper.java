import java.awt.*;
import javax.swing.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import org.knowm.xchart.CategoryChart;

@Data
@RequiredArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class HistogramWrapper extends JPanel {
    private final CategoryChart chart;

    @Override
    public void paint(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        if (chart.getSeriesMap().size() != 0) {
            chart.paint(g2d, 300, 200);
        }
    }
}
