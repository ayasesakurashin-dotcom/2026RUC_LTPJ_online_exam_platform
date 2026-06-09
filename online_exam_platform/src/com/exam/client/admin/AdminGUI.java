package com.exam.client.admin;

import com.exam.client.common.ModernTheme;
import com.exam.client.common.NetworkClient;
import com.exam.client.common.ResponseListener;
import com.exam.common.model.ExamResult;
import com.exam.common.protocol.MessageType;
import com.exam.common.protocol.Request;
import com.exam.common.protocol.Response;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

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
        ModernTheme.install();
        this.client.setListener(new AdminResponseListener());
        initUI();
        ModernTheme.applyToFrame(this, getContentPane());
        refreshData();
    }

    private void initUI() {
        setTitle("在线考试系统 · 管理员  " + username);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1050, 750);
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(ModernTheme.bg());

        // ---- 顶部栏 ----
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(ModernTheme.bgDarker());
        header.setBorder(BorderFactory.createEmptyBorder(14, 20, 14, 20));

        JPanel headerLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        headerLeft.setBackground(ModernTheme.bgDarker());
        JLabel icon = new JLabel("🛡");
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 22));
        headerLeft.add(icon);
        JLabel title = new JLabel("管理员控制台");
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

        JLabel userTag = new JLabel(username);
        userTag.setFont(new Font("Microsoft YaHei", Font.BOLD, 12));
        userTag.setForeground(ModernTheme.ACCENT);
        userTag.setBorder(BorderFactory.createEmptyBorder(2, 10, 2, 10));
        headerRight.add(userTag);

        headerRight.add(logoutButton());
        headerRight.add(ModernTheme.themeToggle(this));
        header.add(headerRight, BorderLayout.EAST);

        root.add(header, BorderLayout.NORTH);

        // ---- 标签页 ----
        tabbedPane = new JTabbedPane(JTabbedPane.TOP);
        tabbedPane.setBackground(ModernTheme.bg());
        tabbedPane.setForeground(ModernTheme.text());
        tabbedPane.setFont(ModernTheme.TAB_FONT);

        tabbedPane.addTab("  用户管理  ", buildUserTab());
        tabbedPane.addTab("  考试管理  ", buildExamTab());
        tabbedPane.addTab("  成绩统计  ", buildScoreTab());

        root.add(tabbedPane, BorderLayout.CENTER);
        add(root);
    }

    // ==================== 用户管理 Tab ====================
    private JPanel buildUserTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBackground(ModernTheme.bg());
        panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        userModel = new DefaultTableModel(new String[]{"用户名", "角色"}, 0);
        userTable = ModernTheme.table(userModel);
        panel.add(ModernTheme.tableScroll(userTable), BorderLayout.CENTER);

        JPanel btnBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        btnBar.setBackground(ModernTheme.bg());
        JButton addBtn = ModernTheme.primaryButton("＋ 添加用户");
        JButton delBtn = ModernTheme.dangerButton("✕ 删除用户");
        JButton refreshBtn = ModernTheme.secondaryButton("↻ 刷新");
        addBtn.addActionListener(e -> showAddUserDialog());
        delBtn.addActionListener(e -> deleteSelectedUser());
        refreshBtn.addActionListener(e -> refreshData());
        btnBar.add(addBtn); btnBar.add(delBtn); btnBar.add(refreshBtn);
        panel.add(btnBar, BorderLayout.SOUTH);

        return panel;
    }

    // ==================== 考试管理 Tab ====================
    private JPanel buildExamTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBackground(ModernTheme.bg());
        panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        examModel = new DefaultTableModel(
                new String[]{"考试ID", "标题", "创建者", "时长(秒)", "状态", "题数"}, 0);
        examTable = ModernTheme.table(examModel);
        panel.add(ModernTheme.tableScroll(examTable), BorderLayout.CENTER);

        JPanel btnBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        btnBar.setBackground(ModernTheme.bg());
        JButton refreshBtn = ModernTheme.secondaryButton("↻ 刷新");
        refreshBtn.addActionListener(e -> refreshData());
        btnBar.add(refreshBtn);
        panel.add(btnBar, BorderLayout.SOUTH);

        return panel;
    }

    // ==================== 成绩统计 Tab（选择考试 → 查看成绩 + 统计）====================
    private JPanel buildScoreTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 0));
        panel.setBackground(ModernTheme.bg());
        panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        // ---- 上半：已完成的考试列表（所有教师） ----
        JPanel topPanel = new JPanel(new BorderLayout(0, 6));
        topPanel.setBackground(ModernTheme.bg());
        topPanel.setBorder(BorderFactory.createTitledBorder(
                new ModernTheme.RoundedBorder(ModernTheme.border(), 8, 1),
                "已完成的考试", javax.swing.border.TitledBorder.LEFT,
                javax.swing.border.TitledBorder.TOP,
                ModernTheme.SUBHEADING_FONT, ModernTheme.text()));

        finishedExamModel = new DefaultTableModel(
                new String[]{"考试ID", "标题", "创建者", "总分", "参加人数"}, 0);
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

        // ---- 下半：成绩明细 + 统计 ----
        JPanel bottomPanel = new JPanel(new BorderLayout(0, 4));
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

        // 统计数据条
        statsLabel = new JLabel(" ");
        statsLabel.setFont(new Font("Microsoft YaHei", Font.PLAIN, 13));
        statsLabel.setForeground(ModernTheme.text());
        statsLabel.setBackground(ModernTheme.bgDarker());
        statsLabel.setOpaque(true);
        statsLabel.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));
        bottomPanel.add(statsLabel, BorderLayout.SOUTH);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, topPanel, bottomPanel);
        split.setDividerLocation(240);
        split.setResizeWeight(0.40);
        split.setBorder(null);
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

    /** 加载所有已完成的考试（不限教师） */
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

    /** 查看选中考试的成绩明细与统计 */
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

    // ==================== 用户操作 ====================
    private void showAddUserDialog() {
        JTextField userField = ModernTheme.textField(12);
        JPasswordField passField = ModernTheme.passwordField(12);
        JComboBox<String> roleBox = new JComboBox<>(new String[]{"学生", "教师"});
        roleBox.setFont(ModernTheme.BODY_FONT);

        JPanel panel = new JPanel(new GridLayout(3, 2, 8, 8));
        panel.setBackground(ModernTheme.surface());
        panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        panel.add(label("用户名:")); panel.add(userField);
        panel.add(label("密  码:")); panel.add(passField);
        panel.add(label("角  色:")); panel.add(roleBox);

        int result = JOptionPane.showConfirmDialog(this, panel, "添加用户",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return;

        String nu = userField.getText().trim();
        String np = new String(passField.getPassword()).trim();
        String role = "学生".equals(roleBox.getSelectedItem()) ? "STUDENT" : "TEACHER";
        if (nu.isEmpty() || np.isEmpty()) {
            JOptionPane.showMessageDialog(this, "用户名和密码不能为空");
            return;
        }

        Map<String, String> d = new HashMap<>();
        d.put("username", nu); d.put("password", np); d.put("role", role);
        Response resp = sendAndWait(new Request(MessageType.ADD_USER, (Serializable) d));
        if (resp != null && "OK".equals(resp.getStatus())) {
            setStatus("用户 " + nu + " 已添加");
            loadUsers();
        } else {
            setStatus("添加失败");
        }
    }

    private void deleteSelectedUser() {
        int row = userTable.getSelectedRow();
        if (row < 0) { setStatus("请先选择一个用户"); return; }
        String uname = (String) userModel.getValueAt(row, 0);
        if ("admin".equals(uname)) { setStatus("不能删除管理员"); return; }

        int cfm = JOptionPane.showConfirmDialog(this,
                "确定删除用户 「" + uname + "」 吗？", "确认删除",
                JOptionPane.YES_NO_OPTION);
        if (cfm != JOptionPane.YES_OPTION) return;

        Response resp = sendAndWait(new Request(MessageType.DELETE_USER, uname));
        if (resp != null && "OK".equals(resp.getStatus())) {
            setStatus("用户 " + uname + " 已删除");
            loadUsers();
        }
    }

    private JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Microsoft YaHei", Font.BOLD, 13));
        l.setForeground(ModernTheme.text());
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
