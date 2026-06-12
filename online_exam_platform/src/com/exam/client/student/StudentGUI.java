package com.exam.client.student;

import com.exam.client.common.NetworkClient;
import com.exam.client.common.ResponseListener;
import com.exam.common.model.*;
import com.exam.common.protocol.MessageType;
import com.exam.common.protocol.Request;
import com.exam.common.protocol.Response;
import java.awt.*;
import java.io.Serializable;
import java.util.*;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class StudentGUI extends JFrame {

    private final NetworkClient client;
    private final String username;

    private JTabbedPane tabbedPane;
    private JLabel statusLabel;

    // 考试列表
    private DefaultTableModel examModel;
    private JTable examTable;

    // 我的成绩
    private DefaultTableModel scoreModel;
    private JTable scoreTable;

    // 考试状态
    private ExamDialog currentExamDialog;
    private volatile Response lastResponse;
    private CountDownLatch latch;

    public StudentGUI(NetworkClient client, String username) {
        this.client = client;
        this.username = username;
        this.client.setListener(new StudentResponseListener());
        initUI();
        loadExams();
        loadScores();
    }

    private void initUI() {
        // 设置窗口背景色（天蓝色）
        getContentPane().setBackground(new Color(135, 206, 235));
        
        setTitle("在线考试系统 · 学生  " + username);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1020, 720);
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(new Color(135, 206, 235));

        // Header（顶部栏）
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(100, 180, 220));
        header.setBorder(BorderFactory.createEmptyBorder(14, 20, 14, 20));

        JPanel headerLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        headerLeft.setBackground(new Color(100, 180, 220));
        JLabel icon = new JLabel("🎓");
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 22));
        headerLeft.add(icon);
        JLabel title = new JLabel("学生中心");
        title.setFont(new Font("微软雅黑", Font.BOLD, 18));
        title.setForeground(Color.WHITE);
        headerLeft.add(title);
        header.add(headerLeft, BorderLayout.WEST);

        JPanel headerRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        headerRight.setBackground(new Color(100, 180, 220));
        statusLabel = new JLabel("就绪");
        statusLabel.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        statusLabel.setForeground(Color.WHITE);
        headerRight.add(statusLabel);
        
        JLabel userTag = new JLabel(username);
        userTag.setFont(new Font("微软雅黑", Font.BOLD, 12));
        userTag.setForeground(Color.WHITE);
        userTag.setBorder(BorderFactory.createEmptyBorder(2, 10, 2, 10));
        headerRight.add(userTag);
        
        headerRight.add(logoutButton());
        header.add(headerRight, BorderLayout.EAST);
        root.add(header, BorderLayout.NORTH);

        // Tabs（标签页）
        tabbedPane = new JTabbedPane(JTabbedPane.TOP);
        tabbedPane.setBackground(new Color(135, 206, 235));
        tabbedPane.setForeground(new Color(50, 50, 50));
        tabbedPane.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        tabbedPane.addTab("  考试列表  ", buildExamTab());
        tabbedPane.addTab("  我的成绩  ", buildScoreTab());
        root.add(tabbedPane, BorderLayout.CENTER);
        add(root);
    }

    private JPanel buildExamTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBackground(new Color(135, 206, 235));
        panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        examModel = new DefaultTableModel(
                new String[]{"考试ID", "标题", "状态", "开始时间", "时长(秒)", "题数", "总分", "已交卷"}, 0);
        examTable = createStyledTable(examModel);
        panel.add(createTableScroll(examTable), BorderLayout.CENTER);

        JPanel btnBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        btnBar.setBackground(new Color(135, 206, 235));
        JButton joinBtn = createPrimaryButton("→ 加入考试");
        JButton refreshBtn = createSecondaryButton("↻ 刷新");
        joinBtn.addActionListener(e -> joinSelectedExam());
        refreshBtn.addActionListener(e -> { loadExams(); loadScores(); });
        btnBar.add(joinBtn); btnBar.add(refreshBtn);
        panel.add(btnBar, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel buildScoreTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBackground(new Color(135, 206, 235));
        panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        scoreModel = new DefaultTableModel(
                new String[]{"考试标题", "得分", "总分", "总正确率",
                        "基础题正确率", "中等题正确率", "提高题正确率", "提交时间"}, 0);
        scoreTable = createStyledTable(scoreModel);
        panel.add(createTableScroll(scoreTable), BorderLayout.CENTER);

        JPanel btnBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        btnBar.setBackground(new Color(135, 206, 235));
        JButton refreshBtn = createSecondaryButton("↻ 刷新");
        refreshBtn.addActionListener(e -> loadScores());
        btnBar.add(refreshBtn);
        panel.add(btnBar, BorderLayout.SOUTH);

        return panel;
    }

    // ==================== 样式工具方法 ====================
    private JTable createStyledTable(DefaultTableModel model) {
        JTable table = new JTable(model);
        table.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        table.setRowHeight(30);
        table.setForeground(new Color(50, 50, 50));
        table.setBackground(Color.WHITE);
        table.setSelectionBackground(new Color(100, 180, 220));
        table.setSelectionForeground(Color.WHITE);
        table.getTableHeader().setFont(new Font("微软雅黑", Font.BOLD, 13));
        table.getTableHeader().setBackground(new Color(100, 180, 220));
        table.getTableHeader().setForeground(Color.WHITE);
        return table;
    }

    private JScrollPane createTableScroll(JTable table) {
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBackground(Color.WHITE);
        scroll.getViewport().setBackground(Color.WHITE);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
        return scroll;
    }

    private JButton createPrimaryButton(String text) {
        JButton btn = new JButton(text);
        btn.setBackground(new Color(70, 130, 200));
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("微软雅黑", Font.BOLD, 13));
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private JButton createSecondaryButton(String text) {
        JButton btn = new JButton(text);
        btn.setBackground(new Color(240, 240, 240));
        btn.setForeground(new Color(70, 130, 200));
        btn.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    // ==================== 公开方法 ====================
    public void sendRequest(Request request) throws Exception { client.send(request); }
    public void setSubmitResponse(Response response) {
        if (currentExamDialog != null) currentExamDialog.setSubmitResponse(response);
    }

    // ==================== 数据加载 ====================
    private Response sendAndWait(Request req) {
        latch = new CountDownLatch(1); lastResponse = null;
        try { client.send(req); latch.await(10, TimeUnit.SECONDS); } catch (Exception ignored) {}
        return lastResponse;
    }

    private void loadExams() {
        Map<String, String> d = new HashMap<>(); d.put("username", username); d.put("role", "STUDENT");
        Response resp = sendAndWait(new Request(MessageType.GET_EXAMS, (Serializable) d));
        if (resp != null && "OK".equals(resp.getStatus())) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> exams = (List<Map<String, Object>>) resp.getData();
            SwingUtilities.invokeLater(() -> {
                examModel.setRowCount(0);
                for (Map<String, Object> e : exams) {
                    String statusText;
                    switch ((String) e.get("status")) {
                        case "NOT_STARTED": statusText = "未开始"; break;
                        case "IN_PROGRESS": statusText = "进行中"; break;
                        case "FINISHED":    statusText = "已结束"; break;
                        default:            statusText = (String) e.get("status");
                    }
                    long sst = e.get("scheduledStartTime") != null
                            ? ((Number) e.get("scheduledStartTime")).longValue() : 0;
                    String startStr = sst > 0
                            ? new java.text.SimpleDateFormat("MM-dd HH:mm").format(new java.util.Date(sst))
                            : "-";
                    examModel.addRow(new Object[]{
                            e.get("id"), e.get("title"), statusText,
                            startStr,
                            e.get("duration"),
                            e.get("questionCount"), e.get("totalScore"),
                            Boolean.TRUE.equals(e.get("submitted")) ? "是" : "否"});
                }
            });
        }
    }

    private void loadScores() {
        Map<String, String> d = new HashMap<>(); d.put("username", username); d.put("role", "STUDENT");
        Response resp = sendAndWait(new Request(MessageType.GET_SCORES, (Serializable) d));
        if (resp != null && "OK".equals(resp.getStatus())) {
            @SuppressWarnings("unchecked")
            List<ExamResult> scores = (List<ExamResult>) resp.getData();
            SwingUtilities.invokeLater(() -> {
                scoreModel.setRowCount(0);
                for (ExamResult r : scores) {
                    double rate = r.getTotalScore() > 0
                            ? 100.0 * r.getScore() / r.getTotalScore() : 0;
                    scoreModel.addRow(new Object[]{
                            r.getExamTitle(), r.getScore(), r.getTotalScore(),
                            String.format("%.1f%% (%d/%d)", rate, r.getScore(), r.getTotalScore()),
                            diffRate(r.getBasicCorrect(), r.getBasicTotal()),
                            diffRate(r.getMediumCorrect(), r.getMediumTotal()),
                            diffRate(r.getAdvancedCorrect(), r.getAdvancedTotal()),
                            new java.util.Date(r.getSubmitTime())});
                }
            });
        }
    }

    // ==================== 加入考试 ====================
    private void joinSelectedExam() {
        int row = examTable.getSelectedRow();
        if (row < 0) { setStatus("请先选择一个考试"); return; }
        String examId   = (String) examModel.getValueAt(row, 0);
        String statusText = (String) examModel.getValueAt(row, 2);
        String subFlag  = (String) examModel.getValueAt(row, 7);

        if ("是".equals(subFlag)) { setStatus("您已交卷，不能重复参加"); return; }
        if ("已结束".equals(statusText)) { setStatus("考试已结束"); return; }
        if ("未开始".equals(statusText)) { setStatus("考试尚未开始"); return; }

        try {
            Map<String, String> d = new HashMap<>();
            d.put("examId", examId); d.put("studentId", username);
            client.send(new Request(MessageType.JOIN_EXAM, (Serializable) d));
            setStatus("正在加入考试...");
        } catch (Exception ex) { setStatus("加入失败: " + ex.getMessage()); }
    }

    // ==================== 打开考试 ====================
    private void openExamDialog(Response response) {
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) response.getData();
        String examId = (String) data.get("examId");
        String title  = (String) data.get("title");
        int duration  = (int) data.get("duration");
        int remaining = data.containsKey("remainingSeconds")
                ? ((Number) data.get("remainingSeconds")).intValue() : duration;
        @SuppressWarnings("unchecked")
        List<Question> questions = (List<Question>) data.get("questions");

        currentExamDialog = new ExamDialog(StudentGUI.this, examId, title, username, questions, remaining);
        currentExamDialog.setVisible(true);
        currentExamDialog = null;
        SwingUtilities.invokeLater(() -> { loadExams(); loadScores(); });
    }

    // ==================== 响应处理 ====================
    private JButton logoutButton() {
        JButton btn = new JButton("← 退出");
        btn.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        btn.setForeground(Color.WHITE);
        btn.setBackground(new Color(100, 180, 220));
        btn.setBorder(new javax.swing.border.CompoundBorder(
                new javax.swing.border.LineBorder(Color.WHITE, 1),
                new javax.swing.border.EmptyBorder(4, 12, 4, 12)));
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setOpaque(true);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addActionListener(e -> {
            dispose();
            client.disconnect();
            SwingUtilities.invokeLater(() -> {
                NetworkClient nc = new NetworkClient();
                try { nc.connect("localhost", 8888); }
                catch (Exception ex) { JOptionPane.showMessageDialog(null, "无法连接服务器"); System.exit(1); }
                new com.exam.client.LoginGUI(nc).setVisible(true);
            });
        });
        return btn;
    }

    private class StudentResponseListener implements ResponseListener {
        @Override
        public void onResponse(Response response) {
            switch (response.getType()) {
                case SUBMIT_ANSWER: break;
                case GET_EXAMS: case GET_SCORES:
                    lastResponse = response;
                    if (latch != null) latch.countDown();
                    break;
                case JOIN_EXAM:
                    SwingUtilities.invokeLater(() -> handleJoinExamResponse(response));
                    break;
                case EXAM_STARTED:
                    SwingUtilities.invokeLater(() -> handleExamStarted(response));
                    break;
                case EXAM_FINISHED:
                    SwingUtilities.invokeLater(() -> handleExamFinished(response));
                    break;
                case SUBMIT_EXAM:
                    if (currentExamDialog != null) currentExamDialog.setSubmitResponse(response);
                    break;
                default:
                    lastResponse = response;
                    if (latch != null) latch.countDown();
            }
        }
    }

    private void handleJoinExamResponse(Response response) {
        if (!"OK".equals(response.getStatus())) {
            setStatus(response.getMessage());
            return;
        }
        if ("考试开始".equals(response.getMessage()) && response.getData() != null) {
            openExamDialog(response);
        }
    }

    private void handleExamStarted(Response response) {
        openExamDialog(response);
    }

    private void handleExamFinished(Response response) {
        setStatus("考试结束！请查看成绩");
        loadExams();
        loadScores();
    }

    private String diffRate(int correct, int total) {
        if (total == 0) return "0% (0/0)";
        return String.format("%.1f%% (%d/%d)", 100.0 * correct / total, correct, total);
    }

    private void setStatus(String msg) {
        SwingUtilities.invokeLater(() -> statusLabel.setText(msg));
    }
}