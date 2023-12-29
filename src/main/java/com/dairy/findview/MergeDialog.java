package com.dairy.findview;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.*;
import java.util.Arrays;
import java.util.List;

public class MergeDialog extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JCheckBox kotlinCheckBox;
    private JComboBox<String> typeComboBox;
    private JTable table;
    private final TableModelProxy tableModel;

    private OnMergeClickListener mClickListener;

    public MergeDialog(List<ResBean> resBeanList) {
        setPreferredSize(new Dimension(600, 300));
        setContentPane(contentPane);

        setModal(true);
        getRootPane().setDefaultButton(buttonOK);

        String[] columnNames = {"", "Element", "ID", "Name"};
        tableModel = new TableModelProxy(columnNames, resBeanList);
        table.setModel(tableModel);
        table.setDragEnabled(false);
        table.setCellSelectionEnabled(false);
        table.setRowSelectionAllowed(false);
        table.setShowHorizontalLines(true);
        table.setShowVerticalLines(true);

        TableColumn tableColumn = table.getColumnModel().getColumn(0);
        tableColumn.setCellEditor(table.getDefaultEditor(Boolean.class));
        tableColumn.setCellRenderer(table.getDefaultRenderer(Boolean.class));
        tableColumn.setPreferredWidth(33);
        tableColumn.setMaxWidth(33);
        tableColumn.setMinWidth(33);

        DefaultTableCellRenderer cellRenderer = new DefaultTableCellRenderer();
        cellRenderer.setHorizontalAlignment(JLabel.CENTER);
        cellRenderer.setVerticalAlignment(JLabel.CENTER);
        table.setDefaultRenderer(Object.class, cellRenderer);

        CheckBoxHeaderRenderer headerRenderer = new CheckBoxHeaderRenderer(table, tableModel);
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setDefaultRenderer(headerRenderer);

        typeComboBox.addItem("aa_bb_cc");
        typeComboBox.addItem("aaBbCc");
        typeComboBox.addItem("mAaBbCc");
        typeComboBox.setSelectedItem("mAaBbCc");
        typeComboBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    for (ResBean bean : resBeanList) {
                        bean.setNameType(typeComboBox.getSelectedIndex() + 1);
                    }
                }
                tableModel.fireTableDataChanged();
            }
        });

        buttonOK.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });

        buttonCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });

        // call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        // call onCancel() on ESCAPE
        contentPane.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    }

    private void onOK() {
        if (mClickListener != null) {
            mClickListener.onOk(kotlinCheckBox.isSelected());
        }
        dispose();
    }

    private void onCancel() {
        if (mClickListener != null) {
            mClickListener.onCancel();
        }
        dispose();
    }

    public void setClickListener(OnMergeClickListener clickListener) {
        mClickListener = clickListener;
    }

    public static void main(String[] args) {
        List<ResBean> resBeans = Arrays.asList(new ResBean("TextView", "@+id/aaa_bbb"));
        MergeDialog dialog = new MergeDialog(resBeans);
        dialog.pack();
        dialog.setVisible(true);
        System.exit(0);
    }
}
