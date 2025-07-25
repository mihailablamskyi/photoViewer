import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.List;

public static Color bgColor = new Color(70, 70, 70);

public static JFrame mainFrame = new JFrame("Просмотр фото");

public static JPanel mainPanel = new JPanel();
public static JPanel infoPanel = new JPanel();

public static JLabel photoLabel = new JLabel();
public static JLabel[] switchLabels = new JLabel[2];
public static JLabel infoLabel = new JLabel();

public static BufferedImage currentImage;

public static double scaleFactor = 1.0;
public static final double SCALE_STEP = 0.1;
public static final double MAX_SCALE = 5.0;
public static final double MIN_SCALE = 0.1;

public static List<File> imageFilesInFolder = new ArrayList<>();

public static int currentImageIndex = -1;

public static File currentImageFile;

public static void main(String[] args) throws IOException {
    initializeFrame();
}

private static void initializeFrame() throws IOException {
    mainFrame.setExtendedState(Frame.MAXIMIZED_BOTH);
    mainFrame.getContentPane().setBackground(bgColor);
    mainFrame.setLayout(new BorderLayout());
    mainFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    mainFrame.setLocationRelativeTo(null);
    mainFrame.setUndecorated(true);

    mainFrame.setJMenuBar(menuBar());
    initializePanels();
    mainFrame.setVisible(true);
}

private static JMenuBar menuBar() {
    JMenuBar menuBar = new JMenuBar();
    menuBar.setPreferredSize(new Dimension(1920, 30));

    JMenuItem openPhotoItem = new JMenuItem("Выбрать фото");
    openPhotoItem.addActionListener(_ -> openImage());

    JMenuItem closePhotoItem = new JMenuItem("Закрыть фото");
    closePhotoItem.addActionListener(_ -> closeImage());

    JMenuItem zoomPhotoItem = new JMenuItem("Приблизить фото");
    zoomPhotoItem.addActionListener(_ -> zoomImage(true));

    JMenuItem unzoomPhotoItem = new JMenuItem("Отдалить фото");
    unzoomPhotoItem.addActionListener(_ -> zoomImage(false));

    JMenuItem iconifiedFrameItem = new JMenuItem("Свернуть окно");
    iconifiedFrameItem.addActionListener(_ -> mainFrame.setState(Frame.ICONIFIED));

    JMenuItem closeFrameItem = new JMenuItem("Закрыть окно");
    closeFrameItem.addActionListener(_ -> System.exit(1));

    menuBar.add(openPhotoItem);
    menuBar.add(closePhotoItem);
    menuBar.add(zoomPhotoItem);
    menuBar.add(unzoomPhotoItem);
    menuBar.add(iconifiedFrameItem);
    menuBar.add(closeFrameItem);
    return menuBar;
}

private static void initializePanels() throws IOException {
    mainPanel.setBackground(bgColor);
    mainPanel.setLayout(new BorderLayout());
    initializeLabels();

    infoPanel.setPreferredSize(new Dimension(1900, 70));
    infoPanel.setBackground(bgColor);
    infoPanel.setBorder(BorderFactory.createLineBorder(Color.WHITE));
    infoPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 5));

    infoLabel.setForeground(Color.WHITE);
    infoLabel.setFont(new Font("Times New Roman", Font.PLAIN, 30));
    infoLabel.setText("Фото не загружено");
    infoPanel.add(infoLabel);

    mainFrame.add(mainPanel, BorderLayout.CENTER);
    mainFrame.add(infoPanel, BorderLayout.SOUTH);
}

private static void initializeLabels() throws IOException {
    photoLabel.setHorizontalAlignment(SwingConstants.CENTER);
    photoLabel.setVerticalAlignment(SwingConstants.CENTER);
    photoLabel.setOpaque(true);
    photoLabel.setBorder(BorderFactory.createLineBorder(Color.WHITE));
    photoLabel.setBackground(bgColor);

    photoLabel.addMouseWheelListener(e -> {
        if (currentImage != null) {
            int notches = e.getWheelRotation();
            zoomImage(notches < 0);
        }
    });

    BufferedImage originalImage = ImageIO.read(new File("D:\\Java\\photoViewer\\src\\icons/lastPhoto.png"));

    ImageIcon originalIcon = createRoundedIcon(mirrorImage(originalImage, false));
    ImageIcon mirroredIcon = createRoundedIcon(mirrorImage(originalImage, true));

    for (int i = 0; i < 2; i++) {
        int finalI = i;
        switchLabels[i] = new JLabel();
        switchLabels[i].setPreferredSize(new Dimension(100, 800));
        switchLabels[i].setBackground(bgColor);
        switchLabels[i].setOpaque(false);
        switchLabels[i].setHorizontalAlignment(SwingConstants.CENTER);
        switchLabels[i].addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                switchLabels[finalI].setBorder(BorderFactory.createLineBorder(Color.white));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                switchLabels[finalI].setBorder(null);
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (currentImage != null && !imageFilesInFolder.isEmpty()) {
                    if (finalI == 0) {
                        showNextImage();
                    } else {
                        showPreviousImage();
                    }
                }
            }
        });

        if (i == 0) {
            switchLabels[i].setIcon(originalIcon);
        } else {
            switchLabels[i].setIcon(mirroredIcon);
        }
    }

    mainPanel.add(switchLabels[0], BorderLayout.WEST);
    mainPanel.add(photoLabel, BorderLayout.CENTER);
    mainPanel.add(switchLabels[1], BorderLayout.EAST);
}

private static void openImage() {
    FileDialog fd = new FileDialog(mainFrame, "Выберите изображение", FileDialog.LOAD);
    fd.setFilenameFilter((_, name) ->
            name.toLowerCase().endsWith(".jpg") || name.toLowerCase().endsWith(".jpeg") || name.toLowerCase().endsWith(".png") || name.toLowerCase().endsWith(".gif") || name.toLowerCase().endsWith(".bmp"));

    fd.setVisible(true);

    if (fd.getFile() != null) {
        File selectedFile = new File(fd.getDirectory(), fd.getFile());
        loadImageFilesFromFolder(selectedFile);
        loadAndPreviewImage(selectedFile);
    }
}

private static void loadImageFilesFromFolder(File selectedFile) {
    imageFilesInFolder.clear();
    File folder = selectedFile.getParentFile();
    File[] files = folder.listFiles((_, name) -> name.toLowerCase().matches(".*\\.(jpg|jpeg|png|gif|bmp)$"));

    if (files != null) {
        for (File file : files) {
            imageFilesInFolder.add(file);
            if (file.equals(selectedFile)) {
                currentImageIndex = imageFilesInFolder.size() - 1;
            }
        }
    }
}

private static void loadAndPreviewImage(File file) {
    try {
        currentImage = ImageIO.read(file);
        currentImageFile = file;
        if (currentImage != null) {
            scaleFactor = 1.0;
            updateImage();

            String info = String.format("Файл: %s | Размер: %dx%d | Размер файла: %.2f KB | %d/%d",
                    file.getName(), currentImage.getWidth(), currentImage.getHeight(), file.length() / 1024.0, currentImageIndex + 1, imageFilesInFolder.size());
            infoLabel.setText(info);
        }
    } catch (IOException ex) {
        JOptionPane.showMessageDialog(mainFrame, "Ошибка загрузки изображения", "Ошибка", JOptionPane.ERROR_MESSAGE);
    }
}

private static void showNextImage() {
    if (currentImageIndex < imageFilesInFolder.size() - 1) {
        currentImageIndex++;
        loadAndPreviewImage(imageFilesInFolder.get(currentImageIndex));
    }
}

private static void showPreviousImage() {
    if (currentImageIndex > 0) {
        currentImageIndex--;
        loadAndPreviewImage(imageFilesInFolder.get(currentImageIndex));
    }
}

private static void closeImage() {
    currentImage = null;
    currentImageFile = null;
    currentImageIndex = -1;
    imageFilesInFolder.clear();
    photoLabel.setIcon(null);
    infoLabel.setText("Фото не загружено");
}

private static void zoomImage(boolean zoomIn) {
    if (currentImage == null) return;

    if (zoomIn) {
        scaleFactor = Math.min(scaleFactor + SCALE_STEP, MAX_SCALE);
    } else {
        scaleFactor = Math.max(scaleFactor - SCALE_STEP, MIN_SCALE);
    }

    updateImage();
}

private static void updateImage() {
    if (currentImage != null) {
        int newWidth = (int) (currentImage.getWidth() * scaleFactor);
        int newHeight = (int) (currentImage.getHeight() * scaleFactor);

        Image scaledImage = currentImage.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH);
        photoLabel.setIcon(new ImageIcon(scaledImage));
    }
}

private static ImageIcon createRoundedIcon(BufferedImage image) {
    BufferedImage roundedImage = new BufferedImage(70, 70, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g2 = roundedImage.createGraphics();
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

    g2.setColor(new Color(0, 0, 0, 0));
    g2.fillRect(0, 0, 70, 70);
    g2.setComposite(AlphaComposite.Src);

    g2.setColor(bgColor);
    g2.fillRoundRect(0, 0, 70, 70, 25, 25);
    g2.setComposite(AlphaComposite.SrcAtop);

    g2.drawImage(image.getScaledInstance(70, 70, Image.SCALE_SMOOTH), 0, 0, null);
    g2.dispose();

    return new ImageIcon(roundedImage);
}

private static BufferedImage mirrorImage(BufferedImage image, boolean horizontal) {
    int width = image.getWidth();
    int height = image.getHeight();

    BufferedImage mirrored = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g2d = mirrored.createGraphics();

    AffineTransform transform;
    if (horizontal) {
        transform = AffineTransform.getScaleInstance(-1, 1);
        transform.translate(-width, 0);
    } else {
        transform = AffineTransform.getScaleInstance(1, -1);
        transform.translate(0, -height);
    }

    g2d.setTransform(transform);
    g2d.drawImage(image, 0, 0, null);
    g2d.dispose();

    return mirrored;
}
