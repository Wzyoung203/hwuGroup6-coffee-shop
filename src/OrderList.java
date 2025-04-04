import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class OrderList {

    private final List<Order> orders = new ArrayList<Order>(); // List to store all orders
    private static final String FILE_PATH = "orders.txt";
    private static OrderList instance;
    private final Map<String, Order> activeOrders = new HashMap<>();


    private OrderList() throws InvalidOrderException {
    	loadOrderListFromFile(FILE_PATH);
    }
    
    public static synchronized OrderList getInstance() throws InvalidOrderException {
    	if (instance==null) {
    		instance = new OrderList();
    	}
    	return instance;
    }

    public List<Order> getOrderList() {
        return orders; // Getter to retrieve thae list of orders
    }

    private static final int MAX_QUEUE_SIZE = 10; 

    // Modified addOrder method
    public synchronized boolean addOrder(Order order) throws InterruptedException {
        if (orders.size() >= MAX_QUEUE_SIZE) {
            CafeLogger.getInstance().log("Queue full (max " + MAX_QUEUE_SIZE + "). Cannot add order: " + order.getOrderId() + "\n");
            return false; // Addition failed
        }
        orders.add(order);
        CafeLogger.getInstance().log("order:" + order.getOrderId() + " is added into wait queue.\n");
        notifyAll();
        return true; // Addition succeeded
    }
    
    // consumer method
    public synchronized Order getNextOrder(String serverName) throws InterruptedException {
        while (orders.isEmpty()) {
            wait();
        }
        Order order = orders.remove(0);
        activeOrders.put(serverName, order); 
        return order;
    }

    public synchronized Map<String, Order> getActiveOrders() {
        return new HashMap<>(activeOrders);
    }

    public synchronized void finishOrder(String serverName) {
        activeOrders.remove(serverName);
    }




    /**
     * Load orders from a file.
     *
     * @param filePath The path to the file containing order data.
     * @throws InvalidOrderException If there is an issue with the order data.
     */
    private void loadOrderListFromFile(String filePath) throws InvalidOrderException {
        File file;
        try {
            file = new File(filePath);
        } catch (NullPointerException e) {
            System.out.println("The file is invalid.");
            return;
        }

        // Maps to temporarily store order information and items
        Map<String, List<Item>> orderItemsMap = new LinkedHashMap<>();
        Map<String, String[]> orderInfoMap = new LinkedHashMap<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue; // Skip empty lines
                }

                // Expected format: orderId,customerId,timeStamp,itemId
                String[] parts = line.split(",");
                if (parts.length != 4) {
                    System.out.println("Error: Invalid line format - " + line);
                    continue;
                }

                String orderId = parts[0];
                String customerId = parts[1];
                String timeStamp = parts[2];
                String itemId = parts[3];

                // Retrieve the item from the menu
                Item item = Menu.getInstance().findItemById(itemId);
                if (item == null) {
                    System.out.println("Warning: Item not found for ID - " + itemId + " in the menu.");
                    continue;
                }

                // Store order information and items
                orderInfoMap.putIfAbsent(orderId, new String[]{customerId, timeStamp});
                orderItemsMap.computeIfAbsent(orderId, k -> new ArrayList<>()).add(item);
            }
        } catch (IOException e) {
            System.out.println("Error reading the file: " + e.getMessage());
        }

        // Clear existing orders and add new ones from the file
        orders.clear();

        addOrderByTime(orderItemsMap,orderInfoMap);
    }
    
    private void addOrderByTime(Map<String, List<Item>> orderItemsMap, Map<String, String[]> orderInfoMap) {
        new Thread(() -> {
            for (String orderId : orderItemsMap.keySet()) {
                List<Item> itemList = orderItemsMap.get(orderId);
                if (itemList.isEmpty()) {
                    System.out.println("Warning: Order " + orderId + " has no items.");
                    continue;  // Skip if the order has no items
                }

                String[] info = orderInfoMap.get(orderId);
                try {
                    Order order = new Order(orderId, info[0], info[1], itemList);

                    synchronized (orders) {
                    	addOrder(order);
                    	System.out.println("order added.");
                    }

                    Thread.sleep(1000); 
                } catch (InvalidOrderException e) {
                    System.out.println(e.getMessage());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt(); // Restore interrupted status
                    System.out.println("Thread was interrupted.");
                }
            }
        }).start();
    }
}
