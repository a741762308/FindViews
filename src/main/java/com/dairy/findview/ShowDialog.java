package com.dairy.findview;

import javax.swing.*;
import javax.swing.table.*;
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
    private JComboBox mTypeComboBox;
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

        CheckBoxHeaderRenderer headerRenderer = new CheckBoxHeaderRenderer();
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
                dialog.setVisible(true);
            }
        });
        mSettings.setVisible(false);

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

    public interface OnClickListener {
        void onOk(boolean kotlin);

        default void onCancel() {

        }
    }

    private class TableModelProxy extends AbstractTableModel {
        private String[] columnData;
        private List<ResBean> datas;

        public TableModelProxy(String[] columnData, List<ResBean> datas) {
            this.columnData = columnData;
            this.datas = datas;
        }

        @Override
        public int getRowCount() {
            return datas.size();
        }

        @Override
        public int getColumnCount() {
            return columnData.length;
        }

        @Override
        public String getColumnName(int columnIndex) {
            return columnData[columnIndex];
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            if (columnIndex == 0) {
                return Boolean.class;
            }
            return String.class;
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return getColumnClass(columnIndex) == Boolean.class;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            ResBean bean = datas.get(rowIndex);
            switch (columnIndex) {
                case 0:
                    return bean.isChecked();
                case 1:
                    return bean.getName();
                case 2:
                    return bean.getId();
                case 3:
                    return bean.getFieldName();
            }
            return null;
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            ResBean bean = datas.get(rowIndex);
            if (columnIndex == 0 && aValue instanceof Boolean) {
                bean.setChecked((Boolean) aValue);
            }
        }
    }

    class CheckBoxHeaderRenderer implements TableCellRenderer {
        private JTableHeader mTableHeader;
        private JCheckBox mCheckBox;

        public CheckBoxHeaderRenderer() {
            mTableHeader = mTable.getTableHeader();
            mCheckBox = new JCheckBox();
            mCheckBox.setSelected(true);
            mTableHeader.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    int selectColumn = mTableHeader.columnAtPoint(e.getPoint());
                    if (selectColumn == 0) {
                        mCheckBox.setSelected(!mCheckBox.isSelected());
                        mTableHeader.repaint();

                        if (mCheckBox.isSelected()) {
                            selectAll();
                        } else {
                            selectNone();
                        }
                    }
                }
            });
        }

        private void selectAll() {
            for (int i = 0; i < mTableModel.getRowCount(); i++) {
                mTableModel.setValueAt(true, i, 0);
            }
            mTableModel.fireTableDataChanged();
        }

        private void selectNone() {
            for (int i = 0; i < mTableModel.getRowCount(); i++) {
                mTableModel.setValueAt(false, i, 0);
            }
            mTableModel.fireTableDataChanged();
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            String v;
            if (value instanceof String) {
                v = (String) value;
            } else {
                v = "";
            }

            JLabel label = new JLabel(v);
            label.setHorizontalAlignment(SwingConstants.CENTER);
            label.setVerticalAlignment(SwingConstants.CENTER);
            mCheckBox.setHorizontalAlignment(SwingConstants.CENTER);
            mCheckBox.setVerticalAlignment(SwingConstants.CENTER);
            mCheckBox.setBorderPainted(true);
            JComponent component = column == 0 ? mCheckBox : label;
            component.setForeground(mTableHeader.getForeground());
            component.setBackground(mTableHeader.getBackground());
            component.setFont(mTableHeader.getFont());
            component.setBorder(UIManager.getBorder("TableHeader.cellBorder"));
            return component;
        }
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
