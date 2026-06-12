package com.exam.client.teacher;

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
        // 设置窗口背景色（天蓝色）
        getContentPane().setBackground(new Color(135, 206, 235));
        
        setTitle("在线考试系统 · 教师  " + username);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1050, 760);
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(new Color(135, 206, 235));

        // Header（顶部栏）
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(100, 180, 220));
        header.setBorder(BorderFactory.createEmptyBorder(14, 20, 14, 20));

        JPanel headerLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        headerLeft.setBackground(new Color(100, 180, 220));
        headerLeft.add(iconLabel("📚"));
        JLabel title = new JLabel("教师工作台");
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
        headerRight.add(userTag());
        headerRight.add(logoutButton());
        header.add(headerRight, BorderLayout.EAST);
        root.add(header, BorderLayout.NORTH);

        // Tabs
        tabbedPane = new JTabbedPane(JTabbedPane.TOP);
        tabbedPane.setBackground(new Color(135, 206, 235));
        tabbedPane.setForeground(new Color(50, 50, 50));
        tabbedPane.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        tabbedPane.addTab("  发布考试  ", buildCreateTab());
        tabbedPane.addTab("  我的考试  ", buildMyExamTab());
        tabbedPane.addTab("  批改简答题  ", buildGradingTab());
        tabbedPane.addTab("  成绩统计  ", buildScoreTab());

        root.add(tabbedPane, BorderLayout.CENTER);
        add(root);
    }

    // ==================== Tab1: 发布考试 ====================
    private JPanel buildCreateTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(new Color(135, 206, 235));
        panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        // 基本信息卡片
        JPanel infoCard = new JPanel(new GridLayout(3, 3, 10, 8));
        infoCard.setBackground(Color.WHITE);
        infoCard.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
        infoCard.add(createLabel("考试标题"));
        titleField = createStyledTextField();
        infoCard.add(titleField);
        infoCard.add(new JLabel()); // spacer

        infoCard.add(createLabel("开始日期 (yyyy-MM-dd)"));
        dateField = createStyledTextField();
        dateField.setText(new SimpleDateFormat("yyyy-MM-dd").format(new Date()));
        infoCard.add(dateField);
        JButton nowBtn = createSecondaryButton("现在");
        nowBtn.addActionListener(e -> {
            Date now = new Date();
            dateField.setText(new SimpleDateFormat("yyyy-MM-dd").format(now));
            timeField.setText(new SimpleDateFormat("HH:mm").format(now));
        });
        infoCard.add(nowBtn);

        infoCard.add(createLabel("开始时间 (HH:mm)"));
        timeField = createStyledTextField();
        timeField.setText(new SimpleDateFormat("HH:mm").format(new Date()));
        infoCard.add(timeField);
        infoCard.add(createLabel("考试时长（秒）"));
        durationField = createStyledTextField();
        durationField.setText("600");
        infoCard.add(durationField);
        panel.add(infoCard, BorderLayout.NORTH);

        // 题目列表
        questionListModel = new DefaultListModel<>();
        questionList = new JList<>(questionListModel);
        questionList.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        questionList.setBackground(Color.WHITE);
        questionList.setForeground(new Color(50, 50, 50));
        questionList.setSelectionBackground(new Color(100, 180, 220));
        questionList.setSelectionForeground(Color.WHITE);
        JScrollPane sp = new JScrollPane(questionList);
        sp.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
        sp.setBackground(Color.WHITE);
        panel.add(sp, BorderLayout.CENTER);

        // 按钮
        JPanel btnBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        btnBar.setBackground(new Color(135, 206, 235));
        JButton addChoice = createPrimaryButton("＋ 选择题");
        JButton addBlank  = createSecondaryButton("＋ 填空题");
        JButton addEssay  = createSecondaryButton("＋ 简答题");
        JButton removeQ   = createSecondaryButton("✕ 删除");
        JButton publish   = createPrimaryButton("✔ 发布考试");
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
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBackground(new Color(135, 206, 235));
        panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        myExamModel = new DefaultTableModel(
                new String[]{"考试ID", "标题", "状态", "时长(秒)", "已加入", "题目数", "总分"}, 0);
        myExamTable = createStyledTable(myExamModel);
        panel.add(createTableScroll(myExamTable), BorderLayout.CENTER);

        JPanel btnBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        btnBar.setBackground(new Color(135, 206, 235));
        JButton refresh = createSecondaryButton("↻ 刷新");
        refresh.addActionListener(e -> loadMyExams());
        btnBar.add(refresh);
        panel.add(btnBar, BorderLayout.SOUTH);

        return panel;
    }

    // ==================== Tab3: 成绩统计 ====================
    private JPanel buildScoreTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 0));
        panel.setBackground(new Color(135, 206, 235));
        panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        // 上半：已完成的考试
        JPanel topPanel = new JPanel(new BorderLayout(0, 6));
        topPanel.setBackground(Color.WHITE);
        topPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                "已完成的考试",
                javax.swing.border.TitledBorder.LEFT,
                javax.swing.border.TitledBorder.TOP,
                new Font("微软雅黑", Font.BOLD, 13), new Color(50, 50, 50)));

        finishedExamModel = new DefaultTableModel(
                new String[]{"考试ID", "标题", "总分", "参加人数"}, 0);
        finishedExamTable = createStyledTable(finishedExamModel);
        topPanel.add(createTableScroll(finishedExamTable), BorderLayout.CENTER);

        JPanel topBtnBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        topBtnBar.setBackground(Color.WHITE);
        JButton loadFinished = createSecondaryButton("↻ 刷新");
        JButton viewScores  = createPrimaryButton("→ 查看成绩");
        loadFinished.addActionListener(e -> loadFinishedExams());
        viewScores.addActionListener(e -> viewSelectedExamScores());
        topBtnBar.add(loadFinished); topBtnBar.add(viewScores);
        topPanel.add(topBtnBar, BorderLayout.SOUTH);

        // 下半：成绩明细
        JPanel bottomPanel = new JPanel(new BorderLayout(0, 6));
        bottomPanel.setBackground(Color.WHITE);
        bottomPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                "成绩明细",
                javax.swing.border.TitledBorder.LEFT,
                javax.swing.border.TitledBorder.TOP,
                new Font("微软雅黑", Font.BOLD, 13), new Color(50, 50, 50)));

        scoreDetailModel = new DefaultTableModel(
                new String[]{"学生", "得分", "总分", "总正确率",
                        "基础题正确率", "中等题正确率", "提高题正确率", "提交时间"}, 0);
        scoreDetailTable = createStyledTable(scoreDetailModel);
        bottomPanel.add(createTableScroll(scoreDetailTable), BorderLayout.CENTER);

        // 统计条
        statsLabel = new JLabel(" ");
        statsLabel.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        statsLabel.setForeground(new Color(50, 50, 50));
        statsLabel.setBackground(new Color(240, 248, 255));
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
        JTextField contentField = createStyledTextField();
        JTextField scoreField = createStyledTextField();
        scoreField.setText("5");
        JTextArea optionsArea = new JTextArea(4, 24);
        optionsArea.setText("选项1\n选项2\n选项3\n选项4");
        optionsArea.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        optionsArea.setBackground(Color.WHITE);
        optionsArea.setForeground(new Color(50, 50, 50));
        JTextField correctField = createStyledTextField();
        correctField.setText("0");

        // 难度单选
        JRadioButton basicR = new JRadioButton("基础题", true);
        JRadioButton mediumR = new JRadioButton("中等题");
        JRadioButton advancedR = new JRadioButton("提高题");
        ButtonGroup diffGroup = new ButtonGroup();
        diffGroup.add(basicR); diffGroup.add(mediumR); diffGroup.add(advancedR);
        basicR.setBackground(Color.WHITE); basicR.setForeground(new Color(50, 50, 50));
        mediumR.setBackground(Color.WHITE); mediumR.setForeground(new Color(50, 50, 50));
        advancedR.setBackground(Color.WHITE); advancedR.setForeground(new Color(50, 50, 50));
        JPanel diffPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        diffPanel.setBackground(Color.WHITE);
        diffPanel.add(basicR); diffPanel.add(mediumR); diffPanel.add(advancedR);

        JPanel panel = new JPanel(new GridLayout(5, 2, 6, 8));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        panel.add(createLabel("题目内容"));   panel.add(contentField);
        panel.add(createLabel("分值"));       panel.add(scoreField);
        panel.add(createLabel("难度（必选）")); panel.add(diffPanel);
        panel.add(createLabel("选项（每行一个）")); panel.add(new JScrollPane(optionsArea));
        panel.add(createLabel("正确索引（0开始）"));  panel.add(correctField);

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
        JTextField contentField = createStyledTextField();
        JTextField scoreField = createStyledTextField();
        scoreField.setText("5");
        JTextField answerField = createStyledTextField();

        JRadioButton basicR = new JRadioButton("基础题", true);
        JRadioButton mediumR = new JRadioButton("中等题");
        JRadioButton advancedR = new JRadioButton("提高题");
        ButtonGroup diffGroup = new ButtonGroup();
        diffGroup.add(basicR); diffGroup.add(mediumR); diffGroup.add(advancedR);
        basicR.setBackground(Color.WHITE); basicR.setForeground(new Color(50, 50, 50));
        mediumR.setBackground(Color.WHITE); mediumR.setForeground(new Color(50, 50, 50));
        advancedR.setBackground(Color.WHITE); advancedR.setForeground(new Color(50, 50, 50));
        JPanel diffPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        diffPanel.setBackground(Color.WHITE);
        diffPanel.add(basicR); diffPanel.add(mediumR); diffPanel.add(advancedR);

        JPanel panel = new JPanel(new GridLayout(4, 2, 6, 8));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        panel.add(createLabel("题目内容"));   panel.add(contentField);
        panel.add(createLabel("分值"));       panel.add(scoreField);
        panel.add(createLabel("难度（必选）")); panel.add(diffPanel);
        panel.add(createLabel("正确答案"));   panel.add(answerField);

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

    private void showAddEssayDialog() {
        JTextField contentField = createStyledTextField();
        JTextField scoreField = createStyledTextField();
        scoreField.setText("10");
        JTextArea refAnswerArea = new JTextArea(4, 24);
        refAnswerArea.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        refAnswerArea.setBackground(Color.WHITE);
        refAnswerArea.setForeground(new Color(50, 50, 50));
        refAnswerArea.setLineWrap(true);
        refAnswerArea.setWrapStyleWord(true);

        JRadioButton basicR = new JRadioButton("基础题", true);
        JRadioButton mediumR = new JRadioButton("中等题");
        JRadioButton advancedR = new JRadioButton("提高题");
        ButtonGroup diffGroup = new ButtonGroup();
        diffGroup.add(basicR); diffGroup.add(mediumR); diffGroup.add(advancedR);
        basicR.setBackground(Color.WHITE); basicR.setForeground(new Color(50, 50, 50));
        mediumR.setBackground(Color.WHITE); mediumR.setForeground(new Color(50, 50, 50));
        advancedR.setBackground(Color.WHITE); advancedR.setForeground(new Color(50, 50, 50));
        JPanel diffPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        diffPanel.setBackground(Color.WHITE);
        diffPanel.add(basicR); diffPanel.add(mediumR); diffPanel.add(advancedR);

        JPanel panel = new JPanel(new GridLayout(4, 2, 6, 8));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        panel.add(createLabel("题目内容"));   panel.add(contentField);
        panel.add(createLabel("分值"));       panel.add(scoreField);
        panel.add(createLabel("难度（必选）")); panel.add(diffPanel);
        panel.add(createLabel("参考答案"));   panel.add(new JScrollPane(refAnswerArea));

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

    private JPanel buildGradingTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 0));
        panel.setBackground(new Color(135, 206, 235));
        panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        // Left: exam list + student list
        JPanel leftPanel = new JPanel(new BorderLayout(0, 8));
        leftPanel.setBackground(new Color(135, 206, 235));
        leftPanel.setPreferredSize(new Dimension(320, 0));

        // Exam list
        JPanel examPanel = new JPanel(new BorderLayout(0, 4));
        examPanel.setBackground(Color.WHITE);
        examPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                "含简答题的考试",
                javax.swing.border.TitledBorder.LEFT,
                javax.swing.border.TitledBorder.TOP,
                new Font("微软雅黑", Font.BOLD, 13), new Color(50, 50, 50)));

        gradingExamModel = new DefaultTableModel(new String[]{"考试ID", "标题", "发布状态"}, 0);
        gradingExamTable = createStyledTable(gradingExamModel);
        examPanel.add(createTableScroll(gradingExamTable), BorderLayout.CENTER);

        JPanel examBtnBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        examBtnBar.setBackground(Color.WHITE);
        JButton refreshExams = createSecondaryButton("↻ 刷新");
        JButton selectExam = createPrimaryButton("→ 查看提交");
        refreshExams.addActionListener(e -> loadGradingExams());
        selectExam.addActionListener(e -> loadGradingStudents());
        examBtnBar.add(refreshExams); examBtnBar.add(selectExam);
        examPanel.add(examBtnBar, BorderLayout.SOUTH);
        leftPanel.add(examPanel, BorderLayout.NORTH);

        // Student list
        JPanel studentPanel = new JPanel(new BorderLayout(0, 4));
        studentPanel.setBackground(Color.WHITE);
        studentPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                "已提交学生",
                javax.swing.border.TitledBorder.LEFT,
                javax.swing.border.TitledBorder.TOP,
                new Font("微软雅黑", Font.BOLD, 13), new Color(50, 50, 50)));

        gradingStudentModel = new DefaultTableModel(new String[]{"学生", "批改状态"}, 0);
        gradingStudentTable = createStyledTable(gradingStudentModel);
        studentPanel.add(createTableScroll(gradingStudentTable), BorderLayout.CENTER);

        JPanel stuBtnBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        stuBtnBar.setBackground(Color.WHITE);
        JButton selectStudent = createPrimaryButton("→ 批改");
        selectStudent.addActionListener(e -> loadStudentEssays());
        stuBtnBar.add(selectStudent);
        studentPanel.add(stuBtnBar, BorderLayout.SOUTH);
        leftPanel.add(studentPanel, BorderLayout.CENTER);

        // Right: grading form
        gradingPanel = new JPanel();
        gradingPanel.setLayout(new BoxLayout(gradingPanel, BoxLayout.Y_AXIS));
        gradingPanel.setBackground(new Color(135, 206, 235));
        gradingPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JLabel placeholder = new JLabel("请先选择考试和学生");
        placeholder.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        placeholder.setForeground(new Color(100, 100, 100));
        gradingPanel.add(placeholder);

        gradingScroll = new JScrollPane(gradingPanel);
        gradingScroll.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                "简答题批改",
                javax.swing.border.TitledBorder.LEFT,
                javax.swing.border.TitledBorder.TOP,
                new Font("微软雅黑", Font.BOLD, 13), new Color(50, 50, 50)));
        gradingScroll.setBackground(new Color(135, 206, 235));
        gradingScroll.getViewport().setBackground(new Color(135, 206, 235));
        gradingScroll.getVerticalScrollBar().setUnitIncrement(16);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, gradingScroll);
        split.setDividerLocation(320);
        split.setResizeWeight(0.3);
        split.setBorder(null);
        panel.add(split, BorderLayout.CENTER);

        // Bottom buttons
        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        bottomBar.setBackground(new Color(135, 206, 235));
        JButton saveBtn = createPrimaryButton("✔ 保存评分");
        JButton publishBtn = createDangerButton("📢 发布成绩");
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
            card.setBackground(Color.WHITE);
            card.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(200, 200, 200)),
                    BorderFactory.createEmptyBorder(12, 16, 12, 16)));
            card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 400));
            card.setAlignmentX(Component.LEFT_ALIGNMENT);

            JLabel titleLabel = new JLabel("简答题 " + idx + "  (" + eq.getScore() + "分)");
            titleLabel.setFont(new Font("微软雅黑", Font.BOLD, 14));
            titleLabel.setForeground(new Color(70, 130, 200));
            titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            card.add(titleLabel);
            card.add(Box.createVerticalStrut(6));

            JLabel contentLabel = new JLabel("<html><b>题目：</b>" + eq.getContent() + "</html>");
            contentLabel.setFont(new Font("微软雅黑", Font.PLAIN, 13));
            contentLabel.setForeground(new Color(50, 50, 50));
            contentLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            card.add(contentLabel);
            card.add(Box.createVerticalStrut(4));

            JLabel refLabel = new JLabel("<html><b>参考答案：</b>" + eq.getReferenceAnswer() + "</html>");
            refLabel.setFont(new Font("微软雅黑", Font.PLAIN, 13));
            refLabel.setForeground(new Color(100, 100, 100));
            refLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            card.add(refLabel);
            card.add(Box.createVerticalStrut(6));

            JLabel ansLabel = new JLabel("<html><b>学生答案：</b></html>");
            ansLabel.setFont(new Font("微软雅黑", Font.PLAIN, 13));
            ansLabel.setForeground(new Color(50, 50, 50));
            ansLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            card.add(ansLabel);

            JTextArea ansArea = new JTextArea(studentAnswer);
            ansArea.setFont(new Font("微软雅黑", Font.PLAIN, 13));
            ansArea.setBackground(new Color(245, 245, 245));
            ansArea.setForeground(new Color(50, 50, 50));
            ansArea.setEditable(false);
            ansArea.setLineWrap(true);
            ansArea.setWrapStyleWord(true);
            ansArea.setRows(3);
            JScrollPane sp = new JScrollPane(ansArea);
            sp.setAlignmentX(Component.LEFT_ALIGNMENT);
            sp.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
            sp.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
            card.add(sp);
            card.add(Box.createVerticalStrut(6));

            JPanel scoreRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
            scoreRow.setBackground(Color.WHITE);
            scoreRow.setAlignmentX(Component.LEFT_ALIGNMENT);
            JLabel scoreHint = new JLabel("评分 (0~" + eq.getScore() + ")：");
            scoreHint.setFont(new Font("微软雅黑", Font.BOLD, 13));
            scoreHint.setForeground(new Color(50, 50, 50));
            scoreRow.add(scoreHint);
            JTextField scoreField = createStyledTextField();
            scoreField.setPreferredSize(new Dimension(80, 30));
            if (existingScore >= 0) scoreField.setText(String.valueOf(existingScore));
            scoreRow.add(scoreField);
            JLabel maxLabel = new JLabel(" / " + eq.getScore());
            maxLabel.setFont(new Font("微软雅黑", Font.PLAIN, 13));
            maxLabel.setForeground(new Color(100, 100, 100));
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
        JLabel placeholder = new JLabel("请选择学生进行批改");
        placeholder.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        placeholder.setForeground(new Color(100, 100, 100));
        gradingPanel.add(placeholder);
        gradingPanel.revalidate();
        gradingPanel.repaint();
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

    private JButton createDangerButton(String text) {
        JButton btn = new JButton(text);
        btn.setBackground(new Color(220, 80, 80));
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("微软雅黑", Font.BOLD, 13));
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private JTextField createStyledTextField() {
        JTextField field = new JTextField();
        field.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        field.setBackground(Color.WHITE);
        field.setForeground(new Color(50, 50, 50));
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)));
        return field;
    }

    private JLabel createLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("微软雅黑", Font.BOLD, 13));
        l.setForeground(new Color(50, 50, 50));
        return l;
    }

    private JLabel iconLabel(String emoji) {
        JLabel l = new JLabel(emoji);
        l.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 22));
        return l;
    }

    private JLabel userTag() {
        JLabel l = new JLabel(username);
        l.setFont(new Font("微软雅黑", Font.BOLD, 12));
        l.setForeground(Color.WHITE);
        l.setBorder(BorderFactory.createEmptyBorder(2, 10, 2, 10));
        return l;
    }

    private void setStatus(String msg) {
        SwingUtilities.invokeLater(() -> statusLabel.setText(msg));
    }

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

    private class TeacherResponseListener implements ResponseListener {
        @Override
        public void onResponse(Response response) {
            lastResponse = response;
            if (latch != null) latch.countDown();
        }
    }
}