package com.exam.client.admin;

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
        getContentPane().setBackground(new Color(135, 206, 235));
        
        setTitle("在线考试系统 · 管理员  " + username);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1050, 750);
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(new Color(135, 206, 235));

        // 顶部栏
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(100, 180, 220));
        header.setBorder(BorderFactory.createEmptyBorder(14, 20, 14, 20));

        JPanel headerLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        headerLeft.setBackground(new Color(100, 180, 220));
        JLabel icon = new JLabel("🛡");
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 22));
        headerLeft.add(icon);
        JLabel title = new JLabel("管理员控制台");
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

        // 标签页
        tabbedPane = new JTabbedPane(JTabbedPane.TOP);
        tabbedPane.setBackground(new Color(135, 206, 235));
        tabbedPane.setForeground(new Color(50, 50, 50));
        tabbedPane.setFont(new Font("微软雅黑", Font.PLAIN, 13));

        tabbedPane.addTab("  用户管理  ", buildUserTab());
        tabbedPane.addTab("  考试管理  ", buildExamTab());
        tabbedPane.addTab("  成绩统计  ", buildScoreTab());

        root.add(tabbedPane, BorderLayout.CENTER);
        add(root);
    }

    // ==================== 用户管理 Tab ====================
    private JPanel buildUserTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBackground(new Color(135, 206, 235));
        panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        userModel = new DefaultTableModel(new String[]{"用户名", "角色"}, 0);
        userTable = createStyledTable(userModel);
        userTable.setForeground(new Color(0, 0, 0));
        panel.add(createTableScroll(userTable), BorderLayout.CENTER);

        JPanel btnBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        btnBar.setBackground(new Color(135, 206, 235));
        JButton addBtn = createPrimaryButton("+ 添加用户");
        JButton delBtn = createDangerButton("X 删除用户");
        JButton refreshBtn = createSecondaryButton("刷新");  // 改为文字
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
        panel.setBackground(new Color(135, 206, 235));
        panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        examModel = new DefaultTableModel(
                new String[]{"考试ID", "标题", "创建者", "时长(秒)", "状态", "题数"}, 0);
        examTable = createStyledTable(examModel);
        // 设置表格文字颜色黑色
        examTable.setForeground(new Color(0, 0, 0));
        panel.add(createTableScroll(examTable), BorderLayout.CENTER);

        JPanel btnBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        btnBar.setBackground(new Color(135, 206, 235));
        JButton refreshBtn = createSecondaryButton("刷新");  // 改为文字
        refreshBtn.addActionListener(e -> refreshData());
        btnBar.add(refreshBtn);
        panel.add(btnBar, BorderLayout.SOUTH);

        return panel;
    }

    // ==================== 成绩统计 Tab ====================
    private JPanel buildScoreTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 0));
        panel.setBackground(new Color(135, 206, 235));
        panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        // 上半：已完成的考试列表
        JPanel topPanel = new JPanel(new BorderLayout(0, 6));
        topPanel.setBackground(Color.WHITE);
        topPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                "已完成的考试", 
                javax.swing.border.TitledBorder.LEFT,
                javax.swing.border.TitledBorder.TOP,
                new Font("微软雅黑", Font.BOLD, 13), new Color(50, 50, 50)));

        finishedExamModel = new DefaultTableModel(
                new String[]{"考试ID", "标题", "创建者", "总分", "参加人数"}, 0);
        finishedExamTable = createStyledTable(finishedExamModel);
        finishedExamTable.setForeground(new Color(0, 0, 0));
        topPanel.add(createTableScroll(finishedExamTable), BorderLayout.CENTER);

        JPanel topBtnBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        topBtnBar.setBackground(Color.WHITE);
        JButton loadFinished = createSecondaryButton("刷新");  // 改为文字
        JButton viewScores = createPrimaryButton("→ 查看成绩");
        loadFinished.addActionListener(e -> loadFinishedExams());
        viewScores.addActionListener(e -> viewSelectedExamScores());
        topBtnBar.add(loadFinished); topBtnBar.add(viewScores);
        topPanel.add(topBtnBar, BorderLayout.SOUTH);

        // 下半：成绩明细
        JPanel bottomPanel = new JPanel(new BorderLayout(0, 4));
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
        scoreDetailTable.setForeground(new Color(0, 0, 0));
        bottomPanel.add(createTableScroll(scoreDetailTable), BorderLayout.CENTER);

        // 统计数据条
        statsLabel = new JLabel(" ");
        statsLabel.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        statsLabel.setForeground(new Color(50, 50, 50));
        statsLabel.setBackground(new Color(240, 248, 255));
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

    // ==================== 样式工具方法 ====================
    private JTable createStyledTable(DefaultTableModel model) {
        JTable table = new JTable(model);
        table.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        table.setRowHeight(30);
        table.setForeground(Color.BLACK);
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
        btn.setBackground(new Color(25, 100, 180));
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
        btn.setForeground(new Color(25, 100, 180));
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
        dialog.setSize(450, 320);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());
        dialog.getContentPane().setBackground(new Color(135, 206, 235));

        // 标题面板
        JPanel titlePanel = new JPanel();
        titlePanel.setBackground(new Color(100, 180, 220));
        titlePanel.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
        JLabel titleLabel = new JLabel("添加新用户");
        titleLabel.setFont(new Font("微软雅黑", Font.BOLD, 16));
        titleLabel.setForeground(Color.WHITE);
        titlePanel.add(titleLabel);
        dialog.add(titlePanel, BorderLayout.NORTH);

        // 表单面板
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(new Color(135, 206, 235));
        formPanel.setBorder(BorderFactory.createEmptyBorder(30, 35, 25, 35));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(12, 12, 12, 12);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JTextField userField = new JTextField(20);
        userField.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        userField.setPreferredSize(new Dimension(500, 35));
        
        JPasswordField passField = new JPasswordField(20);
        passField.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        passField.setPreferredSize(new Dimension(500, 35));
        
        JComboBox<String> roleBox = new JComboBox<>(new String[]{"学生", "教师"});
        roleBox.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        roleBox.setPreferredSize(new Dimension(500, 35));

        // 用户名标签（深蓝色）
        gbc.gridx = 0; gbc.gridy = 0;
        JLabel userLabel = new JLabel("用户名：");
        userLabel.setFont(new Font("微软雅黑", Font.BOLD, 14));
        userLabel.setForeground(new Color(25, 25, 112));
        formPanel.add(userLabel, gbc);
        gbc.gridx = 1;
        formPanel.add(userField, gbc);

        // 密码标签（深蓝色）
        gbc.gridx = 0; gbc.gridy = 1;
        JLabel passLabel = new JLabel("密码：");
        passLabel.setFont(new Font("微软雅黑", Font.BOLD, 14));
        passLabel.setForeground(new Color(25, 25, 112));
        formPanel.add(passLabel, gbc);
        gbc.gridx = 1;
        formPanel.add(passField, gbc);

        // 角色标签（深蓝色）
        gbc.gridx = 0; gbc.gridy = 2;
        JLabel roleLabel = new JLabel("角色：");
        roleLabel.setFont(new Font("微软雅黑", Font.BOLD, 14));
        roleLabel.setForeground(new Color(25, 25, 112));
        formPanel.add(roleLabel, gbc);
        gbc.gridx = 1;
        formPanel.add(roleBox, gbc);

        dialog.add(formPanel, BorderLayout.CENTER);

        // 按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 25, 20));
        buttonPanel.setBackground(new Color(135, 206, 235));

        JButton okBtn = new JButton("确定");
        okBtn.setBackground(new Color(25, 100, 180));
        okBtn.setForeground(Color.WHITE);
        okBtn.setFont(new Font("微软雅黑", Font.BOLD, 14));
        okBtn.setFocusPainted(false);
        okBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        okBtn.setPreferredSize(new Dimension(100, 38));

        JButton cancelBtn = new JButton("取消");
        cancelBtn.setBackground(new Color(150, 150, 150));
        cancelBtn.setForeground(Color.WHITE);
        cancelBtn.setFont(new Font("微软雅黑", Font.BOLD, 14));
        cancelBtn.setFocusPainted(false);
        cancelBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        cancelBtn.setPreferredSize(new Dimension(100, 38));

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
        confirmDialog.getContentPane().setBackground(new Color(135, 206, 235));

        // 标题面板
        JPanel titlePanel = new JPanel();
        titlePanel.setBackground(new Color(100, 180, 220));
        titlePanel.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
        JLabel titleLabel = new JLabel("⚠️ 确认删除");
        titleLabel.setFont(new Font("微软雅黑", Font.BOLD, 16));
        titleLabel.setForeground(Color.WHITE);
        titlePanel.add(titleLabel);
        confirmDialog.add(titlePanel, BorderLayout.NORTH);

        // 消息面板
        JPanel messagePanel = new JPanel();
        messagePanel.setBackground(new Color(135, 206, 235));
        messagePanel.setBorder(BorderFactory.createEmptyBorder(40, 25, 20, 25));
        
        JLabel messageLabel = new JLabel("确定要删除用户 「" + uname + "」 吗？");
        messageLabel.setFont(new Font("微软雅黑", Font.PLAIN, 15));
        messageLabel.setForeground(new Color(25, 25, 112));
        messagePanel.add(messageLabel);
        
        confirmDialog.add(messagePanel, BorderLayout.CENTER);

        // 按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 25, 20));
        buttonPanel.setBackground(new Color(135, 206, 235));

        JButton yesBtn = new JButton("确定删除");
        yesBtn.setBackground(new Color(220, 80, 80));
        yesBtn.setForeground(Color.WHITE);
        yesBtn.setFont(new Font("微软雅黑", Font.BOLD, 13));
        yesBtn.setFocusPainted(false);
        yesBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        yesBtn.setPreferredSize(new Dimension(110, 38));
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

        JButton noBtn = new JButton("取消");
        noBtn.setBackground(new Color(100, 100, 100));
        noBtn.setForeground(Color.WHITE);
        noBtn.setFont(new Font("微软雅黑", Font.BOLD, 13));
        noBtn.setFocusPainted(false);
        noBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        noBtn.setPreferredSize(new Dimension(110, 38));
        noBtn.addActionListener(e -> confirmDialog.dispose());

        buttonPanel.add(yesBtn);
        buttonPanel.add(noBtn);
        confirmDialog.add(buttonPanel, BorderLayout.SOUTH);

        confirmDialog.setVisible(true);
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