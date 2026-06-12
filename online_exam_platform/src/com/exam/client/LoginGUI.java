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
        // 设置窗口背景色（天蓝色）
        getContentPane().setBackground(new Color(135, 206, 235));
        
        setTitle("🎓 在线考试系统 - 欢迎登录");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(520, 600);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel root = new JPanel(new GridBagLayout());
        root.setBackground(new Color(135, 206, 235));
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0; gc.fill = GridBagConstraints.HORIZONTAL;
        gc.insets = new Insets(0, 0, 0, 0);

        // ---- 主体卡片（白色背景）----
        JPanel card = new JPanel();
        card.setBackground(Color.WHITE);  // 改为白色
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
            BorderFactory.createEmptyBorder(0, 0, 0, 0)
        ));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setPreferredSize(new Dimension(440, 380));

        // Logo 区（白色背景）
        JPanel logoPanel = new JPanel(new BorderLayout());
        logoPanel.setBackground(Color.WHITE);  // 改为白色
        logoPanel.setBorder(new javax.swing.border.EmptyBorder(30, 30, 0, 30));
        logoPanel.setMaximumSize(new Dimension(440, 90));

        JLabel icon = new JLabel("📝");
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 42));
        icon.setHorizontalAlignment(SwingConstants.CENTER);
        logoPanel.add(icon, BorderLayout.CENTER);

        JLabel title = new JLabel("在线考试平台", SwingConstants.CENTER);
        title.setFont(new Font("微软雅黑", Font.BOLD, 26));
        title.setForeground(new Color(25, 25, 112));
        logoPanel.add(title, BorderLayout.SOUTH);

        card.add(logoPanel);
        card.add(Box.createVerticalStrut(12));

        // 副标题
        JLabel subtitle = new JLabel("专业的在线考试解决方案", SwingConstants.CENTER);
        subtitle.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        subtitle.setForeground(new Color(100, 100, 100));
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(subtitle);
        card.add(Box.createVerticalStrut(28));

        // 用户名输入区（白色背景）
        JPanel fieldPanel = new JPanel();
        fieldPanel.setBackground(Color.WHITE);
        fieldPanel.setLayout(new BoxLayout(fieldPanel, BoxLayout.Y_AXIS));
        fieldPanel.setBorder(new javax.swing.border.EmptyBorder(0, 30, 0, 30));
        fieldPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        fieldPanel.setMaximumSize(new Dimension(440, 160));

        JLabel userLabel = new JLabel("用户名");
        userLabel.setFont(new Font("微软雅黑", Font.BOLD, 13));
        userLabel.setForeground(new Color(80, 80, 80));
        userLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        fieldPanel.add(userLabel);
        fieldPanel.add(Box.createVerticalStrut(4));

        usernameField = new JTextField();
        usernameField.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        usernameField.setPreferredSize(new Dimension(380, 40));
        usernameField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        usernameField.setAlignmentX(Component.LEFT_ALIGNMENT);
        fieldPanel.add(usernameField);
        fieldPanel.add(Box.createVerticalStrut(14));

        JLabel passLabel = new JLabel("密码");
        passLabel.setFont(new Font("微软雅黑", Font.BOLD, 13));
        passLabel.setForeground(new Color(80, 80, 80));
        passLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        fieldPanel.add(passLabel);
        fieldPanel.add(Box.createVerticalStrut(4));

        passwordField = new JPasswordField();
        passwordField.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        passwordField.setPreferredSize(new Dimension(380, 40));
        passwordField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        passwordField.setAlignmentX(Component.LEFT_ALIGNMENT);
        fieldPanel.add(passwordField);

        card.add(fieldPanel);
        card.add(Box.createVerticalStrut(22));

        // 登录按钮
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        btnPanel.setBackground(Color.WHITE);
        btnPanel.setBorder(new javax.swing.border.EmptyBorder(0, 30, 0, 30));
        btnPanel.setMaximumSize(new Dimension(440, 50));
        btnPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        loginBtn = new JButton("登  录");
        loginBtn.setBackground(new Color(25, 100, 180));
        loginBtn.setForeground(Color.WHITE);
        loginBtn.setFont(new Font("微软雅黑", Font.BOLD, 16));
        loginBtn.setFocusPainted(false);
        loginBtn.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        loginBtn.setPreferredSize(new Dimension(380, 46));
        loginBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        loginBtn.addActionListener(e -> doLogin());
        btnPanel.add(loginBtn);

        card.add(btnPanel);
        card.add(Box.createVerticalStrut(8));

        // 注册链接 + 状态（白色背景）
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        bottomPanel.setBackground(Color.WHITE);
        bottomPanel.setBorder(new javax.swing.border.EmptyBorder(0, 30, 24, 30));
        bottomPanel.setMaximumSize(new Dimension(440, 50));
        bottomPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        registerLink = new JButton("没有账号？立即注册");
        registerLink.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        registerLink.setForeground(new Color(70, 130, 200));
        registerLink.setBackground(Color.WHITE);
        registerLink.setBorder(BorderFactory.createEmptyBorder());
        registerLink.setFocusPainted(false);
        registerLink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        registerLink.setContentAreaFilled(false);
        registerLink.addActionListener(e -> doRegister());
        bottomPanel.add(registerLink);

        card.add(bottomPanel);

        // 状态标签
        statusLabel = new JLabel(" ", SwingConstants.CENTER);
        statusLabel.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        statusLabel.setForeground(new Color(220, 50, 50));

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