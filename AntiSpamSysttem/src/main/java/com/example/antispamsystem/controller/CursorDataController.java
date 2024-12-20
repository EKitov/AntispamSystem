package com.example.antispamsystem.controller;

import com.example.antispamsystem.service.ImageProcessingService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@RestController
public class CursorDataController {
    private static final String BASE_DIRECTORY = "user/cursor_paths/";
    private static final String TEMP_DIRECTORY = BASE_DIRECTORY + "temp/";

    // Поле для хранения текущего пути изображения
    private String currentImagePath;

    private final ImageProcessingService imageProcessingService = new ImageProcessingService();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    @PostMapping("/save-cursor-image")
    public String saveCursorImage(@RequestBody Map<String, Object> cursorDataRequest, HttpServletRequest request) {
        // Получаем параметры с клиента
        List<Map<String, Object>> cursorData = (List<Map<String, Object>>) cursorDataRequest.get("cursorData");
        double typingSpeed = Double.parseDouble(cursorDataRequest.get("typingSpeed").toString()); // Скорость набора текста
        int typoCount = Integer.parseInt(cursorDataRequest.get("typoCount").toString()); // Количество опечаток
        List<Integer> typingSpeeds = (List<Integer>) cursorDataRequest.get("typingSpeeds");
        List<Integer> keyPressDurations = (List<Integer>) cursorDataRequest.get("keyPressDurations");

        int width = 1024;
        int height = 1024;

        createDirectories(BASE_DIRECTORY);
        createDirectories(TEMP_DIRECTORY);

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();

        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, width, height);

        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
        for (Map<String, Object> point : cursorData) {
            int x = (int) point.get("x");
            int y = (int) point.get("y");
            if (x < minX) minX = x;
            if (x > maxX) maxX = x;
            if (y < minY) minY = y;
            if (y > maxY) maxY = y;
        }
        g2d.setColor(Color.BLUE);
        g2d.setStroke(new BasicStroke(2));
        for (int i = 1; i < cursorData.size(); i++) {
            int x1 = (int) cursorData.get(i - 1).get("x");
            int y1 = (int) cursorData.get(i - 1).get("y");
            int x2 = (int) cursorData.get(i).get("x");
            int y2 = (int) cursorData.get(i).get("y");

            int normX1 = (x1 - minX) * width / (maxX - minX);
            int normY1 = (y1 - minY) * height / (maxY - minY);
            int normX2 = (x2 - minX) * width / (maxX - minX);
            int normY2 = (y2 - minY) * height / (maxY - minY);

            g2d.drawLine(normX1, normY1, normX2, normY2);
        }

        g2d.dispose();

        String fileName = "cursor_path_" + System.currentTimeMillis() + ".png";
        File outputFile = new File(TEMP_DIRECTORY + fileName);

        try {
            System.out.println("Путь к создаваемому изображению: " + outputFile.getPath());
            ImageIO.write(image, "png", outputFile);
        } catch (IOException e) {
            e.printStackTrace();
            return "{\"status\":\"error\"}";
        }

        this.currentImagePath = TEMP_DIRECTORY + fileName;

        // Передаем дополнительные параметры для проверки на робота
        int isBehaviourNormal = imageProcessingService.processAndCompareTrajectory(currentImagePath, BASE_DIRECTORY, typingSpeed, typoCount, typingSpeeds, keyPressDurations);
        if(isBehaviourNormal == 11) {
            File newPath = new File(BASE_DIRECTORY + fileName);
            outputFile.renameTo(newPath);
            this.currentImagePath = newPath.getPath();
            return "{\"status\":\"success\", \"file\":\"" + fileName + "\", \"trajectory\":\"normal\"}";
        }
        else {
            if(isBehaviourNormal == 1) {
                scheduleDeletion(currentImagePath);
                return "{\"status\":\"success\", \"file\":\"" + fileName + "\", \"trajectory\":\"suspicious\"}";
            }
            else {
                if(isBehaviourNormal == 10) {
                    File newPath = new File(BASE_DIRECTORY + fileName);
                    outputFile.renameTo(newPath);
                    return "{\"status\":\"success\", \"file\":\"" + fileName + "\", \"trajectory normal and keyScore\":\"suspicious\"}";
                }
                else {
                    scheduleDeletion(currentImagePath);
                    return "{\"status\":\"success\", \"file\":\"" + fileName + "\", \"trajectory\":\"spam\"}";
                }
            }
        }
    }

    @PostMapping("/move-to-ethalon")
    public ResponseEntity<String> moveToEthalon() {
        try {
            // Проверяем, установлен ли путь к текущему изображению
            if (currentImagePath == null) {
                return ResponseEntity.badRequest().body("Путь к изображению не установлен.");
            }

            String targetDirectory = BASE_DIRECTORY; // Путь для эталонных траекторий
            imageProcessingService.moveImageToDirectory(currentImagePath, targetDirectory);
            this.currentImagePath = targetDirectory + new File(currentImagePath).getName(); // Обновляем текущий путь
            return ResponseEntity.ok("Изображение перемещено к эталонным траекториям.");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Ошибка перемещения изображения.");
        }
    }

    private void createDirectories(String directoryPath) {
        File directory = new File(directoryPath);
        if (!directory.exists()) {
            directory.mkdirs();
        }
    }

    private void scheduleDeletion(String imagePath) {
        scheduler.schedule(() -> {
            imageProcessingService.deleteImage(imagePath);
            System.out.println("Изображение удалено из временной директории: " + imagePath);
        }, 1, TimeUnit.MINUTES);
    }
}
