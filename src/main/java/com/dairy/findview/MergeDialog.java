package com.dairy.findview;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Arrays;
import java.util.List;

public class MergeDialog extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JCheckBox kotlinCheckBox;
    private OnMergeClickListener mClickListener;

    public MergeDialog(List<ResBean> resBeanList) {
        setPreferredSize(new Dimension(600, 300));
        setContentPane(contentPane);

        setModal(true);
        getRootPane().setDefaultButton(buttonOK);

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
