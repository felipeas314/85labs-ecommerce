package br.com.labs;

public class Main {
    public static void main(String[] args) {

        Product iphone = new Product();

        iphone.setName("Iphone");
        iphone.setPrice(12000);

        System.out.println("Nome: "+iphone.getName());
        System.out.println(iphone.getPrice());

        Product book = new Product();

        book.setName("Cronica de gelo e fogo");
        book.setPrice(54);

        System.out.println("Nome: "+book.getName());
        System.out.println(book.getPrice());

    }
}