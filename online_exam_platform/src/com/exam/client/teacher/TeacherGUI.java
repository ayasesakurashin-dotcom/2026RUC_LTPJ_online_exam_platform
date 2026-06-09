package com.exam.client.teacher;

import com.exam.client.common.ModernTheme;
import com.exam.client.common.NetworkClient;
import com.exam.client.common.ResponseListener;
import com.exam.common.model.*;
import com.exam.common.protocol.MessageType;
import com.exam.common.protocol.Request;
import com.exam.common.protocol.Response;
import com.exam.common.util.QuestionFactory;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class TeacherGUI extends JFrame {

    private final NetworkClient client;
    private final String username;

    private JTabbedPane tabbedPane;
    private JLabel statusLabel;

    // Tab1: 发布考试
    private JTextField titleField, durationField, dateField, timeField;
    private DefaultListModel<String> questionListModel;
    private JList<String> questionList;
    private List<Question> pendingQuestions = new ArrayList<>();

    // Tab2: 我的考试
    private DefaultTableModel myExamModel;
    private JTable myExamTable;

    // Tab3: 成绩统计
    private DefaultTableModel finishedExamModel, scoreDetailModel;
    private JTable finishedExamTable, scoreDetailTable;
    private JLabel statsLabel;

    private volatile Response lastResponse;
    private CountDownLatch latch;

    public TeacherGUI(NetworkClient client, String username) {
        this.client = client;
        this.username = username;
        ModernTheme.install();
        this.client.setListener(new TeacherResponseListener());
        initUI();
        ModernTheme.applyToFrame(this, getContentPane());
        loadMyExams();
    }

    private void initUI() {
        setTitle("在线考试系统 · 教师  " + username);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1050, 760);
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(ModernTheme.bg());

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(ModernTheme.bgDarker());
        header.setBorder(BorderFactory.createEmptyBorder(14, 20, 14, 20));

        JPanel headerLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        headerLeft.setBackground(ModernTheme.bgDarker());
        headerLeft.add(iconLabel("📚"));
        JLabel title = new JLabel("教师工作台");
        title.setFont(ModernTheme.SUBHEADING_FONT);
        title.setForeground(ModernTheme.text());
        headerLeft.add(title);
        header.add(headerLeft, BorderLayout.WEST);

        JPanel headerRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        headerRight.setBackground(ModernTheme.bgDarker());
        statusLabel = new JLabel("就绪");
        statusLabel.setFont(ModernTheme.SMALL_FONT);
        statusLabel.setForeground(ModernTheme.subtext());
        headerRight.add(statusLabel);
        headerRight.add(userTag());
        headerRight.add(logoutButton());
        headerRight.add(ModernTheme.themeToggle(this));
        header.add(headerRight, BorderLayout.EAST);
        root.add(header, BorderLayout.NORTH);

        // Tabs
        tabbedPane = new JTabbedPane(JTabbedPane.TOP);
        tabbedPane.setBackground(ModernTheme.bg());
        tabbedPane.setForeground(ModernTheme.text());
        tabbedPane.setFont(ModernTheme.TAB_FONT);
        tabbedPane.addTab("  发布考试  ", buildCreateTab());
        tabbedPane.addTab("  我的考试  ", buildMyExamTab());
        tabbedPane.addTab("  成绩统计  ", buildScoreTab());

        root.add(tabbedPane, BorderLayout.CENTER);
        add(root);
    }

    // ==================== Tab1: 发布考试 ====================
    private JPanel buildCreateTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(ModernTheme.bg());
        panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        // 基本信息卡片
        JPanel infoCard = new JPanel(new GridLayout(3, 3, 10, 8));
        infoCard.setBackground(ModernTheme.surface());
        infoCard.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
        infoCard.add(label("考试标题"));
        titleField = ModernTheme.textField(0);
        infoCard.add(titleField);
        infoCard.add(new JLabel()); // spacer

        infoCard.add(label("开始日期 (yyyy-MM-dd)"));
        dateField = ModernTheme.textField(0);
        dateField.setText(new java.text.SimpleDateFormat("yyyy-MM-dd").format(new Date()));
        infoCard.add(dateField);
        JButton nowBtn = ModernTheme.secondaryButton("现在");
        nowBtn.addActionListener(e -> {
            Date now = new Date();
            dateField.setText(new java.text.SimpleDateFormat("yyyy-MM-dd").format(now));
            timeField.setText(new java.text.SimpleDateFormat("HH:mm").format(now));
        });
        infoCard.add(nowBtn);

        infoCard.add(label("开始时间 (HH:mm)"));
        timeField = ModernTheme.textField(0);
        timeField.setText(new java.text.SimpleDateFormat("HH:mm").format(new Date()));
        infoCard.add(timeField);
        infoCard.add(label("考试时长（秒）"));
        durationField = ModernTheme.textField(0);
        durationField.setText("600");
        infoCard.add(durationField);
        panel.add(infoCard, BorderLayout.NORTH);

        // 题目列表
        questionListModel = new DefaultListModel<>();
        questionList = new JList<>(questionListModel);
        questionList.setFont(ModernTheme.BODY_FONT);
        questionList.setBackground(ModernTheme.surface());
        questionList.setForeground(ModernTheme.text());
        questionList.setSelectionBackground(ModernTheme.ACCENT);
        questionList.setSelectionForeground(Color.WHITE);
        JScrollPane sp = new JScrollPane(questionList);
        sp.setBorder(new ModernTheme.RoundedBorder(ModernTheme.border(), 8, 1));
        sp.setBackground(ModernTheme.surface());
        ModernTheme.styleScrollBar(sp);
        panel.add(sp, BorderLayout.CENTER);

        // 按钮
        JPanel btnBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        btnBar.setBackground(ModernTheme.bg());
        JButton addChoice = ModernTheme.primaryButton("＋ 选择题");
        JButton addBlank  = ModernTheme.secondaryButton("＋ 填空题");
        JButton removeQ   = ModernTheme.secondaryButton("✕ 删除");
        JButton publish   = ModernTheme.primaryButton("✔ 发布考试");
        addChoice.addActionListener(e -> showAddChoiceDialog());
        addBlank.addActionListener(e -> showAddBlankDialog());
        removeQ.addActionListener(e -> removeSelectedQuestion());
        publish.addActionListener(e -> publishExam());
        btnBar.add(addChoice); btnBar.add(addBlank); btnBar.add(removeQ);
        btnBar.add(Box.createHorizontalStrut(20)); btnBar.add(publish);
        panel.add(btnBar, BorderLayout.SOUTH);

        return panel;
    }

    // ==================== Tab2: 我的考试 ====================
    private JPanel buildMyExamTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBackground(ModernTheme.bg());
        panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        myExamModel = new DefaultTableModel(
                new String[]{"考试ID", "标题", "状态", "时长(秒)", "已加入", "题目数", "总分"}, 0);
        myExamTable = ModernTheme.table(myExamModel);
        panel.add(ModernTheme.tableScroll(myExamTable), BorderLayout.CENTER);

        JPanel btnBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        btnBar.setBackground(ModernTheme.bg());
        JButton refresh = ModernTheme.secondaryButton("↻ 刷新");
        refresh.addActionListener(e -> loadMyExams());
        btnBar.add(refresh);
        panel.add(btnBar, BorderLayout.SOUTH);

        return panel;
    }

    // ==================== Tab3: 成绩统计 ====================
    private JPanel buildScoreTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 0));
        panel.setBackground(ModernTheme.bg());
        panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        // 上半：已完成的考试
        JPanel topPanel = new JPanel(new BorderLayout(0, 6));
        topPanel.setBackground(ModernTheme.bg());
        topPanel.setBorder(BorderFactory.createTitledBorder(
                new ModernTheme.RoundedBorder(ModernTheme.border(), 8, 1),
                "已完成的考试", javax.swing.border.TitledBorder.LEFT,
                javax.swing.border.TitledBorder.TOP,
                ModernTheme.SUBHEADING_FONT, ModernTheme.text()));

        finishedExamModel = new DefaultTableModel(
                new String[]{"考试ID", "标题", "总分", "参加人数"}, 0);
        finishedExamTable = ModernTheme.table(finishedExamModel);
        topPanel.add(ModernTheme.tableScroll(finishedExamTable), BorderLayout.CENTER);

        JPanel topBtnBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        topBtnBar.setBackground(ModernTheme.bg());
        JButton loadFinished = ModernTheme.secondaryButton("↻ 刷新");
        JButton viewScores  = ModernTheme.primaryButton("→ 查看成绩");
        loadFinished.addActionListener(e -> loadFinishedExams());
        viewScores.addActionListener(e -> viewSelectedExamScores());
        topBtnBar.add(loadFinished); topBtnBar.add(viewScores);
        topPanel.add(topBtnBar, BorderLayout.SOUTH);

        // 下半：成绩明细
        JPanel bottomPanel = new JPanel(new BorderLayout(0, 6));
        bottomPanel.setBackground(ModernTheme.bg());
        bottomPanel.setBorder(BorderFactory.createTitledBorder(
                new ModernTheme.RoundedBorder(ModernTheme.border(), 8, 1),
                "成绩明细", javax.swing.border.TitledBorder.LEFT,
                javax.swing.border.TitledBorder.TOP,
                ModernTheme.SUBHEADING_FONT, ModernTheme.text()));

        scoreDetailModel = new DefaultTableModel(
                new String[]{"学生", "得分", "总分", "总正确率",
                        "基础题正确率", "中等题正确率", "提高题正确率", "提交时间"}, 0);
        scoreDetailTable = ModernTheme.table(scoreDetailModel);
        bottomPanel.add(ModernTheme.tableScroll(scoreDetailTable), BorderLayout.CENTER);

        // 统计条
        statsLabel = new JLabel(" ");
        statsLabel.setFont(new Font("Microsoft YaHei", Font.PLAIN, 13));
        statsLabel.setForeground(ModernTheme.text());
        statsLabel.setBackground(ModernTheme.bgDarker());
        statsLabel.setOpaque(true);
        statsLabel.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));
        bottomPanel.add(statsLabel, BorderLayout.SOUTH);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, topPanel, bottomPanel);
        split.setDividerLocation(260);
        split.setResizeWeight(0.42);
        split.setBorder(null);
        panel.add(split, BorderLayout.CENTER);

        return panel;
    }

    // ==================== 添加题目对话框 ====================
    private void showAddChoiceDialog() {
        JTextField contentField = ModernTheme.textField(24);
        JTextField scoreField = ModernTheme.textField(6);
        scoreField.setText("5");
        JTextArea optionsArea = new JTextArea(4, 24);
        optionsArea.setText("选项1\n选项2\n选项3\n选项4");
        optionsArea.setFont(ModernTheme.BODY_FONT);
        optionsArea.setBackground(ModernTheme.surface());
        optionsArea.setForeground(ModernTheme.text());
        JTextField correctField = ModernTheme.textField(6);
        correctField.setText("0");

        // 难度单选
        JRadioButton basicR = new JRadioButton("基础题", true);
        JRadioButton mediumR = new JRadioButton("中等题");
        JRadioButton advancedR = new JRadioButton("提高题");
        ButtonGroup diffGroup = new ButtonGroup();
        diffGroup.add(basicR); diffGroup.add(mediumR); diffGroup.add(advancedR);
        basicR.setBackground(ModernTheme.surface()); basicR.setForeground(ModernTheme.text());
        mediumR.setBackground(ModernTheme.surface()); mediumR.setForeground(ModernTheme.text());
        advancedR.setBackground(ModernTheme.surface()); advancedR.setForeground(ModernTheme.text());
        JPanel diffPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        diffPanel.setBackground(ModernTheme.surface());
        diffPanel.add(basicR); diffPanel.add(mediumR); diffPanel.add(advancedR);

        JPanel panel = new JPanel(new GridLayout(5, 2, 6, 8));
        panel.setBackground(ModernTheme.surface());
        panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        panel.add(label("题目内容"));   panel.add(contentField);
        panel.add(label("分值"));       panel.add(scoreField);
        panel.add(label("难度（必选）")); panel.add(diffPanel);
        panel.add(label("选项（每行一个）")); panel.add(new JScrollPane(optionsArea));
        panel.add(label("正确索引（0开始）"));  panel.add(correctField);

        int r = JOptionPane.showConfirmDialog(this, panel, "添加选择题",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (r != JOptionPane.OK_OPTION) return;

        try {
            String diff = basicR.isSelected() ? "BASIC" : mediumR.isSelected() ? "MEDIUM" : "ADVANCED";
            Question q = QuestionFactory.createChoice(
                    contentField.getText().trim(),
                    Integer.parseInt(scoreField.getText().trim()),
                    diff,
                    Arrays.asList(optionsArea.getText().split("\n")),
                    Integer.parseInt(correctField.getText().trim()));
            pendingQuestions.add(q);
            questionListModel.addElement(("📋 [" + diffLabel(diff) + "] " + q.getContent() + "  [" + q.getScore() + "分]"));
        } catch (Exception ex) {
            setStatus("输入格式错误");
        }
    }

    private void showAddBlankDialog() {
        JTextField contentField = ModernTheme.textField(24);
        JTextField scoreField = ModernTheme.textField(6);
        scoreField.setText("5");
        JTextField answerField = ModernTheme.textField(14);

        JRadioButton basicR = new JRadioButton("基础题", true);
        JRadioButton mediumR = new JRadioButton("中等题");
        JRadioButton advancedR = new JRadioButton("提高题");
        ButtonGroup diffGroup = new ButtonGroup();
        diffGroup.add(basicR); diffGroup.add(mediumR); diffGroup.add(advancedR);
        basicR.setBackground(ModernTheme.surface()); basicR.setForeground(ModernTheme.text());
        mediumR.setBackground(ModernTheme.surface()); mediumR.setForeground(ModernTheme.text());
        advancedR.setBackground(ModernTheme.surface()); advancedR.setForeground(ModernTheme.text());
        JPanel diffPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        diffPanel.setBackground(ModernTheme.surface());
        diffPanel.add(basicR); diffPanel.add(mediumR); diffPanel.add(advancedR);

        JPanel panel = new JPanel(new GridLayout(4, 2, 6, 8));
        panel.setBackground(ModernTheme.surface());
        panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        panel.add(label("题目内容"));   panel.add(contentField);
        panel.add(label("分值"));       panel.add(scoreField);
        panel.add(label("难度（必选）")); panel.add(diffPanel);
        panel.add(label("正确答案"));   panel.add(answerField);

        int r = JOptionPane.showConfirmDialog(this, panel, "添加填空题",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (r != JOptionPane.OK_OPTION) return;

        try {
            String diff = basicR.isSelected() ? "BASIC" : mediumR.isSelected() ? "MEDIUM" : "ADVANCED";
            Question q = QuestionFactory.createBlank(
                    contentField.getText().trim(),
                    Integer.parseInt(scoreField.getText().trim()),
                    diff,
                    answerField.getText().trim());
            pendingQuestions.add(q);
            questionListModel.addElement("✏ [" + diffLabel(diff) + "] " + q.getContent() + "  [" + q.getScore() + "分]");
        } catch (Exception ex) {
            setStatus("输入格式错误");
        }
    }

    private String diffRate(int correct, int total) {
        if (total == 0) return "0% (0/0)";
        return String.format("%.1f%% (%d/%d)", 100.0 * correct / total, correct, total);
    }

    private static String diffLabel(String diff) {
        if ("MEDIUM".equals(diff)) return "中等题";
        if ("ADVANCED".equals(diff)) return "提高题";
        return "基础题";
    }

    private void removeSelectedQuestion() {
        int idx = questionList.getSelectedIndex();
        if (idx >= 0) { pendingQuestions.remove(idx); questionListModel.remove(idx); }
    }

    private void publishExam() {
        String t = titleField.getText().trim();
        if (t.isEmpty()) { setStatus("请输入考试标题"); return; }
        if (pendingQuestions.isEmpty()) { setStatus("请至少添加一道题目"); return; }
        for (Question q : pendingQuestions) {
            if (q.getDifficulty() == null || q.getDifficulty().isEmpty()) {
                setStatus("每道题目必须指定难度"); return;
            }
        }
        int dur;
        try { dur = Integer.parseInt(durationField.getText().trim()); }
        catch (NumberFormatException e) { setStatus("时长格式错误"); return; }
        long startTime;
        try {
            String dt = dateField.getText().trim() + " " + timeField.getText().trim();
            startTime = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").parse(dt).getTime();
        } catch (Exception e) { setStatus("日期/时间格式错误"); return; }

        Exam exam = new Exam(UUID.randomUUID().toString().substring(0, 8), t, username, dur, startTime);
        exam.setQuestions(new ArrayList<>(pendingQuestions));

        Response resp = sendAndWait(new Request(MessageType.CREATE_EXAM, exam));
        if (resp != null && "OK".equals(resp.getStatus())) {
            setStatus("考试「" + t + "」发布成功");
            titleField.setText(""); durationField.setText("600");
            pendingQuestions.clear(); questionListModel.clear();
            loadMyExams();
        } else { setStatus("发布失败"); }
    }

    // ==================== 数据加载 ====================
    private Response sendAndWait(Request req) {
        latch = new CountDownLatch(1); lastResponse = null;
        try { client.send(req); latch.await(10, TimeUnit.SECONDS); } catch (Exception ignored) {}
        return lastResponse;
    }

    private void loadMyExams() {
        Map<String, String> d = new HashMap<>(); d.put("username", username); d.put("role", "TEACHER");
        Response resp = sendAndWait(new Request(MessageType.GET_EXAMS, (Serializable) d));
        if (resp != null && "OK".equals(resp.getStatus())) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> all = (List<Map<String, Object>>) resp.getData();
            SwingUtilities.invokeLater(() -> {
                myExamModel.setRowCount(0);
                for (Map<String, Object> e : all) {
                    if (username.equals(e.get("teacherId"))) {
                        myExamModel.addRow(new Object[]{
                                e.get("id"), e.get("title"), e.get("status"),
                                e.get("duration"), e.get("joinedCount"),
                                e.get("questionCount"), e.get("totalScore")});
                    }
                }
            });
        }
    }

    private void loadFinishedExams() {
        Map<String, String> d = new HashMap<>(); d.put("username", username); d.put("role", "TEACHER");
        Response resp = sendAndWait(new Request(MessageType.GET_EXAMS, (Serializable) d));
        if (resp != null && "OK".equals(resp.getStatus())) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> all = (List<Map<String, Object>>) resp.getData();
            SwingUtilities.invokeLater(() -> {
                finishedExamModel.setRowCount(0);
                for (Map<String, Object> e : all) {
                    if (username.equals(e.get("teacherId")) && "FINISHED".equals(e.get("status"))) {
                        finishedExamModel.addRow(new Object[]{
                                e.get("id"), e.get("title"), e.get("totalScore"), e.get("joinedCount")});
                    }
                }
                scoreDetailModel.setRowCount(0);
            });
        }
    }

    private void viewSelectedExamScores() {
        int row = finishedExamTable.getSelectedRow();
        if (row < 0) { setStatus("请先选择一场已完成考试"); return; }
        String examId = (String) finishedExamModel.getValueAt(row, 0);

        Map<String, String> d = new HashMap<>(); d.put("username", username); d.put("role", "TEACHER");
        Response resp = sendAndWait(new Request(MessageType.GET_SCORES, (Serializable) d));
        if (resp != null && "OK".equals(resp.getStatus())) {
            @SuppressWarnings("unchecked")
            final List<ExamResult> all = (List<ExamResult>) resp.getData();
            SwingUtilities.invokeLater(() -> {
                scoreDetailModel.setRowCount(0);
                int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE, totalScore = 0, count = 0;
                long totalMax = 0;
                int bCorr = 0, bTot = 0, mCorr = 0, mTot = 0, aCorr = 0, aTot = 0;
                for (ExamResult r : all) {
                    if (examId.equals(r.getExamId())) {
                        double rate = r.getTotalScore() > 0
                                ? 100.0 * r.getScore() / r.getTotalScore() : 0;
                        scoreDetailModel.addRow(new Object[]{
                                r.getStudentId(), r.getScore(), r.getTotalScore(),
                                String.format("%.1f%% (%d/%d)", rate, r.getScore(), r.getTotalScore()),
                                diffRate(r.getBasicCorrect(), r.getBasicTotal()),
                                diffRate(r.getMediumCorrect(), r.getMediumTotal()),
                                diffRate(r.getAdvancedCorrect(), r.getAdvancedTotal()),
                                new java.util.Date(r.getSubmitTime())});
                        int s = r.getScore();
                        if (s < min) min = s;
                        if (s > max) max = s;
                        totalScore += s;
                        totalMax = r.getTotalScore();
                        count++;
                        bCorr += r.getBasicCorrect(); bTot += r.getBasicTotal();
                        mCorr += r.getMediumCorrect(); mTot += r.getMediumTotal();
                        aCorr += r.getAdvancedCorrect(); aTot += r.getAdvancedTotal();
                    }
                }
                if (count > 0) {
                    double avg = (double) totalScore / count;
                    double avgRate = totalMax > 0 ? 100.0 * avg / totalMax : 0;
                    String bRate = bTot > 0 ? String.format("%.1f%%", 100.0 * bCorr / bTot) : "0%";
                    String mRate = mTot > 0 ? String.format("%.1f%%", 100.0 * mCorr / mTot) : "0%";
                    String aRate = aTot > 0 ? String.format("%.1f%%", 100.0 * aCorr / aTot) : "0%";
                    statsLabel.setText(String.format(
                            "📊  参加人数: %d　｜　最低分: %d　｜　最高分: %d　｜　平均分: %.1f　｜　总平均正确率: %.1f%%"
                            + "\n      基础题平均正确率: %s　｜　中等题平均正确率: %s　｜　提高题平均正确率: %s",
                            count, min, max, avg, avgRate, bRate, mRate, aRate));
                } else {
                    statsLabel.setText("暂无成绩记录");
                }
            });
        }
    }

    private JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Microsoft YaHei", Font.BOLD, 13));
        l.setForeground(ModernTheme.text());
        return l;
    }

    private JLabel iconLabel(String emoji) {
        JLabel l = new JLabel(emoji);
        l.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 22));
        return l;
    }

    private JLabel userTag() {
        JLabel l = new JLabel(username);
        l.setFont(new Font("Microsoft YaHei", Font.BOLD, 12));
        l.setForeground(ModernTheme.ACCENT);
        l.setBorder(BorderFactory.createEmptyBorder(2, 10, 2, 10));
        return l;
    }

    private void setStatus(String msg) {
        SwingUtilities.invokeLater(() -> statusLabel.setText(msg));
    }

    private JButton logoutButton() {
        JButton btn = new JButton("← 退出");
        btn.setFont(ModernTheme.SMALL_FONT);
        btn.setForeground(ModernTheme.subtext());
        btn.setBackground(ModernTheme.bg());
        btn.setBorder(new javax.swing.border.CompoundBorder(
                new javax.swing.border.LineBorder(ModernTheme.border(), 1),
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

    private class TeacherResponseListener implements ResponseListener {
        @Override
        public void onResponse(Response response) {
            lastResponse = response;
            if (latch != null) latch.countDown();
        }
    }
}
