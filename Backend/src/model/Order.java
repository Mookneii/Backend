package model;

import java.util.List;

public class Order {
    public int id;
    public String createdAt;
    public String username;

    public String firstName;
    public String lastName;
    public String email;
    public String phone;
    public String address;
    public String shippingMethod;

    public double shippingCost;
    public double tax;
    public double subtotal;
    public double total;

    public String paymentLast4;

    public List<OrderItem> items;
}
