package controller;

import com.sun.net.httpserver.HttpServer;

import model.Order;
import model.OrderItem;
import model.Product;
import model.User;
import repository.OrderRepository;
import repository.ProductRepository;
import service.AuthService;

import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.io.IOException;
import java.util.concurrent.Executors;

public class ProductController {

    // ---------- Helpers ----------
    private static Map<String, String> parseQuery(String query) {
        Map<String, String> map = new HashMap<>();
        if (query == null || query.isBlank()) return map;

        for (String pair : query.split("&")) {
            String[] kv = pair.split("=", 2);
            String key = kv[0];
            String val = kv.length > 1 ? kv[1] : "";
            map.put(key, urlDecode(val));
        }
        return map;
    }

    private static Map<String, String> parseFormBody(String body) {
        Map<String, String> data = new HashMap<>();
        if (body == null || body.isBlank()) return data;

        for (String pair : body.split("&")) {
            String[] kv = pair.split("=", 2);
            String key = kv[0];
            String val = kv.length > 1 ? kv[1] : "";
            data.put(key, urlDecode(val));
        }
        return data;
    }

    private static String urlDecode(String s) {
        return URLDecoder.decode(s, StandardCharsets.UTF_8);
    }

    private static String readBody(com.sun.net.httpserver.HttpExchange exchange) throws IOException {
        return new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
    }

    private static void sendJson(com.sun.net.httpserver.HttpExchange exchange, int status, String json)
            throws IOException {
        byte[] resp = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, resp.length);
        exchange.getResponseBody().write(resp);
        exchange.getResponseBody().close();
    }

    private static void sendStatus(com.sun.net.httpserver.HttpExchange exchange, int status) throws IOException {
        exchange.sendResponseHeaders(status, 0);
        exchange.getResponseBody().close();
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String extractBearerToken(com.sun.net.httpserver.HttpExchange exchange) {
        String auth = exchange.getRequestHeaders().getFirst("Authorization");
        if (auth == null) return null;
        auth = auth.trim();
        if (!auth.startsWith("Bearer ")) return null;
        return auth.substring("Bearer ".length()).trim();
    }

    private static User requireLogin(com.sun.net.httpserver.HttpExchange exchange, AuthService auth)
            throws IOException {
        String token = extractBearerToken(exchange);
        User u = auth.getUserByToken(token);
        if (u == null) {
            sendJson(exchange, 401, "{\"error\":\"Unauthorized\"}");
            return null;
        }
        return u;
    }

    private static boolean requireAdmin(com.sun.net.httpserver.HttpExchange exchange, AuthService auth)
            throws IOException {
        User u = requireLogin(exchange, auth);
        if (u == null) return false;

        if (!"ADMIN".equalsIgnoreCase(u.getRole())) {
            sendJson(exchange, 403, "{\"error\":\"Forbidden (ADMIN only)\"}");
            return false;
        }
        return true;
    }

    // ---------- JSON parsing helpers (simple) ----------
    // Note: This is a simple parser good enough for your cart/shipping JSON.
    private static Map<String, String> simpleJsonToMap(String json) {
        Map<String, String> map = new HashMap<>();
        if (json == null) return map;

        json = json.trim();
        if (json.startsWith("{")) json = json.substring(1);
        if (json.endsWith("}")) json = json.substring(0, json.length() - 1);

        if (json.isBlank()) return map;

        for (String part : json.split(",")) {
            String[] kv = part.split(":", 2);
            if (kv.length < 2) continue;

            String key = kv[0].trim().replace("\"", "");
            String val = kv[1].trim();

            if (val.startsWith("\"")) val = val.substring(1);
            if (val.endsWith("\"")) val = val.substring(0, val.length() - 1);

            map.put(key, val);
        }
        return map;
    }

    private static java.util.List<OrderItem> simpleJsonToItems(String json) {
        java.util.List<OrderItem> items = new java.util.ArrayList<>();
        if (json == null || json.isBlank()) return items;

        json = json.trim();
        if (json.startsWith("[")) json = json.substring(1);
        if (json.endsWith("]")) json = json.substring(0, json.length() - 1);

        if (json.isBlank()) return items;

        // split objects: } , {
        String[] objs = json.split("\\},\\s*\\{");

        for (String obj : objs) {
            obj = obj.trim();
            if (!obj.startsWith("{")) obj = "{" + obj;
            if (!obj.endsWith("}")) obj = obj + "}";

            Map<String, String> m = simpleJsonToMap(obj);

            OrderItem it = new OrderItem();
            it.productId = parseIntSafe(m.get("id"));
            it.name = m.getOrDefault("name", "");
            it.price = parseDoubleSafe(m.get("price"));
            it.qty = parseIntSafe(m.get("qty"));
            it.size = m.getOrDefault("size", "");
            it.color = m.getOrDefault("color", "");
            it.image = m.getOrDefault("image", "");
            items.add(it);
        }

        return items;
    }

    private static int parseIntSafe(String s) {
        try { return Integer.parseInt(s); } catch (Exception e) { return 0; }
    }

    private static double parseDoubleSafe(String s) {
        try { return Double.parseDouble(s); } catch (Exception e) { return 0; }
    }

    public static void main(String[] args) throws IOException {

       int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.setExecutor(Executors.newCachedThreadPool());

        ProductRepository repo = new ProductRepository();
        AuthService auth = new AuthService();        // auto-creates default admin if none
        OrderRepository orderRepo = new OrderRepository(); // creates orders tables

        // ---------- AUTH ROUTES ----------
        server.createContext("/auth", exchange -> {

            // CORS
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");

            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                sendStatus(exchange, 204);
                return;
            }

            try {
                String path = exchange.getRequestURI().getPath();

                // POST /auth/register
                if ("POST".equals(exchange.getRequestMethod()) && path.endsWith("/register")) {
                    String body = readBody(exchange);
                    Map<String, String> data = parseFormBody(body);

                    auth.registerUser(data.get("username"), data.get("password"));
                    sendJson(exchange, 201, "{\"status\":\"registered\"}");
                    return;
                }

                // POST /auth/login
                if ("POST".equals(exchange.getRequestMethod()) && path.endsWith("/login")) {
                    String body = readBody(exchange);
                    Map<String, String> data = parseFormBody(body);

                    var result = auth.login(data.get("username"), data.get("password"));

                    String json = String.format(
                            "{\"token\":\"%s\",\"username\":\"%s\",\"role\":\"%s\"}",
                            escapeJson(result.token()),
                            escapeJson(result.username()),
                            escapeJson(result.role())
                    );
                    sendJson(exchange, 200, json);
                    return;
                }

                // POST /auth/logout
                if ("POST".equals(exchange.getRequestMethod()) && path.endsWith("/logout")) {
                    String token = extractBearerToken(exchange);
                    auth.logout(token);
                    sendJson(exchange, 200, "{\"status\":\"logged_out\"}");
                    return;
                }

                // GET /auth/me
                if ("GET".equals(exchange.getRequestMethod()) && path.endsWith("/me")) {
                    User u = requireLogin(exchange, auth);
                    if (u == null) return;

                    String json = String.format(
                            "{\"id\":%d,\"username\":\"%s\",\"role\":\"%s\"}",
                            u.getId(),
                            escapeJson(u.getUsername()),
                            escapeJson(u.getRole())
                    );
                    sendJson(exchange, 200, json);
                    return;
                }

                sendJson(exchange, 404, "{\"error\":\"Not Found\"}");

            } catch (Exception e) {
                e.printStackTrace();
                sendJson(exchange, 500, "{\"error\":\"Server error\"}");
            }
        });

        // ---------- ORDERS ROUTES ----------
        server.createContext("/orders", exchange -> {

            // CORS
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");

            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                exchange.close();
                return;
            }

            try {
                String path = exchange.getRequestURI().getPath(); // /orders or /orders/create

                // ✅ POST /orders/create  => save order
                if ("POST".equals(exchange.getRequestMethod()) && path.endsWith("/create")) {

                    String body = readBody(exchange);
                    Map<String, String> data = parseFormBody(body);

                    String cartJson = data.get("cart");
                    String shippingJson = data.get("shipping");
                    String last4 = data.getOrDefault("last4", "");

                    double subtotal = Double.parseDouble(data.getOrDefault("subtotal", "0"));
                    double tax = Double.parseDouble(data.getOrDefault("tax", "0"));
                    double shippingCost = Double.parseDouble(data.getOrDefault("shippingCost", "0"));
                    double total = Double.parseDouble(data.getOrDefault("total", "0"));

                    Map<String, String> shipping = simpleJsonToMap(shippingJson);
                    java.util.List<OrderItem> items = simpleJsonToItems(cartJson);

                    Order o = new Order();
                    o.username = null; // optional: can set from logged in user later

                    o.firstName = shipping.getOrDefault("firstName", "");
                    o.lastName = shipping.getOrDefault("lastName", "");
                    o.email = shipping.getOrDefault("email", "");
                    o.phone = shipping.getOrDefault("phone", "");
                    o.address = shipping.getOrDefault("address", "");
                    o.shippingMethod = shipping.getOrDefault("shippingMethod", "");

                    o.shippingCost = shippingCost;
                    o.tax = tax;
                    o.subtotal = subtotal;
                    o.total = total;

                    o.paymentLast4 = last4;
                    o.items = items;

                    int orderId = orderRepo.createOrder(o);

                    sendJson(exchange, 201, "{\"orderId\":" + orderId + "}");
                    return;
                }

                // ✅ GET /orders?id=123 => receipt data
                if ("GET".equals(exchange.getRequestMethod())) {
                    String query = exchange.getRequestURI().getQuery(); // id=123
                    if (query == null || !query.contains("id=")) {
                        sendJson(exchange, 400, "{\"error\":\"Missing id\"}");
                        return;
                    }

                    int id = Integer.parseInt(query.split("id=")[1]);
                    Order order = orderRepo.getOrder(id);

                    if (order == null) {
                        sendJson(exchange, 404, "{\"error\":\"Order not found\"}");
                        return;
                    }

                    StringBuilder itemsJson = new StringBuilder("[");
                    for (int i = 0; i < order.items.size(); i++) {
                        OrderItem it = order.items.get(i);

                        itemsJson.append(String.format(
                                "{\"name\":\"%s\",\"price\":%.2f,\"qty\":%d,\"size\":\"%s\",\"color\":\"%s\",\"image\":\"%s\"}",
                                escapeJson(it.name),
                                it.price,
                                it.qty,
                                escapeJson(it.size),
                                escapeJson(it.color),
                                escapeJson(it.image)
                        ));

                        if (i < order.items.size() - 1) itemsJson.append(",");
                    }
                    itemsJson.append("]");

                    String json = String.format(
                            "{\"id\":%d,\"createdAt\":\"%s\",\"firstName\":\"%s\",\"lastName\":\"%s\",\"email\":\"%s\",\"phone\":\"%s\",\"address\":\"%s\",\"shippingMethod\":\"%s\",\"shippingCost\":%.2f,\"tax\":%.2f,\"subtotal\":%.2f,\"total\":%.2f,\"paymentLast4\":\"%s\",\"items\":%s}",
                            order.id,
                            escapeJson(order.createdAt),
                            escapeJson(order.firstName),
                            escapeJson(order.lastName),
                            escapeJson(order.email),
                            escapeJson(order.phone),
                            escapeJson(order.address),
                            escapeJson(order.shippingMethod),
                            order.shippingCost,
                            order.tax,
                            order.subtotal,
                            order.total,
                            escapeJson(order.paymentLast4),
                            itemsJson.toString()
                    );

                    sendJson(exchange, 200, json);
                    return;
                }

                sendJson(exchange, 405, "{\"error\":\"Method Not Allowed\"}");

            } catch (Exception e) {
                e.printStackTrace();
                sendJson(exchange, 500, "{\"error\":\"Server error\"}");
            }
        });

        // ---------- PRODUCT ROUTES ----------
        server.createContext("/products", exchange -> {

            // CORS
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");

            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                sendStatus(exchange, 204);
                return;
            }

            try {
                // ===== GET (Everyone can read) =====
                if ("GET".equals(exchange.getRequestMethod())) {

                    Map<String, String> q = parseQuery(exchange.getRequestURI().getQuery());
                    if (q.containsKey("id")) {
                        int id = Integer.parseInt(q.get("id"));
                        Product p = repo.getProductById(id);

                        if (p == null) {
                            sendJson(exchange, 404, "{\"error\":\"Product not found\"}");
                            return;
                        }

                        String json = String.format(
                                "{\"id\":%d,\"name\":\"%s\",\"description\":\"%s\",\"brand\":\"%s\",\"type\":\"%s\",\"category\":\"%s\",\"size\":\"%s\",\"color\":\"%s\",\"price\":%.2f,\"stock\":%d,\"image\":\"%s\"}",
                                p.getId(),
                                escapeJson(p.getName()),
                                escapeJson(p.getDescription()),
                                escapeJson(p.getBrand()),
                                escapeJson(p.getType()),
                                escapeJson(p.getCategory()),
                                escapeJson(p.getSize()),
                                escapeJson(p.getColor()),
                                p.getPrice(),
                                p.getStock(),
                                escapeJson(p.getImage())
                        );
                        sendJson(exchange, 200, json);
                        return;
                    }

                    var products = repo.getAllProducts();
                    StringBuilder json = new StringBuilder("[");

                    for (int i = 0; i < products.size(); i++) {
                        var p = products.get(i);
                        json.append(String.format(
                                "{\"id\":%d,\"name\":\"%s\",\"description\":\"%s\",\"brand\":\"%s\",\"type\":\"%s\",\"category\":\"%s\",\"size\":\"%s\",\"color\":\"%s\",\"price\":%.2f,\"stock\":%d,\"image\":\"%s\"}",
                                p.getId(),
                                escapeJson(p.getName()),
                                escapeJson(p.getDescription()),
                                escapeJson(p.getBrand()),
                                escapeJson(p.getType()),
                                escapeJson(p.getCategory()),
                                escapeJson(p.getSize()),
                                escapeJson(p.getColor()),
                                p.getPrice(),
                                p.getStock(),
                                escapeJson(p.getImage())
                        ));
                        if (i < products.size() - 1) json.append(",");
                    }
                    json.append("]");

                    sendJson(exchange, 200, json.toString());
                    return;
                }

                // ===== POST (ADMIN ONLY) =====
                if ("POST".equals(exchange.getRequestMethod())) {
                    if (!requireAdmin(exchange, auth)) return;

                    String body = readBody(exchange);
                    Map<String, String> data = parseFormBody(body);

                    Product p = new Product(
                            0,
                            data.get("name"),
                            data.get("description"),
                            data.get("brand"),
                            data.get("type"),
                            data.get("category"),
                            data.get("size"),
                            data.get("color"),
                            Double.parseDouble(data.get("price")),
                            Integer.parseInt(data.get("stock")),
                            data.get("image")
                    );

                    repo.addProduct(p);
                    sendStatus(exchange, 201);
                    return;
                }

                // ===== PUT (ADMIN ONLY) =====
                if ("PUT".equals(exchange.getRequestMethod())) {
                    if (!requireAdmin(exchange, auth)) return;

                    String body = readBody(exchange);
                    Map<String, String> data = parseFormBody(body);

                    Product p = new Product(
                            Integer.parseInt(data.get("id")),
                            data.get("name"),
                            data.get("description"),
                            data.get("brand"),
                            data.get("type"),
                            data.get("category"),
                            data.get("size"),
                            data.get("color"),
                            Double.parseDouble(data.get("price")),
                            Integer.parseInt(data.get("stock")),
                            data.get("image")
                    );

                    repo.updateProduct(p);
                    sendStatus(exchange, 204);
                    return;
                }

                // ===== DELETE (ADMIN ONLY) =====
                if ("DELETE".equals(exchange.getRequestMethod())) {
                    if (!requireAdmin(exchange, auth)) return;

                    Map<String, String> q = parseQuery(exchange.getRequestURI().getQuery());
                    if (!q.containsKey("id")) {
                        sendJson(exchange, 400, "{\"error\":\"Missing id\"}");
                        return;
                    }

                    int id = Integer.parseInt(q.get("id"));
                    repo.deleteProduct(id);
                    sendStatus(exchange, 204);
                    return;
                }

                sendStatus(exchange, 405);

            } catch (Exception e) {
                e.printStackTrace();
                sendJson(exchange, 500, "{\"error\":\"Server error\"}");
            }
        });

        server.start();
        System.out.println("✅ Server running:");
        System.out.println("   Products: http://localhost:8080/products");
        System.out.println("   Auth:     http://localhost:8080/auth/login (POST)");
        System.out.println("   Orders:   http://localhost:8080/orders/create (POST)");
        System.out.println("   Receipt:  http://localhost:8080/orders?id=1 (GET)");
    }
}
