package br.com.labs.model;

import java.math.BigDecimal;

public class Product {

    private String name;

    private String description;

    private String code;

    private BigDecimal price;

    private Category category;

    public Product() {}

    public Product(String name, String description, String code) {
        this.name = name;
        validCode();
    }

    public String getName() {
        return this.name;
    }

    private boolean validCode() {
        return code.length() >= 7;
    }

    private void validPrice() {

    }
}
