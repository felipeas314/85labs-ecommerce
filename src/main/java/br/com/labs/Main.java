package br.com.labs;

public class Main {
    public static void main(String[] args) {

        Product product = new Product();

        product.name = "Iphone";
        product.setPrice(12);

        System.out.println("Nome: "+product.name);
        System.out.println(product.getPrice());


    }
}