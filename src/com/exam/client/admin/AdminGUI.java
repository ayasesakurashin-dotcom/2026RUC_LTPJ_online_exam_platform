package com.exam.client.admin;

import com.exam.client.common.ModernTheme;
import com.exam.client.common.NetworkClient;
import com.exam.client.common.ResponseListener;
import com.exam.common.model.ExamResult;
import com.exam.common.protocol.MessageType;
import com.exam.common.protocol.Request;
import com.exam.common.protocol.Response;
import java.awt.*;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class AdminGUI extends JFrame {

    private final NetworkClient client;
    private final String username;

    private JTabbedPane tabbedPane;
    private DefaultTableModel userModel, examModel;
    private JTable userTable, examTable;
    private JLabel statusLabel;

    // 成绩统计 tab (二级查询)
    private DefaultTableModel finishedExamModel, scoreDetailModel;
    private JTable finishedExamTable, scoreDetailTable;
    private JLabel statsLabel;

    private volatile Response lastResponse;
    private CountDownLatch latch;

    public AdminGUI(NetworkClient client, String username) {
        this.client = client;
        this.username = username;
        this.client.setListener(new AdminResponseListener());
        initUI();
        refreshData();
    }

    private void initUI() {
        setTitle("在线考试系统 · 管理员  " + username);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1050, 750);
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(ModernTheme.bg());

        // ---- Header ----
        JPanel header = ModernTheme.headerBar();
        header.add(ModernTheme.headerLeft("🛡", "管理员控制台"), BorderLayout.WEST);

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
        tabbedPane.addTab("  用户管理  ", buildUserTab());
        tabbedPane.addTab("  考试管理  ", buildExamTab());
        tabbedPane.addTab("  成绩统计  ", buildScoreTab());
        root.add(tabbedPane, BorderLayout.CENTER);
        add(root);
    }

    // ==================== 用户管理 Tab ====================
    private JPanel buildUserTab() {
        JPanel panel = ModernTheme.insetPanel(16, 16, 16, 16);
        panel.setLayout(new BorderLayout(0, 10));

        userModel = new DefaultTableModel(new String[]{"用户名", "角色"}, 0);
        userTable = ModernTheme.table(userModel);
        panel.add(ModernTheme.tableScroll(userTable), BorderLayout.CENTER);

        JPanel btnBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        btnBar.setBackground(ModernTheme.bg());
        JButton addBtn = ModernTheme.primaryButton("+ 添加用户");
        JButton delBtn = ModernTheme.dangerButton("X 删除用户");
        JButton refreshBtn = ModernTheme.secondaryButton("刷新");
        addBtn.addActionListener(e -> showAddUserDialog());
        delBtn.addActionListener(e -> deleteSelectedUser());
        refreshBtn.addActionListener(e -> refreshData());
        btnBar.add(addBtn); btnBar.add(delBtn); btnBar.add(refreshBtn);
        panel.add(btnBar, BorderLayout.SOUTH);

        return panel;
    }

    // ==================== 考试管理 Tab ====================
    private JPanel buildExamTab() {
        JPanel panel = ModernTheme.insetPanel(16, 16, 16, 16);
        panel.setLayout(new BorderLayout(0, 10));

        examModel = new DefaultTableModel(
                new String[]{"考试ID", "标题", "创建者", "时长(秒)", "状态", "题数"}, 0);
        examTable = ModernTheme.table(examModel);
        panel.add(ModernTheme.tableScroll(examTable), BorderLayout.CENTER);

        JPanel btnBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        btnBar.setBackground(ModernTheme.bg());
        JButton refreshBtn = ModernTheme.secondaryButton("刷新");
        refreshBtn.addActionListener(e -> refreshData());
        btnBar.add(refreshBtn);
        panel.add(btnBar, BorderLayout.SOUTH);

        return panel;
    }

    // ==================== 成绩统计 Tab ====================
    private JPanel buildScoreTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 0));
        panel.setBackground(ModernTheme.bg());
        panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        // 上半：已完成的考试列表
        JPanel topPanel = new JPanel(new BorderLayout(0, 6));
        topPanel.setBackground(ModernTheme.surface());
        topPanel.setBorder(ModernTheme.titledBorder("已完成的考试"));

        finishedExamModel = new DefaultTableModel(
                new String[]{"考试ID", "标题", "创建者", "总分", "参加人数"}, 0);
        finishedExamTable = ModernTheme.table(finishedExamModel);
        topPanel.add(ModernTheme.tableScroll(finishedExamTable), BorderLayout.CENTER);

        JPanel topBtnBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        topBtnBar.setBackground(ModernTheme.surface());
        JButton loadFinished = ModernTheme.secondaryButton("刷新");
        JButton viewScores = ModernTheme.primaryButton("→ 查看成绩");
        loadFinished.addActionListener(e -> loadFinishedExams());
        viewScores.addActionListener(e -> viewSelectedExamScores());
        topBtnBar.add(loadFinished); topBtnBar.add(viewScores);
        topPanel.add(topBtnBar, BorderLayout.SOUTH);

        // 下半：成绩明细
        JPanel bottomPanel = new JPanel(new BorderLayout(0, 4));
        bottomPanel.setBackground(ModernTheme.surface());
        bottomPanel.setBorder(ModernTheme.titledBorder("成绩明细"));

        scoreDetailModel = new DefaultTableModel(
                new String[]{"学生", "得分", "总分", "总正确率",
                        "基础题正确率", "中等题正确率", "提高题正确率", "提交时间"}, 0);
        scoreDetailTable = ModernTheme.table(scoreDetailModel);
        bottomPanel.add(ModernTheme.tableScroll(scoreDetailTable), BorderLayout.CENTER);

        // 统计数据条
        statsLabel = new JLabel(" ");
        statsLabel.setFont(ModernTheme.BODY_FONT);
        statsLabel.setForeground(ModernTheme.text());
        statsLabel.setBackground(ModernTheme.elevated());
        statsLabel.setOpaque(true);
        statsLabel.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));
        bottomPanel.add(statsLabel, BorderLayout.SOUTH);

        JSplitPane split = ModernTheme.splitPane(JSplitPane.VERTICAL_SPLIT, topPanel, bottomPanel, 0.40);
        split.setDividerLocation(240);
        panel.add(split, BorderLayout.CENTER);

        return panel;
    }

    // ==================== 数据加载 ====================
    private void refreshData() {
        setStatus("加载中...");
        loadUsers();
        loadExams();
        loadFinishedExams();
        setStatus("就绪");
    }

    private Response sendAndWait(Request request) {
        latch = new CountDownLatch(1);
        lastResponse = null;
        try {
            client.send(request);
            latch.await(10, TimeUnit.SECONDS);
        } catch (Exception ignored) {}
        return lastResponse;
    }

    private void loadUsers() {
        Response resp = sendAndWait(new Request(MessageType.GET_USERS, null));
        if (resp != null && "OK".equals(resp.getStatus())) {
            @SuppressWarnings("unchecked")
            List<Map<String, String>> users = (List<Map<String, String>>) resp.getData();
            SwingUtilities.invokeLater(() -> {
                userModel.setRowCount(0);
                for (Map<String, String> u : users) {
                    userModel.addRow(new Object[]{u.get("username"), u.get("role")});
                }
            });
        }
    }

    private void loadExams() {
        Map<String, String> data = new HashMap<>();
        data.put("username", username);
        data.put("role", "ADMIN");
        Response resp = sendAndWait(new Request(MessageType.GET_EXAMS, (Serializable) data));
        if (resp != null && "OK".equals(resp.getStatus())) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> exams = (List<Map<String, Object>>) resp.getData();
            SwingUtilities.invokeLater(() -> {
                examModel.setRowCount(0);
                for (Map<String, Object> e : exams) {
                    examModel.addRow(new Object[]{
                            e.get("id"), e.get("title"), e.get("teacherId"),
                            e.get("duration"), e.get("status"), e.get("questionCount")
                    });
                }
            });
        }
    }

    private void loadFinishedExams() {
        Map<String, String> data = new HashMap<>();
        data.put("username", username);
        data.put("role", "ADMIN");
        Response resp = sendAndWait(new Request(MessageType.GET_EXAMS, (Serializable) data));
        if (resp != null && "OK".equals(resp.getStatus())) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> all = (List<Map<String, Object>>) resp.getData();
            SwingUtilities.invokeLater(() -> {
                finishedExamModel.setRowCount(0);
                for (Map<String, Object> e : all) {
                    if ("FINISHED".equals(e.get("status"))) {
                        finishedExamModel.addRow(new Object[]{
                                e.get("id"), e.get("title"), e.get("teacherId"),
                                e.get("totalScore"), e.get("joinedCount")
                        });
                    }
                }
                scoreDetailModel.setRowCount(0);
                statsLabel.setText(" ");
            });
        }
    }

    private void viewSelectedExamScores() {
        int row = finishedExamTable.getSelectedRow();
        if (row < 0) { setStatus("请先选择一场已完成考试"); return; }
        String examId = (String) finishedExamModel.getValueAt(row, 0);

        Map<String, String> data = new HashMap<>();
        data.put("username", username);
        data.put("role", "ADMIN");
        Response resp = sendAndWait(new Request(MessageType.GET_SCORES, (Serializable) data));
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
                                new java.util.Date(r.getSubmitTime())
                        });
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

    // ==================== 添加用户对话框 ====================
    private void showAddUserDialog() {
        JDialog dialog = new JDialog(this, "添加用户", true);
        dialog.setSize(450, 340);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());
        dialog.getContentPane().setBackground(ModernTheme.bg());

        // 标题栏
        dialog.add(ModernTheme.dialogHeader("添加新用户", ModernTheme.ADMIN_ACCENT), BorderLayout.NORTH);

        // 表单面板
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(ModernTheme.bg());
        formPanel.setBorder(BorderFactory.createEmptyBorder(24, 35, 16, 35));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 12, 10, 12);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JTextField userField = ModernTheme.textField(20);
        JPasswordField passField = ModernTheme.passwordField(20);
        JComboBox<String> roleBox = ModernTheme.comboBox(new String[]{"学生", "教师"});

        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(ModernTheme.label("用户名："), gbc);
        gbc.gridx = 1;
        formPanel.add(userField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        formPanel.add(ModernTheme.label("密码："), gbc);
        gbc.gridx = 1;
        formPanel.add(passField, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        formPanel.add(ModernTheme.label("角色："), gbc);
        gbc.gridx = 1;
        formPanel.add(roleBox, gbc);

        dialog.add(formPanel, BorderLayout.CENTER);

        // 按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 25, 16));
        buttonPanel.setBackground(ModernTheme.bg());

        JButton okBtn = ModernTheme.primaryButton("确定");
        JButton cancelBtn = ModernTheme.secondaryButton("取消");

        okBtn.addActionListener(e -> {
            String nu = userField.getText().trim();
            String np = new String(passField.getPassword()).trim();
            String role = "学生".equals(roleBox.getSelectedItem()) ? "STUDENT" : "TEACHER";
            if (nu.isEmpty() || np.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "用户名和密码不能为空");
                return;
            }
            Map<String, String> d = new HashMap<>();
            d.put("username", nu);
            d.put("password", np);
            d.put("role", role);
            Response resp = sendAndWait(new Request(MessageType.ADD_USER, (Serializable) d));
            if (resp != null && "OK".equals(resp.getStatus())) {
                setStatus("用户 " + nu + " 已添加");
                loadUsers();
                dialog.dispose();
            } else {
                setStatus("添加失败");
                JOptionPane.showMessageDialog(dialog, "添加失败：" + (resp != null ? resp.getMessage() : "未知错误"));
            }
        });

        cancelBtn.addActionListener(e -> dialog.dispose());

        buttonPanel.add(okBtn);
        buttonPanel.add(cancelBtn);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        dialog.setVisible(true);
    }

    // ==================== 删除用户对话框 ====================
    private void deleteSelectedUser() {
        int row = userTable.getSelectedRow();
        if (row < 0) { setStatus("请先选择一个用户"); return; }
        String uname = (String) userModel.getValueAt(row, 0);
        if ("admin".equals(uname)) { setStatus("不能删除管理员"); return; }

        JDialog confirmDialog = new JDialog(this, "确认删除", true);
        confirmDialog.setSize(400, 200);
        confirmDialog.setLocationRelativeTo(this);
        confirmDialog.setLayout(new BorderLayout());
        confirmDialog.getContentPane().setBackground(ModernTheme.bg());

        confirmDialog.add(ModernTheme.dialogHeader("⚠️ 确认删除", ModernTheme.ERROR), BorderLayout.NORTH);

        JPanel messagePanel = new JPanel();
        messagePanel.setBackground(ModernTheme.bg());
        messagePanel.setBorder(BorderFactory.createEmptyBorder(40, 25, 20, 25));
        JLabel messageLabel = new JLabel("确定要删除用户 「" + uname + "」 吗？");
        messageLabel.setFont(ModernTheme.BODY_FONT);
        messageLabel.setForeground(ModernTheme.text());
        messagePanel.add(messageLabel);
        confirmDialog.add(messagePanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 25, 20));
        buttonPanel.setBackground(ModernTheme.bg());

        JButton yesBtn = ModernTheme.dangerButton("确定删除");
        JButton noBtn = ModernTheme.secondaryButton("取消");

        yesBtn.addActionListener(e -> {
            Response resp = sendAndWait(new Request(MessageType.DELETE_USER, uname));
            if (resp != null && "OK".equals(resp.getStatus())) {
                setStatus("用户 " + uname + " 已删除");
                loadUsers();
            } else {
                setStatus("删除失败");
                JOptionPane.showMessageDialog(confirmDialog, "删除失败：" + (resp != null ? resp.getMessage() : "未知错误"));
            }
            confirmDialog.dispose();
        });

        noBtn.addActionListener(e -> confirmDialog.dispose());

        buttonPanel.add(yesBtn);
        buttonPanel.add(noBtn);
        confirmDialog.add(buttonPanel, BorderLayout.SOUTH);

        confirmDialog.setVisible(true);
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

    private String diffRate(int correct, int total) {
        if (total == 0) return "0% (0/0)";
        return String.format("%.1f%% (%d/%d)", 100.0 * correct / total, correct, total);
    }

    private class AdminResponseListener implements ResponseListener {
        @Override
        public void onResponse(Response response) {
            lastResponse = response;
            if (latch != null) latch.countDown();
        }
    }
}
