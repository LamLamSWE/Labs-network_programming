package client;

import javax.swing.*;

/**
 * Điểm khởi chạy ứng dụng Client (Client Entry Point).
 * Khởi tạo giao diện đồ họa Swing và hiển thị màn hình đăng nhập (LoginFrame).
 */
public class RMIClient {
    public static void main(String[] args) {
        // Thiết lập Look and Feel theo hệ điều hành để giao diện đẹp hơn
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
            }

            LoginFrame loginFrame = new LoginFrame();
            loginFrame.setVisible(true);
        });
    }
}

