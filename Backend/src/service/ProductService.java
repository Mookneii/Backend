

package service;

import model.Product;
import repository.ProductRepository;

import java.sql.SQLException;
import java.util.List;

public class ProductService {

    private ProductRepository repo = new ProductRepository();

    // ADD PRODUCT
    public void addProduct(Product p) throws SQLException {

        // Validation rules
        if (p.getName() == null || p.getName().isEmpty()) {
            throw new IllegalArgumentException("Product name is required");
        }

        if (p.getPrice() <= 0) {
            throw new IllegalArgumentException("Price must be greater than 0");
        }

        if (p.getStock() < 0) {
            throw new IllegalArgumentException("Stock cannot be negative");
        }

        // Check duplicate ID
        Product existing = repo.getProductById(p.getId());
        if (existing != null) {
            throw new IllegalArgumentException("Product ID already exists");
        }

        repo.addProduct(p);
    }

    // GET ALL PRODUCTS
    public List<Product> getAllProducts() throws SQLException {
        return repo.getAllProducts();
    }

    // GET PRODUCT BY ID
    public Product getProductById(int id) throws SQLException {
        return repo.getProductById(id);
    }

    // UPDATE PRODUCT
    public void updateProduct(Product p) throws SQLException {

        Product existing = repo.getProductById(p.getId());
        if (existing == null) {
            throw new IllegalArgumentException("Product not found");
        }

        if (p.getPrice() <= 0) {
            throw new IllegalArgumentException("Invalid price");
        }

        repo.updateProduct(p);
    }

    // DELETE PRODUCT
    public void deleteProduct(int id) throws SQLException {

        Product existing = repo.getProductById(id);
        if (existing == null) {
            throw new IllegalArgumentException("Product not found");
        }

        repo.deleteProduct(id);
    }

    // SEARCH (Bonus)
    public List<Product> searchProducts(String name, String size)
            throws SQLException {

        if (name == null) name = "";
        if (size == null) size = "";

        return repo.search(name, size);
    }
}

