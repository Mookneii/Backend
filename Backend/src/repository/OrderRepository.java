package repository;

import model.Order;
import model.OrderItem;
import util.DBConnection;

import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class OrderRepository {

    public OrderRepository() {
        try {
            ensureTables();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void ensureTables() throws SQLException {
        String ordersSql = """
            CREATE TABLE IF NOT EXISTS orders (
              id INTEGER PRIMARY KEY AUTOINCREMENT,
              created_at TEXT NOT NULL,
              username TEXT,
              first_name TEXT,
              last_name TEXT,
              email TEXT,
              phone TEXT,
              address TEXT,
              shipping_method TEXT,
              shipping_cost REAL NOT NULL,
              tax REAL NOT NULL,
              subtotal REAL NOT NULL,
              total REAL NOT NULL,
              payment_last4 TEXT
            )
        """;

        String itemsSql = """
            CREATE TABLE IF NOT EXISTS order_items (
              id INTEGER PRIMARY KEY AUTOINCREMENT,
              order_id INTEGER NOT NULL,
              product_id INTEGER,
              name TEXT NOT NULL,
              price REAL NOT NULL,
              qty INTEGER NOT NULL,
              size TEXT,
              color TEXT,
              image TEXT,
              FOREIGN KEY(order_id) REFERENCES orders(id)
            )
        """;

        try (Connection con = DBConnection.getConnection();
             Statement st = con.createStatement()) {
            st.execute(ordersSql);
            st.execute(itemsSql);
        }
    }

    public int createOrder(Order o) throws SQLException {
        String insertOrder = """
            INSERT INTO orders(
              created_at, username, first_name, last_name, email, phone, address,
              shipping_method, shipping_cost, tax, subtotal, total, payment_last4
            ) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)
        """;

        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);

            int orderId;
            try (PreparedStatement ps = con.prepareStatement(insertOrder, Statement.RETURN_GENERATED_KEYS)) {

                ps.setString(1, Instant.now().toString());
                ps.setString(2, o.username);
                ps.setString(3, o.firstName);
                ps.setString(4, o.lastName);
                ps.setString(5, o.email);
                ps.setString(6, o.phone);
                ps.setString(7, o.address);

                ps.setString(8, o.shippingMethod);
                ps.setDouble(9, o.shippingCost);
                ps.setDouble(10, o.tax);
                ps.setDouble(11, o.subtotal);
                ps.setDouble(12, o.total);
                ps.setString(13, o.paymentLast4);

                ps.executeUpdate();

                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (!rs.next()) throw new SQLException("Failed to get order id");
                    orderId = rs.getInt(1);
                }
            }

            String insertItem = """
                INSERT INTO order_items(order_id, product_id, name, price, qty, size, color, image)
                VALUES (?,?,?,?,?,?,?,?)
            """;

            try (PreparedStatement ps2 = con.prepareStatement(insertItem)) {
                for (OrderItem it : o.items) {
                    ps2.setInt(1, orderId);
                    ps2.setInt(2, it.productId);
                    ps2.setString(3, it.name);
                    ps2.setDouble(4, it.price);
                    ps2.setInt(5, it.qty);
                    ps2.setString(6, it.size);
                    ps2.setString(7, it.color);
                    ps2.setString(8, it.image);
                    ps2.addBatch();
                }
                ps2.executeBatch();
            }

            con.commit();
            con.setAutoCommit(true);

            return orderId;
        }
    }

    public Order getOrder(int id) throws SQLException {
        String qOrder = "SELECT * FROM orders WHERE id = ?";
        String qItems = "SELECT * FROM order_items WHERE order_id = ?";

        try (Connection con = DBConnection.getConnection()) {
            Order o = null;

            try (PreparedStatement ps = con.prepareStatement(qOrder)) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) return null;

                    o = new Order();
                    o.id = rs.getInt("id");
                    o.createdAt = rs.getString("created_at");
                    o.username = rs.getString("username");

                    o.firstName = rs.getString("first_name");
                    o.lastName  = rs.getString("last_name");
                    o.email     = rs.getString("email");
                    o.phone     = rs.getString("phone");
                    o.address   = rs.getString("address");

                    o.shippingMethod = rs.getString("shipping_method");
                    o.shippingCost = rs.getDouble("shipping_cost");
                    o.tax = rs.getDouble("tax");
                    o.subtotal = rs.getDouble("subtotal");
                    o.total = rs.getDouble("total");
                    o.paymentLast4 = rs.getString("payment_last4");
                }
            }

            List<OrderItem> items = new ArrayList<>();
            try (PreparedStatement ps = con.prepareStatement(qItems)) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        OrderItem it = new OrderItem();
                        it.id = rs.getInt("id");
                        it.orderId = rs.getInt("order_id");
                        it.productId = rs.getInt("product_id");
                        it.name = rs.getString("name");
                        it.price = rs.getDouble("price");
                        it.qty = rs.getInt("qty");
                        it.size = rs.getString("size");
                        it.color = rs.getString("color");
                        it.image = rs.getString("image");
                        items.add(it);
                    }
                }
            }

            o.items = items;
            return o;
        }
    }
}
