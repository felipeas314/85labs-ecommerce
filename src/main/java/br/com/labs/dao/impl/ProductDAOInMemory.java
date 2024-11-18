package br.com.labs.dao.impl;

import br.com.labs.dao.ProductDAO;
import br.com.labs.model.Product;

import java.util.ArrayList;
import java.util.List;

public class ProductDAOInMemory implements ProductDAO {

    private List<Product> products = new ArrayList<>();

    @Override
    public void add(Product product) {
        products.add(product);
    }

    public List<Product> list(int quantity, int page) {
        return products;
    }
}
