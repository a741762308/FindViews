package com.dairy.findview;

public interface OnClickListener {
    void onOk(boolean kotlin);

    default void onCancel() {

    }
}