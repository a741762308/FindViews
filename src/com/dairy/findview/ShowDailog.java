package com.dairy.findview;

import javax.swing.*;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.*;
import java.util.Arrays;
import java.util.List;


public class ShowDailog extends JDialog {
    private JPanel mContentPane;
    private JButton mButtonOK;
    private JButton mButtonCancel;
    private JScrollPane mScrollPane;
    private JTable mTable;

    private OnClickListener mClickListener;

    public ShowDailog(List<ResBean> resBeanList) {
        setPreferredSize(new Dimension(600, 300));
        setContentPane(mContentPane);
        setModal(true);
        getRootPane().setDefaultButton(mButtonOK);

        String[] columnNames = {"", "Element", "ID", "Name"};
        TableModelProxy tableModel = new TableModelProxy(columnNames, resBeanList);
        mTable.setModel(tableModel);

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
            mClickListener.onOk();
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
        void onOk();

        default void onCancel() {

        }
    }

    private class TableModelProxy implements TableModel {
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

        @Override
        public void addTableModelListener(TableModelListener l) {

        }

        @Override
        public void removeTableModelListener(TableModelListener l) {

        }
    }

    public static void main(String[] args) {
        ShowDailog dialog = new ShowDailog(Arrays.asList(new ResBean("TextView", "@+id/aaa")));
        dialog.setClickListener(() -> {

        });
        dialog.pack();
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
        System.exit(0);
    }
}
