package com.github.uchan_nos.c_helper.questionnaire;

import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.Composite;

public class DialogArea {
    private Composite composite;
    private Layout layout;

    public DialogArea(Composite composite, Layout layout) {
        this.composite = composite;
        this.layout = layout;
    }

    public Composite composite() {
        return composite;
    }

    public Layout layout() {
        return layout;
    }
}

