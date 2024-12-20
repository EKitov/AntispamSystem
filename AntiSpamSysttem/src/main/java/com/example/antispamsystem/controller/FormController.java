package com.example.antispamsystem.controller;

import com.example.antispamsystem.model.User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
@Controller
public class FormController {
    @GetMapping("/form")
    public String showForm(Model model) {
        model.addAttribute("user", new User());
        return "form";
    }

    @PostMapping("/form")
    public String submitForm(@ModelAttribute User user, Model model) {
        // Проверка, отмечена ли галочка "Я не робот"
        if (user.getRobotCheck() == null || !user.getRobotCheck()) {
            model.addAttribute("error", "Пожалуйста, подтвердите, что вы не робот.");
            return "form";
        }

        // Логируем данные о курсоре и нажатиях клавиш (или обрабатываем их другим способом)
        System.out.println("Cursor Data: " + user.getCursorData());
        System.out.println("Key Press Data: " + user.getKeyPressData());

        // Обработка успешной отправки формы
        return "success";
    }
}
