package com.example.antispamsystem.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Base64;

@RestController
public class ImageController {
    @PostMapping("/save-image")
    public String saveImage(@RequestBody ImageRequest imageRequest) {
        try {
            // Извлекаем base64 строку и убираем префикс "data:image/png;base64,"
            String base64Image = imageRequest.getImage().split(",")[1];
            byte[] imageBytes = Base64.getDecoder().decode(base64Image);

            // Указываем путь к файлу, куда будем сохранять изображение
            try (OutputStream stream = new FileOutputStream("saved_cursor_path.png")) {
                stream.write(imageBytes);
            }

            return "{\"status\":\"success\"}";
        } catch (Exception e) {
            e.printStackTrace();
            return "{\"status\":\"error\"}";
        }
    }

    // DTO класс для приёма JSON с изображением
    static class ImageRequest {
        private String image;

        public String getImage() {
            return image;
        }

        public void setImage(String image) {
            this.image = image;
        }
    }
}
