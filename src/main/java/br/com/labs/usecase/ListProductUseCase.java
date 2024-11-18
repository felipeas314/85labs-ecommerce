package br.com.labs.usecase;

import br.com.labs.dao.ProductDAO;
import br.com.labs.model.Product;

import java.util.List;

public class ListProductUseCase {

    private ProductDAO productDAO;

    public List<Product> execute(int quantity, int page) {
        return productDAO.list(quantity, page);
    }
}
