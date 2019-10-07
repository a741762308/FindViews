package com.dairy.findview;

import javax.swing.*;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class CheckBoxHeaderRenderer implements TableCellRenderer {


    private JTableHeader mTableHeader;
    private JCheckBox mCheckBox;
    private TableModelProxy mTableModel;

    public CheckBoxHeaderRenderer(JTable table, TableModelProxy tableModel) {
        this.mTableHeader = table.getTableHeader();
        this.mTableModel = tableModel;
        this.mCheckBox = new JCheckBox();
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