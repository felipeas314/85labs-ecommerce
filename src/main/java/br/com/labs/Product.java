package br.com.labs;

public class Product {

    private String name;

    private float price;

    public Product() {

    }

    public Product(String name,float price) {
        this.name = name;
        this.price = price;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPrice(float price) {
        this.price = price;
    }

    public float getPrice() {
        return price;
    }
}
