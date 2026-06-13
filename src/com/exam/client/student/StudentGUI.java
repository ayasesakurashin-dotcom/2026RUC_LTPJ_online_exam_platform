package com.exam.client.student;

import com.exam.client.common.ModernTheme;
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

    // 考试复盘
    private DefaultTableModel reviewExamModel;
    private JTable reviewExamTable;
    private AIChatPanel currentChatPanel;

    public StudentGUI(NetworkClient client, String username) {
        this.client = client;
        this.username = username;
        this.client.setListener(new StudentResponseListener());
        initUI();
        loadExams();
        loadScores();
    }

    private void initUI() {
        setTitle("在线考试系统 · 学生  " + username);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1020, 720);
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(ModernTheme.bg());

        // ---- Header ----
        JPanel header = ModernTheme.headerBar();
        header.add(ModernTheme.headerLeft("🎓", "学生中心"), BorderLayout.WEST);

        statusLabel = ModernTheme.headerStatusLabel();
        JPanel headerRight = ModernTheme.headerRight(
                statusLabel,
                ModernTheme.userTag(username),
                ModernTheme.headerButton("← 退出")
        );
        // 为退出按钮设置事件
        for (Component c : headerRight.getComponents()) {
            if (c instanceof JButton && "← 退出".equals(((JButton) c).getText())) {
                ((JButton) c).addActionListener(e -> logout());
            }
        }
        header.add(headerRight, BorderLayout.EAST);
        root.add(header, BorderLayout.NORTH);

        // ---- Tabs ----
        tabbedPane = ModernTheme.tabbedPane();
        tabbedPane.addTab("  考试列表  ", buildExamTab());
        tabbedPane.addTab("  我的成绩  ", buildScoreTab());
        tabbedPane.addTab("  考试复盘  ", buildReviewTab());
        root.add(tabbedPane, BorderLayout.CENTER);
        add(root);
    }

    private JPanel buildExamTab() {
        JPanel panel = ModernTheme.insetPanel(16, 16, 16, 16);
        panel.setLayout(new BorderLayout(0, 10));

        examModel = new DefaultTableModel(
                new String[]{"考试ID", "标题", "状态", "开始时间", "时长(秒)", "题数", "总分", "已交卷"}, 0);
        examTable = ModernTheme.table(examModel);
        panel.add(ModernTheme.tableScroll(examTable), BorderLayout.CENTER);

        JPanel btnBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        btnBar.setBackground(ModernTheme.bg());
        JButton joinBtn = ModernTheme.primaryButton("→ 加入考试");
        JButton refreshBtn = ModernTheme.secondaryButton("↻ 刷新");
        joinBtn.addActionListener(e -> joinSelectedExam());
        refreshBtn.addActionListener(e -> { loadExams(); loadScores(); });
        btnBar.add(joinBtn);
        btnBar.add(refreshBtn);
        panel.add(btnBar, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel buildScoreTab() {
        JPanel panel = ModernTheme.insetPanel(16, 16, 16, 16);
        panel.setLayout(new BorderLayout(0, 10));

        scoreModel = new DefaultTableModel(
                new String[]{"考试标题", "得分", "总分", "总正确率",
                        "基础题正确率", "中等题正确率", "提高题正确率", "提交时间"}, 0);
        scoreTable = ModernTheme.table(scoreModel);
        panel.add(ModernTheme.tableScroll(scoreTable), BorderLayout.CENTER);

        JPanel btnBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        btnBar.setBackground(ModernTheme.bg());
        JButton refreshBtn = ModernTheme.secondaryButton("↻ 刷新");
        refreshBtn.addActionListener(e -> loadScores());
        btnBar.add(refreshBtn);
        panel.add(btnBar, BorderLayout.SOUTH);

        return panel;
    }

    // ==================== 考试复盘 Tab ====================

    private JPanel buildReviewTab() {
        JPanel panel = ModernTheme.insetPanel(16, 16, 16, 16);
        panel.setLayout(new BorderLayout(0, 10));

        reviewExamModel = new DefaultTableModel(
                new String[]{"考试ID", "标题", "状态", "题数", "总分", "已交卷"}, 0);
        reviewExamTable = ModernTheme.table(reviewExamModel);
        panel.add(ModernTheme.tableScroll(reviewExamTable), BorderLayout.CENTER);

        JPanel btnBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        btnBar.setBackground(ModernTheme.bg());
        JButton refreshBtn = ModernTheme.secondaryButton("↻ 刷新");
        JButton reviewBtn = ModernTheme.primaryButton("→ 考试复盘");
        refreshBtn.addActionListener(e -> loadReviewExams());
        reviewBtn.addActionListener(e -> showStudentReviewDialog());
        btnBar.add(refreshBtn);
        btnBar.add(reviewBtn);
        panel.add(btnBar, BorderLayout.SOUTH);

        return panel;
    }

    private void loadReviewExams() {
        Map<String, String> d = new HashMap<>(); d.put("username", username); d.put("role", "STUDENT");
        Response resp = sendAndWait(new Request(MessageType.GET_EXAMS, (Serializable) d));
        if (resp != null && "OK".equals(resp.getStatus())) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> exams = (List<Map<String, Object>>) resp.getData();
            SwingUtilities.invokeLater(() -> {
                reviewExamModel.setRowCount(0);
                for (Map<String, Object> e : exams) {
                    if ("FINISHED".equals(e.get("status"))
                            && Boolean.TRUE.equals(e.get("submitted"))
                            && Boolean.TRUE.equals(e.get("scoresPublished"))) {
                        reviewExamModel.addRow(new Object[]{
                                e.get("id"), e.get("title"), "已发布成绩",
                                e.get("questionCount"), e.get("totalScore"), "是"
                        });
                    }
                }
            });
        }
    }

    @SuppressWarnings("unchecked")
    private void showStudentReviewDialog() {
        int row = reviewExamTable.getSelectedRow();
        if (row < 0) { setStatus("请先选择一场已结束的考试"); return; }
        String examId = (String) reviewExamModel.getValueAt(row, 0);
        String examTitle = (String) reviewExamModel.getValueAt(row, 1);

        Map<String, String> reqData = new HashMap<>();
        reqData.put("examId", examId);
        reqData.put("username", username);
        reqData.put("role", "STUDENT");
        Response resp = sendAndWait(new Request(MessageType.GET_EXAM_REVIEW, (Serializable) reqData));
        if (resp == null || !"OK".equals(resp.getStatus())) {
            setStatus(resp != null ? resp.getMessage() : "加载复盘数据失败");
            return;
        }

        Map<String, Object> reviewData = (Map<String, Object>) resp.getData();
        List<Map<String, Object>> questions = (List<Map<String, Object>>) reviewData.get("questions");
        Map<String, String> myAnswers = (Map<String, String>) reviewData.get("studentAnswers");
        Map<String, Integer> questionScores = (Map<String, Integer>) reviewData.get("questionScores");

        // 构建对话框
        JDialog dialog = new JDialog(this, "考试复盘 - " + examTitle, true);
        dialog.setSize(1200, 720);
        dialog.setLocationRelativeTo(this);
        dialog.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosed(java.awt.event.WindowEvent e) {
                currentChatPanel = null;
            }
        });
        dialog.getContentPane().setBackground(ModernTheme.bg());
        dialog.setLayout(new BorderLayout(0, 0));

        // 标题栏
        dialog.add(ModernTheme.dialogHeader("📝 考试复盘：" + examTitle, ModernTheme.STUDENT_ACCENT), BorderLayout.NORTH);

        // 题目列表
        JPanel reviewPanel = new JPanel();
        reviewPanel.setLayout(new BoxLayout(reviewPanel, BoxLayout.Y_AXIS));
        reviewPanel.setBackground(ModernTheme.bg());
        reviewPanel.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));

        for (int i = 0; i < questions.size(); i++) {
            Map<String, Object> qInfo = questions.get(i);
            String qId = (String) qInfo.get("id");
            String type = (String) qInfo.get("type");
            String diff = (String) qInfo.get("difficulty");
            String correctAnswer = (String) qInfo.get("correctAnswer");
            String explanation = (String) qInfo.get("explanation");
            int qScore = ((Number) qInfo.get("score")).intValue();

            String myAnswer = myAnswers.getOrDefault(qId, "");
            if (myAnswer == null) myAnswer = "";
            if ("__SUBMITTED__".equals(myAnswer)) myAnswer = "";

            Integer earnedScore = questionScores.getOrDefault(qId, 0);
            boolean essayUngraded = (earnedScore != null && earnedScore < 0);

            // 格式化正确答案显示
            String correctAnswerDisplay = correctAnswer;
            if ("CHOICE".equals(type) && qInfo.containsKey("correctAnswerText")) {
                correctAnswerDisplay = correctAnswer + " (" + qInfo.get("correctAnswerText") + ")";
            }

            // 格式化我的答案显示
            String myAnswerDisplay = myAnswer;
            if (myAnswer.isEmpty()) myAnswerDisplay = "（未作答）";

            // 构建题目卡片
            JPanel card = new JPanel();
            card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
            card.setBackground(ModernTheme.surface());
            card.setBorder(BorderFactory.createCompoundBorder(
                    new ModernTheme.ShadowBorder(ModernTheme.border(), 8, 1),
                    BorderFactory.createEmptyBorder(12, 16, 12, 16)));
            card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 420));
            card.setAlignmentX(Component.LEFT_ALIGNMENT);

            // 题头
            String typeTag = "CHOICE".equals(type) ? "选择题" : "ESSAY".equals(type) ? "简答题" : "填空题";
            JLabel headerLabel = new JLabel("第 " + (i + 1) + " 题 [" + typeTag + " · " + ModernTheme.diffLabel(diff) + "]  (" + qScore + "分)");
            headerLabel.setFont(ModernTheme.SUBHEADING_FONT);
            headerLabel.setForeground(ModernTheme.diffColor(diff));
            headerLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            card.add(headerLabel);
            card.add(Box.createVerticalStrut(6));

            // 题目内容
            JLabel contentLabel = new JLabel("<html><b>题目：</b>" + qInfo.get("content") + "</html>");
            contentLabel.setFont(ModernTheme.BODY_FONT);
            contentLabel.setForeground(ModernTheme.text());
            contentLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            card.add(contentLabel);
            card.add(Box.createVerticalStrut(4));

            // 正确答案
            JLabel answerLabel = new JLabel("<html><b>✅ 正确答案：</b>" + correctAnswerDisplay + "</html>");
            answerLabel.setFont(ModernTheme.BODY_FONT);
            answerLabel.setForeground(ModernTheme.SUCCESS);
            answerLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            card.add(answerLabel);

            // 我的答案
            JLabel myAnswerLabel = new JLabel("<html><b>📝 我的答案：</b>" + myAnswerDisplay + "</html>");
            myAnswerLabel.setFont(ModernTheme.BODY_FONT);
            myAnswerLabel.setForeground(ModernTheme.text());
            myAnswerLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            card.add(myAnswerLabel);

            // 得分
            JLabel scoreLabel;
            if ("ESSAY".equals(type) && essayUngraded) {
                scoreLabel = new JLabel("📊 得分：待批改 / " + qScore + " 分");
                scoreLabel.setForeground(ModernTheme.WARNING);
            } else if ("ESSAY".equals(type)) {
                scoreLabel = new JLabel("📊 得分：" + earnedScore + " / " + qScore + " 分");
                scoreLabel.setForeground(ModernTheme.ACCENT);
            } else {
                boolean correct = qScore == earnedScore;
                scoreLabel = new JLabel("📊 得分：" + earnedScore + " / " + qScore + " 分  " + (correct ? "✅ 正确" : "❌ 错误"));
                scoreLabel.setForeground(correct ? ModernTheme.SUCCESS : ModernTheme.ERROR);
            }
            scoreLabel.setFont(ModernTheme.BODY_FONT);
            scoreLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            card.add(scoreLabel);
            card.add(Box.createVerticalStrut(4));

            // 题目解析
            if (explanation != null && !explanation.isEmpty()) {
                JLabel explLabel = new JLabel("<html><b>💡 题目解析：</b>" + explanation + "</html>");
                explLabel.setFont(ModernTheme.BODY_FONT);
                explLabel.setForeground(ModernTheme.ACCENT);
                explLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
                card.add(explLabel);
            } else {
                JLabel noExplLabel = new JLabel("💡 题目解析：（无）");
                noExplLabel.setFont(ModernTheme.SMALL_FONT);
                noExplLabel.setForeground(ModernTheme.subtext());
                noExplLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
                card.add(noExplLabel);
            }

            reviewPanel.add(card);
            reviewPanel.add(Box.createVerticalStrut(10));
        }

        JScrollPane scroll = new JScrollPane(reviewPanel);
        scroll.setBorder(null);
        scroll.setBackground(ModernTheme.bg());
        scroll.getViewport().setBackground(ModernTheme.bg());
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        ModernTheme.styleScrollBar(scroll);

        // 创建 AI 对话面板（右侧）
        currentChatPanel = new AIChatPanel(this, examId, username);
        currentChatPanel.setPreferredSize(new Dimension(380, 0));
        currentChatPanel.setMinimumSize(new Dimension(300, 0));

        // 左右分屏：左侧复盘，右侧 AI
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scroll, currentChatPanel);
        splitPane.setResizeWeight(0.58);
        splitPane.setBorder(null);
        splitPane.setBackground(ModernTheme.bg());
        splitPane.setDividerSize(6);
        splitPane.setDividerLocation(500);
        dialog.add(splitPane, BorderLayout.CENTER);

        // 底部关闭按钮
        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        bottomBar.setBackground(ModernTheme.bg());
        bottomBar.setBorder(BorderFactory.createEmptyBorder(0, 0, 16, 0));
        JButton closeBtn = ModernTheme.secondaryButton("关闭");
        closeBtn.addActionListener(e -> dialog.dispose());
        bottomBar.add(closeBtn);
        dialog.add(bottomBar, BorderLayout.SOUTH);

        dialog.setVisible(true);
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

    // ==================== 退出登录 ====================
    private void logout() {
        dispose();
        client.disconnect();
        SwingUtilities.invokeLater(() -> {
            NetworkClient nc = new NetworkClient();
            try { nc.connect("localhost", 8888); }
            catch (Exception ex) { JOptionPane.showMessageDialog(null, "无法连接服务器"); System.exit(1); }
            new com.exam.client.LoginGUI(nc).setVisible(true);
        });
    }

    // ==================== 响应处理 ====================
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
                case AI_CHAT:
                    if (currentChatPanel != null) currentChatPanel.setChatResponse(response);
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
