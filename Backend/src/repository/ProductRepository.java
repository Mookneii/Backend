package repository;

import model.Product;
import util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProductRepository {

    
    
    public void addProduct(Product p) throws SQLException {
        String sql = """
                INSERT INTO products (name, description, brand, type, category, size, color, price, stock, image)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        Connection con = DBConnection.getConnection();
        PreparedStatement ps = con.prepareStatement(sql);

        
        ps.setString(1, p.getName());
        ps.setString(2, p.getDescription());
        ps.setString(3, p.getBrand());
        ps.setString(4, p.getType());
        ps.setString(5, p.getCategory());
        ps.setString(6, p.getSize());
        ps.setString(7, p.getColor());
        ps.setDouble(8, p.getPrice());
        ps.setInt(9, p.getStock());
        ps.setString(10, p.getImage());
        System.out.println("INSERTING: " + p.getName());


        ps.executeUpdate();
        con.close();
    }

    
    public List<Product> getAllProducts() {
        List<Product> products = new ArrayList<>();
        String sql = "SELECT * FROM products";

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Product p = new Product(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("description"),
                        rs.getString("brand"),
                        rs.getString("type"),
                        rs.getString("category"),
                        rs.getString("size"),
                        rs.getString("color"),
                        rs.getDouble("price"),
                        rs.getInt("stock"),
                        rs.getString("image")
                );
                products.add(p);
            }
              System.out.println("Products found: " + products.size());
        } catch (Exception e) {
            e.printStackTrace();
        }

        return products;
    }


    //  READ (BY ID)
    public Product getProductById(int id) throws SQLException {
        String sql = "SELECT * FROM products WHERE id = ?";
        Connection con = DBConnection.getConnection();
        PreparedStatement ps = con.prepareStatement(sql);
        ps.setInt(1, id);

        ResultSet rs = ps.executeQuery();
        Product p = null;

        if (rs.next()) {
            p = new Product(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getString("description"),
                    rs.getString("brand"),
                    rs.getString("type"),
                    rs.getString("category"),

                    rs.getString("size"),
                    rs.getString("color"),
                    rs.getDouble("price"),
                    rs.getInt("stock"),
                    rs.getString("image"));
        }
        con.close();
        return p;
    }

    // UPDATE
   public void updateProduct(Product p) throws SQLException {
    String sql = """
        UPDATE products
        SET name = ?, description = ?, brand = ?, type = ?, category = ?,
            size = ?, color = ?, price = ?, stock = ?, image = ?
        WHERE id = ?
    """;

    var conn = DBConnection.getConnection();
    var ps = conn.prepareStatement(sql);

    ps.setString(1, p.getName());
    ps.setString(2, p.getDescription());
    ps.setString(3, p.getBrand());
    ps.setString(4, p.getType());
    ps.setString(5, p.getCategory());
    ps.setString(6, p.getSize());
    ps.setString(7, p.getColor());
    ps.setDouble(8, p.getPrice());
    ps.setInt(9, p.getStock());
    ps.setString(10, p.getImage());
    ps.setInt(11, p.getId()); // 🔥 VERY IMPORTANT

    int rows = ps.executeUpdate();
    System.out.println("Updated rows: " + rows);
}


    

    //  DELETE
    public void deleteProduct(int id) throws SQLException {
        String sql = "DELETE FROM products WHERE id = ?";
        Connection con = DBConnection.getConnection();
        PreparedStatement ps = con.prepareStatement(sql);
        ps.setInt(1, id);

        ps.executeUpdate();
        con.close();
    }

    // SEARCH (Bonus)
    public List<Product> search(String name, String size) throws SQLException {
        String sql = """
                    SELECT * FROM products
                    WHERE name LIKE ? AND size LIKE ?
                """;

        Connection con = DBConnection.getConnection();
        PreparedStatement ps = con.prepareStatement(sql);

        ps.setString(1, "%" + name + "%");
        ps.setString(2, "%" + size + "%");

        ResultSet rs = ps.executeQuery();
        List<Product> list = new ArrayList<>();

        while (rs.next()) {
            list.add(new Product(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getString("description"),
                    rs.getString("brand"),
                    rs.getString("type"),
                    rs.getString("category"),
                    rs.getString("size"),
                    rs.getString("color"),
                    rs.getDouble("price"),
                    rs.getInt("stock"),
                    rs.getString("image")
            ));
        }
        con.close();
        return list;
    }

}
