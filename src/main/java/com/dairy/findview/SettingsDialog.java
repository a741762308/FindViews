package com.dairy.findview;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

import static com.dairy.findview.ModifierType.DEFAULT;

public class SettingsDialog extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JCheckBox mKotlinLazy;
    private JComboBox<String> mModifierCombox;
    private JCheckBox mButterKnife;
    private JRadioButton mR2RadioButton;
    private JRadioButton mRRadioButton;
    private JCheckBox mButterKnifeBind;
    private JCheckBox mButterKnifeUnBind;

    public SettingsDialog() {
        setPreferredSize(new Dimension(400, 280));
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);
        mModifierCombox.addItem("private");
        mModifierCombox.addItem("default");
        mModifierCombox.addItem("protect");
        mModifierCombox.addItem("public");

        mKotlinLazy.setSelected(Config.get().isKotlinLazy());
        mModifierCombox.setSelectedItem(Config.get().getModifierType().toLowerCase());

        mButterKnife.setSelected(Config.get().isButterKnife());
        mButterKnifeBind.setSelected(Config.get().isButterKnifeBind());
        mButterKnifeUnBind.setSelected(Config.get().isButterKnifeUnBind());
        mR2RadioButton.setSelected(Config.get().isButterKnifeR2());

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


    private void saveModifierType() {
        ModifierType type;
        switch (mModifierCombox.getSelectedIndex()) {
            case 1:
                type = DEFAULT;
                break;
            case 2:
                type = ModifierType.PROTECT;
                break;
            case 3:
                type = ModifierType.PUBLIC;
                break;
            case 0:
            default:
                type = ModifierType.PRIVATE;
                break;
        }
        Config.get().saveModifierType(type);
    }

    private void onOK() {
        saveModifierType();
        saveButterKnife();
        Config.get().saveKotlinLazy(mKotlinLazy.isSelected());
        // add your code here if necessary
        dispose();
    }

    private void saveButterKnife() {
        Config.get().saveButterKnife(mButterKnife.isSelected());
        Config.get().saveButterKnifeLibrary(mR2RadioButton.isSelected());
        Config.get().saveButterKnifeBind(mButterKnifeBind.isSelected());
        Config.get().saveButterKnifeUnBind(mButterKnifeUnBind.isSelected());
    }

    private void onCancel() {
        // add your code here if necessary
        dispose();
    }

    public static void main(String[] args) {
        SettingsDialog dialog = new SettingsDialog();
        dialog.pack();
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
        System.exit(0);
    }
}