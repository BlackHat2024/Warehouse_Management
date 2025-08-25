package com.warehouse.warehouse_management.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;

@Controller("/order")
public class OrderController {
    @PostMapping("/new")
    public String newOrder() {

    }
}
