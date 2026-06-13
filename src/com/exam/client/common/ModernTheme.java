package com.exam.client.common;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * 现代主题系统 — 清爽学术风 (Clean Academic)
 * 浅色蓝白配色，圆角卡片，统一间距，专注教育场景
 */
public class ModernTheme {

    // ==================== 浅色调色盘 (唯一主题) ====================
    public static final Color BG            = new Color(0xF5, 0xF6, 0xFA); // 主背景 — 柔和灰白
    public static final Color SURFACE       = new Color(0xFF, 0xFF, 0xFF); // 卡片/面板 — 纯白
    public static final Color ELEVATED      = new Color(0xF0, 0xF2, 0xF8); // 悬浮层 — 浅灰蓝
    public static final Color BORDER        = new Color(0xE4, 0xE7, 0xED); // 边框 — 柔和灰
    public static final Color TEXT          = new Color(0x1D, 0x21, 0x29); // 主文字 — 深灰黑
    public static final Color SUBTEXT       = new Color(0x86, 0x90, 0x9C); // 次要文字 — 中灰
    public static final Color PLACEHOLDER   = new Color(0xC9, 0xCD, 0xD4); // 占位文字 — 浅灰

    // ==================== 点缀色 ====================
    public static final Color ACCENT        = new Color(0x4F, 0x6E, 0xF7); // 学术蓝 — 主按钮/链接
    public static final Color ACCENT_HOVER  = new Color(0x3B, 0x5D, 0xE7); // 深蓝 — hover
    public static final Color ACCENT_LIGHT  = new Color(0xEE, 0xF1, 0xFE); // 浅蓝 — hover背景
    public static final Color SUCCESS       = new Color(0x22, 0xC5, 0x5E); // 成功绿
    public static final Color ERROR         = new Color(0xEF, 0x44, 0x44); // 错误红
    public static final Color WARNING       = new Color(0xF5, 0x9E, 0x0B); // 警告橙

    // ==================== 角色点缀色 ====================
    public static final Color ADMIN_ACCENT    = new Color(0x4F, 0x6E, 0xF7); // 学术蓝 — 管理员
    public static final Color TEACHER_ACCENT  = new Color(0x63, 0x66, 0xF1); // 紫蓝 — 教师
    public static final Color STUDENT_ACCENT  = new Color(0x10, 0xB9, 0x81); // 翠绿 — 学生

    // ==================== 难度色标 ====================
    public static final Color DIFF_BASIC_COLOR    = new Color(0x22, 0xC5, 0x5E); // 基础题 — 绿
    public static final Color DIFF_MEDIUM_COLOR   = new Color(0x4F, 0x6E, 0xF7); // 中等题 — 蓝
    public static final Color DIFF_ADVANCED_COLOR = new Color(0xF5, 0x9E, 0x0B); // 提高题 — 橙

    // ==================== 头部栏专用色 ====================
    public static final Color HEADER_BG      = new Color(0xFF, 0xFF, 0xFF); // 头部栏 — 白色
    public static final Color HEADER_BORDER  = new Color(0xE4, 0xE7, 0xED); // 头部栏底线

    // ==================== 字体（微软雅黑确保CJK正确渲染） ====================
    public static final Font HEADING_FONT    = new Font("Microsoft YaHei", Font.BOLD, 24);
    public static final Font SUBHEADING_FONT = new Font("Microsoft YaHei", Font.BOLD, 16);
    public static final Font BODY_FONT       = new Font("Microsoft YaHei", Font.PLAIN, 14);
    public static final Font SMALL_FONT      = new Font("Microsoft YaHei", Font.PLAIN, 12);
    public static final Font MONO_FONT       = new Font("JetBrains Mono", Font.BOLD, 28);
    public static final Font TAB_FONT        = new Font("Microsoft YaHei", Font.PLAIN, 14);

    // ==================== 快捷取色 ====================
    public static Color bg()         { return BG; }
    public static Color surface()    { return SURFACE; }
    public static Color elevated()   { return ELEVATED; }
    public static Color border()     { return BORDER; }
    public static Color text()       { return TEXT; }
    public static Color subtext()    { return SUBTEXT; }

    /** 微妙的底色区分 (表格斑马纹、头部栏底等) */
    public static Color bgDarker() {
        return new Color(0xEB, 0xED, 0xF2);
    }

    // ==================== 难度取色 ====================
    public static Color diffColor(String difficulty) {
        if ("MEDIUM".equals(difficulty))  return DIFF_MEDIUM_COLOR;
        if ("ADVANCED".equals(difficulty)) return DIFF_ADVANCED_COLOR;
        return DIFF_BASIC_COLOR;
    }

    public static String diffLabel(String difficulty) {
        if ("MEDIUM".equals(difficulty))  return "中等题";
        if ("ADVANCED".equals(difficulty)) return "提高题";
        return "基础题";
    }

    // ==================== 全局外观安装 ====================
    public static void install() {
        // 优先使用系统 L&F，回退到 Nimbus
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ignored) {}

        // 全局默认字体
        UIManager.put("defaultFont", BODY_FONT);
        UIManager.put("Label.font", BODY_FONT);
        UIManager.put("Button.font", BODY_FONT);
        UIManager.put("TextField.font", BODY_FONT);
        UIManager.put("TextArea.font", BODY_FONT);
        UIManager.put("ComboBox.font", BODY_FONT);
        UIManager.put("Table.font", new Font("Microsoft YaHei", Font.PLAIN, 13));
        UIManager.put("TableHeader.font", new Font("Microsoft YaHei", Font.BOLD, 12));
        UIManager.put("TitledBorder.font", SUBHEADING_FONT);
        UIManager.put("TabbedPane.font", TAB_FONT);
        UIManager.put("OptionPane.font", BODY_FONT);
        UIManager.put("OptionPane.messageFont", BODY_FONT);
        UIManager.put("OptionPane.buttonFont", BODY_FONT);

        // 全局颜色
        UIManager.put("Panel.background", BG);
        UIManager.put("OptionPane.background", BG);
        UIManager.put("OptionPane.messageForeground", TEXT);

        // ToolTip
        UIManager.put("ToolTip.background", SURFACE);
        UIManager.put("ToolTip.foreground", TEXT);
        UIManager.put("ToolTip.border", new LineBorder(BORDER, 1));
    }

    // ==================== 组件样式工厂 ====================

    /** 圆角主按钮 */
    public static JButton primaryButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Microsoft YaHei", Font.BOLD, 14));
        btn.setForeground(Color.WHITE);
        btn.setBackground(ACCENT);
        btn.setBorder(new EmptyBorder(10, 24, 10, 24));
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setOpaque(true);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.setBackground(ACCENT_HOVER); }
            public void mouseExited(MouseEvent e)  { btn.setBackground(ACCENT); }
        });
        return btn;
    }

    /** 次要按钮 (线框风格) */
    public static JButton secondaryButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(BODY_FONT);
        btn.setForeground(TEXT);
        btn.setBackground(SURFACE);
        btn.setBorder(new CompoundBorder(new LineBorder(BORDER, 1), new EmptyBorder(9, 20, 9, 20)));
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setOpaque(true);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.setBackground(ELEVATED); }
            public void mouseExited(MouseEvent e)  { btn.setBackground(SURFACE); }
        });
        return btn;
    }

    /** 危险按钮(删除等) */
    public static JButton dangerButton(String text) {
        JButton btn = primaryButton(text);
        btn.setBackground(ERROR);
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.setBackground(new Color(0xDC, 0x26, 0x26)); }
            public void mouseExited(MouseEvent e)  { btn.setBackground(ERROR); }
        });
        return btn;
    }

    /** 现代文本框 */
    public static JTextField textField(int columns) {
        JTextField tf = new JTextField(columns);
        tf.setFont(BODY_FONT);
        tf.setBackground(SURFACE);
        tf.setForeground(TEXT);
        tf.setCaretColor(ACCENT);
        tf.setBorder(new CompoundBorder(
                new LineBorder(BORDER, 1),
                new EmptyBorder(8, 12, 8, 12)));
        return tf;
    }

    /** 现代密码框 */
    public static JPasswordField passwordField(int columns) {
        JPasswordField pf = new JPasswordField(columns);
        pf.setFont(BODY_FONT);
        pf.setBackground(SURFACE);
        pf.setForeground(TEXT);
        pf.setCaretColor(ACCENT);
        pf.setBorder(new CompoundBorder(
                new LineBorder(BORDER, 1),
                new EmptyBorder(8, 12, 8, 12)));
        return pf;
    }

    /** 圆角卡片面板 (带柔和阴影效果) */
    public static JPanel card() {
        JPanel p = new JPanel();
        p.setBackground(SURFACE);
        p.setBorder(new CompoundBorder(
                new ShadowBorder(BORDER, 10, 1),
                new EmptyBorder(20, 24, 20, 24)));
        return p;
    }

    /** 无边框卡片(仅内边距) */
    public static JPanel insetPanel(int top, int left, int bottom, int right) {
        JPanel p = new JPanel();
        p.setBackground(BG);
        p.setBorder(new EmptyBorder(top, left, bottom, right));
        return p;
    }

    /** 给任意组件包一层padding */
    public static JPanel pad(JComponent c, int top, int left, int bottom, int right) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(c.getBackground());
        p.setBorder(new EmptyBorder(top, left, bottom, right));
        p.add(c, BorderLayout.CENTER);
        return p;
    }

    /** 现代化表格 (带斑马纹) */
    public static JTable table(javax.swing.table.DefaultTableModel model) {
        JTable table = new JTable(model);
        table.setFont(new Font("Microsoft YaHei", Font.PLAIN, 13));
        table.setForeground(TEXT);
        table.setBackground(SURFACE);
        table.setGridColor(new Color(0xF0, 0xF1, 0xF5));
        table.setRowHeight(38);
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);
        table.setSelectionBackground(ACCENT_LIGHT);
        table.setSelectionForeground(TEXT);
        table.setIntercellSpacing(new Dimension(0, 0));

        // 斑马纹渲染
        table.setDefaultRenderer(Object.class, new ZebraCellRenderer());

        // Header 样式
        JTableHeader header = table.getTableHeader();
        header.setFont(new Font("Microsoft YaHei", Font.BOLD, 12));
        header.setBackground(new Color(0xF5, 0xF6, 0xFA));
        header.setForeground(SUBTEXT);
        header.setBorder(new LineBorder(BORDER, 1));
        header.setPreferredSize(new Dimension(0, 38));

        // 居中渲染
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        centerRenderer.setBorder(new EmptyBorder(4, 8, 4, 8));
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }

        return table;
    }

    /** 斑马纹单元格渲染器 */
    private static class ZebraCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (!isSelected) {
                c.setBackground(row % 2 == 0 ? SURFACE : new Color(0xFA, 0xFB, 0xFD));
            } else {
                c.setBackground(ACCENT_LIGHT);
                setForeground(TEXT);
            }
            setBorder(new EmptyBorder(4, 12, 4, 12));
            setHorizontalAlignment(SwingConstants.CENTER);
            return c;
        }
    }

    /** 包裹在带边框滚动面板中的表格 */
    public static JScrollPane tableScroll(JTable table) {
        JScrollPane sp = new JScrollPane(table);
        sp.setBackground(SURFACE);
        sp.setBorder(new LineBorder(BORDER, 1));
        sp.getViewport().setBackground(SURFACE);
        sp.getVerticalScrollBar().setUnitIncrement(16);
        styleScrollBar(sp);
        return sp;
    }

    /** 现代化滚动条 */
    public static void styleScrollBar(JScrollPane sp) {
        sp.getVerticalScrollBar().setUI(new BasicScrollBarUI() {
            @Override protected void configureScrollBarColors() {
                thumbColor = new Color(0xCC, 0xCE, 0xD5);
                trackColor = BG;
            }
            @Override protected JButton createDecreaseButton(int o) { return zeroSizeBtn(); }
            @Override protected JButton createIncreaseButton(int o) { return zeroSizeBtn(); }
            private JButton zeroSizeBtn() {
                JButton b = new JButton();
                b.setPreferredSize(new Dimension(0, 0));
                return b;
            }
        });
        sp.getHorizontalScrollBar().setUI(new BasicScrollBarUI() {
            @Override protected void configureScrollBarColors() {
                thumbColor = new Color(0xCC, 0xCE, 0xD5);
                trackColor = BG;
            }
            @Override protected JButton createDecreaseButton(int o) { return zeroSizeBtn(); }
            @Override protected JButton createIncreaseButton(int o) { return zeroSizeBtn(); }
            private JButton zeroSizeBtn() {
                JButton b = new JButton();
                b.setPreferredSize(new Dimension(0, 0));
                return b;
            }
        });
    }

    /** 给 Frame 应用全局背景 */
    public static void applyToFrame(JFrame frame, Container contentPane) {
        frame.getContentPane().setBackground(BG);
        if (contentPane != frame.getContentPane()) {
            contentPane.setBackground(BG);
        }
    }

    /** TitledBorder 风格 */
    public static Border titledBorder(String title) {
        return BorderFactory.createTitledBorder(
                new LineBorder(BORDER, 1),
                title,
                javax.swing.border.TitledBorder.LEFT,
                javax.swing.border.TitledBorder.TOP,
                SUBHEADING_FONT,
                TEXT);
    }

    // ==================== 头部栏组件 ====================

    /** 创建统一头部栏容器 (白色背景 + 底部细线分隔) */
    public static JPanel headerBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(HEADER_BG);
        bar.setBorder(new CompoundBorder(
                new BottomLineBorder(HEADER_BORDER, 1),
                new EmptyBorder(12, 20, 12, 20)));
        return bar;
    }

    /** 底部单线边框 */
    private static class BottomLineBorder implements Border {
        private final Color color;
        private final int thickness;
        BottomLineBorder(Color color, int thickness) {
            this.color = color; this.thickness = thickness;
        }
        @Override public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            g.setColor(color);
            for (int i = 0; i < thickness; i++) {
                g.drawLine(x, y + h - 1 - i, x + w, y + h - 1 - i);
            }
        }
        @Override public Insets getBorderInsets(Component c) {
            return new Insets(0, 0, thickness, 0);
        }
        @Override public boolean isBorderOpaque() { return false; }
    }

    /** 头部栏图标 */
    public static JLabel headerIcon(String emoji) {
        JLabel l = new JLabel(emoji);
        l.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 22));
        return l;
    }

    /** 头部栏标题 (深色粗体) */
    public static JLabel headerTitle(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Microsoft YaHei", Font.BOLD, 18));
        l.setForeground(TEXT);
        return l;
    }

    /** 头部栏左侧面板 (icon + title, 水平 FlowLayout) */
    public static JPanel headerLeft(String emoji, String title) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        p.setBackground(HEADER_BG);
        p.add(headerIcon(emoji));
        p.add(headerTitle(title));
        return p;
    }

    /** 头部栏按钮 (线框风格) */
    public static JButton headerButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(SMALL_FONT);
        btn.setForeground(TEXT);
        btn.setBackground(HEADER_BG);
        btn.setBorder(new CompoundBorder(
                new LineBorder(BORDER, 1),
                new EmptyBorder(4, 12, 4, 12)));
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setOpaque(true);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.setBackground(ELEVATED); }
            public void mouseExited(MouseEvent e) { btn.setBackground(HEADER_BG); }
        });
        return btn;
    }

    /** 头部栏用户标签 */
    public static JLabel userTag(String username) {
        JLabel l = new JLabel(username);
        l.setFont(new Font("Microsoft YaHei", Font.BOLD, 12));
        l.setForeground(ACCENT);
        l.setBorder(new EmptyBorder(2, 10, 2, 10));
        return l;
    }

    /** 头部栏状态标签 */
    public static JLabel headerStatusLabel() {
        JLabel l = new JLabel("就绪");
        l.setFont(SMALL_FONT);
        l.setForeground(SUBTEXT);
        return l;
    }

    /** 头部栏右侧面板 (水平 FlowLayout.RIGHT, 包含给定组件) */
    public static JPanel headerRight(JComponent... components) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        p.setBackground(HEADER_BG);
        for (JComponent c : components) p.add(c);
        return p;
    }

    // ==================== 其他组件工厂 ====================

    /** 现代下拉框 */
    public static JComboBox<String> comboBox(String[] items) {
        JComboBox<String> cb = new JComboBox<>(items);
        cb.setFont(BODY_FONT);
        cb.setBackground(SURFACE);
        cb.setForeground(TEXT);
        cb.setBorder(new CompoundBorder(
                new LineBorder(BORDER, 1),
                new EmptyBorder(6, 10, 6, 10)));
        return cb;
    }

    /** 文字链接按钮 (无色背景, 强调色文字) */
    public static JButton linkButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(BODY_FONT);
        btn.setForeground(ACCENT);
        btn.setBackground(BG);
        btn.setBorder(new EmptyBorder(4, 8, 4, 8));
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setOpaque(true);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.setForeground(ACCENT_HOVER); }
            public void mouseExited(MouseEvent e)  { btn.setForeground(ACCENT); }
        });
        return btn;
    }

    /** 表单标签 */
    public static JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Microsoft YaHei", Font.BOLD, 13));
        l.setForeground(TEXT);
        return l;
    }

    /** 现代文本区域 */
    public static JTextArea textArea(int rows, int columns) {
        JTextArea ta = new JTextArea(rows, columns);
        ta.setFont(BODY_FONT);
        ta.setBackground(SURFACE);
        ta.setForeground(TEXT);
        ta.setCaretColor(ACCENT);
        ta.setLineWrap(true);
        ta.setWrapStyleWord(true);
        return ta;
    }

    /** 对角线分割面板 (带主题色分割线) */
    public static JSplitPane splitPane(int orientation, Component left, Component right, double resizeWeight) {
        JSplitPane sp = new JSplitPane(orientation, left, right);
        sp.setResizeWeight(resizeWeight);
        sp.setBorder(null);
        sp.setBackground(BG);
        sp.setDividerSize(8);
        sp.setDividerLocation((int)(resizeWeight * 600));
        return sp;
    }

    /** 普通滚动面板 (非表格, 用于文本区域等) */
    public static JScrollPane styledScrollPane(Component view) {
        JScrollPane sp = new JScrollPane(view);
        sp.setBackground(SURFACE);
        sp.setBorder(new LineBorder(BORDER, 1));
        sp.getViewport().setBackground(SURFACE);
        sp.getVerticalScrollBar().setUnitIncrement(16);
        styleScrollBar(sp);
        return sp;
    }

    /** 对话框标题栏 */
    public static JPanel dialogHeader(String title, Color accentColor) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT));
        p.setBackground(accentColor != null ? accentColor : ACCENT);
        p.setBorder(new EmptyBorder(14, 20, 14, 20));
        JLabel l = new JLabel(title);
        l.setFont(new Font("Microsoft YaHei", Font.BOLD, 16));
        l.setForeground(Color.WHITE);
        p.add(l);
        return p;
    }

    /** 自定义 TabbedPane (下划线指示器风格) */
    public static JTabbedPane tabbedPane() {
        JTabbedPane tp = new JTabbedPane(JTabbedPane.TOP);
        tp.setBackground(BG);
        tp.setForeground(SUBTEXT);
        tp.setFont(TAB_FONT);
        UIManager.put("TabbedPane.selected", SURFACE);
        UIManager.put("TabbedPane.background", BG);
        UIManager.put("TabbedPane.foreground", TEXT);
        UIManager.put("TabbedPane.contentBorderInsets", new Insets(0, 0, 0, 0));
        UIManager.put("TabbedPane.selectedForeground", ACCENT);
        UIManager.put("TabbedPane.tabAreaBackground", SURFACE);
        return tp;
    }

    /** 简易消息/占位标签 */
    public static JLabel placeholder(String text) {
        JLabel l = new JLabel(text);
        l.setFont(BODY_FONT);
        l.setForeground(SUBTEXT);
        l.setHorizontalAlignment(SwingConstants.CENTER);
        return l;
    }

    /** 欢迎副标题 (居中, 微妙) */
    public static JLabel subtitle(String text) {
        JLabel l = new JLabel(text, SwingConstants.CENTER);
        l.setFont(SMALL_FONT);
        l.setForeground(SUBTEXT);
        return l;
    }

    /** 难度标签 (带色标) */
    public static JLabel diffTag(String difficulty) {
        String text = diffLabel(difficulty);
        Color color = diffColor(difficulty);
        JLabel tag = new JLabel(text);
        tag.setFont(new Font("Microsoft YaHei", Font.BOLD, 11));
        tag.setForeground(color);
        tag.setBorder(new CompoundBorder(
                new LineBorder(color, 1),
                new EmptyBorder(2, 8, 2, 8)));
        return tag;
    }

    // ==================== 圆角 + 阴影边框 ====================

    /** 圆角边框 */
    public static class RoundedBorder implements Border {
        private final Color color;
        private final int radius;
        private final int thickness;
        public RoundedBorder(Color color, int radius, int thickness) {
            this.color = color; this.radius = radius; this.thickness = thickness;
        }
        @Override public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(thickness));
            g2.drawRoundRect(x + thickness/2, y + thickness/2, w - thickness, h - thickness, radius, radius);
            g2.dispose();
        }
        @Override public Insets getBorderInsets(Component c) {
            return new Insets(radius/2, radius/2, radius/2, radius/2);
        }
        @Override public boolean isBorderOpaque() { return false; }
    }

    /** 阴影边框 (模拟卡片阴影) */
    public static class ShadowBorder implements Border {
        private final Color color;
        private final int radius;
        private final int thickness;
        public ShadowBorder(Color color, int radius, int thickness) {
            this.color = color; this.radius = radius; this.thickness = thickness;
        }
        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // 外层阴影
            g2.setColor(new Color(0, 0, 0, 8));
            g2.setStroke(new BasicStroke(1));
            g2.drawRoundRect(x + 2, y + 2, w - 5, h - 5, radius, radius);

            // 中层阴影
            g2.setColor(new Color(0, 0, 0, 5));
            g2.setStroke(new BasicStroke(1));
            g2.drawRoundRect(x + 1, y + 1, w - 3, h - 3, radius, radius);

            // 内层边框
            g2.setColor(color);
            g2.setStroke(new BasicStroke(thickness));
            g2.drawRoundRect(x, y, w - 2, h - 2, radius, radius);

            g2.dispose();
        }
        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(radius/2 + 2, radius/2 + 2, radius/2 + 2, radius/2 + 2);
        }
        @Override
        public boolean isBorderOpaque() { return false; }
    }
}
