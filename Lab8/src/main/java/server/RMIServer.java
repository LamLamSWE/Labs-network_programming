package server;

import common.LibraryService;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

/**
 * Class khởi động RMI Server:
 * 1. Khởi động RMI Registry trên cổng 1099.
 * 2. Khởi tạo đối tượng Remote Object (LibraryServiceImpl).
 * 3. Đăng ký đối tượng vào RMI Registry với tên "LibraryService".
 */
public class RMIServer {
    public static final int PORT = 1099;
    public static final String SERVICE_NAME = "LibraryService";

    public static void main(String[] args) {
        try {
            System.out.println("==================================================");
            System.out.println("        MINI LIBRARY RMI SERVER STARTING...       ");
            System.out.println("==================================================");

            // 1. Khởi động hoặc lấy RMI Registry tại cổng 1099
            Registry registry;
            try {
                registry = LocateRegistry.createRegistry(PORT);
                System.out.println("[RMI] Created new RMI Registry on port " + PORT);
            } catch (Exception e) {
                // Trường hợp RMI Registry đã được khởi tạo trước đó
                registry = LocateRegistry.getRegistry(PORT);
                System.out.println("[RMI] Using existing RMI Registry on port " + PORT);
            }

            // 2. Khởi tạo Remote Object
            LibraryService libraryService = new LibraryServiceImpl();

            // 3. Đăng ký (bind/rebind) Remote Object vào Registry
            registry.rebind(SERVICE_NAME, libraryService);

            System.out.println("[RMI] Remote Service '" + SERVICE_NAME + "' registered successfully!");
            System.out.println("[RMI] Server URL: rmi://localhost:" + PORT + "/" + SERVICE_NAME);
            System.out.println("[RMI] Server is ready to receive requests from clients...");
            System.out.println("==================================================");
        } catch (Exception e) {
            System.err.println("[SERVER ERROR] Could not start RMI Server: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

