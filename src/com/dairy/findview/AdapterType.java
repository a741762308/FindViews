package com.dairy.findview;

public enum AdapterType {
    ADAPTER_NONE(-1),
    ADAPTER_BASE(1),
    ADAPTER_RECYCLER(2);
    int index;

    AdapterType(int index) {
        this.index = index;
    }
}
