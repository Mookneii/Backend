package model;

public class Product {
    private int id;
    private String name;
    private String description;
    private String brand;
    private String type;
    private String category;
    private String size;
    private String color;
    private double price;
    private int stock;
    private String image; 

    public Product(int id, String name, String description,
                   String brand, String type, String category,
                   String size, String color, double price, int stock, String image) {

        this.id = id;
        this.name = name;
        this.description = description;
        this.brand = brand;
        this.type = type;
        this.category = category;
        this.size = size;
        this.color = color;
        this.price = price;
        this.stock = stock;
        this.image = image;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getBrand() { return brand; }
    public String getType() { return type; }
    public String getCategory() { return category; }
    public String getSize() { return size; }
    public String getColor() { return color; }
    public double getPrice() { return price; }
    public int getStock() { return stock; }
    public String getImage() { return image; }
}
