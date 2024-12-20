package com.example.antispamsystem.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.regex.Pattern;

@Controller
@RequestMapping("/adminpanel")
public class AdminPanelController {

    // Файлы черных списков
    private final String IP_FILE = "adminpanel/BL_IPs.txt";
    private final String PHONE_FILE = "adminpanel/BL_Phones.txt";
    private final String EMAIL_FILE = "adminpanel/BL_Emails.txt";

    // Файлы белых списков
    private final String WHITE_IP_FILE = "adminpanel/WL_IPs.txt";
    private final String WHITE_PHONE_FILE = "adminpanel/WL_Phones.txt";
    private final String WHITE_EMAIL_FILE = "adminpanel/WL_Emails.txt";

    // Параметры (можно сохранить в БД или файл)
    private Map<String, Integer> blackListParams;
    private Map<String, Integer> whiteListParams;

    // Шаблоны для проверки форматов
    private final Pattern IP_PATTERN = Pattern.compile("^([0-9]{1,3}\\.){3}[0-9]{1,3}$");
    private final Pattern PHONE_PATTERN = Pattern.compile("^\\+[0-9]{1,3}[0-9]{9,12}$");
    private final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");

    // Списки черных и белых списков
    private List<String> ipBlacklist;
    private List<String> phoneBlacklist;
    private List<String> emailBlacklist;

    private List<String> ipWhitelist;
    private List<String> phoneWhitelist;
    private List<String> emailWhitelist;

    public AdminPanelController() {
        loadAllData();
        initializeParams();
    }

    // Инициализация параметров черного и белого списка
    private void initializeParams() {
        blackListParams = new HashMap<>();
        whiteListParams = new HashMap<>();
        blackListParams.put("param1", 20);
        blackListParams.put("param2", 500);
        blackListParams.put("param3", 100);
        whiteListParams.put("param1", 10);
        whiteListParams.put("param2", 200);
        whiteListParams.put("param3", 50);
    }

    // Загрузка данных из файлов
    private void loadAllData() {
        try {
            ipBlacklist = loadFromFile(IP_FILE);
            phoneBlacklist = loadFromFile(PHONE_FILE);
            emailBlacklist = loadFromFile(EMAIL_FILE);

            ipWhitelist = loadFromFile(WHITE_IP_FILE);
            phoneWhitelist = loadFromFile(WHITE_PHONE_FILE);
            emailWhitelist = loadFromFile(WHITE_EMAIL_FILE);
        } catch (IOException e) {
            ipBlacklist = new ArrayList<>();
            phoneBlacklist = new ArrayList<>();
            emailBlacklist = new ArrayList<>();
            ipWhitelist = new ArrayList<>();
            phoneWhitelist = new ArrayList<>();
            emailWhitelist = new ArrayList<>();
            e.printStackTrace();
        }
    }

    // Загрузка данных из файла
    private List<String> loadFromFile(String filePath) throws IOException {
        return Files.exists(Paths.get(filePath)) ? Files.readAllLines(Paths.get(filePath)) : new ArrayList<>();
    }

    // Основной метод для отображения панели управления
    @GetMapping
    public String showAdminPanel(Model model) {
        loadAllData();

        model.addAttribute("ipBlacklist", String.join("\r\n", ipBlacklist));
        model.addAttribute("phoneBlacklist", String.join("\r\n", phoneBlacklist));
        model.addAttribute("emailBlacklist", String.join("\r\n", emailBlacklist));

        model.addAttribute("ipWhitelist", String.join("\r\n", ipWhitelist));
        model.addAttribute("phoneWhitelist", String.join("\r\n", phoneWhitelist));
        model.addAttribute("emailWhitelist", String.join("\r\n", emailWhitelist));

        model.addAttribute("blacklistParam1", blackListParams.get("param1"));
        model.addAttribute("blacklistParam2", blackListParams.get("param2"));
        model.addAttribute("blacklistParam3", blackListParams.get("param3"));

        model.addAttribute("whitelistParam1", whiteListParams.get("param1"));
        model.addAttribute("whitelistParam2", whiteListParams.get("param2"));
        model.addAttribute("whitelistParam3", whiteListParams.get("param3"));

        return "adminpanel";
    }

    // Сохранение данных черного списка (параметров и списков)
    @PostMapping("/saveblacklistData")
    @ResponseBody
    public String saveBlacklistData(@RequestBody Map<String, Object> data) {
        return saveData(data, "blacklist");
    }

    // Сохранение данных белого списка (параметров и списков)
    @PostMapping("/savewhitelistData")
    @ResponseBody
    public String saveWhitelistData(@RequestBody Map<String, Object> data) {
        return saveData(data, "whitelist");
    }

    // Общий метод для сохранения данных черного и белого списка
    private String saveData(Map<String, Object> data, String listType) {
        try {
            // Сохраняем параметры
            if (listType.equals("blacklist")) {
                blackListParams.put("param1", Integer.parseInt((String) data.get("param1")));
                blackListParams.put("param2", Integer.parseInt((String) data.get("param2")));
                blackListParams.put("param3", Integer.parseInt((String) data.get("param3")));
            } else {
                whiteListParams.put("param1", Integer.parseInt((String) data.get("param1")));
                whiteListParams.put("param2", Integer.parseInt((String) data.get("param2")));
                whiteListParams.put("param3", Integer.parseInt((String) data.get("param3")));
            }

            // Сохраняем данные списков в файлы
            List<String> ipData = (List<String>) data.get("ipData");
            List<String> phoneData = (List<String>) data.get("phoneData");
            List<String> emailData = (List<String>) data.get("emailData");

            if (listType.equals("blacklist")) {
                saveToFile(IP_FILE, ipData);
                saveToFile(PHONE_FILE, phoneData);
                saveToFile(EMAIL_FILE, emailData);
            } else {
                saveToFile(WHITE_IP_FILE, ipData);
                saveToFile(WHITE_PHONE_FILE, phoneData);
                saveToFile(WHITE_EMAIL_FILE, emailData);
            }

            return listType.equals("blacklist") ?
                    "Черный список успешно сохранен!" :
                    "Белый список успешно сохранен!";
        } catch (IOException e) {
            e.printStackTrace();
            return "Ошибка при сохранении данных!";
        }
    }

    // Метод для сохранения данных в файл
    private void saveToFile(String filePath, List<String> data) throws IOException {
        Files.write(Paths.get(filePath), data, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    // Метод для генерации ответа
    private Map<String, String> generateResponse(String message, List<String> list, String listName) {
        Map<String, String> response = new HashMap<>();
        response.put("message", message);
        response.put(listName, String.join("\r\n", list));
        return response;
    }
}
