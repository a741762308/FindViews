package com.dairy.findview;

import com.intellij.ui.JBColor;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.*;
import java.util.Arrays;
import java.util.List;

public class XMLDialog extends JDialog {
    private JPanel mContentPane;
    private JButton mButtonOK;
    private JButton mButtonCancel;
    private JTable mTable;
    private JComboBox<String> mTypeComboBox;
    private JButton mSettings;
    private JRadioButton mKotlinRadioButton;
    private JRadioButton mJavaRadioButton;
    private JTextField mNameTextField;
    private JTextArea mTextArea;

    private OnClickListener mClickListener;
    private final TableModelProxy mTableModel;

    public XMLDialog(List<ResBean> resBeanList, String xmlPath) {
        setPreferredSize(new Dimension(750, 400));
        setContentPane(mContentPane);
        setModal(true);
        getRootPane().setDefaultButton(mButtonOK);

        String[] columnNames = {"", "Element", "ID", "Name"};
        mTableModel = new TableModelProxy(columnNames, resBeanList) {

            @Override
            void onCheckedChange() {
                mTextArea.setText(CodeConstant.getGenerateAdapterCode(resBeanList, mKotlinRadioButton.isSelected(), getAdapterName(), xmlPath));
            }
        };
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
        mTypeComboBox.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                for (ResBean bean : resBeanList) {
                    bean.setNameType(mTypeComboBox.getSelectedIndex() + 1);
                }
            }
            mTableModel.fireTableDataChanged();
            mTextArea.setText(CodeConstant.getGenerateAdapterCode(resBeanList, mKotlinRadioButton.isSelected(), getAdapterName(), xmlPath));
        });

        RadioChangedListener radioChangedListener = new RadioChangedListener(resBeanList, xmlPath);
        mKotlinRadioButton.addItemListener(radioChangedListener);
        mJavaRadioButton.addItemListener(radioChangedListener);

        mNameTextField.addFocusListener(new JTextFieldHintListener(mNameTextField, "please input adapter name，default TestAdapter"));

        mTextArea.setText(CodeConstant.getGenerateAdapterCode(resBeanList, mKotlinRadioButton.isSelected(), getAdapterName(), xmlPath));

        mSettings.addActionListener(e -> {
            SettingsDialog dialog = new SettingsDialog();
            dialog.pack();
            dialog.setLocationRelativeTo(null);
            dialog.setVisible(true);
        });

        mButtonOK.addActionListener(e -> onOK());

        mButtonCancel.addActionListener(e -> onCancel());

        // call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        // call onCancel() on ESCAPE
        mContentPane.registerKeyboardAction(e -> onCancel(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    }

    private class RadioChangedListener implements ItemListener {
        List<ResBean> resBeanList;
        String xmlPath;

        public RadioChangedListener(List<ResBean> resBeanList, String xmlPath) {
            this.resBeanList = resBeanList;
            this.xmlPath = xmlPath;
        }

        @Override
        public void itemStateChanged(ItemEvent e) {
            JRadioButton radioButton = (JRadioButton) e.getSource();
            if (radioButton.isSelected()) {
                mTextArea.setText(CodeConstant.getGenerateAdapterCode(resBeanList, mKotlinRadioButton.isSelected(), getAdapterName(), xmlPath));
            }
        }
    }

    ;

    private void onOK() {
        if (mClickListener != null) {
            mClickListener.onOk(mKotlinRadioButton.isSelected());
        }
        dispose();
    }

    private void onCancel() {
        if (mClickListener != null) {
            mClickListener.onCancel();
        }
        dispose();
    }

    public String getAdapterName() {
        if (mNameTextField.getForeground() == JBColor.GRAY || mNameTextField.getText().length() == 0) {
            return "TestAdapter";
        }
        return mNameTextField.getText();
    }

    public void setClickListener(OnClickListener clickListener) {
        mClickListener = clickListener;
    }


    public static void main(String[] args) {
        List<ResBean> resBeans = Arrays.asList(new ResBean("TextView", "@+id/aaa_bbb"));
        XMLDialog dialog = new XMLDialog(resBeans, "item_test.xml");
        dialog.setClickListener((boolean kotlin) -> {

        });
        dialog.pack();
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
        System.exit(0);
    }
}
