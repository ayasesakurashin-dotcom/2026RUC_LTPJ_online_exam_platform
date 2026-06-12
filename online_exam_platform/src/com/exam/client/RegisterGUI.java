package com.exam.client;

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
        // 设置窗口背景色（天蓝色）
        getContentPane().setBackground(new Color(135, 206, 235));
        
        setTitle("注册新账号 — 在线考试系统");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(520, 560);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel root = new JPanel(new GridBagLayout());
        root.setBackground(new Color(135, 206, 235));

        // 主体卡片（白色背景）
        JPanel card = new JPanel();
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setPreferredSize(new Dimension(440, 420));

        // Logo 区域（白色背景）
        JPanel logoPanel = new JPanel(new BorderLayout());
        logoPanel.setBackground(Color.WHITE);
        logoPanel.setBorder(BorderFactory.createEmptyBorder(30, 30, 0, 30));
        logoPanel.setMaximumSize(new Dimension(440, 80));
        
        JLabel icon = new JLabel("✨", SwingConstants.CENTER);
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 36));
        logoPanel.add(icon, BorderLayout.CENTER);
        
        JLabel title = new JLabel("创建新账号", SwingConstants.CENTER);
        title.setFont(new Font("微软雅黑", Font.BOLD, 24));
        title.setForeground(new Color(25, 25, 112));
        logoPanel.add(title, BorderLayout.SOUTH);
        card.add(logoPanel);
        card.add(Box.createVerticalStrut(20));

        // 输入区域（白色背景）
        JPanel fieldPanel = new JPanel();
        fieldPanel.setBackground(Color.WHITE);
        fieldPanel.setLayout(new BoxLayout(fieldPanel, BoxLayout.Y_AXIS));
        fieldPanel.setBorder(BorderFactory.createEmptyBorder(0, 30, 0, 30));
        fieldPanel.setMaximumSize(new Dimension(440, 220));

        fieldPanel.add(fieldLabel("用户名"));
        fieldPanel.add(Box.createVerticalStrut(4));
        usernameField = new JTextField();
        usernameField.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        usernameField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        fieldPanel.add(usernameField);
        fieldPanel.add(Box.createVerticalStrut(12));

        fieldPanel.add(fieldLabel("密码"));
        fieldPanel.add(Box.createVerticalStrut(4));
        passwordField = new JPasswordField();
        passwordField.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        passwordField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        fieldPanel.add(passwordField);
        fieldPanel.add(Box.createVerticalStrut(12));

        fieldPanel.add(fieldLabel("角色"));
        fieldPanel.add(Box.createVerticalStrut(4));
        roleBox = new JComboBox<>(new String[]{"学生", "教师"});
        roleBox.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        roleBox.setBackground(Color.WHITE);
        roleBox.setForeground(new Color(50, 50, 50));
        roleBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        ((JComponent) roleBox.getRenderer()).setFont(new Font("微软雅黑", Font.PLAIN, 14));
        fieldPanel.add(roleBox);

        card.add(fieldPanel);
        card.add(Box.createVerticalStrut(20));

        // 注册按钮
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        btnPanel.setBackground(Color.WHITE);
        btnPanel.setMaximumSize(new Dimension(440, 50));
        
        registerBtn = new JButton("注  册");
        registerBtn.setBackground(new Color(25, 100, 180));
        registerBtn.setForeground(Color.WHITE);
        registerBtn.setFont(new Font("微软雅黑", Font.BOLD, 16));
        registerBtn.setFocusPainted(false);
        registerBtn.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        registerBtn.setPreferredSize(new Dimension(380, 44));
        registerBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        registerBtn.addActionListener(e -> doRegister());
        btnPanel.add(registerBtn);
        card.add(btnPanel);
        card.add(Box.createVerticalStrut(8));

        // 返回登录链接
        JPanel linkPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        linkPanel.setBackground(Color.WHITE);
        linkPanel.setBorder(BorderFactory.createEmptyBorder(0, 30, 20, 30));
        linkPanel.setMaximumSize(new Dimension(440, 40));
        
        JButton backLink = new JButton("← 返回登录");
        backLink.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        backLink.setForeground(new Color(70, 130, 200));
        backLink.setBackground(Color.WHITE);
        backLink.setBorder(BorderFactory.createEmptyBorder());
        backLink.setFocusPainted(false);
        backLink.setContentAreaFilled(false);
        backLink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        backLink.addActionListener(e -> { dispose(); });
        linkPanel.add(backLink);
        card.add(linkPanel);

        // 状态标签
        statusLabel = new JLabel(" ", SwingConstants.CENTER);
        statusLabel.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        statusLabel.setForeground(new Color(220, 50, 50));

        // 组装
        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setBackground(new Color(135, 206, 235));
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
        l.setFont(new Font("微软雅黑", Font.BOLD, 13));
        l.setForeground(new Color(80, 80, 80));
        return l;
    }

    private void doRegister() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword()).trim();
        if (username.isEmpty() || password.isEmpty()) {
            statusLabel.setText("请输入用户名和密码");
            statusLabel.setForeground(new Color(220, 50, 50));
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
                            statusLabel.setForeground(new Color(60, 179, 113));
                            setLoading(false);
                        });
                    } else {
                        String msg = lastResponse != null ? lastResponse.getMessage() : "注册失败";
                        SwingUtilities.invokeLater(() -> {
                            statusLabel.setText(msg);
                            statusLabel.setForeground(new Color(220, 50, 50));
                            setLoading(false);
                        });
                    }
                } else {
                    SwingUtilities.invokeLater(() -> {
                        statusLabel.setText("连接超时");
                        statusLabel.setForeground(new Color(220, 50, 50));
                        setLoading(false);
                    });
                }
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    statusLabel.setText("网络错误");
                    statusLabel.setForeground(new Color(220, 50, 50));
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