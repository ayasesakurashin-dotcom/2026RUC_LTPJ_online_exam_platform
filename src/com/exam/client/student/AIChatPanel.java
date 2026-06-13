package com.exam.client.student;

import com.exam.client.common.ModernTheme;
import com.exam.common.protocol.MessageType;
import com.exam.common.protocol.Request;
import com.exam.common.protocol.Response;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * AI 对话面板 — 显示在考试复盘界面右侧。
 * 通过 NetworkClient 与服务器 AI 服务通信。
 */
public class AIChatPanel extends JPanel {

    private final StudentGUI parent;
    private final String examId;
    private final String username;

    private JPanel messagePanel;
    private JScrollPane scrollPane;
    private JTextArea inputArea;
    private JButton sendBtn;
    private JLabel loadingLabel;

    private boolean initialized = false;
    private volatile Response lastResponse;
    private CountDownLatch latch;

    // 颜色常量
    private static final Color AI_BUBBLE_BG   = new Color(0xF0, 0xF2, 0xF8);
    private static final Color USER_BUBBLE_BG = new Color(0xE0, 0xEC, 0xFF);
    private static final Color AI_BORDER      = new Color(0xD0, 0xD5, 0xE0);
    private static final Color USER_BORDER    = new Color(0xB0, 0xC8, 0xF0);

    public AIChatPanel(StudentGUI parent, String examId, String username) {
        this.parent = parent;
        this.examId = examId;
        this.username = username;

        initUI();
        initChat();

        // 监听面板尺寸变化，触发已有气泡重新布局
        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                revalidateBubbles();
            }
        });
    }

    private void initUI() {
        setLayout(new BorderLayout(0, 0));
        setBackground(ModernTheme.surface());
        setBorder(BorderFactory.createCompoundBorder(
                new ModernTheme.RoundedBorder(ModernTheme.border(), 8, 1),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)));

        // ---- 标题栏 ----
        JPanel titleBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        titleBar.setBackground(ModernTheme.surface());
        JLabel titleIcon = new JLabel("AI");
        titleIcon.setFont(new Font("Microsoft YaHei", Font.BOLD, 13));
        titleIcon.setForeground(ModernTheme.ACCENT);
        titleBar.add(titleIcon);
        JLabel titleLabel = new JLabel("AI 复盘助手");
        titleLabel.setFont(new Font("Microsoft YaHei", Font.BOLD, 14));
        titleLabel.setForeground(ModernTheme.text());
        titleBar.add(titleLabel);
        titleBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, ModernTheme.border()));
        add(titleBar, BorderLayout.NORTH);

        // ---- 消息区域 ----
        messagePanel = new JPanel();
        messagePanel.setLayout(new BoxLayout(messagePanel, BoxLayout.Y_AXIS));
        messagePanel.setBackground(ModernTheme.surface());

        scrollPane = new JScrollPane(messagePanel);
        scrollPane.setBorder(null);
        scrollPane.setBackground(ModernTheme.surface());
        scrollPane.getViewport().setBackground(ModernTheme.surface());
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        ModernTheme.styleScrollBar(scrollPane);
        add(scrollPane, BorderLayout.CENTER);

        // ---- 加载指示器 ----
        loadingLabel = new JLabel("AI 正在思考...", SwingConstants.CENTER);
        loadingLabel.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
        loadingLabel.setForeground(ModernTheme.ACCENT);
        loadingLabel.setVisible(false);
        add(loadingLabel, BorderLayout.SOUTH);

        // ---- 输入区域 ----
        JPanel inputPanel = new JPanel(new BorderLayout(6, 0));
        inputPanel.setBackground(ModernTheme.surface());
        inputPanel.setBorder(BorderFactory.createEmptyBorder(8, 2, 2, 2));

        inputArea = new JTextArea(2, 20);
        inputArea.setFont(new Font("Microsoft YaHei", Font.PLAIN, 13));
        inputArea.setBackground(ModernTheme.elevated());
        inputArea.setForeground(ModernTheme.text());
        inputArea.setCaretColor(ModernTheme.ACCENT);
        inputArea.setLineWrap(true);
        inputArea.setWrapStyleWord(true);
        inputArea.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ModernTheme.border(), 1),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)));
        inputArea.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER && !e.isShiftDown()) {
                    e.consume();
                    sendMessage();
                }
            }
        });
        JScrollPane inputScroll = new JScrollPane(inputArea);
        inputScroll.setBorder(null);
        inputScroll.setPreferredSize(new Dimension(0, 56));
        inputScroll.getVerticalScrollBar().setUnitIncrement(12);
        ModernTheme.styleScrollBar(inputScroll);
        inputPanel.add(inputScroll, BorderLayout.CENTER);

        sendBtn = ModernTheme.primaryButton("发送");
        sendBtn.setFont(new Font("Microsoft YaHei", Font.BOLD, 12));
        sendBtn.setPreferredSize(new Dimension(60, 40));
        sendBtn.addActionListener(e -> sendMessage());
        inputPanel.add(sendBtn, BorderLayout.EAST);

        add(inputPanel, BorderLayout.SOUTH);
    }

    /**
     * 初始化对话：向服务器发送 INIT 请求，获取欢迎语。
     */
    private void initChat() {
        setLoading(true);
        new Thread(() -> {
            try {
                Map<String, String> data = new HashMap<>();
                data.put("examId", examId);
                data.put("username", username);
                data.put("role", "STUDENT");
                data.put("action", "INIT");
                data.put("message", "");

                latch = new CountDownLatch(1);
                parent.sendRequest(new Request(MessageType.AI_CHAT, (Serializable) data));

                if (latch.await(15, TimeUnit.SECONDS)) {
                    if (lastResponse != null && "OK".equals(lastResponse.getStatus())) {
                        @SuppressWarnings("unchecked")
                        Map<String, String> result = (Map<String, String>) lastResponse.getData();
                        String reply = result.get("reply");
                        SwingUtilities.invokeLater(() -> {
                            addAIMessage(reply);
                            initialized = true;
                            setLoading(false);
                        });
                    } else {
                        String err = lastResponse != null ? lastResponse.getMessage() : "连接失败";
                        SwingUtilities.invokeLater(() -> {
                            addAIMessage("AI 助手初始化失败: " + err + "\n请检查 ai.properties 配置。");
                            setLoading(false);
                        });
                    }
                } else {
                    SwingUtilities.invokeLater(() -> {
                        addAIMessage("AI 助手连接超时，请检查服务器配置。");
                        setLoading(false);
                    });
                }
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    addAIMessage("AI 助手初始化失败: " + ex.getMessage());
                    setLoading(false);
                });
            }
        }, "AI-Init").start();
    }

    /**
     * 发送用户消息。
     */
    private void sendMessage() {
        if (!initialized) return;
        String text = inputArea.getText().trim();
        if (text.isEmpty()) return;

        inputArea.setText("");
        addUserMessage(text);
        setLoading(true);

        new Thread(() -> {
            try {
                Map<String, String> data = new HashMap<>();
                data.put("examId", examId);
                data.put("username", username);
                data.put("role", "STUDENT");
                data.put("action", "CHAT");
                data.put("message", text);

                latch = new CountDownLatch(1);
                parent.sendRequest(new Request(MessageType.AI_CHAT, (Serializable) data));

                if (latch.await(60, TimeUnit.SECONDS)) {
                    if (lastResponse != null && "OK".equals(lastResponse.getStatus())) {
                        @SuppressWarnings("unchecked")
                        Map<String, String> result = (Map<String, String>) lastResponse.getData();
                        String reply = result.get("reply");
                        SwingUtilities.invokeLater(() -> {
                            addAIMessage(reply);
                            setLoading(false);
                        });
                    } else {
                        String err = lastResponse != null ? lastResponse.getMessage() : "未知错误";
                        SwingUtilities.invokeLater(() -> {
                            addAIMessage("[错误] " + err);
                            setLoading(false);
                        });
                    }
                } else {
                    SwingUtilities.invokeLater(() -> {
                        addAIMessage("[超时] AI 服务响应超时，请稍后重试。");
                        setLoading(false);
                    });
                }
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    addAIMessage("[错误] 发送失败: " + ex.getMessage());
                    setLoading(false);
                });
            }
        }, "AI-Chat").start();
    }

    /**
     * 处理来自服务器的 AI_CHAT 响应 (由 StudentResponseListener 调用)
     */
    public void setChatResponse(Response response) {
        this.lastResponse = response;
        if (latch != null) latch.countDown();
    }

    // ==================== UI 辅助方法 ====================

    private void addUserMessage(String text) {
        JPanel bubble = createBubble(text, USER_BUBBLE_BG, USER_BORDER, "你", true);
        messagePanel.add(bubble);
        messagePanel.add(Box.createVerticalStrut(6));
        refreshMessages();
    }

    private void addAIMessage(String text) {
        JPanel bubble = createBubble(text, AI_BUBBLE_BG, AI_BORDER, "AI", false);
        messagePanel.add(bubble);
        messagePanel.add(Box.createVerticalStrut(6));
        refreshMessages();
    }

    /**
     * 创建一条消息气泡。
     * 使用 JTextPane (HTML) 替代 JLabel，正确支持宽度约束下的自动换行。
     *
     * @param text        消息文本 (纯文本，内部转为 HTML)
     * @param bgColor     气泡背景色
     * @param borderColor 气泡边框色
     * @param sender      发送者标签 ("AI" / "你")
     * @param rightAlign  true=右对齐(用户), false=左对齐(AI)
     */
    private JPanel createBubble(String text, Color bgColor, Color borderColor,
                                 String sender, boolean rightAlign) {
        // 行容器 — 控制水平对齐
        JPanel row = new JPanel(new BorderLayout());
        row.setBackground(ModernTheme.surface());

        // 气泡内容面板
        JPanel bubble = new JPanel(new BorderLayout(6, 4));
        bubble.setBackground(bgColor);
        bubble.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(borderColor, 1),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)));

        // 发送者标签 (NORTH)
        JLabel senderLabel = new JLabel(sender);
        senderLabel.setFont(new Font("Microsoft YaHei", Font.BOLD, 11));
        senderLabel.setForeground(rightAlign ? ModernTheme.ACCENT : ModernTheme.subtext());
        bubble.add(senderLabel, BorderLayout.NORTH);

        // 消息内容 — 使用 JTextPane 渲染 HTML，自动换行
        JTextPane contentPane = new JTextPane();
        contentPane.setContentType("text/html");
        contentPane.setText(textToHtml(text));
        contentPane.setEditable(false);
        contentPane.setBackground(bgColor);
        contentPane.setForeground(ModernTheme.text());
        contentPane.setFont(new Font("Microsoft YaHei", Font.PLAIN, 13));
        contentPane.setBorder(null);
        contentPane.setFocusable(false);
        // 关键：不允许 JTextPane 撑开父容器宽度
        contentPane.putClientProperty("JTextPane.w3cWidth", "true");

        bubble.add(contentPane, BorderLayout.CENTER);

        // 计算气泡最大宽度：面板宽度的 85%~90%，最少 200
        int panelW = getWidth();
        int bubbleMaxW;
        if (panelW > 50) {
            bubbleMaxW = Math.min((int) (panelW * 0.88), 550);
        } else {
            bubbleMaxW = 350; // 初始默认值，ComponentListener 会更新
        }
        if (bubbleMaxW < 180) bubbleMaxW = 180;

        Dimension bubblePref = new Dimension(bubbleMaxW, bubble.getPreferredSize().height);
        bubble.setPreferredSize(bubblePref);
        bubble.setMaximumSize(new Dimension(bubbleMaxW, Integer.MAX_VALUE));

        // 对齐控制
        if (rightAlign) {
            // 右侧：左边填充空白
            JPanel leftSpacer = new JPanel();
            leftSpacer.setBackground(ModernTheme.surface());
            leftSpacer.setPreferredSize(new Dimension(20, 0));
            row.add(leftSpacer, BorderLayout.WEST);
            row.add(bubble, BorderLayout.EAST);
        } else {
            row.add(bubble, BorderLayout.WEST);
            // 右侧填充空白
            JPanel rightSpacer = new JPanel();
            rightSpacer.setBackground(ModernTheme.surface());
            rightSpacer.setPreferredSize(new Dimension(20, 0));
            row.add(rightSpacer, BorderLayout.EAST);
        }

        return row;
    }

    private String textToHtml(String text) {
        // 转义 HTML 特殊字符
        String escaped = text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
        // 换行转 <br>
        escaped = escaped.replace("\n", "<br>");
        // 粗体: **text** -> <b>text</b>
        escaped = escaped.replaceAll("\\*\\*(.+?)\\*\\*", "<b>$1</b>");
        return "<html><body style='font-family:Microsoft YaHei; font-size:13pt; "
                + "margin:2px; padding:0;'>" + escaped + "</body></html>";
    }

    private void setLoading(boolean loading) {
        loadingLabel.setVisible(loading);
        sendBtn.setEnabled(!loading);
        inputArea.setEnabled(!loading);
    }

    private void refreshMessages() {
        messagePanel.revalidate();
        messagePanel.repaint();
        // 滚动到底部
        SwingUtilities.invokeLater(() -> {
            JScrollBar bar = scrollPane.getVerticalScrollBar();
            bar.setValue(bar.getMaximum());
        });
    }

    /**
     * 面板尺寸变化时更新所有气泡的宽度约束，让文字重新换行。
     */
    private void revalidateBubbles() {
        int panelW = getWidth();
        if (panelW <= 50) return;
        int bubbleMaxW = Math.min((int) (panelW * 0.88), 550);

        Component[] components = messagePanel.getComponents();
        for (Component c : components) {
            if (c instanceof JPanel && c != null) {
                JPanel row = (JPanel) c;
                // Row 是 BorderLayout：气泡在 EAST(右对齐) 或 WEST(左对齐)
                BorderLayout bl = (BorderLayout) row.getLayout();
                if (bl == null) continue;
                Component bubbleComp = bl.getLayoutComponent(BorderLayout.EAST);
                if (bubbleComp == null) {
                    bubbleComp = bl.getLayoutComponent(BorderLayout.WEST);
                }
                if (bubbleComp instanceof JPanel) {
                    JPanel bubble = (JPanel) bubbleComp;
                    bubble.setPreferredSize(new Dimension(bubbleMaxW,
                            bubble.getPreferredSize().height));
                    bubble.setMaximumSize(new Dimension(bubbleMaxW, Integer.MAX_VALUE));
                    bubble.revalidate();
                }
            }
        }
        messagePanel.revalidate();
        messagePanel.repaint();
    }
}
