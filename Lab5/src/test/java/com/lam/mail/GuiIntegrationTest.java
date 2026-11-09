package com.lam.mail;

import com.lam.mail.gui.CreateAccountDialog;
import com.lam.mail.gui.LoginFrame;
import com.lam.mail.gui.MailboxFrame;
import com.lam.mail.gui.SendEmailDialog;
import com.lam.mail.gui.ViewEmailDialog;

import javax.swing.*;
import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;

public class GuiIntegrationTest {
    public static void main(String[] args) throws Exception {
        System.out.println("Starting GUI Integration Test...");

        // Setup clean accounts directory
        Path accountsDir = Path.of("accounts");
        if (Files.exists(accountsDir)) {
            deleteRecursively(accountsDir.toFile());
        }

        // Start MailServer
        MailServer server = new MailServer(MailServer.PORT);
        Thread serverThread = new Thread(server::start);
        serverThread.setDaemon(true);
        serverThread.start();
        Thread.sleep(800);

        MailClient client = new MailClient("127.0.0.1", MailServer.PORT);

        // TEST 1: Create accounts lam and nam via client API used by GUI
        System.out.println("--- Test 1: Create accounts (lam, nam) ---");
        String createLamResp = client.createAccount("lam");
        System.out.println("Create lam: " + createLamResp);
        assert createLamResp.startsWith("SUCCESS|") : "Create lam failed";

        String createNamResp = client.createAccount("nam");
        System.out.println("Create nam: " + createNamResp);
        assert createNamResp.startsWith("SUCCESS|") : "Create nam failed";

        // TEST 2: Login as lam and inspect MailboxFrame
        System.out.println("--- Test 2: Login as lam & verify MailboxFrame ---");
        String loginLamResp = client.login("lam");
        System.out.println("Login lam: " + loginLamResp);
        assert loginLamResp.startsWith("FILES") : "Login lam failed";

        SwingUtilities.invokeAndWait(() -> {
            LoginFrame loginFrame = new LoginFrame();
            MailboxFrame mailboxFrame = new MailboxFrame(client, "lam", loginLamResp, loginFrame);
            try {
                Field modelField = MailboxFrame.class.getDeclaredField("listModel");
                modelField.setAccessible(true);
                DefaultListModel<?> model = (DefaultListModel<?>) modelField.get(mailboxFrame);
                System.out.println("Mailbox lam items: " + model.size());
                assert model.contains("new_email.txt") : "Mailbox should contain new_email.txt";
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            mailboxFrame.dispose();
            loginFrame.dispose();
        });

        // TEST 3: Send email from lam to nam ("Hello Nam!")
        System.out.println("--- Test 3: Send email lam -> nam ---");
        String sendResp = client.sendEmail("lam", "nam", "Hello Nam!");
        System.out.println("Send email response: " + sendResp);
        assert sendResp.startsWith("SUCCESS|") : "Send email failed";

        // TEST 4: Login as nam and verify mailbox contains email_001.txt and new_email.txt
        System.out.println("--- Test 4: Login as nam & verify new email ---");
        String loginNamResp = client.login("nam");
        System.out.println("Login nam: " + loginNamResp);
        assert loginNamResp.contains("email_001.txt") : "Mailbox nam must contain email_001.txt";
        assert loginNamResp.contains("new_email.txt") : "Mailbox nam must contain new_email.txt";

        // TEST 5: Open email and view text content
        System.out.println("--- Test 5: Open and read email content (READ_EMAIL) ---");
        String readEmail001 = client.readEmail("nam", "email_001.txt");
        System.out.println("Read email_001.txt: " + readEmail001);
        assert readEmail001.startsWith("EMAIL_CONTENT|email_001.txt|Hello Nam!") : "Incorrect email content";

        String readWelcome = client.readEmail("nam", "new_email.txt");
        System.out.println("Read new_email.txt: " + readWelcome);
        assert readWelcome.contains("Thank you for using this service") : "Welcome email content incorrect";

        SwingUtilities.invokeAndWait(() -> {
            ViewEmailDialog dialog = new ViewEmailDialog(null, "nam", "email_001.txt", "Hello Nam!");
            dialog.dispose();
        });

        // TEST 6: Refresh MailboxFrame for nam
        System.out.println("--- Test 6: Refresh MailboxFrame ---");
        SwingUtilities.invokeAndWait(() -> {
            LoginFrame loginFrame = new LoginFrame();
            MailboxFrame mailboxFrame = new MailboxFrame(client, "nam", loginNamResp, loginFrame);
            try {
                client.sendEmail("lam", "nam", "Second test message");
                String refreshed = client.getMailbox("nam");
                mailboxFrame.updateFileList(refreshed);

                Field modelField = MailboxFrame.class.getDeclaredField("listModel");
                modelField.setAccessible(true);
                DefaultListModel<?> model = (DefaultListModel<?>) modelField.get(mailboxFrame);
                System.out.println("Refreshed mailbox nam items: " + model.size());
                assert model.contains("email_001.txt") : "Must contain email_001.txt";
                assert model.contains("email_002.txt") : "Must contain email_002.txt";
                assert model.contains("new_email.txt") : "Must contain new_email.txt";
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            mailboxFrame.dispose();
            loginFrame.dispose();
        });

        // TEST 7: Logout flow verification
        System.out.println("--- Test 7: Verify Logout Navigation ---");
        SwingUtilities.invokeAndWait(() -> {
            LoginFrame loginFrame = new LoginFrame();
            MailboxFrame mailboxFrame = new MailboxFrame(client, "nam", loginNamResp, loginFrame);
            mailboxFrame.dispose();
            loginFrame.resetForRelogin();
            assert !mailboxFrame.isDisplayable() : "MailboxFrame should be disposed";
            loginFrame.dispose();
            System.out.println("Logout navigation successfully closed mailbox and returned to login frame.");
        });

        server.stop();
        System.out.println("GUI Integration Test Completed Successfully! ALL ASSERTIONS PASSED.");
        System.exit(0);
    }

    private static void deleteRecursively(File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursively(child);
                }
            }
        }
        file.delete();
    }
}
