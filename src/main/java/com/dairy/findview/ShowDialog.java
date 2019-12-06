package com.dairy.findview;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.*;
import java.util.Arrays;
import java.util.List;


public class ShowDialog extends JDialog {
    private JPanel mContentPane;
    private JButton mButtonOK;
    private JButton mButtonCancel;
    private JScrollPane mScrollPane;
    private JTable mTable;
    private JComboBox<String> mTypeComboBox;
    private JCheckBox mViewHolderCheckBox;
    private JCheckBox mKotlinCheckBox;
    private JButton mSettings;

    private OnClickListener mClickListener;
    private final TableModelProxy mTableModel;

    public ShowDialog(List<ResBean> resBeanList) {
        setPreferredSize(new Dimension(600, 300));
        setContentPane(mContentPane);
        setModal(true);
        getRootPane().setDefaultButton(mButtonOK);

        String[] columnNames = {"", "Element", "ID", "Name"};
        mTableModel = new TableModelProxy(columnNames, resBeanList);
        mTable.setModel(mTableModel);
        mTable.setDragEnabled(false);
        mTable.setCellSelectionEnabled(false);
        mTable.setRowSelectionAllowed(false);
        mTable.setShowHorizontalLines(true);
        mTable.setShowVerticalLines(true);

        TableColumn tableColumn = mTable.getColumnModel().getColumn(0);
        tableColumn.setCellEditor(mTable.getDefaultEditor(Boolean.class));
        tableColumn.setCellRenderer(mTable.getDefaultRenderer(Boolean.class));
        tableColumn.setPreferredWidth(33);
        tableColumn.setMaxWidth(33);
        tableColumn.setMinWidth(33);

        DefaultTableCellRenderer cellRenderer = new DefaultTableCellRenderer();
        cellRenderer.setHorizontalAlignment(JLabel.CENTER);
        cellRenderer.setVerticalAlignment(JLabel.CENTER);
        mTable.setDefaultRenderer(Object.class, cellRenderer);

        CheckBoxHeaderRenderer headerRenderer = new CheckBoxHeaderRenderer(mTable, mTableModel);
        mTable.getTableHeader().setReorderingAllowed(false);
        mTable.getTableHeader().setDefaultRenderer(headerRenderer);

        mTypeComboBox.addItem("aa_bb_cc");
        mTypeComboBox.addItem("aaBbCc");
        mTypeComboBox.addItem("mAaBbCc");
        mTypeComboBox.setSelectedItem("mAaBbCc");
        mTypeComboBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    for (ResBean bean : resBeanList) {
                        bean.setNameType(mTypeComboBox.getSelectedIndex() + 1);
                    }
                }
                mTableModel.fireTableDataChanged();
            }
        });

        mKotlinCheckBox.setSelected(Config.get().getFileType() == FileType.KOTLIN);
//        mKotlinCheckBox.setVisible(false);
//        mViewHolderCheckBox.setSelected(Config.get().getClassType() == ClassType.ADAPTER);
        mViewHolderCheckBox.setVisible(false);

        mSettings.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                SettingsDialog dialog = new SettingsDialog();
                dialog.pack();
                dialog.setLocationRelativeTo(null);
                dialog.setAlwaysOnTop(true);
                dialog.setVisible(true);
            }
        });

        mButtonOK.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });

        mButtonCancel.addActionListener(new ActionListener() {
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
        mContentPane.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    }

    private void onOK() {
        if (mClickListener != null) {
            mClickListener.onOk(mKotlinCheckBox.isSelected());
        }
        dispose();
    }

    private void onCancel() {
        if (mClickListener != null) {
            mClickListener.onCancel();
        }
        dispose();
    }

    public void setClickListener(OnClickListener clickListener) {
        mClickListener = clickListener;
    }


    public static void main(String[] args) {
        List<ResBean> resBeans = Arrays.asList(new ResBean("TextView", "@+id/aaa_bbb"));
        ShowDialog dialog = new ShowDialog(resBeans);
        dialog.setClickListener((boolean kotlin) -> {

        });
        dialog.pack();
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
        System.exit(0);
    }
}