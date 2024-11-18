import br.com.labs.dao.ProductDAO;
import br.com.labs.usecase.CreateProductUseCase;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CreateProductUseCaseTest {

    public void list_products_and_expect_list_with_products() {

        ProductDAO productDAO = Mockito.mock(ProductDAO.class);
        CreateProductUseCase createProductUseCase = new CreateProductUseCase(productDAO);


    }
}
