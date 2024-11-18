package br.com.labs.usecase;

import br.com.labs.dao.ProductDAO;

public class CreateProductUseCase {

    private ProductDAO productDAO;

    public CreateProductUseCase(ProductDAO productDAO) {
        this.productDAO = productDAO;
    }

    public void execute() {

    }
}
