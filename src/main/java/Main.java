import lombok.SneakyThrows;
import org.knowm.xchart.CategoryChart;
import org.knowm.xchart.CategoryChartBuilder;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.awt.Color.BLACK;
import static java.awt.Color.WHITE;

public class Main {
  private static final int WIDTH = 1000;
  private static final int HEIGHT = 1000;
  private static final int CANVAS_HEIGHT = 600;
  private static final int CANVAS_WIDTH = 600;
  private static int TOTAL_PIXELS;
  private static CategoryChart redChart;
  private static CategoryChart greenChart;
  private static CategoryChart blueChart;
  private static ImageWrapper imageWrapper;
  private static HistogramWrapper blueChartPanel;
  private static HistogramWrapper greenChartPanel;
  private static HistogramWrapper redChartPanel;
  private static List<Double> redCountPerColorValue;
  private static List<Double> greenCountPerColorValue;
  private static List<Double> blueCountPerColorValue;
  private static List<Integer> redLookUpTable;
  private static List<Integer> greenLookUpTable;
  private static List<Integer> blueLookUpTable;
  private static JFrame window = new JFrame("Histogram");
  private static BufferedImage copy;

  public static void main(String[] args) {
    window.setLayout(new FlowLayout());

    // buttons
    JPanel menuButton = new JPanel();
    window.add(menuButton);
    JButton openFileBtn = new JButton("Open file");
    menuButton.add(openFileBtn);
    JButton monochromeBtn = new JButton("Monochrome");
    menuButton.add(monochromeBtn);
    JButton stretchHistogram = new JButton("Stretch");
    menuButton.add(stretchHistogram);
    JButton equalizeBtn = new JButton("Equalize");
    menuButton.add(equalizeBtn);
    JButton hitOrMiss = new JButton("HitOrMiss");
    menuButton.add(hitOrMiss);
    JTextField thresholdTextField = new JTextField(5);
    menuButton.add(thresholdTextField);
    JButton manualBinarizeBtn = new JButton("Manual binarization");
    menuButton.add(manualBinarizeBtn);
    menuButton.add(new JLabel());
    menuButton.add(new JLabel());
    JTextField percentageTextField = new JTextField(5);
    menuButton.add(percentageTextField);
    JButton percentageBinarizeBtn = new JButton("Percentage binarization");
    menuButton.add(percentageBinarizeBtn);
    menuButton.add(new JLabel());
    menuButton.add(new JLabel());

    JButton erode = new JButton("Erode");
    menuButton.add(erode);

    JButton dilate = new JButton("Dilate");
    menuButton.add(dilate);

    JButton open = new JButton("Open");
    menuButton.add(open);

    JButton close = new JButton("Close");
    menuButton.add(close);
    // image
    imageWrapper = new ImageWrapper();
    window.add(imageWrapper);

    // file chooser
    final JFileChooser fc = new JFileChooser();
    fc.setAcceptAllFileFilterUsed(false);
    FileNameExtensionFilter filter = new FileNameExtensionFilter("JPEG files", "jpg");
    fc.addChoosableFileFilter(filter);

    // charts
    JPanel charts = new JPanel();
    window.add(charts);
    redChart = prepareChart("red", Color.RED);
    redChartPanel = new HistogramWrapper(redChart);
    charts.add(redChartPanel);
    greenChart = prepareChart("green", Color.GREEN);
    greenChartPanel = new HistogramWrapper(greenChart);
    charts.add(greenChartPanel);
    blueChart = prepareChart("blue", Color.BLUE);
    blueChartPanel = new HistogramWrapper(blueChart);
    charts.add(blueChartPanel);
    charts.setLayout(new FlowLayout());

    openFileBtn.addActionListener(
        e -> {
          readFile(window, charts, fc);
          window.repaint();
        });

    stretchHistogram.addActionListener(
        e -> {
          stretch();
          update(charts);
          redLookUpTable = createLookUpTable(ColorType.RED);
          greenLookUpTable = createLookUpTable(ColorType.GREEN);
          blueLookUpTable = createLookUpTable(ColorType.BLUE);
        });

    monochromeBtn.addActionListener(
        e -> {
          monochrome();
          update(charts);
        });

    equalizeBtn.addActionListener(
        e -> {
          equalize();
          update(charts);
        });

    manualBinarizeBtn.addActionListener(
        e -> {
          manualBinarization(Integer.parseInt(thresholdTextField.getText()));
          update(charts);
        });

    percentageBinarizeBtn.addActionListener(
        e -> {
          percentageBinarization(Integer.parseInt(percentageTextField.getText()));
          update(charts);
        });

    erode.addActionListener(
            e -> {
              erode();
              update(charts);
            });

    dilate.addActionListener(
            e -> {
              dilate();
              update(charts);
            });

    open.addActionListener(
            e -> {
              open();
              update(charts);
            });

    close.addActionListener(
            e -> {
              close();
              update(charts);
            });

    hitOrMiss.addActionListener(
            e -> {
              hitOrMiss();
            });

    menuButton.setPreferredSize(new Dimension(WIDTH, 100));
    menuButton.setSize(new Dimension(WIDTH, 100));
    menuButton.setLayout(new GridLayout(3, 4));

    imageWrapper.setPreferredSize(new Dimension(CANVAS_WIDTH, CANVAS_HEIGHT));
    imageWrapper.setSize(new Dimension(CANVAS_WIDTH, CANVAS_HEIGHT));

    redChartPanel.setSize(new Dimension(300, 200));
    redChartPanel.setPreferredSize(new Dimension(300, 200));

    greenChartPanel.setSize(new Dimension(300, 200));
    greenChartPanel.setPreferredSize(new Dimension(300, 200));

    blueChartPanel.setSize(new Dimension(300, 200));
    blueChartPanel.setPreferredSize(new Dimension(300, 200));

    charts.setPreferredSize(new Dimension(300, 650));
    charts.setSize(new Dimension(300, 600));

    window.setSize(new Dimension(WIDTH, HEIGHT));
    window.setVisible(true);
    window.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
  }

  private static void update(JPanel charts) {
    calculateCountsOfColors(imageWrapper.getImage());
    updateSeries(charts);
    imageWrapper.repaint();
    window.repaint();
  }

  private static void readFile(JFrame window, JPanel charts, JFileChooser fc) {
    int returnVal = fc.showOpenDialog(window);
    redChart.removeSeries("test 1");
    greenChart.removeSeries("test 1");
    blueChart.removeSeries("test 1");

    if (returnVal == JFileChooser.APPROVE_OPTION) {
      File file = fc.getSelectedFile();
      BufferedImage image = ImageUtils.readFromFile(file);
      TOTAL_PIXELS = image.getHeight() * image.getWidth();
      calculateCountsOfColors(image);
      redLookUpTable = createLookUpTable(ColorType.RED);
      greenLookUpTable = createLookUpTable(ColorType.GREEN);
      blueLookUpTable = createLookUpTable(ColorType.BLUE);
      imageWrapper.setImage(image);
      imageWrapper.repaint();
      addSeries(charts);
    }
    monochrome();
    percentageBinarization(30);
    copy = deepCopy(imageWrapper.getImage());
    window.repaint();
  }

  private static void equalize() {
    final BufferedImage image = imageWrapper.getImage();

    for (int i = 0; i < image.getWidth(); i++) {
      for (int j = 0; j < image.getHeight(); j++) {
        final int colorRed = new Color(image.getRGB(i, j)).getRed();
        final int colorGreen = new Color(image.getRGB(i, j)).getGreen();
        final int colorBlue = new Color(image.getRGB(i, j)).getBlue();
        final Integer newRedColor = redLookUpTable.get(colorRed);
        final Integer newGreenColor = greenLookUpTable.get(colorGreen);
        final Integer newBlueColor = blueLookUpTable.get(colorBlue);
        image.setRGB(i, j, new Color(newRedColor, newGreenColor, newBlueColor).getRGB());
      }
    }
  }

  private static void addSeries(JPanel charts) {
    redChart.addSeries(
        "test 1",
        IntStream.range(0, 256).boxed().collect(Collectors.toList()),
        redCountPerColorValue);
    redChartPanel.repaint();

    greenChart.addSeries(
        "test 1",
        IntStream.range(0, 256).boxed().collect(Collectors.toList()),
        greenCountPerColorValue);
    greenChartPanel.repaint();

    blueChart.addSeries(
        "test 1",
        IntStream.range(0, 256).boxed().collect(Collectors.toList()),
        blueCountPerColorValue);
    charts.repaint();
    blueChartPanel.repaint();
  }

  private static void updateSeries(JPanel charts) {
    redChart.updateCategorySeries(
        "test 1",
        IntStream.range(0, 256).boxed().collect(Collectors.toList()),
        redCountPerColorValue,
        null);
    redChartPanel.repaint();

    greenChart.updateCategorySeries(
        "test 1",
        IntStream.range(0, 256).boxed().collect(Collectors.toList()),
        greenCountPerColorValue,
        null);
    greenChartPanel.repaint();

    blueChart.updateCategorySeries(
        "test 1",
        IntStream.range(0, 256).boxed().collect(Collectors.toList()),
        blueCountPerColorValue,
        null);
    charts.repaint();
    blueChartPanel.repaint();
  }

  private static void calculateCountsOfColors(BufferedImage image) {
    redCountPerColorValue = calculateCountPerColor(image, ColorType.RED);
    greenCountPerColorValue = calculateCountPerColor(image, ColorType.GREEN);
    blueCountPerColorValue = calculateCountPerColor(image, ColorType.BLUE);
  }

  private static CategoryChart prepareChart(String title, Color color) {
    CategoryChart chart = new CategoryChartBuilder().width(300).title(title).height(200).build();
    chart.getStyler().setLegendVisible(false);
    chart.getStyler().setAxisTicksVisible(false);
    chart.getStyler().setChartPadding(0);
    chart.getStyler().setPlotMargin(0);
    chart.getStyler().setSeriesColors(new Color[] {color});
    return chart;
  }

  private static List<Double> calculateCountPerColor(BufferedImage image, ColorType c) {
    final List<Double> counts = new ArrayList<>();
    for (int i = 0; i < 256; i++) {
      counts.add(0D);
    }

    for (int i = 0; i < image.getWidth(); i++) {
      for (int j = 0; j < image.getHeight(); j++) {
        final int colorValue = getColorValue(image, i, j, c);
        counts.set(colorValue, counts.get(colorValue) + 1);
      }
    }
    return counts;
  }

  private static void stretch() {
    final BufferedImage image = imageWrapper.getImage();
    final int lowestRedValue = findLowestValue(redCountPerColorValue);
    final int highestRedValue = findHighestValue(redCountPerColorValue);
    final int lowestGreenValue = findLowestValue(greenCountPerColorValue);
    final int highestGreenValue = findHighestValue(greenCountPerColorValue);
    final int lowestBlueValue = findLowestValue(blueCountPerColorValue);
    final int highestBlueValue = findHighestValue(blueCountPerColorValue);

    for (int i = 0; i < image.getWidth(); i++) {
      for (int j = 0; j < image.getHeight(); j++) {
        final Color color = new Color(image.getRGB(i, j));
        final int newRed = calculateNewColorValue(color.getRed(), lowestRedValue, highestRedValue);
        final int newGreen =
            calculateNewColorValue(color.getGreen(), lowestGreenValue, highestGreenValue);
        final int newBlue =
            calculateNewColorValue(color.getBlue(), lowestBlueValue, highestBlueValue);
        image.setRGB(i, j, new Color(newRed, newGreen, newBlue).getRGB());
      }
    }
  }

  private static List<Double> calculateDistributor(ColorType colorType) {
    List<Double> temp = new ArrayList<>();
    double sum = 0;
    if (ColorType.RED.equals(colorType)) {
      for (int i = 0; i < 256; i++) {
        sum += redCountPerColorValue.get(i);
        temp.add(sum / (TOTAL_PIXELS * 1.0));
      }
    } else if (ColorType.GREEN.equals(colorType)) {
      for (int i = 0; i < 256; i++) {
        sum += greenCountPerColorValue.get(i);
        temp.add(sum / (TOTAL_PIXELS * 1.0));
      }
    } else {
      for (int i = 0; i < 256; i++) {
        sum += blueCountPerColorValue.get(i);
        temp.add(sum / (TOTAL_PIXELS * 1.0));
      }
    }
    return temp;
  }

  private static int calcLutValue(int i, List<Double> distributor, Double first) {
    return (int) ((distributor.get(i) - first) / (1 - first) * 255);
  }

  private static List<Integer> createLookUpTable(ColorType colorType) {
    List<Integer> lut = new ArrayList<>();
    for (int i = 0; i < 256; i++) {
      lut.add(0);
    }
    final List<Double> distributor = calculateDistributor(colorType);
    final Double first = distributor.stream().filter(d -> d > 0).findFirst().orElse(0D);
    for (int i = 0; i < 256; i++) {
      lut.set(i, calcLutValue(i, distributor, first));
    }

    return lut;
  }

  private static int findLowestValue(List<Double> list) {
    int index = 0;
    for (int i = 0; i < list.size(); i++) {
      if (list.get(i) != 0) {
        index = i;
        break;
      }
    }
    return index;
  }

  private static int findHighestValue(List<Double> list) {
    int index = 0;
    for (int i = list.size() - 1; i >= 0; i--) {
      if (list.get(i) != 0) {
        index = i;
        break;
      }
    }
    return index;
  }

  private static int calculateNewColorValue(int Sk, int kMin, int kMax) {
    return (int) ((Sk - kMin) / ((kMax - kMin) * 1.0) * 255);
  }

  private static int getColorValue(BufferedImage image, int i, int j, ColorType colorType) {
    if (ColorType.RED.equals(colorType)) {
      return new Color(image.getRGB(i, j)).getRed();
    } else if (ColorType.GREEN.equals(colorType)) {
      return new Color(image.getRGB(i, j)).getGreen();
    }
    return new Color(image.getRGB(i, j)).getBlue();
  }

  private static void monochrome() {
    final BufferedImage image = imageWrapper.getImage();

    for (int i = 0; i < image.getWidth(); i++) {
      for (int j = 0; j < image.getHeight(); j++) {
        final Color rgb = new Color(image.getRGB(i, j));
        int red = rgb.getRed();
        image.setRGB(i, j, new Color(red, red, red).getRGB());
      }
    }
  }

  private static void manualBinarization(int threshold) {
    final BufferedImage image = imageWrapper.getImage();

    for (int i = 0; i < image.getWidth(); i++) {
      for (int j = 0; j < image.getHeight(); j++) {
        final Color rgb = new Color(image.getRGB(i, j));
        int red = rgb.getRed();
        image.setRGB(
            i,
            j,
            Optional.of(red)
                .filter(r -> r > threshold)
                .map(r -> Color.BLACK.getRGB())
                .orElse(Color.WHITE.getRGB()));
      }
    }
  }

  private static void percentageBinarization(int percentage) {
    final BufferedImage image = imageWrapper.getImage();

    int[] pixelCounts = new int[256];
    for (int i = 0; i < image.getWidth(); i++) {
      for (int j = 0; j < image.getHeight(); j++) {
        final Color rgb = new Color(image.getRGB(i, j));
        int red = rgb.getRed();
        pixelCounts[red]++;
      }
    }

    int[] lut = new int[256];
    double limes = ((double) percentage / 100) * ((double) TOTAL_PIXELS);
    int nextSum = 0;
    for (int i = 0; i < 256; ++i) {
      nextSum = nextSum + pixelCounts[i];
      if (nextSum < limes) {
        lut[i] = Color.WHITE.getRGB();
      } else {
        lut[i] = Color.BLACK.getRGB();
      }
    }

    for (int i = 0; i < image.getWidth(); i++) {
      for (int j = 0; j < image.getHeight(); j++) {
        final Color rgb = new Color(image.getRGB(i, j));
        int red = rgb.getRed();
        image.setRGB(i, j, lut[red]);
      }
    }
  }

  static BufferedImage deepCopy(BufferedImage bi) {
    ColorModel cm = bi.getColorModel();
    boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
    WritableRaster raster = bi.copyData(null);
    return new BufferedImage(cm, raster, isAlphaPremultiplied, null);
  }

  public static void erode() {

    for (int i = 0; i < imageWrapper.getImage().getWidth(); i++) {
      copy.setRGB(i, 0, WHITE.getRGB());
      copy.setRGB(i, imageWrapper.getImage().getHeight() - 1, WHITE.getRGB());
    }
    for (int i = 0; i < imageWrapper.getImage().getWidth(); i++) {
      copy.setRGB(0, i, WHITE.getRGB());
      copy.setRGB(imageWrapper.getImage().getWidth() - 1, i, WHITE.getRGB());
    }

    for (int i = 1; i < imageWrapper.getImage().getWidth() - 1; i++) {
      for (int j = 1; j < imageWrapper.getImage().getHeight() - 1; j++) {
        int[] hits = new int[5];

        if (new Color(imageWrapper.getImage().getRGB(i - 1, j)).getRed() == 0) {
          hits[0] = 1;
        }

        if (new Color(imageWrapper.getImage().getRGB(i, j - 1)).getRed() == 0) {
          hits[1] = 1;
        }
        if (new Color(imageWrapper.getImage().getRGB(i, j)).getRed() == 0) {
          hits[2] = 1;
        }
        if (new Color(imageWrapper.getImage().getRGB(i, j + 1)).getRed() == 0) {
          hits[3] = 1;
        }

        if (new Color(imageWrapper.getImage().getRGB(i + 1, j)).getRed() == 0) {
          hits[4] = 1;
        }

        if (Arrays.stream(hits).reduce(0, Integer::sum) == 5) {
          copy.setRGB(i, j, BLACK.getRGB());
          continue;
        }
        copy.setRGB(i, j, WHITE.getRGB());
      }
    }
    imageWrapper.setImage(deepCopy(copy));
  }

  public static void dilate() {

    for (int i = 1; i < imageWrapper.getImage().getWidth() - 2; i++) {
      for (int j = 1; j < imageWrapper.getImage().getHeight() - 2; j++) {
        int[] hits = new int[5];

        try {
          if (new Color(imageWrapper.getImage().getRGB(i - 1, j)).getRed() == 0) {
            hits[0] = 1;
          }
        } catch (ArrayIndexOutOfBoundsException ignored) {
        }
        try {
          if (new Color(imageWrapper.getImage().getRGB(i, j - 1)).getRed() == 0) {
            hits[1] = 1;
          }
        } catch (ArrayIndexOutOfBoundsException ignored) {
        }
        try {
          if (new Color(imageWrapper.getImage().getRGB(i, j)).getRed() == 0) {
            hits[2] = 1;
          }
        } catch (ArrayIndexOutOfBoundsException ignored) {
        }
        try {
          if (new Color(imageWrapper.getImage().getRGB(i, j + 1)).getRed() == 0) {
            hits[3] = 1;
          }
        } catch (ArrayIndexOutOfBoundsException ignored) {
        }
        try {
          if (new Color(imageWrapper.getImage().getRGB(i + 1, j)).getRed() == 0) {
            hits[4] = 1;
          }
        } catch (ArrayIndexOutOfBoundsException ignored) {
        }

        if (Arrays.stream(hits).reduce(0, Integer::sum) == 0) {
          copy.setRGB(i, j, WHITE.getRGB());
          continue;
        }
        copy.setRGB(i, j, BLACK.getRGB());
      }
    }
    imageWrapper.setImage(deepCopy(copy));
  }

  public static void open() {
    erode();
    dilate();
  }

  public static void close() {
    dilate();
    erode();
  }

  @SneakyThrows
  public static void hitOrMiss() {
    boolean changed;
    do {
      changed = false;
      for (int i = 0; i < imageWrapper.getImage().getWidth(); i++) {
        for (int j = 0; j < imageWrapper.getImage().getHeight(); j++) {
          int[] pass1 = new int[6];
          int[] pass2 = new int[6];
          int[] pass3 = new int[6];
          int[] pass4 = new int[6];

          try {
            if (new Color(imageWrapper.getImage().getRGB(i - 1, j - 1)).getRed() == 0) {
              pass1[0] = 1;
            }
          } catch (ArrayIndexOutOfBoundsException ignored) {
          }
          try {
            if (new Color(imageWrapper.getImage().getRGB(i - 1, j)).getRed() == 0) {
              pass1[1] = 1;
            }
          } catch (ArrayIndexOutOfBoundsException ignored) {
          }
          try {
            if (new Color(imageWrapper.getImage().getRGB(i, j - 1)).getRed() == 0) {
              pass1[2] = 1;
            }
          } catch (ArrayIndexOutOfBoundsException ignored) {
          }
          try {
            if (new Color(imageWrapper.getImage().getRGB(i, j)).getRed() == 255) {
              pass1[3] = 1;
            }
          } catch (ArrayIndexOutOfBoundsException ignored) {
          }

          try {
            if (new Color(imageWrapper.getImage().getRGB(i + 1, j - 1)).getRed() == 0) {
              pass1[4] = 1;
            }
          } catch (ArrayIndexOutOfBoundsException ignored) {
          }
          try {
            if (new Color(imageWrapper.getImage().getRGB(i + 1, j + 1)).getRed() == 255) {
              pass1[5] = 1;
            }
          } catch (ArrayIndexOutOfBoundsException ignored) {
          }

          // pass2
          try {
            if (new Color(imageWrapper.getImage().getRGB(i - 1, j - 1)).getRed() == 0) {
              pass2[0] = 1;
            }
          } catch (ArrayIndexOutOfBoundsException ignored) {
          }
          try {
            if (new Color(imageWrapper.getImage().getRGB(i - 1, j)).getRed() == 0) {
              pass2[1] = 1;
            }
          } catch (ArrayIndexOutOfBoundsException ignored) {
          }
          try {
            if (new Color(imageWrapper.getImage().getRGB(i - 1, j + 1)).getRed() == 0) {
              pass2[2] = 1;
            }
          } catch (ArrayIndexOutOfBoundsException ignored) {
          }
          try {
            if (new Color(imageWrapper.getImage().getRGB(i, j + 1)).getRed() == 0) {
              pass2[3] = 1;
            }
          } catch (ArrayIndexOutOfBoundsException ignored) {
          }

          try {
            if (new Color(imageWrapper.getImage().getRGB(i, j)).getRed() == 255) {
              pass2[4] = 1;
            }
          } catch (ArrayIndexOutOfBoundsException ignored) {
          }
          try {
            if (new Color(imageWrapper.getImage().getRGB(i + 1, j - 1)).getRed() == 255) {
              pass2[5] = 1;
            }
          } catch (ArrayIndexOutOfBoundsException ignored) {
          }
          // pass3
          try {
            if (new Color(imageWrapper.getImage().getRGB(i - 1, j + 1)).getRed() == 0) {
              pass3[0] = 1;
            }
          } catch (ArrayIndexOutOfBoundsException ignored) {
          }
          try {
            if (new Color(imageWrapper.getImage().getRGB(i, j + 1)).getRed() == 0) {
              pass3[1] = 1;
            }
          } catch (ArrayIndexOutOfBoundsException ignored) {
          }
          try {
            if (new Color(imageWrapper.getImage().getRGB(i + 1, j + 1)).getRed() == 0) {
              pass3[2] = 1;
            }
          } catch (ArrayIndexOutOfBoundsException ignored) {
          }
          try {
            if (new Color(imageWrapper.getImage().getRGB(i - 1, j - 1)).getRed() == 255) {
              pass3[3] = 1;
            }
          } catch (ArrayIndexOutOfBoundsException ignored) {
          }

          try {
            if (new Color(imageWrapper.getImage().getRGB(i, j)).getRed() == 255) {
              pass3[4] = 1;
            }
          } catch (ArrayIndexOutOfBoundsException ignored) {
          }
          try {
            if (new Color(imageWrapper.getImage().getRGB(i + 1, j)).getRed() == 0) {
              pass3[5] = 1;
            }
          } catch (ArrayIndexOutOfBoundsException ignored) {
          }

          // pass4
          try {
            if (new Color(imageWrapper.getImage().getRGB(i + 1, j - 1)).getRed() == 0) {
              pass4[0] = 1;
            }
          } catch (ArrayIndexOutOfBoundsException ignored) {
          }
          try {
            if (new Color(imageWrapper.getImage().getRGB(i + 1, j)).getRed() == 0) {
              pass4[1] = 1;
            }
          } catch (ArrayIndexOutOfBoundsException ignored) {
          }
          try {
            if (new Color(imageWrapper.getImage().getRGB(i + 1, j + 1)).getRed() == 0) {
              pass4[2] = 1;
            }
          } catch (ArrayIndexOutOfBoundsException ignored) {
          }
          try {
            if (new Color(imageWrapper.getImage().getRGB(i, j - 1)).getRed() == 0) {
              pass4[3] = 1;
            }
          } catch (ArrayIndexOutOfBoundsException ignored) {
          }

          try {
            if (new Color(imageWrapper.getImage().getRGB(i, j)).getRed() == 255) {
              pass4[4] = 1;
            }
          } catch (ArrayIndexOutOfBoundsException ignored) {
          }
          try {
            if (new Color(imageWrapper.getImage().getRGB(i - 1, j + 1)).getRed() == 255) {
              pass4[5] = 1;
            }
          } catch (ArrayIndexOutOfBoundsException ignored) {
          }
          boolean localChange = true;
          final int sum1 = Arrays.stream(pass1).reduce(0, Integer::sum);
          final int sum2 = Arrays.stream(pass2).reduce(0, Integer::sum);
          final int sum3 = Arrays.stream(pass3).reduce(0, Integer::sum);
          final int sum4 = Arrays.stream(pass4).reduce(0, Integer::sum);
          if (sum1 != sum2 || sum2 != sum3 || sum3 != sum4 || sum4 != 6) {
            localChange = false;
          }
          if (localChange) {
            imageWrapper.getImage().setRGB(i, j, BLACK.getRGB());
            if (!changed) {
              changed = true;
            }
          } else {
            imageWrapper.getImage().setRGB(i, j, WHITE.getRGB());
          }
        }
      }
      copy = deepCopy(imageWrapper.getImage());
      window.repaint();
      Thread.sleep(1000);
    } while (changed);
  }

  private enum ColorType {
    RED,
    GREEN,
    BLUE;
  }
}
