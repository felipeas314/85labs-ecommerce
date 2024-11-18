package br.com.labs.dao;

import br.com.labs.model.Product;

import java.util.List;

public interface ProductDAO {

    public void add(Product product);

    public List<Product> list(int quantity, int page);
}
