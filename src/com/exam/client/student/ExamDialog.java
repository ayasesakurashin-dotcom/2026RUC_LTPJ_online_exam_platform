package com.exam.client.student;

import com.exam.client.common.ModernTheme;
import com.exam.common.model.*;
import com.exam.common.protocol.MessageType;
import com.exam.common.protocol.Request;
import com.exam.common.protocol.Response;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.Serializable;
import java.util.*;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class ExamDialog extends JDialog {

    private final StudentGUI parent;
    private final String examId;
    private final String studentId;
    private final List<Question> questions;
    private final List<JComponent> answerComponents = new ArrayList<>();

    private javax.swing.Timer countdownTimer;
    private int remainingSeconds;
    private JLabel timerLabel;
    private JProgressBar progressBar;

    private volatile boolean submitted = false;
    private Response submitResponse;
    private CountDownLatch submitLatch;

    public ExamDialog(StudentGUI parent, String examId, String title, String studentId,
                      List<Question> questions, int remainingSeconds) {
        super(parent, true);
        this.parent = parent;
        this.examId = examId;
        this.studentId = studentId;
        this.questions = questions;
        this.remainingSeconds = remainingSeconds;

        initUI(title);
        startCountdown();

        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) {
                if (!submitted) {
                    int r = JOptionPane.showConfirmDialog(ExamDialog.this,
                            "确定要退出吗？试卷将自动提交。", "确认", JOptionPane.YES_NO_OPTION);
                    if (r == JOptionPane.YES_OPTION) submitExam();
                }
            }
        });

        setSize(900, 680);
        setLocationRelativeTo(parent);
        getContentPane().setBackground(ModernTheme.bg());
    }

    private void initUI(String title) {
        setTitle("考试: " + title);
        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(ModernTheme.bg());

        // ---- 顶部栏：倒计时 + 进度 ----
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(new Color(0xFF, 0xFF, 0xFF));
        topBar.setBorder(BorderFactory.createCompoundBorder(
                new ModernTheme.ShadowBorder(ModernTheme.border(), 0, 1),
                BorderFactory.createEmptyBorder(14, 20, 14, 20)));

        JPanel timerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        timerPanel.setBackground(new Color(0xFF, 0xFF, 0xFF));

        JLabel timerIcon = new JLabel("⏱");
        timerIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 20));
        timerPanel.add(timerIcon);

        JLabel timerLabelHint = new JLabel("剩余时间");
        timerLabelHint.setFont(new Font("Microsoft YaHei", Font.BOLD, 13));
        timerLabelHint.setForeground(ModernTheme.subtext());
        timerPanel.add(timerLabelHint);

        timerLabel = new JLabel(formatTime(remainingSeconds));
        timerLabel.setFont(new Font("JetBrains Mono", Font.BOLD, 28));
        timerLabel.setForeground(ModernTheme.ACCENT);
        timerPanel.add(timerLabel);

        topBar.add(timerPanel, BorderLayout.WEST);

        // 进度条
        progressBar = new JProgressBar(0, questions.size());
        progressBar.setValue(0);
        progressBar.setStringPainted(false);
        progressBar.setPreferredSize(new Dimension(200, 8));
        progressBar.setBackground(ModernTheme.border());
        progressBar.setForeground(ModernTheme.ACCENT);
        progressBar.setBorder(null);
        JPanel progressPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        progressPanel.setBackground(new Color(0xFF, 0xFF, 0xFF));
        progressPanel.add(progressBar);
        topBar.add(progressPanel, BorderLayout.EAST);

        root.add(topBar, BorderLayout.NORTH);

        // ---- 中间：题目列表 ----
        JPanel questionPanel = new JPanel();
        questionPanel.setLayout(new BoxLayout(questionPanel, BoxLayout.Y_AXIS));
        questionPanel.setBackground(ModernTheme.bg());
        questionPanel.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));

        for (int i = 0; i < questions.size(); i++) {
            questionPanel.add(buildQuestionCard(i, questions.get(i)));
            questionPanel.add(Box.createVerticalStrut(10));
        }

        JScrollPane scroll = new JScrollPane(questionPanel);
        scroll.setBorder(null);
        scroll.setBackground(ModernTheme.bg());
        scroll.getViewport().setBackground(ModernTheme.bg());
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        ModernTheme.styleScrollBar(scroll);
        root.add(scroll, BorderLayout.CENTER);

        // ---- 底部：提交按钮 ----
        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        bottomBar.setBackground(new Color(0xFF, 0xFF, 0xFF));
        bottomBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, ModernTheme.border()),
                BorderFactory.createEmptyBorder(16, 20, 16, 20)));

        JButton submitBtn = ModernTheme.primaryButton("✔  提 交 试 卷");
        submitBtn.setFont(new Font("Microsoft YaHei", Font.BOLD, 16));
        submitBtn.setPreferredSize(new Dimension(240, 48));
        submitBtn.addActionListener(e -> submitExam());
        bottomBar.add(submitBtn);

        root.add(bottomBar, BorderLayout.SOUTH);
        add(root);
    }

    private JPanel buildQuestionCard(int index, Question q) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(ModernTheme.surface());

        // 左侧难度色条 + 圆角边框
        String diff = q.getDifficulty();
        Color diffColor = ModernTheme.diffColor(diff);
        card.setBorder(BorderFactory.createCompoundBorder(
                new ModernTheme.ShadowBorder(ModernTheme.border(), 8, 1),
                BorderFactory.createEmptyBorder(0, 6, 0, 0)));

        String typeTag = (q instanceof ChoiceQuestion) ? "选择" :
                         (q instanceof EssayQuestion) ? "简答" : "填空";
        int maxH = (q instanceof EssayQuestion) ? 360 : 260;
        card.setMaximumSize(new Dimension(710, maxH));

        // 题头
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(ModernTheme.surface());
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 4, 0, 0, diffColor),
                BorderFactory.createEmptyBorder(12, 12, 8, 16)));

        String diffText = ModernTheme.diffLabel(diff);
        JLabel numLabel = new JLabel("第 " + (index + 1) + " 题  [" + typeTag + " · " + diffText + "]");
        numLabel.setFont(new Font("Microsoft YaHei", Font.BOLD, 14));
        numLabel.setForeground(diffColor);

        JLabel scoreLabel = new JLabel(q.getScore() + " 分");
        scoreLabel.setFont(ModernTheme.SMALL_FONT);
        scoreLabel.setForeground(ModernTheme.subtext());

        header.add(numLabel, BorderLayout.WEST);
        header.add(scoreLabel, BorderLayout.EAST);
        card.add(header);

        // 题目内容
        JLabel content = new JLabel("<html><div style='padding:4px 0'>" + q.getContent() + "</div></html>");
        content.setFont(new Font("Microsoft YaHei", Font.PLAIN, 15));
        content.setForeground(ModernTheme.text());
        content.setBorder(BorderFactory.createEmptyBorder(0, 16, 8, 16));
        card.add(content);

        // 答题区
        JPanel answerArea = new JPanel();
        answerArea.setBackground(ModernTheme.surface());
        answerArea.setBorder(BorderFactory.createEmptyBorder(4, 16, 14, 16));

        if (q instanceof ChoiceQuestion) {
            answerArea.setLayout(new GridLayout(0, 1, 4, 4));
            ChoiceQuestion cq = (ChoiceQuestion) q;
            ButtonGroup group = new ButtonGroup();
            for (int j = 0; j < cq.getOptions().size(); j++) {
                JRadioButton rb = new JRadioButton(cq.getOptions().get(j));
                rb.setFont(ModernTheme.BODY_FONT);
                rb.setForeground(ModernTheme.text());
                rb.setBackground(ModernTheme.surface());
                rb.setActionCommand(String.valueOf(j));
                rb.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                group.add(rb);
                answerArea.add(rb);
            }
            answerComponents.add(answerArea);
        } else if (q instanceof EssayQuestion) {
            answerArea.setLayout(new BorderLayout(0, 4));
            JLabel hint = new JLabel("请输入你的答案：");
            hint.setFont(ModernTheme.BODY_FONT);
            hint.setForeground(ModernTheme.subtext());
            answerArea.add(hint, BorderLayout.NORTH);
            JTextArea ta = new JTextArea(5, 40);
            ta.setFont(ModernTheme.BODY_FONT);
            ta.setBackground(ModernTheme.surface());
            ta.setForeground(ModernTheme.text());
            ta.setLineWrap(true);
            ta.setWrapStyleWord(true);
            JScrollPane sp = new JScrollPane(ta);
            sp.setBorder(new ModernTheme.RoundedBorder(ModernTheme.border(), 6, 1));
            answerArea.add(sp, BorderLayout.CENTER);
            answerComponents.add(ta);
        } else if (q instanceof BlankQuestion) {
            answerArea.setLayout(new FlowLayout(FlowLayout.LEFT, 8, 0));
            JLabel hint = new JLabel("答案：");
            hint.setFont(ModernTheme.BODY_FONT);
            hint.setForeground(ModernTheme.subtext());
            answerArea.add(hint);
            JTextField tf = ModernTheme.textField(24);
            answerArea.add(tf);
            answerComponents.add(tf);
        }

        card.add(answerArea);
        return card;
    }

    // ==================== 倒计时 ====================
    private void startCountdown() {
        countdownTimer = new javax.swing.Timer(1000, e -> {
            remainingSeconds--;
            timerLabel.setText(formatTime(remainingSeconds));
            if (remainingSeconds <= 60) {
                timerLabel.setForeground(ModernTheme.ERROR);
            } else if (remainingSeconds <= 180) {
                timerLabel.setForeground(ModernTheme.WARNING);
            }
            if (remainingSeconds <= 0) {
                countdownTimer.stop();
                autoSubmit();
            }
        });
        countdownTimer.start();
    }

    private String formatTime(int seconds) {
        int m = seconds / 60, s = seconds % 60;
        return String.format("%02d:%02d", m, s);
    }

    private void autoSubmit() {
        if (submitted) return;
        JOptionPane.showMessageDialog(this, "考试时间到！系统将自动提交试卷。",
                "时间到", JOptionPane.WARNING_MESSAGE);
        submitExam();
    }

    // ==================== 提交 ====================
    private void submitExam() {
        if (submitted) return;
        submitted = true;
        if (countdownTimer != null) countdownTimer.stop();

        Map<String, String> answerMap = collectAnswers();

        new Thread(() -> {
            try {
                for (Map.Entry<String, String> e : answerMap.entrySet()) {
                    Map<String, String> d = new HashMap<>();
                    d.put("examId", examId); d.put("studentId", studentId);
                    d.put("questionId", e.getKey()); d.put("answer", e.getValue());
                    parent.sendRequest(new Request(MessageType.SUBMIT_ANSWER, (Serializable) d));
                    Thread.sleep(50);
                }

                Map<String, String> sub = new HashMap<>();
                sub.put("examId", examId); sub.put("studentId", studentId);
                submitLatch = new CountDownLatch(1);
                parent.sendRequest(new Request(MessageType.SUBMIT_EXAM, (Serializable) sub));

                if (submitLatch.await(30, TimeUnit.SECONDS)) {
                    ExamDialog.this.dispose();
                    boolean hasEssay = questions.stream().anyMatch(qq -> qq instanceof EssayQuestion);
                    String msg = hasEssay
                            ? "交卷成功！本次考试含简答题，待教师批改后发布成绩。"
                            : "交卷成功！考试结束后可在\"我的成绩\"中查看分数。";
                    SwingUtilities.invokeLater(() ->
                        JOptionPane.showMessageDialog(parent, msg,
                                "交卷成功", JOptionPane.INFORMATION_MESSAGE));
                } else {
                    ExamDialog.this.dispose();
                    SwingUtilities.invokeLater(() ->
                            JOptionPane.showMessageDialog(parent, "提交超时", "错误", JOptionPane.ERROR_MESSAGE));
                }
            } catch (Exception ex) {
                ExamDialog.this.dispose();
                SwingUtilities.invokeLater(() ->
                        JOptionPane.showMessageDialog(parent, "提交失败: " + ex.getMessage()));
            }
        }, "Exam-Submit").start();
    }

    private Map<String, String> collectAnswers() {
        Map<String, String> ans = new HashMap<>();
        for (int i = 0; i < questions.size(); i++) {
            Question q = questions.get(i);
            JComponent comp = answerComponents.get(i);
            String answer = "";
            if (q instanceof ChoiceQuestion && comp instanceof JPanel) {
                for (Component c : ((JPanel) comp).getComponents()) {
                    if (c instanceof JRadioButton && ((JRadioButton) c).isSelected()) {
                        answer = ((JRadioButton) c).getActionCommand();
                    }
                }
            } else if (q instanceof EssayQuestion && comp instanceof JTextArea) {
                answer = ((JTextArea) comp).getText().trim();
            } else if (q instanceof BlankQuestion && comp instanceof JTextField) {
                answer = ((JTextField) comp).getText().trim();
            }
            ans.put(q.getId(), answer);
        }
        return ans;
    }

    public void setSubmitResponse(Response response) {
        this.submitResponse = response;
        if (submitLatch != null) submitLatch.countDown();
    }

}
