import model.Product;
import model.User;
import repository.ProductRepository;

public class Main {
    public static void main(String[] args) {
        try {
            ProductRepository repo = new ProductRepository();

            Product p = new Product(
                4,
                "Bluey",
                "Winter Jacket",
                "Adidas",
                "Jacket",
                "Clothing",
                "M",
                "Blue",
                54.0,
                2,  
                "https://i.pinimg.com/736x/eb/6c/72/eb6c7203fe5e7f4985e7f3e0b9ae022f.jpg"
            );

            repo.addProduct(p);
            System.out.println("DONE");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
