package com.lam.httpbrowser.ui;

import com.lam.httpbrowser.analyzer.HtmlAnalyzer;
import com.lam.httpbrowser.model.HtmlStatistics;
import com.lam.httpbrowser.model.HttpResult;
import com.lam.httpbrowser.service.HttpService;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.util.concurrent.ExecutionException;

/**
 * Main GUI frame for the SimpleHTTPBrowser.
 * Connects UI actions to background networking via SwingWorker.
 *
 * Layout:
 * ┌──────────────────────────────────────────────┐
 * │  [URL Field]  [Method ▼]  [SEND]  [Loading]  │  <- Top bar
 * ├──────────────────────────────────────────────┤
 * │  POST Body (visible only when POST selected) │  <- POST panel
 * ├──────────────────────────────────────────────┤
 * │  ┌─Response Body─┬─Response Headers─┐        │
 * │  │               │                  │        │  <- Tabbed response
 * │  │               │                  │        │
 * │  └───────────────┴──────────────────┘        │
 * ├──────────────────────────────────────────────┤
 * │  Status: 200 OK  │  HTML Statistics panel    │  <- Bottom bar
 * └──────────────────────────────────────────────┘
 */
public class BrowserFrame extends JFrame {

    // ─── Services ───
    private final HttpService httpService;
    private final HtmlAnalyzer htmlAnalyzer;

    // ─── Input Components ───
    private JTextField urlField;
    private JComboBox<String> methodCombo;
    private JButton sendButton;

    // ─── POST Body ───
    private JTextArea postBodyArea;
    private JPanel postBodyPanel;

    // ─── Response Display ───
    private JTextArea responseBodyArea;
    private JTextArea responseHeadersArea;
    private JTabbedPane responseTabs;

    // ─── Status & Statistics ───
    private JLabel statusLabel;
    private JLabel statCharsLabel;
    private JLabel statBytesLabel;
    private JLabel statPLabel;
    private JLabel statDivLabel;
    private JLabel statSpanLabel;
    private JLabel statImgLabel;

    // ─── Loading Indicator ───
    private JProgressBar loadingBar;

    public BrowserFrame() {
        this.httpService = new HttpService();
        this.htmlAnalyzer = new HtmlAnalyzer();

        setTitle("Simple HTTP Browser - Java Socket HTTP/1.1");
        setSize(980, 720);
        setMinimumSize(new Dimension(750, 520));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        initComponents();
        layoutComponents();
        initEventHandlers();
    }

    // ══════════════════════════════════════════════
    //  COMPONENT INITIALIZATION
    // ══════════════════════════════════════════════

    private void initComponents() {
        // --- URL input ---
        urlField = new JTextField("http://example.com");
        urlField.setFont(new Font("SansSerif", Font.PLAIN, 14));

        // --- HTTP method selector ---
        methodCombo = new JComboBox<>(new String[]{"GET", "POST", "HEAD"});
        methodCombo.setFont(new Font("SansSerif", Font.BOLD, 13));

        // Show/hide POST body panel when method changes
        methodCombo.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                boolean isPost = "POST".equals(methodCombo.getSelectedItem());
                postBodyPanel.setVisible(isPost);
                revalidate();
            }
        });

        // --- Send button ---
        sendButton = new JButton("SEND");
        sendButton.setFont(new Font("SansSerif", Font.BOLD, 13));
        sendButton.setBackground(new Color(59, 130, 246));
        sendButton.setForeground(Color.WHITE);
        sendButton.setFocusPainted(false);
        sendButton.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // --- Loading indicator ---
        loadingBar = new JProgressBar();
        loadingBar.setIndeterminate(true);
        loadingBar.setPreferredSize(new Dimension(100, 20));
        loadingBar.setVisible(false);

        // --- POST Body ---
        postBodyArea = new JTextArea(3, 40);
        postBodyArea.setFont(new Font("Monospaced", Font.PLAIN, 13));
        postBodyArea.setLineWrap(true);
        postBodyArea.setWrapStyleWord(true);
        postBodyArea.setText("name=Lam&age=20");

        // --- Response Body viewer ---
        responseBodyArea = new JTextArea();
        responseBodyArea.setFont(new Font("Monospaced", Font.PLAIN, 13));
        responseBodyArea.setEditable(false);
        responseBodyArea.setLineWrap(true);
        responseBodyArea.setWrapStyleWord(true);

        // --- Response Headers viewer ---
        responseHeadersArea = new JTextArea();
        responseHeadersArea.setFont(new Font("Monospaced", Font.PLAIN, 13));
        responseHeadersArea.setEditable(false);
        responseHeadersArea.setLineWrap(true);
        responseHeadersArea.setWrapStyleWord(true);

        // --- Status label ---
        statusLabel = new JLabel("Ready");
        statusLabel.setFont(new Font("SansSerif", Font.BOLD, 13));

        // --- HTML Statistics labels ---
        statCharsLabel = createStatLabel("Characters: -");
        statBytesLabel = createStatLabel("Bytes (UTF-8): -");
        statPLabel     = createStatLabel("<p>: -");
        statDivLabel   = createStatLabel("<div>: -");
        statSpanLabel  = createStatLabel("<span>: -");
        statImgLabel   = createStatLabel("<img>: -");
    }

    private JLabel createStatLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("SansSerif", Font.PLAIN, 12));
        return label;
    }

    // ══════════════════════════════════════════════
    //  LAYOUT
    // ══════════════════════════════════════════════

    private void layoutComponents() {
        setLayout(new BorderLayout(5, 5));

        // ── Top Panel: URL + Method + Send + Loading ──
        JPanel topPanel = createTopPanel();

        // ── POST Body Panel (hidden by default) ──
        postBodyPanel = createPostBodyPanel();
        postBodyPanel.setVisible(false);

        // ── North container (top + post body) ──
        JPanel northPanel = new JPanel(new BorderLayout());
        northPanel.add(topPanel, BorderLayout.NORTH);
        northPanel.add(postBodyPanel, BorderLayout.CENTER);

        // ── Center: Response Tabs ──
        responseTabs = new JTabbedPane();
        responseTabs.addTab("Response Body", new JScrollPane(responseBodyArea));
        responseTabs.addTab("Response Headers", new JScrollPane(responseHeadersArea));
        responseTabs.setFont(new Font("SansSerif", Font.PLAIN, 13));

        // ── Bottom: Status + HTML Statistics ──
        JPanel bottomPanel = createBottomPanel();

        // ── Assemble ──
        add(northPanel, BorderLayout.NORTH);
        add(responseTabs, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        // Padding around the frame
        ((JComponent) getContentPane()).setBorder(
                BorderFactory.createEmptyBorder(8, 8, 8, 8));
    }

    private JPanel createTopPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 0));
        panel.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));

        panel.add(urlField, BorderLayout.CENTER);

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        rightPanel.add(methodCombo);
        rightPanel.add(sendButton);
        rightPanel.add(loadingBar);
        panel.add(rightPanel, BorderLayout.EAST);

        return panel;
    }

    private JPanel createPostBodyPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(),
                "POST Body (application/x-www-form-urlencoded)",
                TitledBorder.LEFT, TitledBorder.TOP,
                new Font("SansSerif", Font.PLAIN, 12)));

        JScrollPane scrollPane = new JScrollPane(postBodyArea);
        scrollPane.setPreferredSize(new Dimension(0, 80));
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createBottomPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 0));
        panel.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));

        // Left: HTTP Status
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
        statusPanel.setBorder(BorderFactory.createTitledBorder("HTTP Status"));
        statusPanel.add(statusLabel);
        statusPanel.setPreferredSize(new Dimension(220, 55));

        // Right: HTML Statistics
        JPanel statsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 2));
        statsPanel.setBorder(BorderFactory.createTitledBorder("HTML Statistics"));
        statsPanel.add(statCharsLabel);
        statsPanel.add(statBytesLabel);
        statsPanel.add(statPLabel);
        statsPanel.add(statDivLabel);
        statsPanel.add(statSpanLabel);
        statsPanel.add(statImgLabel);

        panel.add(statusPanel, BorderLayout.WEST);
        panel.add(statsPanel, BorderLayout.CENTER);

        return panel;
    }

    // ══════════════════════════════════════════════
    //  EVENT HANDLING & BACKGROUND EXECUTION
    // ══════════════════════════════════════════════

    private void initEventHandlers() {
        ActionListener sendListener = e -> executeHttpRequest();
        sendButton.addActionListener(sendListener);
        urlField.addActionListener(sendListener);
    }

    /**
     * Executes the HTTP request asynchronously on a background thread using SwingWorker.
     * Keeps the GUI responsive and updates components on the Event Dispatch Thread (EDT).
     */
    public void executeHttpRequest() {
        String url = getUrl();
        if (url.isEmpty()) {
            showError("Input Error", "Please enter a URL.");
            return;
        }

        String method = getSelectedMethod();
        String postBody = getPostBody();

        // Update UI state for request start
        setSendEnabled(false);
        setLoading(true);
        clearResponse();
        setStatusText("Connecting to " + url + "...");

        SwingWorker<HttpResult, Void> worker = new SwingWorker<>() {
            @Override
            protected HttpResult doInBackground() throws Exception {
                return httpService.executeRequest(url, method, postBody);
            }

            @Override
            protected void done() {
                setSendEnabled(true);
                setLoading(false);

                try {
                    HttpResult result = get();

                    // Display HTTP Status
                    setStatus(result.getStatusCode(), result.getStatusMessage());

                    // Display Response Headers
                    setResponseHeaders(result.getRawHeadersText());

                    // Display Response Body
                    setResponseBody(result.getBody());

                    // Tab switching and HTML analysis logic
                    if ("HEAD".equalsIgnoreCase(method)) {
                        showResponseHeadersTab();
                        clearHtmlStatistics();
                    } else if (result.isHtml()) {
                        showResponseBodyTab();
                        HtmlStatistics stats = htmlAnalyzer.analyze(result.getBody());
                        setHtmlStatistics(
                                stats.getCharacterCount(),
                                stats.getUtf8ByteCount(),
                                stats.getPCount(),
                                stats.getDivCount(),
                                stats.getSpanCount(),
                                stats.getImgCount()
                        );
                    } else {
                        showResponseBodyTab();
                        clearHtmlStatistics();
                    }

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    setStatusText("Request interrupted.");
                } catch (ExecutionException e) {
                    Throwable cause = e.getCause() != null ? e.getCause() : e;
                    String errorMessage = cause.getMessage() != null ? cause.getMessage() : cause.toString();
                    setStatusText("Failed: " + errorMessage);
                    showError("Request Failed", errorMessage);
                }
            }
        };

        worker.execute();
    }

    // ══════════════════════════════════════════════
    //  PUBLIC GETTERS AND UI HELPERS
    // ══════════════════════════════════════════════

    public String getUrl() {
        return urlField.getText().trim();
    }

    public void setUrl(String url) {
        urlField.setText(url);
    }

    public String getSelectedMethod() {
        return (String) methodCombo.getSelectedItem();
    }

    public void setSelectedMethod(String method) {
        methodCombo.setSelectedItem(method);
    }

    public String getPostBody() {
        return postBodyArea.getText();
    }

    public void setPostBody(String text) {
        postBodyArea.setText(text);
    }

    public void addSendAction(ActionListener listener) {
        sendButton.addActionListener(listener);
        urlField.addActionListener(listener);
    }

    public void setSendEnabled(boolean enabled) {
        sendButton.setEnabled(enabled);
    }

    public void setLoading(boolean loading) {
        loadingBar.setVisible(loading);
    }

    public void setStatus(int code, String message) {
        statusLabel.setText(code + " " + message);
        if (code >= 200 && code < 300) {
            statusLabel.setForeground(new Color(22, 163, 74));   // Green
        } else if (code >= 300 && code < 400) {
            statusLabel.setForeground(new Color(202, 138, 4));   // Yellow
        } else {
            statusLabel.setForeground(new Color(220, 38, 38));   // Red
        }
    }

    public void setStatusText(String text) {
        statusLabel.setText(text);
        statusLabel.setForeground(Color.DARK_GRAY);
    }

    public void setResponseBody(String body) {
        responseBodyArea.setText(body);
        responseBodyArea.setCaretPosition(0);
    }

    public void setResponseHeaders(String headers) {
        responseHeadersArea.setText(headers);
        responseHeadersArea.setCaretPosition(0);
    }

    public void setHtmlStatistics(int chars, int bytes, int pCount,
                                   int divCount, int spanCount, int imgCount) {
        statCharsLabel.setText("Characters: " + chars);
        statBytesLabel.setText("Bytes (UTF-8): " + bytes);
        statPLabel.setText("<p>: " + pCount);
        statDivLabel.setText("<div>: " + divCount);
        statSpanLabel.setText("<span>: " + spanCount);
        statImgLabel.setText("<img>: " + imgCount);
    }

    public void clearHtmlStatistics() {
        statCharsLabel.setText("Characters: -");
        statBytesLabel.setText("Bytes (UTF-8): -");
        statPLabel.setText("<p>: -");
        statDivLabel.setText("<div>: -");
        statSpanLabel.setText("<span>: -");
        statImgLabel.setText("<img>: -");
    }

    public void clearResponse() {
        responseBodyArea.setText("");
        responseHeadersArea.setText("");
        clearHtmlStatistics();
    }

    public void showError(String title, String message) {
        JOptionPane.showMessageDialog(this, message, title, JOptionPane.ERROR_MESSAGE);
    }

    public void showResponseBodyTab() {
        responseTabs.setSelectedIndex(0);
    }

    public void showResponseHeadersTab() {
        responseTabs.setSelectedIndex(1);
    }
}
