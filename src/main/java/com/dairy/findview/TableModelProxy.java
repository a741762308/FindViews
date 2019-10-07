package com.dairy.findview;

import javax.swing.table.AbstractTableModel;
import java.util.List;

public class TableModelProxy extends AbstractTableModel {
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
        onCheckedChange();
    }

    void onCheckedChange() {

    }
}