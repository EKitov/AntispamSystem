package com.example.antispamsystem.controller;

import com.example.antispamsystem.model.Bid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Controller
public class BidListController {
    @GetMapping("/bidlist")
    public String showBids(Model model) {
        List<Bid> bids = new ArrayList<>();

        Bid bid1 = new Bid(1L, "Иван Иванов", "+79022478718", "ivan@example.com", "Новая", "http://example.com/presentation1", "Отличная заявка", LocalDateTime.now().minusDays(1), "127.0.0.1");
        Bid bid2 = new Bid(2L, "Петр Петров", "+79198442992", "petr@example.com", "В обработке", "http://example.com/presentation2", "Требует уточнения", LocalDateTime.now(), "127.0.0.2");

        bids.add(bid1);
        bids.add(bid2);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm - dd.MM.yyyy");
        bids.forEach(bid -> bid.setFormattedDate(bid.getCreatedDate().format(formatter)));

        model.addAttribute("bids", bids);
        return "bidlist";
    }
}
