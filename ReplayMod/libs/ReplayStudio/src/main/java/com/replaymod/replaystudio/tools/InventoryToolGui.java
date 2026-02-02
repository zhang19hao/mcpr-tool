package com.replaymod.replaystudio.tools;

import javax.swing.*;
import java.awt.*;
import java.io.File;

public class InventoryToolGui {
    private final JFrame frame = new JFrame("MCPR 背包工具");
    private final JTextField mcprField = new JTextField();
    private final JTextField outputField = new JTextField();
    private final JCheckBox useTime = new JCheckBox("使用时间", false);
    private final JSpinner hours = new JSpinner(new SpinnerNumberModel(0, 0, 23, 1));
    private final JSpinner minutes = new JSpinner(new SpinnerNumberModel(0, 0, 59, 1));
    private final JSpinner seconds = new JSpinner(new SpinnerNumberModel(0, 0, 59, 1));
    private final JTextField versionField = new JTextField("1.20.1");
    private final JTextField packNameField = new JTextField("InventoryRestore");
    private final JTextField namespaceField = new JTextField("inventory_restore");
    private final JTextField functionNameField = new JTextField("restore");
    private final JTextArea logArea = new JTextArea();

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new InventoryToolGui().show());
    }

    private void show() {
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setSize(720, 520);
        frame.setLocationRelativeTo(null);
        frame.setLayout(new BorderLayout(10, 10));

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 6, 6, 6);
        c.fill = GridBagConstraints.HORIZONTAL;

        addRow(form, c, 0, "回放文件", mcprField, this::chooseMcpr);
        addRow(form, c, 1, "输出目录", outputField, this::chooseOutput);

        JPanel timePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        timePanel.add(useTime);
        timePanel.add(hours);
        timePanel.add(new JLabel("时"));
        timePanel.add(minutes);
        timePanel.add(new JLabel("分"));
        timePanel.add(seconds);
        timePanel.add(new JLabel("秒"));
        addRow(form, c, 2, "时间", timePanel, null);

        addRow(form, c, 3, "游戏版本", versionField, null);
        addRow(form, c, 4, "数据包名称", packNameField, null);
        addRow(form, c, 5, "命名空间", namespaceField, null);
        addRow(form, c, 6, "函数名", functionNameField, null);

        JButton runBtn = new JButton("开始生成");
        runBtn.addActionListener(e -> run());
        JButton openBtn = new JButton("打开输出目录");
        openBtn.addActionListener(e -> openOutput());
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        actionPanel.add(runBtn);
        actionPanel.add(openBtn);
        addRow(form, c, 7, "操作", actionPanel, null);

        logArea.setEditable(false);
        JScrollPane logPane = new JScrollPane(logArea);

        frame.add(form, BorderLayout.NORTH);
        frame.add(logPane, BorderLayout.CENTER);
        frame.setVisible(true);
    }

    private void addRow(JPanel panel, GridBagConstraints c, int row, String label, Component field, Runnable browseAction) {
        c.gridy = row;
        c.gridx = 0;
        c.weightx = 0;
        panel.add(new JLabel(label), c);
        c.gridx = 1;
        c.weightx = 1;
        panel.add(field, c);
        if (browseAction != null) {
            c.gridx = 2;
            c.weightx = 0;
            JButton btn = new JButton("选择");
            btn.addActionListener(e -> browseAction.run());
            panel.add(btn, c);
        }
    }

    private void chooseMcpr() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        int result = chooser.showOpenDialog(frame);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            mcprField.setText(file.getAbsolutePath());
            if (outputField.getText().trim().isEmpty()) {
                outputField.setText(file.getParentFile().getAbsolutePath());
            }
        }
    }

    private void chooseOutput() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int result = chooser.showOpenDialog(frame);
        if (result == JFileChooser.APPROVE_OPTION) {
            File dir = chooser.getSelectedFile();
            outputField.setText(dir.getAbsolutePath());
        }
    }

    private void appendLog(String text) {
        logArea.append(text + "\n");
        logArea.setCaretPosition(logArea.getText().length());
    }

    private void openOutput() {
        String path = outputField.getText().trim();
        if (path.isEmpty()) {
            return;
        }
        try {
            Desktop.getDesktop().open(new File(path));
        } catch (Exception ignored) {
        }
    }

    private void run() {
        String mcprPath = mcprField.getText().trim();
        if (mcprPath.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "请选择 MCPR 文件");
            return;
        }
        File mcpr = new File(mcprPath);
        if (!mcpr.exists()) {
            JOptionPane.showMessageDialog(frame, "MCPR 文件不存在");
            return;
        }
        String outputDir = outputField.getText().trim();
        if (outputDir.isEmpty()) {
            outputDir = mcpr.getParentFile().getAbsolutePath();
            outputField.setText(outputDir);
        }
        long timestamp = -1;
        if (useTime.isSelected()) {
            int h = (int) hours.getValue();
            int m = (int) minutes.getValue();
            int s = (int) seconds.getValue();
            timestamp = (h * 3600L + m * 60L + s) * 1000L;
        }
        String version = versionField.getText().trim();
        String packName = packNameField.getText().trim().isEmpty() ? "InventoryRestore" : packNameField.getText().trim();
        String namespace = namespaceField.getText().trim().isEmpty() ? "inventory_restore" : namespaceField.getText().trim();
        String functionName = functionNameField.getText().trim().isEmpty() ? "restore" : functionNameField.getText().trim();

        final File mcprFile = mcpr;
        final String outputDirValue = outputDir;
        final long timestampValue = timestamp;
        final String versionValue = version;
        final String packNameValue = packName;
        final String namespaceValue = namespace;
        final String functionNameValue = functionName;
        new Thread(() -> runWorker(mcprFile, outputDirValue, timestampValue, versionValue, packNameValue, namespaceValue, functionNameValue)).start();
    }

    private void runWorker(File mcpr, String outputDir, long timestamp, String version, String packName, String namespace, String functionName) {
        appendLog("开始提取背包数据");
        try {
            String[] args;
            if (timestamp >= 0 && !version.isEmpty()) {
                args = new String[]{mcpr.getAbsolutePath(), String.valueOf(timestamp), version};
            } else if (timestamp >= 0) {
                args = new String[]{mcpr.getAbsolutePath(), String.valueOf(timestamp)};
            } else if (!version.isEmpty()) {
                args = new String[]{mcpr.getAbsolutePath(), version};
            } else {
                args = new String[]{mcpr.getAbsolutePath()};
            }
            InventoryExtractor.main(args);
        } catch (Exception e) {
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(frame, "提取失败: " + e.getMessage()));
            return;
        }

        File jsonFile = timestamp >= 0
                ? new File(mcpr.getParentFile(), mcpr.getName() + "." + timestamp + ".inventory.json")
                : new File(mcpr.getParentFile(), mcpr.getName() + ".inventory.json");
        if (!jsonFile.exists()) {
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(frame, "未找到生成的 inventory.json"));
            return;
        }
        try {
            DatapackGenerator.Result result = DatapackGenerator.generate(jsonFile, new File(outputDir), packName, namespace, functionName);
            appendLog("已生成数据包: " + result.packDir.getAbsolutePath());
            appendLog("函数文件: " + result.functionFile.getAbsolutePath());
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(frame, "生成完成"));
        } catch (Exception e) {
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(frame, "生成数据包失败: " + e.getMessage()));
        }
    }
}
