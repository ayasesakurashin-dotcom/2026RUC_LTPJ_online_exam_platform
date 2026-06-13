package com.exam.client.teacher;

import com.exam.client.common.ModernTheme;
import com.exam.client.common.NetworkClient;
import com.exam.client.common.ResponseListener;
import com.exam.common.model.*;
import com.exam.common.protocol.MessageType;
import com.exam.common.protocol.Request;
import com.exam.common.protocol.Response;
import com.exam.common.util.QuestionFactory;
import java.awt.*;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

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
        this.client.setListener(new TeacherResponseListener());
        initUI();
        loadMyExams();
    }

    private void initUI() {
        setTitle("在线考试系统 · 教师  " + username);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1050, 760);
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(ModernTheme.bg());

        // ---- Header ----
        JPanel header = ModernTheme.headerBar();
        header.add(ModernTheme.headerLeft("📚", "教师工作台"), BorderLayout.WEST);

        statusLabel = ModernTheme.headerStatusLabel();
        JPanel headerRight = ModernTheme.headerRight(
                statusLabel,
                ModernTheme.userTag(username),
                ModernTheme.headerButton("← 退出")
        );
        for (Component c : headerRight.getComponents()) {
            if (c instanceof JButton && "← 退出".equals(((JButton) c).getText())) {
                ((JButton) c).addActionListener(e -> logout());
            }
        }
        header.add(headerRight, BorderLayout.EAST);
        root.add(header, BorderLayout.NORTH);

        // ---- Tabs ----
        tabbedPane = ModernTheme.tabbedPane();
        tabbedPane.addTab("  发布考试  ", buildCreateTab());
        tabbedPane.addTab("  我的考试  ", buildMyExamTab());
        tabbedPane.addTab("  批改简答题  ", buildGradingTab());
        tabbedPane.addTab("  成绩统计  ", buildScoreTab());
        tabbedPane.addTab("  考试复盘  ", buildReviewTab());
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
        infoCard.add(ModernTheme.label("考试标题"));
        titleField = createTextField();
        infoCard.add(titleField);
        infoCard.add(new JLabel());

        infoCard.add(ModernTheme.label("开始日期 (yyyy-MM-dd)"));
        dateField = createTextField();
        dateField.setText(new SimpleDateFormat("yyyy-MM-dd").format(new Date()));
        infoCard.add(dateField);
        JButton nowBtn = ModernTheme.secondaryButton("现在");
        nowBtn.addActionListener(e -> {
            Date now = new Date();
            dateField.setText(new SimpleDateFormat("yyyy-MM-dd").format(now));
            timeField.setText(new SimpleDateFormat("HH:mm").format(now));
        });
        infoCard.add(nowBtn);

        infoCard.add(ModernTheme.label("开始时间 (HH:mm)"));
        timeField = createTextField();
        timeField.setText(new SimpleDateFormat("HH:mm").format(new Date()));
        infoCard.add(timeField);
        infoCard.add(ModernTheme.label("考试时长（秒）"));
        durationField = createTextField();
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
        sp.getViewport().setBackground(ModernTheme.surface());
        panel.add(sp, BorderLayout.CENTER);

        // 按钮
        JPanel btnBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        btnBar.setBackground(ModernTheme.bg());
        JButton addChoice = ModernTheme.primaryButton("＋ 选择题");
        JButton addBlank  = ModernTheme.secondaryButton("＋ 填空题");
        JButton addEssay  = ModernTheme.secondaryButton("＋ 简答题");
        JButton removeQ   = ModernTheme.secondaryButton("✕ 删除");
        JButton publish   = ModernTheme.primaryButton("✔ 发布考试");
        addChoice.addActionListener(e -> showAddChoiceDialog());
        addBlank.addActionListener(e -> showAddBlankDialog());
        addEssay.addActionListener(e -> showAddEssayDialog());
        removeQ.addActionListener(e -> removeSelectedQuestion());
        publish.addActionListener(e -> publishExam());
        btnBar.add(addChoice); btnBar.add(addBlank); btnBar.add(addEssay); btnBar.add(removeQ);
        btnBar.add(Box.createHorizontalStrut(20)); btnBar.add(publish);
        panel.add(btnBar, BorderLayout.SOUTH);

        return panel;
    }

    // ==================== Tab2: 我的考试 ====================
    private JPanel buildMyExamTab() {
        JPanel panel = ModernTheme.insetPanel(16, 16, 16, 16);
        panel.setLayout(new BorderLayout(0, 10));

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
        topPanel.setBackground(ModernTheme.surface());
        topPanel.setBorder(ModernTheme.titledBorder("已完成的考试"));

        finishedExamModel = new DefaultTableModel(
                new String[]{"考试ID", "标题", "总分", "参加人数"}, 0);
        finishedExamTable = ModernTheme.table(finishedExamModel);
        topPanel.add(ModernTheme.tableScroll(finishedExamTable), BorderLayout.CENTER);

        JPanel topBtnBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        topBtnBar.setBackground(ModernTheme.surface());
        JButton loadFinished = ModernTheme.secondaryButton("↻ 刷新");
        JButton viewScores  = ModernTheme.primaryButton("→ 查看成绩");
        loadFinished.addActionListener(e -> loadFinishedExams());
        viewScores.addActionListener(e -> viewSelectedExamScores());
        topBtnBar.add(loadFinished); topBtnBar.add(viewScores);
        topPanel.add(topBtnBar, BorderLayout.SOUTH);

        // 下半：成绩明细
        JPanel bottomPanel = new JPanel(new BorderLayout(0, 6));
        bottomPanel.setBackground(ModernTheme.surface());
        bottomPanel.setBorder(ModernTheme.titledBorder("成绩明细"));

        scoreDetailModel = new DefaultTableModel(
                new String[]{"学生", "得分", "总分", "总正确率",
                        "基础题正确率", "中等题正确率", "提高题正确率", "提交时间"}, 0);
        scoreDetailTable = ModernTheme.table(scoreDetailModel);
        bottomPanel.add(ModernTheme.tableScroll(scoreDetailTable), BorderLayout.CENTER);

        // 统计条
        statsLabel = new JLabel(" ");
        statsLabel.setFont(ModernTheme.BODY_FONT);
        statsLabel.setForeground(ModernTheme.text());
        statsLabel.setBackground(ModernTheme.elevated());
        statsLabel.setOpaque(true);
        statsLabel.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));
        bottomPanel.add(statsLabel, BorderLayout.SOUTH);

        JSplitPane split = ModernTheme.splitPane(JSplitPane.VERTICAL_SPLIT, topPanel, bottomPanel, 0.42);
        split.setDividerLocation(260);
        panel.add(split, BorderLayout.CENTER);

        return panel;
    }

    // ==================== 添加题目对话框 ====================
    private void showAddChoiceDialog() {
        JTextField contentField = createTextField();
        JTextField scoreField = createTextField();
        scoreField.setText("5");
        JTextArea optionsArea = ModernTheme.textArea(4, 24);
        optionsArea.setText("选项1\n选项2\n选项3\n选项4");
        JTextField correctField = createTextField();
        correctField.setText("0");
        JTextArea explanationArea = ModernTheme.textArea(3, 24);
        explanationArea.setLineWrap(true);
        explanationArea.setWrapStyleWord(true);

        // 难度单选
        JRadioButton basicR = new JRadioButton("基础题", true);
        JRadioButton mediumR = new JRadioButton("中等题");
        JRadioButton advancedR = new JRadioButton("提高题");
        ButtonGroup diffGroup = new ButtonGroup();
        diffGroup.add(basicR); diffGroup.add(mediumR); diffGroup.add(advancedR);
        basicR.setBackground(ModernTheme.surface()); basicR.setForeground(ModernTheme.text());
        basicR.setFont(ModernTheme.BODY_FONT);
        mediumR.setBackground(ModernTheme.surface()); mediumR.setForeground(ModernTheme.text());
        mediumR.setFont(ModernTheme.BODY_FONT);
        advancedR.setBackground(ModernTheme.surface()); advancedR.setForeground(ModernTheme.text());
        advancedR.setFont(ModernTheme.BODY_FONT);
        JPanel diffPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        diffPanel.setBackground(ModernTheme.surface());
        diffPanel.add(basicR); diffPanel.add(mediumR); diffPanel.add(advancedR);

        JPanel panel = new JPanel(new GridLayout(6, 2, 6, 8));
        panel.setBackground(ModernTheme.surface());
        panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        panel.add(ModernTheme.label("题目内容"));   panel.add(contentField);
        panel.add(ModernTheme.label("分值"));       panel.add(scoreField);
        panel.add(ModernTheme.label("难度（必选）")); panel.add(diffPanel);
        panel.add(ModernTheme.label("选项（每行一个）")); panel.add(ModernTheme.styledScrollPane(optionsArea));
        panel.add(ModernTheme.label("正确索引（0开始）"));  panel.add(correctField);
        panel.add(ModernTheme.label("题目解析（选填）")); panel.add(ModernTheme.styledScrollPane(explanationArea));

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
            String expl = explanationArea.getText().trim();
            if (!expl.isEmpty()) q.setExplanation(expl);
            pendingQuestions.add(q);
            questionListModel.addElement("📋 [" + diffLabel(diff) + "] " + q.getContent() + "  [" + q.getScore() + "分]");
        } catch (Exception ex) {
            setStatus("输入格式错误");
        }
    }

    private void showAddBlankDialog() {
        JTextField contentField = createTextField();
        JTextField scoreField = createTextField();
        scoreField.setText("5");
        JTextField answerField = createTextField();
        JTextArea explanationArea = ModernTheme.textArea(3, 24);
        explanationArea.setLineWrap(true);
        explanationArea.setWrapStyleWord(true);

        JRadioButton basicR = new JRadioButton("基础题", true);
        JRadioButton mediumR = new JRadioButton("中等题");
        JRadioButton advancedR = new JRadioButton("提高题");
        ButtonGroup diffGroup = new ButtonGroup();
        diffGroup.add(basicR); diffGroup.add(mediumR); diffGroup.add(advancedR);
        basicR.setBackground(ModernTheme.surface()); basicR.setForeground(ModernTheme.text());
        basicR.setFont(ModernTheme.BODY_FONT);
        mediumR.setBackground(ModernTheme.surface()); mediumR.setForeground(ModernTheme.text());
        mediumR.setFont(ModernTheme.BODY_FONT);
        advancedR.setBackground(ModernTheme.surface()); advancedR.setForeground(ModernTheme.text());
        advancedR.setFont(ModernTheme.BODY_FONT);
        JPanel diffPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        diffPanel.setBackground(ModernTheme.surface());
        diffPanel.add(basicR); diffPanel.add(mediumR); diffPanel.add(advancedR);

        JPanel panel = new JPanel(new GridLayout(5, 2, 6, 8));
        panel.setBackground(ModernTheme.surface());
        panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        panel.add(ModernTheme.label("题目内容"));   panel.add(contentField);
        panel.add(ModernTheme.label("分值"));       panel.add(scoreField);
        panel.add(ModernTheme.label("难度（必选）")); panel.add(diffPanel);
        panel.add(ModernTheme.label("正确答案"));   panel.add(answerField);
        panel.add(ModernTheme.label("题目解析（选填）")); panel.add(ModernTheme.styledScrollPane(explanationArea));

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
            String expl = explanationArea.getText().trim();
            if (!expl.isEmpty()) q.setExplanation(expl);
            pendingQuestions.add(q);
            questionListModel.addElement("✏ [" + diffLabel(diff) + "] " + q.getContent() + "  [" + q.getScore() + "分]");
        } catch (Exception ex) {
            setStatus("输入格式错误");
        }
    }

    private void showAddEssayDialog() {
        JTextField contentField = createTextField();
        JTextField scoreField = createTextField();
        scoreField.setText("10");
        JTextArea refAnswerArea = ModernTheme.textArea(4, 24);
        JTextArea explanationArea = ModernTheme.textArea(3, 24);
        explanationArea.setLineWrap(true);
        explanationArea.setWrapStyleWord(true);

        JRadioButton basicR = new JRadioButton("基础题", true);
        JRadioButton mediumR = new JRadioButton("中等题");
        JRadioButton advancedR = new JRadioButton("提高题");
        ButtonGroup diffGroup = new ButtonGroup();
        diffGroup.add(basicR); diffGroup.add(mediumR); diffGroup.add(advancedR);
        basicR.setBackground(ModernTheme.surface()); basicR.setForeground(ModernTheme.text());
        basicR.setFont(ModernTheme.BODY_FONT);
        mediumR.setBackground(ModernTheme.surface()); mediumR.setForeground(ModernTheme.text());
        mediumR.setFont(ModernTheme.BODY_FONT);
        advancedR.setBackground(ModernTheme.surface()); advancedR.setForeground(ModernTheme.text());
        advancedR.setFont(ModernTheme.BODY_FONT);
        JPanel diffPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        diffPanel.setBackground(ModernTheme.surface());
        diffPanel.add(basicR); diffPanel.add(mediumR); diffPanel.add(advancedR);

        JPanel panel = new JPanel(new GridLayout(5, 2, 6, 8));
        panel.setBackground(ModernTheme.surface());
        panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        panel.add(ModernTheme.label("题目内容"));   panel.add(contentField);
        panel.add(ModernTheme.label("分值"));       panel.add(scoreField);
        panel.add(ModernTheme.label("难度（必选）")); panel.add(diffPanel);
        panel.add(ModernTheme.label("参考答案"));   panel.add(ModernTheme.styledScrollPane(refAnswerArea));
        panel.add(ModernTheme.label("题目解析（选填）")); panel.add(ModernTheme.styledScrollPane(explanationArea));

        int r = JOptionPane.showConfirmDialog(this, panel, "添加简答题",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (r != JOptionPane.OK_OPTION) return;

        try {
            String diff = basicR.isSelected() ? "BASIC" : mediumR.isSelected() ? "MEDIUM" : "ADVANCED";
            Question q = QuestionFactory.createEssay(
                    contentField.getText().trim(),
                    Integer.parseInt(scoreField.getText().trim()),
                    diff,
                    refAnswerArea.getText().trim());
            String expl = explanationArea.getText().trim();
            if (!expl.isEmpty()) q.setExplanation(expl);
            pendingQuestions.add(q);
            questionListModel.addElement("📝 [" + diffLabel(diff) + "] " + q.getContent() + "  [" + q.getScore() + "分]");
        } catch (Exception ex) {
            setStatus("输入格式错误");
        }
    }

    // ==================== Tab: 批改简答题 ====================

    private DefaultTableModel gradingExamModel, gradingStudentModel;
    private JTable gradingExamTable, gradingStudentTable;
    private JPanel gradingPanel;
    private JScrollPane gradingScroll;
    private String currentGradingExamId;
    private List<Question> currentGradingQuestions;
    private Map<String, Map<String, String>> currentStudentAnswers;
    private Map<String, ExamResult> currentResultMap;
    private String currentGradingStudentId;
    private List<JTextField> essayScoreFields;
    private List<String> essayQuestionIds;

    // Tab5: 考试复盘
    private DefaultTableModel reviewExamModel;
    private JTable reviewExamTable;

    private JPanel buildGradingTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 0));
        panel.setBackground(ModernTheme.bg());
        panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        // Left: exam list + student list
        JPanel leftPanel = new JPanel(new BorderLayout(0, 8));
        leftPanel.setBackground(ModernTheme.bg());
        leftPanel.setPreferredSize(new Dimension(320, 0));

        // Exam list
        JPanel examPanel = new JPanel(new BorderLayout(0, 4));
        examPanel.setBackground(ModernTheme.surface());
        examPanel.setBorder(ModernTheme.titledBorder("含简答题的考试"));

        gradingExamModel = new DefaultTableModel(new String[]{"考试ID", "标题", "发布状态"}, 0);
        gradingExamTable = ModernTheme.table(gradingExamModel);
        examPanel.add(ModernTheme.tableScroll(gradingExamTable), BorderLayout.CENTER);

        JPanel examBtnBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        examBtnBar.setBackground(ModernTheme.surface());
        JButton refreshExams = ModernTheme.secondaryButton("↻ 刷新");
        JButton selectExam = ModernTheme.primaryButton("→ 查看提交");
        refreshExams.addActionListener(e -> loadGradingExams());
        selectExam.addActionListener(e -> loadGradingStudents());
        examBtnBar.add(refreshExams); examBtnBar.add(selectExam);
        examPanel.add(examBtnBar, BorderLayout.SOUTH);
        leftPanel.add(examPanel, BorderLayout.NORTH);

        // Student list
        JPanel studentPanel = new JPanel(new BorderLayout(0, 4));
        studentPanel.setBackground(ModernTheme.surface());
        studentPanel.setBorder(ModernTheme.titledBorder("已提交学生"));

        gradingStudentModel = new DefaultTableModel(new String[]{"学生", "批改状态"}, 0);
        gradingStudentTable = ModernTheme.table(gradingStudentModel);
        studentPanel.add(ModernTheme.tableScroll(gradingStudentTable), BorderLayout.CENTER);

        JPanel stuBtnBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        stuBtnBar.setBackground(ModernTheme.surface());
        JButton selectStudent = ModernTheme.primaryButton("→ 批改");
        selectStudent.addActionListener(e -> loadStudentEssays());
        stuBtnBar.add(selectStudent);
        studentPanel.add(stuBtnBar, BorderLayout.SOUTH);
        leftPanel.add(studentPanel, BorderLayout.CENTER);

        // Right: grading form
        gradingPanel = new JPanel();
        gradingPanel.setLayout(new BoxLayout(gradingPanel, BoxLayout.Y_AXIS));
        gradingPanel.setBackground(ModernTheme.bg());
        gradingPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JLabel placeholder = ModernTheme.placeholder("请先选择考试和学生");
        gradingPanel.add(placeholder);

        gradingScroll = new JScrollPane(gradingPanel);
        gradingScroll.setBorder(ModernTheme.titledBorder("简答题批改"));
        gradingScroll.setBackground(ModernTheme.bg());
        gradingScroll.getViewport().setBackground(ModernTheme.bg());
        gradingScroll.getVerticalScrollBar().setUnitIncrement(16);
        ModernTheme.styleScrollBar(gradingScroll);

        JSplitPane split = ModernTheme.splitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, gradingScroll, 0.3);
        split.setDividerLocation(320);
        panel.add(split, BorderLayout.CENTER);

        // Bottom buttons
        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        bottomBar.setBackground(ModernTheme.bg());
        JButton saveBtn = ModernTheme.primaryButton("✔ 保存评分");
        JButton publishBtn = ModernTheme.dangerButton("📢 发布成绩");
        saveBtn.addActionListener(e -> saveEssayGrades());
        publishBtn.addActionListener(e -> publishExamScores());
        bottomBar.add(saveBtn); bottomBar.add(publishBtn);
        panel.add(bottomBar, BorderLayout.SOUTH);

        return panel;
    }

    private void loadGradingExams() {
        Map<String, String> d = new HashMap<>(); d.put("username", username); d.put("role", "TEACHER");
        Response resp = sendAndWait(new Request(MessageType.GET_EXAMS, (Serializable) d));
        if (resp != null && "OK".equals(resp.getStatus())) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> all = (List<Map<String, Object>>) resp.getData();
            SwingUtilities.invokeLater(() -> {
                gradingExamModel.setRowCount(0);
                for (Map<String, Object> e : all) {
                    if (username.equals(e.get("teacherId")) && Boolean.TRUE.equals(e.get("hasEssayQuestions"))
                            && "FINISHED".equals(e.get("status"))) {
                        String pubStatus = Boolean.TRUE.equals(e.get("scoresPublished")) ? "已发布" : "未发布";
                        gradingExamModel.addRow(new Object[]{e.get("id"), e.get("title"), pubStatus});
                    }
                }
            });
        }
    }

    @SuppressWarnings("unchecked")
    private void loadGradingStudents() {
        int row = gradingExamTable.getSelectedRow();
        if (row < 0) { setStatus("请先选择一场考试"); return; }
        currentGradingExamId = (String) gradingExamModel.getValueAt(row, 0);

        Map<String, String> d = new HashMap<>(); d.put("examId", currentGradingExamId);
        Response resp = sendAndWait(new Request(MessageType.GET_SUBMISSIONS, (Serializable) d));
        if (resp == null || !"OK".equals(resp.getStatus())) {
            setStatus(resp != null ? resp.getMessage() : "加载失败"); return;
        }

        Map<String, Object> data = (Map<String, Object>) resp.getData();
        currentGradingQuestions = (List<Question>) data.get("questions");
        currentStudentAnswers = (Map<String, Map<String, String>>) data.get("studentAnswers");
        List<ExamResult> results = (List<ExamResult>) data.get("results");
        currentResultMap = new HashMap<>();
        for (ExamResult r : results) currentResultMap.put(r.getStudentId(), r);

        SwingUtilities.invokeLater(() -> {
            gradingStudentModel.setRowCount(0);
            for (String sid : currentStudentAnswers.keySet()) {
                ExamResult r = currentResultMap.get(sid);
                String status = "未批改";
                if (r != null && r.getEssayScores() != null) {
                    boolean allGraded = true;
                    for (int s : r.getEssayScores().values()) {
                        if (s < 0) { allGraded = false; break; }
                    }
                    status = allGraded ? "已批改" : "部分批改";
                }
                gradingStudentModel.addRow(new Object[]{sid, status});
            }
            clearGradingPanel();
        });
    }

    private void loadStudentEssays() {
        int row = gradingStudentTable.getSelectedRow();
        if (row < 0) { setStatus("请先选择一个学生"); return; }
        currentGradingStudentId = (String) gradingStudentModel.getValueAt(row, 0);

        if (currentGradingQuestions == null || currentStudentAnswers == null) {
            setStatus("请先加载考试数据"); return;
        }

        Map<String, String> answers = currentStudentAnswers.get(currentGradingStudentId);
        if (answers == null) { setStatus("该学生无答卷数据"); return; }

        ExamResult result = currentResultMap.get(currentGradingStudentId);
        Map<String, Integer> existingScores = (result != null && result.getEssayScores() != null)
                ? result.getEssayScores() : new HashMap<>();

        essayScoreFields = new ArrayList<>();
        essayQuestionIds = new ArrayList<>();

        gradingPanel.removeAll();
        int idx = 0;
        for (Question q : currentGradingQuestions) {
            if (!(q instanceof EssayQuestion)) continue;
            idx++;
            EssayQuestion eq = (EssayQuestion) q;
            String studentAnswer = answers.getOrDefault(q.getId(), "（未作答）");
            int existingScore = existingScores.getOrDefault(q.getId(), -1);

            JPanel card = new JPanel();
            card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
            card.setBackground(ModernTheme.surface());
            card.setBorder(BorderFactory.createCompoundBorder(
                    new ModernTheme.RoundedBorder(ModernTheme.border(), 8, 1),
                    BorderFactory.createEmptyBorder(12, 16, 12, 16)));
            card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 400));
            card.setAlignmentX(Component.LEFT_ALIGNMENT);

            JLabel titleLabel = new JLabel("简答题 " + idx + "  (" + eq.getScore() + "分)");
            titleLabel.setFont(ModernTheme.SUBHEADING_FONT);
            titleLabel.setForeground(ModernTheme.ACCENT);
            titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            card.add(titleLabel);
            card.add(Box.createVerticalStrut(6));

            JLabel contentLabel = new JLabel("<html><b>题目：</b>" + eq.getContent() + "</html>");
            contentLabel.setFont(ModernTheme.BODY_FONT);
            contentLabel.setForeground(ModernTheme.text());
            contentLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            card.add(contentLabel);
            card.add(Box.createVerticalStrut(4));

            JLabel refLabel = new JLabel("<html><b>参考答案：</b>" + eq.getReferenceAnswer() + "</html>");
            refLabel.setFont(ModernTheme.BODY_FONT);
            refLabel.setForeground(ModernTheme.subtext());
            refLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            card.add(refLabel);
            card.add(Box.createVerticalStrut(6));

            JLabel ansLabel = new JLabel("<html><b>学生答案：</b></html>");
            ansLabel.setFont(ModernTheme.BODY_FONT);
            ansLabel.setForeground(ModernTheme.text());
            ansLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            card.add(ansLabel);

            JTextArea ansArea = new JTextArea(studentAnswer);
            ansArea.setFont(ModernTheme.BODY_FONT);
            ansArea.setBackground(ModernTheme.elevated());
            ansArea.setForeground(ModernTheme.text());
            ansArea.setEditable(false);
            ansArea.setLineWrap(true);
            ansArea.setWrapStyleWord(true);
            ansArea.setRows(3);
            JScrollPane sp = ModernTheme.styledScrollPane(ansArea);
            sp.setAlignmentX(Component.LEFT_ALIGNMENT);
            sp.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
            card.add(sp);
            card.add(Box.createVerticalStrut(6));

            JPanel scoreRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
            scoreRow.setBackground(ModernTheme.surface());
            scoreRow.setAlignmentX(Component.LEFT_ALIGNMENT);
            JLabel scoreHint = new JLabel("评分 (0~" + eq.getScore() + ")：");
            scoreHint.setFont(ModernTheme.SUBHEADING_FONT);
            scoreHint.setForeground(ModernTheme.text());
            scoreRow.add(scoreHint);
            JTextField scoreField = createTextField();
            scoreField.setPreferredSize(new Dimension(80, 30));
            if (existingScore >= 0) scoreField.setText(String.valueOf(existingScore));
            scoreRow.add(scoreField);
            JLabel maxLabel = new JLabel(" / " + eq.getScore());
            maxLabel.setFont(ModernTheme.BODY_FONT);
            maxLabel.setForeground(ModernTheme.subtext());
            scoreRow.add(maxLabel);
            card.add(scoreRow);

            essayScoreFields.add(scoreField);
            essayQuestionIds.add(q.getId());

            gradingPanel.add(card);
            gradingPanel.add(Box.createVerticalStrut(10));
        }

        gradingPanel.revalidate();
        gradingPanel.repaint();
        gradingScroll.getVerticalScrollBar().setValue(0);
    }

    private void saveEssayGrades() {
        if (currentGradingExamId == null || currentGradingStudentId == null
                || essayScoreFields == null || essayScoreFields.isEmpty()) {
            setStatus("请先选择考试和学生"); return;
        }

        for (int i = 0; i < essayScoreFields.size(); i++) {
            String text = essayScoreFields.get(i).getText().trim();
            if (text.isEmpty()) continue;

            try {
                int score = Integer.parseInt(text);
                Map<String, String> d = new HashMap<>();
                d.put("examId", currentGradingExamId);
                d.put("studentId", currentGradingStudentId);
                d.put("questionId", essayQuestionIds.get(i));
                d.put("score", String.valueOf(score));
                Response resp = sendAndWait(new Request(MessageType.GRADE_ESSAY, (Serializable) d));
                if (resp == null || !"OK".equals(resp.getStatus())) {
                    setStatus("评分失败: " + (resp != null ? resp.getMessage() : "超时"));
                    return;
                }
            } catch (NumberFormatException ex) {
                setStatus("第 " + (i + 1) + " 题分数格式错误");
                return;
            }
        }
        setStatus("评分保存成功");
        loadGradingStudents();
    }

    private void publishExamScores() {
        if (currentGradingExamId == null) { setStatus("请先选择一场考试"); return; }

        int cfm = JOptionPane.showConfirmDialog(this,
                "确认发布成绩？发布后学生将可以查看分数。", "确认发布",
                JOptionPane.YES_NO_OPTION);
        if (cfm != JOptionPane.YES_OPTION) return;

        Map<String, String> d = new HashMap<>(); d.put("examId", currentGradingExamId);
        Response resp = sendAndWait(new Request(MessageType.PUBLISH_SCORES, (Serializable) d));
        if (resp != null && "OK".equals(resp.getStatus())) {
            setStatus("成绩已发布！");
            loadGradingExams();
        } else {
            setStatus(resp != null ? resp.getMessage() : "发布失败");
        }
    }

    private void clearGradingPanel() {
        gradingPanel.removeAll();
        JLabel placeholder = ModernTheme.placeholder("请选择学生进行批改");
        gradingPanel.add(placeholder);
        gradingPanel.revalidate();
        gradingPanel.repaint();
    }

    // ==================== Tab5: 考试复盘 ====================

    private JPanel buildReviewTab() {
        JPanel panel = ModernTheme.insetPanel(16, 16, 16, 16);
        panel.setLayout(new BorderLayout(0, 10));

        reviewExamModel = new DefaultTableModel(
                new String[]{"考试ID", "标题", "状态", "参加人数", "题目数", "总分"}, 0);
        reviewExamTable = ModernTheme.table(reviewExamModel);
        panel.add(ModernTheme.tableScroll(reviewExamTable), BorderLayout.CENTER);

        JPanel btnBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        btnBar.setBackground(ModernTheme.bg());
        JButton refreshBtn = ModernTheme.secondaryButton("↻ 刷新");
        JButton reviewBtn = ModernTheme.primaryButton("→ 考试复盘");
        refreshBtn.addActionListener(e -> loadReviewExams());
        reviewBtn.addActionListener(e -> showTeacherReviewDialog());
        btnBar.add(refreshBtn);
        btnBar.add(reviewBtn);
        panel.add(btnBar, BorderLayout.SOUTH);

        return panel;
    }

    private void loadReviewExams() {
        Map<String, String> d = new HashMap<>(); d.put("username", username); d.put("role", "TEACHER");
        Response resp = sendAndWait(new Request(MessageType.GET_EXAMS, (Serializable) d));
        if (resp != null && "OK".equals(resp.getStatus())) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> all = (List<Map<String, Object>>) resp.getData();
            SwingUtilities.invokeLater(() -> {
                reviewExamModel.setRowCount(0);
                for (Map<String, Object> e : all) {
                    if (username.equals(e.get("teacherId"))
                            && "FINISHED".equals(e.get("status"))
                            && Boolean.TRUE.equals(e.get("scoresPublished"))) {
                        reviewExamModel.addRow(new Object[]{
                                e.get("id"), e.get("title"), "已发布成绩",
                                e.get("joinedCount"), e.get("questionCount"), e.get("totalScore")
                        });
                    }
                }
            });
        }
    }

    @SuppressWarnings("unchecked")
    private void showTeacherReviewDialog() {
        int row = reviewExamTable.getSelectedRow();
        if (row < 0) { setStatus("请先选择一场已结束的考试"); return; }
        String examId = (String) reviewExamModel.getValueAt(row, 0);
        String examTitle = (String) reviewExamModel.getValueAt(row, 1);

        Map<String, String> reqData = new HashMap<>();
        reqData.put("examId", examId);
        reqData.put("username", username);
        reqData.put("role", "TEACHER");
        Response resp = sendAndWait(new Request(MessageType.GET_EXAM_REVIEW, (Serializable) reqData));
        if (resp == null || !"OK".equals(resp.getStatus())) {
            setStatus(resp != null ? resp.getMessage() : "加载复盘数据失败");
            return;
        }

        Map<String, Object> reviewData = (Map<String, Object>) resp.getData();
        List<Map<String, Object>> questions = (List<Map<String, Object>>) reviewData.get("questions");
        Map<String, Map<String, String>> allAnswers =
                (Map<String, Map<String, String>>) reviewData.get("allStudentAnswers");
        List<ExamResult> allResults = (List<ExamResult>) reviewData.get("allResults");

        // 构建对话框
        JDialog dialog = new JDialog(this, "考试复盘 - " + examTitle, true);
        dialog.setSize(900, 680);
        dialog.setLocationRelativeTo(this);
        dialog.getContentPane().setBackground(ModernTheme.bg());
        dialog.setLayout(new BorderLayout(0, 0));

        // 标题栏
        dialog.add(ModernTheme.dialogHeader("📊 考试复盘：" + examTitle, ModernTheme.TEACHER_ACCENT), BorderLayout.NORTH);

        // 题目列表
        JPanel reviewPanel = new JPanel();
        reviewPanel.setLayout(new BoxLayout(reviewPanel, BoxLayout.Y_AXIS));
        reviewPanel.setBackground(ModernTheme.bg());
        reviewPanel.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));

        int totalStudents = allAnswers.size();

        for (int i = 0; i < questions.size(); i++) {
            Map<String, Object> qInfo = questions.get(i);
            String type = (String) qInfo.get("type");
            String diff = (String) qInfo.get("difficulty");
            String correctAnswer = (String) qInfo.get("correctAnswer");
            String explanation = (String) qInfo.get("explanation");
            int qScore = ((Number) qInfo.get("score")).intValue();

            // 统计该题答对/答错人数
            int correctCount = 0, incorrectCount = 0;
            for (Map.Entry<String, Map<String, String>> entry : allAnswers.entrySet()) {
                String studentAnswer = entry.getValue().get(qInfo.get("id"));
                if ("CHOICE".equals(type)) {
                    if (correctAnswer.equals(studentAnswer)) correctCount++;
                    else incorrectCount++;
                } else if ("BLANK".equals(type)) {
                    if (studentAnswer != null && studentAnswer.trim().equalsIgnoreCase(correctAnswer.trim()))
                        correctCount++;
                    else incorrectCount++;
                } else {
                    // Essay - 不计入对错统计
                    correctCount = -1;
                    break;
                }
            }

            String correctAnswerDisplay = correctAnswer;
            if ("CHOICE".equals(type) && qInfo.containsKey("correctAnswerText")) {
                correctAnswerDisplay = correctAnswer + " (" + qInfo.get("correctAnswerText") + ")";
            }

            // 构建题目卡片
            JPanel card = new JPanel();
            card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
            card.setBackground(ModernTheme.surface());
            card.setBorder(BorderFactory.createCompoundBorder(
                    new ModernTheme.ShadowBorder(ModernTheme.border(), 8, 1),
                    BorderFactory.createEmptyBorder(12, 16, 12, 16)));
            card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 400));
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
            JLabel answerLabel = new JLabel("<html><b>正确答案：</b>" + correctAnswerDisplay + "</html>");
            answerLabel.setFont(ModernTheme.BODY_FONT);
            answerLabel.setForeground(ModernTheme.SUCCESS);
            answerLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            card.add(answerLabel);

            // 统计信息
            if ("ESSAY".equals(type)) {
                // 从成绩中计算简答题平均分
                double avgScore = 0;
                int essayCount = 0;
                if (allResults != null) {
                    for (ExamResult r : allResults) {
                        Map<String, Integer> es = r.getEssayScores();
                        if (es != null && es.containsKey(qInfo.get("id"))) {
                            int s = es.get(qInfo.get("id"));
                            if (s >= 0) {
                                avgScore += s;
                                essayCount++;
                            }
                        }
                    }
                }
                if (essayCount > 0) {
                    avgScore = avgScore / essayCount;
                }
                String avgStr = essayCount > 0
                        ? String.format("%.1f 分 (已批改 %d 人)", avgScore, essayCount)
                        : "暂无批改数据";
                JLabel statLabel = new JLabel("📝 简答题平均得分：" + avgStr);
                statLabel.setFont(ModernTheme.BODY_FONT);
                statLabel.setForeground(ModernTheme.ACCENT);
                statLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
                card.add(statLabel);
            } else {
                String rateStr = totalStudents > 0
                        ? String.format("%.1f%%", 100.0 * correctCount / totalStudents)
                        : "0%";
                JLabel statLabel = new JLabel("✅ 答对: " + correctCount + "人　｜　❌ 答错: " + incorrectCount + "人　｜　正确率: " + rateStr);
                statLabel.setFont(ModernTheme.BODY_FONT);
                statLabel.setForeground(ModernTheme.text());
                statLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
                card.add(statLabel);
            }
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
        dialog.add(scroll, BorderLayout.CENTER);

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
            startTime = new SimpleDateFormat("yyyy-MM-dd HH:mm").parse(dt).getTime();
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

    // ==================== 辅助方法 ====================
    private JTextField createTextField() {
        return ModernTheme.textField(16);
    }

    private void setStatus(String msg) {
        SwingUtilities.invokeLater(() -> statusLabel.setText(msg));
    }

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

    private class TeacherResponseListener implements ResponseListener {
        @Override
        public void onResponse(Response response) {
            lastResponse = response;
            if (latch != null) latch.countDown();
        }
    }
}
