package com.exam.client;

import com.exam.client.admin.AdminGUI;
import com.exam.client.common.ModernTheme;
import com.exam.client.common.NetworkClient;
import com.exam.client.common.ResponseListener;
import com.exam.client.student.StudentGUI;
import com.exam.client.teacher.TeacherGUI;
import com.exam.common.protocol.MessageType;
import com.exam.common.protocol.Request;
import com.exam.common.protocol.Response;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class LoginGUI extends JFrame {

    private final NetworkClient client;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton loginBtn;
    private JButton registerLink;
    private JLabel statusLabel;

    private Response loginResponse;
    private CountDownLatch loginLatch;

    public LoginGUI(NetworkClient client) {
        this.client = client;
        this.client.setListener(new ClientResponseListener());
        ModernTheme.install();
        initUI();
        ModernTheme.applyToFrame(this, getContentPane());
    }

    private void initUI() {
        setTitle("在线考试系统");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(520, 600);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel root = new JPanel(new GridBagLayout());
        root.setBackground(ModernTheme.bg());
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0; gc.fill = GridBagConstraints.HORIZONTAL;
        gc.insets = new Insets(0, 0, 0, 0);

        // ---- 主体卡片 ----
        JPanel card = new JPanel();
        card.setBackground(ModernTheme.surface());
        card.setBorder(new ModernTheme.RoundedBorder(ModernTheme.border(), 16, 1));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setPreferredSize(new Dimension(440, 380));

        // Logo 区
        JPanel logoPanel = new JPanel(new BorderLayout());
        logoPanel.setBackground(ModernTheme.surface());
        logoPanel.setBorder(new javax.swing.border.EmptyBorder(30, 30, 0, 30));
        logoPanel.setMaximumSize(new Dimension(440, 90));

        JLabel icon = new JLabel("📝"); // 📝
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 36));
        icon.setHorizontalAlignment(SwingConstants.CENTER);
        logoPanel.add(icon, BorderLayout.CENTER);

        JLabel title = new JLabel("在线考试平台", SwingConstants.CENTER);
        title.setFont(new Font("Microsoft YaHei", Font.BOLD, 24));
        title.setForeground(ModernTheme.text());
        logoPanel.add(title, BorderLayout.SOUTH);

        card.add(logoPanel);
        card.add(Box.createVerticalStrut(12));

        // 副标题
        JLabel subtitle = new JLabel("专业的在线考试解决方案", SwingConstants.CENTER);
        subtitle.setFont(ModernTheme.SMALL_FONT);
        subtitle.setForeground(ModernTheme.subtext());
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(subtitle);
        card.add(Box.createVerticalStrut(28));

        // 用户名输入
        JPanel fieldPanel = new JPanel();
        fieldPanel.setBackground(ModernTheme.surface());
        fieldPanel.setLayout(new BoxLayout(fieldPanel, BoxLayout.Y_AXIS));
        fieldPanel.setBorder(new javax.swing.border.EmptyBorder(0, 30, 0, 30));
        fieldPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        fieldPanel.setMaximumSize(new Dimension(440, 160));

        JLabel userLabel = new JLabel("用户名");
        userLabel.setFont(new Font("Microsoft YaHei", Font.BOLD, 12));
        userLabel.setForeground(ModernTheme.subtext());
        userLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        fieldPanel.add(userLabel);
        fieldPanel.add(Box.createVerticalStrut(4));

        usernameField = ModernTheme.textField(0);
        usernameField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        usernameField.setAlignmentX(Component.LEFT_ALIGNMENT);
        // Placeholder effect
        usernameField.setForeground(ModernTheme.text());
        fieldPanel.add(usernameField);
        fieldPanel.add(Box.createVerticalStrut(14));

        JLabel passLabel = new JLabel("密码");
        passLabel.setFont(new Font("Microsoft YaHei", Font.BOLD, 12));
        passLabel.setForeground(ModernTheme.subtext());
        passLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        fieldPanel.add(passLabel);
        fieldPanel.add(Box.createVerticalStrut(4));

        passwordField = ModernTheme.passwordField(0);
        passwordField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        passwordField.setAlignmentX(Component.LEFT_ALIGNMENT);
        fieldPanel.add(passwordField);

        card.add(fieldPanel);
        card.add(Box.createVerticalStrut(22));

        // 登录按钮
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        btnPanel.setBackground(ModernTheme.surface());
        btnPanel.setBorder(new javax.swing.border.EmptyBorder(0, 30, 0, 30));
        btnPanel.setMaximumSize(new Dimension(440, 50));
        btnPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        loginBtn = ModernTheme.primaryButton("登  录");
        loginBtn.setPreferredSize(new Dimension(380, 46));
        loginBtn.addActionListener(e -> doLogin());
        btnPanel.add(loginBtn);

        card.add(btnPanel);
        card.add(Box.createVerticalStrut(8));

        // 注册链接 + 状态
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        bottomPanel.setBackground(ModernTheme.surface());
        bottomPanel.setBorder(new javax.swing.border.EmptyBorder(0, 30, 24, 30));
        bottomPanel.setMaximumSize(new Dimension(440, 50));
        bottomPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        registerLink = new JButton("没有账号？立即注册");
        registerLink.setFont(new Font("Microsoft YaHei", Font.PLAIN, 13));
        registerLink.setForeground(ModernTheme.ACCENT);
        registerLink.setBackground(ModernTheme.surface());
        registerLink.setBorder(BorderFactory.createEmptyBorder());
        registerLink.setFocusPainted(false);
        registerLink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        registerLink.setContentAreaFilled(false);
        registerLink.addActionListener(e -> doRegister());
        bottomPanel.add(registerLink);

        card.add(bottomPanel);

        // 状态标签(卡片外部)
        statusLabel = new JLabel(" ", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Microsoft YaHei", Font.PLAIN, 13));
        statusLabel.setForeground(ModernTheme.ERROR);

        // 组装
        gc.gridy = 0; gc.weighty = 0.7;
        root.add(card, gc);
        gc.gridy = 1; gc.weighty = 0.3; gc.insets = new Insets(8, 0, 20, 0);
        root.add(statusLabel, gc);

        add(root);

        // Enter 键触发登录
        getRootPane().setDefaultButton(loginBtn);
    }

    private void setLoading(boolean loading) {
        loginBtn.setEnabled(!loading);
        loginBtn.setText(loading ? "连接中..." : "登  录");
        statusLabel.setText(" ");
    }

    private void doLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword()).trim();
        if (username.isEmpty() || password.isEmpty()) {
            statusLabel.setText("请输入用户名和密码");
            return;
        }

        setLoading(true);
        new Thread(() -> {
            try {
                Map<String, String> data = new HashMap<>();
                data.put("username", username);
                data.put("password", password);

                loginLatch = new CountDownLatch(1);
                client.send(new Request(MessageType.LOGIN, (java.io.Serializable) data));

                if (loginLatch.await(10, TimeUnit.SECONDS)) {
                    if (loginResponse != null && "OK".equals(loginResponse.getStatus())) {
                        @SuppressWarnings("unchecked")
                        Map<String, String> result = (Map<String, String>) loginResponse.getData();
                        String role = result.get("role");
                        SwingUtilities.invokeLater(() -> openMainGUI(role, username));
                    } else {
                        String msg = loginResponse != null ? loginResponse.getMessage() : "用户名或密码错误";
                        SwingUtilities.invokeLater(() -> {
                            statusLabel.setText(msg);
                            setLoading(false);
                        });
                    }
                } else {
                    SwingUtilities.invokeLater(() -> {
                        statusLabel.setText("连接超时，请检查服务器是否启动");
                        setLoading(false);
                    });
                }
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    statusLabel.setText("网络错误: " + ex.getMessage());
                    setLoading(false);
                });
            }
        }, "Login-Thread").start();
    }

    private void doRegister() {
        new RegisterGUI(client, new ClientResponseListener()).setVisible(true);
    }

    private void openMainGUI(String role, String username) {
        this.setVisible(false);
        this.dispose();
        switch (role) {
            case "ADMIN":
                new AdminGUI(client, username).setVisible(true);
                break;
            case "TEACHER":
                new TeacherGUI(client, username).setVisible(true);
                break;
            case "STUDENT":
                new StudentGUI(client, username).setVisible(true);
                break;
            default:
                JOptionPane.showMessageDialog(null, "未知角色: " + role);
        }
    }

    private class ClientResponseListener implements ResponseListener {
        @Override
        public void onResponse(Response response) {
            if (response.getType() == MessageType.LOGIN || response.getType() == MessageType.REGISTER) {
                loginResponse = response;
                if (loginLatch != null) {
                    loginLatch.countDown();
                }
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            NetworkClient client = new NetworkClient();
            try {
                client.connect("localhost", 8888);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null,
                        "无法连接到服务器: " + e.getMessage(),
                        "连接失败", JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
            new LoginGUI(client).setVisible(true);
        });
    }
}
