package com.exam.client;

import com.exam.client.common.ModernTheme;
import com.exam.client.common.NetworkClient;
import com.exam.client.common.ResponseListener;
import com.exam.common.protocol.MessageType;
import com.exam.common.protocol.Request;
import com.exam.common.protocol.Response;

import javax.swing.*;
import java.awt.*;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class RegisterGUI extends JFrame {

    private final NetworkClient client;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JComboBox<String> roleBox;
    private JButton registerBtn;
    private JLabel statusLabel;

    private Response lastResponse;
    private CountDownLatch latch;

    private final ResponseListener previousListener;

    public RegisterGUI(NetworkClient client, ResponseListener previousListener) {
        this.client = client;
        this.previousListener = previousListener;
        this.client.setListener(new RegisterResponseListener());
        ModernTheme.install();
        initUI();
        ModernTheme.applyToFrame(this, getContentPane());
    }

    @Override
    public void dispose() {
        client.setListener(previousListener);
        super.dispose();
    }

    private void initUI() {
        setTitle("注册新账号 — 在线考试系统");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(520, 560);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel root = new JPanel(new GridBagLayout());
        root.setBackground(ModernTheme.bg());

        // Card
        JPanel card = new JPanel();
        card.setBackground(ModernTheme.surface());
        card.setBorder(new ModernTheme.RoundedBorder(ModernTheme.border(), 16, 1));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setPreferredSize(new Dimension(440, 380));

        // Logo
        JPanel logoPanel = new JPanel(new BorderLayout());
        logoPanel.setBackground(ModernTheme.surface());
        logoPanel.setBorder(BorderFactory.createEmptyBorder(30, 30, 0, 30));
        logoPanel.setMaximumSize(new Dimension(440, 80));
        JLabel icon = new JLabel("✨", SwingConstants.CENTER);
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 32));
        logoPanel.add(icon, BorderLayout.CENTER);
        JLabel title = new JLabel("创建新账号", SwingConstants.CENTER);
        title.setFont(ModernTheme.HEADING_FONT);
        title.setForeground(ModernTheme.text());
        logoPanel.add(title, BorderLayout.SOUTH);
        card.add(logoPanel);
        card.add(Box.createVerticalStrut(20));

        // Fields
        JPanel fieldPanel = new JPanel();
        fieldPanel.setBackground(ModernTheme.surface());
        fieldPanel.setLayout(new BoxLayout(fieldPanel, BoxLayout.Y_AXIS));
        fieldPanel.setBorder(BorderFactory.createEmptyBorder(0, 30, 0, 30));
        fieldPanel.setMaximumSize(new Dimension(440, 200));

        fieldPanel.add(fieldLabel("用户名"));
        fieldPanel.add(Box.createVerticalStrut(4));
        usernameField = ModernTheme.textField(0);
        usernameField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        fieldPanel.add(usernameField);
        fieldPanel.add(Box.createVerticalStrut(12));

        fieldPanel.add(fieldLabel("密码"));
        fieldPanel.add(Box.createVerticalStrut(4));
        passwordField = ModernTheme.passwordField(0);
        passwordField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        fieldPanel.add(passwordField);
        fieldPanel.add(Box.createVerticalStrut(12));

        fieldPanel.add(fieldLabel("角色"));
        fieldPanel.add(Box.createVerticalStrut(4));
        roleBox = new JComboBox<>(new String[]{"学生", "教师"});
        roleBox.setFont(ModernTheme.BODY_FONT);
        roleBox.setBackground(ModernTheme.surface());
        roleBox.setForeground(ModernTheme.text());
        roleBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        ((JComponent) roleBox.getRenderer()).setFont(ModernTheme.BODY_FONT);
        fieldPanel.add(roleBox);

        card.add(fieldPanel);
        card.add(Box.createVerticalStrut(20));

        // Register button
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        btnPanel.setBackground(ModernTheme.surface());
        btnPanel.setMaximumSize(new Dimension(440, 50));
        registerBtn = ModernTheme.primaryButton("注  册");
        registerBtn.setPreferredSize(new Dimension(380, 44));
        registerBtn.addActionListener(e -> doRegister());
        btnPanel.add(registerBtn);
        card.add(btnPanel);
        card.add(Box.createVerticalStrut(8));

        // Back link
        JPanel linkPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        linkPanel.setBackground(ModernTheme.surface());
        linkPanel.setBorder(BorderFactory.createEmptyBorder(0, 30, 20, 30));
        linkPanel.setMaximumSize(new Dimension(440, 40));
        JButton backLink = new JButton("← 返回登录");
        backLink.setFont(new Font("Microsoft YaHei", Font.PLAIN, 13));
        backLink.setForeground(ModernTheme.ACCENT);
        backLink.setBackground(ModernTheme.surface());
        backLink.setBorder(BorderFactory.createEmptyBorder());
        backLink.setFocusPainted(false);
        backLink.setContentAreaFilled(false);
        backLink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        backLink.addActionListener(e -> { dispose(); });
        linkPanel.add(backLink);
        card.add(linkPanel);

        // Status
        statusLabel = new JLabel(" ", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Microsoft YaHei", Font.PLAIN, 13));
        statusLabel.setForeground(ModernTheme.subtext());

        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setBackground(ModernTheme.bg());
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridy = 0; gc.weighty = 0.8; gc.fill = GridBagConstraints.NONE;
        wrapper.add(card, gc);
        gc.gridy = 1; gc.weighty = 0.2; gc.insets = new Insets(6, 0, 16, 0);
        wrapper.add(statusLabel, gc);
        root.add(wrapper);

        add(root);
        getRootPane().setDefaultButton(registerBtn);
    }

    private JLabel fieldLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Microsoft YaHei", Font.BOLD, 12));
        l.setForeground(ModernTheme.subtext());
        return l;
    }

    private void doRegister() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword()).trim();
        if (username.isEmpty() || password.isEmpty()) {
            statusLabel.setText("请输入用户名和密码");
            statusLabel.setForeground(ModernTheme.ERROR);
            return;
        }

        String roleStr = (String) roleBox.getSelectedItem();
        String role = "学生".equals(roleStr) ? "STUDENT" : "TEACHER";

        setLoading(true);
        new Thread(() -> {
            try {
                Map<String, String> data = new HashMap<>();
                data.put("username", username);
                data.put("password", password);
                data.put("role", role);

                latch = new CountDownLatch(1);
                client.send(new Request(MessageType.REGISTER, (Serializable) data));

                if (latch.await(10, TimeUnit.SECONDS)) {
                    if (lastResponse != null && "OK".equals(lastResponse.getStatus())) {
                        SwingUtilities.invokeLater(() -> {
                            statusLabel.setText("注册成功！请返回登录。");
                            statusLabel.setForeground(ModernTheme.SUCCESS);
                            setLoading(false);
                        });
                    } else {
                        String msg = lastResponse != null ? lastResponse.getMessage() : "注册失败";
                        SwingUtilities.invokeLater(() -> {
                            statusLabel.setText(msg);
                            statusLabel.setForeground(ModernTheme.ERROR);
                            setLoading(false);
                        });
                    }
                } else {
                    SwingUtilities.invokeLater(() -> {
                        statusLabel.setText("连接超时");
                        statusLabel.setForeground(ModernTheme.ERROR);
                        setLoading(false);
                    });
                }
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    statusLabel.setText("网络错误");
                    statusLabel.setForeground(ModernTheme.ERROR);
                    setLoading(false);
                });
            }
        }, "Register-Thread").start();
    }

    private void setLoading(boolean loading) {
        registerBtn.setEnabled(!loading);
        registerBtn.setText(loading ? "注册中..." : "注  册");
    }

    private class RegisterResponseListener implements ResponseListener {
        @Override
        public void onResponse(Response response) {
            lastResponse = response;
            if (latch != null) latch.countDown();
        }
    }
}
