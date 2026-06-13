package com.exam.client;

import com.exam.client.common.ModernTheme;
import com.exam.client.common.NetworkClient;
import com.exam.client.common.ResponseListener;
import com.exam.common.protocol.MessageType;
import com.exam.common.protocol.Request;
import com.exam.common.protocol.Response;
import java.awt.*;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.swing.*;

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
        initUI();
    }

    @Override
    public void dispose() {
        client.setListener(previousListener);
        super.dispose();
    }

    private void initUI() {
        setTitle("注册新账号 — 在线考试系统");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(520, 600);
        setLocationRelativeTo(null);
        setResizable(false);

        // 主背景
        JPanel root = new JPanel(new GridBagLayout());
        root.setBackground(ModernTheme.bg());

        // 主体卡片
        JPanel card = ModernTheme.card();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        // Logo 区域
        JPanel logoPanel = new JPanel(new BorderLayout());
        logoPanel.setBackground(ModernTheme.surface());
        logoPanel.setBorder(BorderFactory.createEmptyBorder(10, 30, 0, 30));

        JLabel icon = new JLabel("✨", SwingConstants.CENTER);
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 36));
        logoPanel.add(icon, BorderLayout.CENTER);

        JLabel title = new JLabel("创建新账号", SwingConstants.CENTER);
        title.setFont(ModernTheme.HEADING_FONT);
        title.setForeground(ModernTheme.ACCENT);
        logoPanel.add(title, BorderLayout.SOUTH);
        card.add(logoPanel);
        card.add(Box.createVerticalStrut(20));

        // 输入区域
        JPanel fieldPanel = new JPanel();
        fieldPanel.setBackground(ModernTheme.surface());
        fieldPanel.setLayout(new BoxLayout(fieldPanel, BoxLayout.Y_AXIS));
        fieldPanel.setBorder(BorderFactory.createEmptyBorder(0, 30, 0, 30));

        // 用户名
        JLabel userLabel = ModernTheme.label("用户名");
        userLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        fieldPanel.add(userLabel);
        fieldPanel.add(Box.createVerticalStrut(6));
        usernameField = ModernTheme.textField(20);
        usernameField.setAlignmentX(Component.LEFT_ALIGNMENT);
        fieldPanel.add(usernameField);
        fieldPanel.add(Box.createVerticalStrut(14));

        // 密码
        JLabel passLabel = ModernTheme.label("密码");
        passLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        fieldPanel.add(passLabel);
        fieldPanel.add(Box.createVerticalStrut(6));
        passwordField = ModernTheme.passwordField(20);
        passwordField.setAlignmentX(Component.LEFT_ALIGNMENT);
        fieldPanel.add(passwordField);
        fieldPanel.add(Box.createVerticalStrut(14));

        // 角色
        JLabel roleLabel = ModernTheme.label("角色");
        roleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        fieldPanel.add(roleLabel);
        fieldPanel.add(Box.createVerticalStrut(6));
        roleBox = ModernTheme.comboBox(new String[]{"学生", "教师"});
        roleBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        fieldPanel.add(roleBox);

        card.add(fieldPanel);
        card.add(Box.createVerticalStrut(20));

        // 注册按钮
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        btnPanel.setBackground(ModernTheme.surface());
        btnPanel.setBorder(BorderFactory.createEmptyBorder(0, 30, 0, 30));
        btnPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        registerBtn = ModernTheme.primaryButton("注  册");
        registerBtn.setFont(new Font("Microsoft YaHei", Font.BOLD, 16));
        registerBtn.setPreferredSize(new Dimension(380, 44));
        registerBtn.addActionListener(e -> doRegister());
        btnPanel.add(registerBtn);
        card.add(btnPanel);
        card.add(Box.createVerticalStrut(10));

        // 返回登录链接
        JPanel linkPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        linkPanel.setBackground(ModernTheme.surface());
        linkPanel.setBorder(BorderFactory.createEmptyBorder(0, 30, 16, 30));
        linkPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton backLink = ModernTheme.linkButton("← 返回登录");
        backLink.addActionListener(e -> dispose());
        linkPanel.add(backLink);
        card.add(linkPanel);

        // 状态标签
        statusLabel = new JLabel(" ", SwingConstants.CENTER);
        statusLabel.setFont(ModernTheme.SMALL_FONT);
        statusLabel.setForeground(ModernTheme.ERROR);

        // 组装
        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setBackground(ModernTheme.bg());
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridy = 0;
        gc.weighty = 0.75;
        gc.fill = GridBagConstraints.NONE;
        wrapper.add(card, gc);

        gc.gridy = 1;
        gc.weighty = 0.1;
        gc.insets = new Insets(6, 0, 4, 0);
        wrapper.add(statusLabel, gc);

        gc.gridy = 2;
        gc.weighty = 0.15;
        gc.insets = new Insets(4, 0, 24, 0);
        JPanel spacer = new JPanel();
        spacer.setBackground(ModernTheme.bg());
        wrapper.add(spacer, gc);
        root.add(wrapper);

        add(root);
        getRootPane().setDefaultButton(registerBtn);
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
