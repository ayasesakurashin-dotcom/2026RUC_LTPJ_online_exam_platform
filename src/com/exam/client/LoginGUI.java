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
import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.swing.*;

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
        setTitle("🎓 在线考试系统 - 欢迎登录");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(520, 620);
        setLocationRelativeTo(null);
        setResizable(false);

        // 主背景
        JPanel root = new JPanel(new GridBagLayout());
        root.setBackground(ModernTheme.bg());
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.insets = new Insets(0, 0, 0, 0);

        // ---- 主体卡片 ----
        JPanel card = ModernTheme.card();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        // Logo 区
        JPanel logoPanel = new JPanel(new BorderLayout());
        logoPanel.setBackground(ModernTheme.surface());
        logoPanel.setBorder(new javax.swing.border.EmptyBorder(10, 30, 0, 30));

        JLabel icon = ModernTheme.headerIcon("📝");
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 42));
        icon.setHorizontalAlignment(SwingConstants.CENTER);
        logoPanel.add(icon, BorderLayout.CENTER);

        JLabel title = new JLabel("在线考试平台", SwingConstants.CENTER);
        title.setFont(ModernTheme.HEADING_FONT);
        title.setForeground(ModernTheme.ACCENT);
        logoPanel.add(title, BorderLayout.SOUTH);

        card.add(logoPanel);
        card.add(Box.createVerticalStrut(8));

        // 副标题
        JLabel subtitle = ModernTheme.subtitle("专业的在线考试解决方案");
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(subtitle);
        card.add(Box.createVerticalStrut(24));

        // 输入区域
        JPanel fieldPanel = new JPanel();
        fieldPanel.setBackground(ModernTheme.surface());
        fieldPanel.setLayout(new BoxLayout(fieldPanel, BoxLayout.Y_AXIS));
        fieldPanel.setBorder(new javax.swing.border.EmptyBorder(0, 30, 0, 30));
        fieldPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel userLabel = ModernTheme.label("用户名");
        userLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        fieldPanel.add(userLabel);
        fieldPanel.add(Box.createVerticalStrut(6));

        usernameField = ModernTheme.textField(20);
        usernameField.setAlignmentX(Component.LEFT_ALIGNMENT);
        fieldPanel.add(usernameField);
        fieldPanel.add(Box.createVerticalStrut(16));

        JLabel passLabel = ModernTheme.label("密码");
        passLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        fieldPanel.add(passLabel);
        fieldPanel.add(Box.createVerticalStrut(6));

        passwordField = ModernTheme.passwordField(20);
        passwordField.setAlignmentX(Component.LEFT_ALIGNMENT);
        fieldPanel.add(passwordField);

        card.add(fieldPanel);
        card.add(Box.createVerticalStrut(22));

        // 登录按钮
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        btnPanel.setBackground(ModernTheme.surface());
        btnPanel.setBorder(new javax.swing.border.EmptyBorder(0, 30, 0, 30));
        btnPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        loginBtn = ModernTheme.primaryButton("登  录");
        loginBtn.setFont(new Font("Microsoft YaHei", Font.BOLD, 16));
        loginBtn.setPreferredSize(new Dimension(380, 46));
        loginBtn.addActionListener(e -> doLogin());
        btnPanel.add(loginBtn);

        card.add(btnPanel);
        card.add(Box.createVerticalStrut(10));

        // 注册链接 + 主题切换
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        bottomPanel.setBackground(ModernTheme.surface());
        bottomPanel.setBorder(new javax.swing.border.EmptyBorder(0, 30, 16, 30));
        bottomPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        registerLink = ModernTheme.linkButton("没有账号？立即注册 →");
        registerLink.addActionListener(e -> doRegister());
        bottomPanel.add(registerLink);

        card.add(bottomPanel);

        // 状态标签
        statusLabel = new JLabel(" ", SwingConstants.CENTER);
        statusLabel.setFont(ModernTheme.SMALL_FONT);
        statusLabel.setForeground(ModernTheme.ERROR);

        // 组装
        gc.gridy = 0;
        gc.weighty = 0.7;
        gc.insets = new Insets(0, 0, 0, 0);
        root.add(card, gc);

        gc.gridy = 1;
        gc.weighty = 0.15;
        gc.insets = new Insets(10, 0, 4, 0);
        root.add(statusLabel, gc);

        // 底部留白
        gc.gridy = 2;
        gc.weighty = 0.15;
        gc.insets = new Insets(4, 0, 24, 0);
        JPanel spacer = new JPanel();
        spacer.setBackground(ModernTheme.bg());
        root.add(spacer, gc);

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
