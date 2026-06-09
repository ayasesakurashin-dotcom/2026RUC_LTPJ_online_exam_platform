package com.exam.client.common;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * 现代主题系统 — 参照 ChatGPT / Claude Desktop / Cursor 设计语言
 * 深色模式 + 浅色模式，暖色点缀，圆角卡片，统一间距
 */
public class ModernTheme {

    // ==================== 主题状态 ====================
    private static boolean darkMode = true;

    public static boolean isDark() { return darkMode; }
    public static void toggleDark() { darkMode = !darkMode; }

    // ==================== 深色调色盘 ====================
    public static final Color DARK_BG        = new Color(0x1E, 0x1E, 0x2E); // 主背景
    public static final Color DARK_SURFACE   = new Color(0x28, 0x28, 0x3D); // 卡片/面板
    public static final Color DARK_ELEVATED  = new Color(0x33, 0x33, 0x4A); // 提升卡片(hover)
    public static final Color DARK_BORDER    = new Color(0x3E, 0x3E, 0x58); // 边框
    public static final Color DARK_TEXT      = new Color(0xE5, 0xE5, 0xEE); // 主文字
    public static final Color DARK_SUBTEXT   = new Color(0x98, 0x98, 0xB0); // 次要文字

    // ==================== 浅色调色盘 ====================
    public static final Color LIGHT_BG       = new Color(0xF8, 0xF8, 0xFA); // 主背景
    public static final Color LIGHT_SURFACE  = new Color(0xFF, 0xFF, 0xFF); // 卡片/面板
    public static final Color LIGHT_ELEVATED = new Color(0xF2, 0xF2, 0xF7); // 提升卡片(hover)
    public static final Color LIGHT_BORDER   = new Color(0xE0, 0xE0, 0xEA); // 边框
    public static final Color LIGHT_TEXT     = new Color(0x1A, 0x1A, 0x2E); // 主文字
    public static final Color LIGHT_SUBTEXT  = new Color(0x6B, 0x6B, 0x82); // 次要文字

    // ==================== 点缀色 (通用) ====================
    public static final Color ACCENT         = new Color(0xD4, 0xA5, 0x74); // 暖琥珀主色
    public static final Color ACCENT_HOVER   = new Color(0xBF, 0x90, 0x60); // hover
    public static final Color SUCCESS        = new Color(0x6B, 0xC2, 0x6D); // 成功绿
    public static final Color ERROR          = new Color(0xF0, 0x6B, 0x6B); // 错误红
    public static final Color WARNING        = new Color(0xF5, 0xA6, 0x23); // 警告橙

    // ==================== 字体（微软雅黑确保CJK正确渲染） ====================
    public static final Font HEADING_FONT    = new Font("Microsoft YaHei", Font.BOLD, 22);
    public static final Font SUBHEADING_FONT = new Font("Microsoft YaHei", Font.BOLD, 16);
    public static final Font BODY_FONT       = new Font("Microsoft YaHei", Font.PLAIN, 14);
    public static final Font SMALL_FONT      = new Font("Microsoft YaHei", Font.PLAIN, 12);
    public static final Font MONO_FONT       = new Font("Consolas", Font.BOLD, 24); // 倒计时
    public static final Font TAB_FONT        = new Font("Microsoft YaHei", Font.PLAIN, 13);

    // ==================== 快捷取色 ====================
    public static Color bg()         { return darkMode ? DARK_BG        : LIGHT_BG; }
    public static Color surface()    { return darkMode ? DARK_SURFACE   : LIGHT_SURFACE; }
    public static Color elevated()   { return darkMode ? DARK_ELEVATED  : LIGHT_ELEVATED; }
    public static Color border()     { return darkMode ? DARK_BORDER    : LIGHT_BORDER; }
    public static Color text()       { return darkMode ? DARK_TEXT      : LIGHT_TEXT; }
    public static Color subtext()    { return darkMode ? DARK_SUBTEXT   : LIGHT_SUBTEXT; }

    /**
     * 接近黑色的"面板最底层"
     */
    public static Color bgDarker() {
        return darkMode ? new Color(0x16, 0x16, 0x24) : new Color(0xEE, 0xEE, 0xF2);
    }

    // ==================== 全局外观安装 ====================
    public static void install() {
        // 使用 Nimbus L&F — 跨平台稳定且支持 CJK
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ignored) {}

        // 全局默认字体 — 优先使用 Segoe UI (Win11) 或微软雅黑 (CJK回退)
        UIManager.put("defaultFont", BODY_FONT);
        UIManager.put("Label.font", BODY_FONT);
        UIManager.put("Button.font", BODY_FONT);
        UIManager.put("TextField.font", BODY_FONT);
        UIManager.put("TextArea.font", BODY_FONT);
        UIManager.put("ComboBox.font", BODY_FONT);
        UIManager.put("Table.font", SMALL_FONT);
        UIManager.put("TableHeader.font", new Font("Microsoft YaHei", Font.BOLD, 12));
        UIManager.put("TitledBorder.font", SUBHEADING_FONT);
        UIManager.put("TabbedPane.font", TAB_FONT);
        UIManager.put("OptionPane.font", BODY_FONT);
        UIManager.put("OptionPane.messageFont", BODY_FONT);
        UIManager.put("OptionPane.buttonFont", BODY_FONT);

        // 全局颜色
        UIManager.put("Panel.background", bg());
        UIManager.put("OptionPane.background", bg());
        UIManager.put("OptionPane.messageForeground", text());

        // ToolTip
        UIManager.put("ToolTip.background", elevated());
        UIManager.put("ToolTip.foreground", text());
        UIManager.put("ToolTip.border", new LineBorder(border(), 1));
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

    /** 次要按钮 */
    public static JButton secondaryButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(BODY_FONT);
        btn.setForeground(text());
        btn.setBackground(surface());
        btn.setBorder(new CompoundBorder(new LineBorder(border(), 1), new EmptyBorder(9, 20, 9, 20)));
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setOpaque(true);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.setBackground(elevated()); }
            public void mouseExited(MouseEvent e)  { btn.setBackground(surface()); }
        });
        return btn;
    }

    /** 危险按钮(删除等) */
    public static JButton dangerButton(String text) {
        JButton btn = primaryButton(text);
        btn.setBackground(ERROR);
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.setBackground(ERROR.darker()); }
            public void mouseExited(MouseEvent e)  { btn.setBackground(ERROR); }
        });
        return btn;
    }

    /** 现代文本框 */
    public static JTextField textField(int columns) {
        JTextField tf = new JTextField(columns);
        tf.setFont(BODY_FONT);
        tf.setBackground(surface());
        tf.setForeground(text());
        tf.setCaretColor(text());
        tf.setBorder(new CompoundBorder(
                new LineBorder(border(), 1),
                new EmptyBorder(8, 12, 8, 12)));
        return tf;
    }

    /** 现代密码框 */
    public static JPasswordField passwordField(int columns) {
        JPasswordField pf = new JPasswordField(columns);
        pf.setFont(BODY_FONT);
        pf.setBackground(surface());
        pf.setForeground(text());
        pf.setCaretColor(text());
        pf.setBorder(new CompoundBorder(
                new LineBorder(border(), 1),
                new EmptyBorder(8, 12, 8, 12)));
        return pf;
    }

    /** 圆角卡片面板 */
    public static JPanel card() {
        JPanel p = new JPanel();
        p.setBackground(surface());
        p.setBorder(new CompoundBorder(
                new RoundedBorder(border(), 12, 1),
                new EmptyBorder(20, 24, 20, 24)));
        return p;
    }

    /** 无边框卡片(仅内边距) */
    public static JPanel insetPanel(int top, int left, int bottom, int right) {
        JPanel p = new JPanel();
        p.setBackground(bg());
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

    /** 现代化表格 */
    public static JTable table(javax.swing.table.DefaultTableModel model) {
        JTable table = new JTable(model);
        table.setFont(SMALL_FONT);
        table.setForeground(text());
        table.setBackground(surface());
        table.setGridColor(border());
        table.setRowHeight(36);
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);
        table.setSelectionBackground(ACCENT);
        table.setSelectionForeground(Color.WHITE);
        table.setIntercellSpacing(new Dimension(0, 0));

        // Header 样式
        JTableHeader header = table.getTableHeader();
        header.setFont(new Font("Microsoft YaHei", Font.BOLD, 12));
        header.setBackground(bgDarker());
        header.setForeground(subtext());
        header.setBorder(new LineBorder(border(), 1));
        header.setPreferredSize(new Dimension(0, 36));

        // 居中渲染
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        centerRenderer.setBorder(new EmptyBorder(4, 8, 4, 8));
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }

        return table;
    }

    /** 包裹在带边框滚动面板中的表格 */
    public static JScrollPane tableScroll(JTable table) {
        JScrollPane sp = new JScrollPane(table);
        sp.setBackground(surface());
        sp.setBorder(new LineBorder(border(), 1));
        sp.getViewport().setBackground(surface());
        sp.getVerticalScrollBar().setUnitIncrement(16);
        styleScrollBar(sp);
        return sp;
    }

    /** 现代化滚动条 */
    public static void styleScrollBar(JScrollPane sp) {
        sp.getVerticalScrollBar().setUI(new BasicScrollBarUI() {
            @Override protected void configureScrollBarColors() {
                thumbColor = darkMode ? new Color(0x55, 0x55, 0x70) : new Color(0xCC, 0xCC, 0xD5);
                trackColor = bg();
            }
            @Override protected JButton createDecreaseButton(int o) { return zeroSizeBtn(); }
            @Override protected JButton createIncreaseButton(int o) { return zeroSizeBtn(); }
            private JButton zeroSizeBtn() {
                JButton b = new JButton(); b.setPreferredSize(new Dimension(0,0)); return b;
            }
        });
        sp.getHorizontalScrollBar().setUI(new BasicScrollBarUI() {
            @Override protected void configureScrollBarColors() {
                thumbColor = darkMode ? new Color(0x55, 0x55, 0x70) : new Color(0xCC, 0xCC, 0xD5);
                trackColor = bg();
            }
            @Override protected JButton createDecreaseButton(int o) { return zeroSizeBtn(); }
            @Override protected JButton createIncreaseButton(int o) { return zeroSizeBtn(); }
            private JButton zeroSizeBtn() {
                JButton b = new JButton(); b.setPreferredSize(new Dimension(0,0)); return b;
            }
        });
    }

    /** 给 Frame 应用全局背景 */
    public static void applyToFrame(JFrame frame, Container contentPane) {
        frame.getContentPane().setBackground(bg());
        if (contentPane != frame.getContentPane()) {
            contentPane.setBackground(bg());
        }
    }

    /** TitledBorder 风格 */
    public static Border titledBorder(String title) {
        return BorderFactory.createTitledBorder(
                new LineBorder(border(), 1),
                title,
                javax.swing.border.TitledBorder.LEFT,
                javax.swing.border.TitledBorder.TOP,
                SUBHEADING_FONT,
                text());
    }

    /** 主题切换 */
    public static void switchTheme() {
        toggleDark();
        install();
    }

    /** 切换按钮 */
    public static JButton themeToggle(final JFrame frame) {
        JButton btn = new JButton(darkMode ? " ☀ 浅色" : " ☾ 深色");
        btn.setFont(SMALL_FONT);
        btn.setForeground(subtext());
        btn.setBackground(bg());
        btn.setBorder(new CompoundBorder(new LineBorder(border(), 1), new EmptyBorder(4, 10, 4, 10)));
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setOpaque(true);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addActionListener(e -> {
            toggleDark();
            install();
            SwingUtilities.updateComponentTreeUI(frame);
            refreshFrameColors(frame.getContentPane());
            btn.setText(darkMode ? " ☀ 浅色" : " ☾ 深色");
            btn.setForeground(subtext());
            btn.setBackground(bg());
        });
        return btn;
    }

    /** 递归刷新容器内所有组件的主题颜色 */
    public static void refreshFrameColors(Container c) {
        for (Component comp : c.getComponents()) {
            if (comp instanceof JPanel) {
                JPanel p = (JPanel) comp;
                Color bg = p.getBackground();
                // 根据当前背景色推断应该使用哪种主题色
                if (isDarkSurface(bg) || isLightSurface(bg)) {
                    p.setBackground(surface());
                } else if (isDarkBg(bg) || isLightBg(bg)) {
                    p.setBackground(bg());
                } else if (isDarkDarker(bg) || isLightDarker(bg)) {
                    p.setBackground(bgDarker());
                }
            }
            if (comp instanceof JLabel) {
                JLabel l = (JLabel) comp;
                Color fg = l.getForeground();
                if (isDarkText(fg) || isLightText(fg)) {
                    l.setForeground(text());
                } else if (isDarkSubtext(fg) || isLightSubtext(fg)) {
                    l.setForeground(subtext());
                }
            }
            if (comp instanceof JButton) {
                JButton b = (JButton) comp;
                Color btnBg = b.getBackground();
                if (isDarkSurface(btnBg) || isLightSurface(btnBg)) {
                    b.setBackground(surface());
                }
                Color btnFg = b.getForeground();
                if (isDarkText(btnFg) || isLightText(btnFg)) {
                    b.setForeground(text());
                }
            }
            if (comp instanceof Container) {
                refreshFrameColors((Container) comp);
            }
        }
    }

    private static boolean isDarkSurface(Color c)  { return c != null && c.equals(DARK_SURFACE); }
    private static boolean isLightSurface(Color c) { return c != null && c.equals(LIGHT_SURFACE); }
    private static boolean isDarkBg(Color c)       { return c != null && c.equals(DARK_BG); }
    private static boolean isLightBg(Color c)      { return c != null && c.equals(LIGHT_BG); }
    private static boolean isDarkDarker(Color c)   { return c != null && c.getRed() == 0x16 && c.getGreen() == 0x16 && c.getBlue() == 0x24; }
    private static boolean isLightDarker(Color c)  { return c != null && c.getRed() == 0xEE && c.getGreen() == 0xEE && c.getBlue() == 0xF2; }
    private static boolean isDarkText(Color c)     { return c != null && c.equals(DARK_TEXT); }
    private static boolean isLightText(Color c)    { return c != null && c.equals(LIGHT_TEXT); }
    private static boolean isDarkSubtext(Color c)  { return c != null && c.equals(DARK_SUBTEXT); }
    private static boolean isLightSubtext(Color c) { return c != null && c.equals(LIGHT_SUBTEXT); }

    // ==================== 圆角边框 ====================
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
}
