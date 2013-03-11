package com.github.uchan_nos.c_helper.questionnaire;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.jface.dialogs.Dialog;

import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.*;

import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import com.github.uchan_nos.c_helper.Activator;

public class QuestionnaireDialog extends Dialog {

    public QuestionnaireDialog(Shell parentShell) {
        super(parentShell);
    }

    @Override
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);
        // .ダイアログのタイトルとアイコンを設定する
        newShell.setText("C-Helper アンケート");
        newShell.setImage(
                Activator.getImageDescriptor("icons/analysis.png").createImage());
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        createButton(parent, OK, "CLOSE", true);
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        StringBuilder sb = new StringBuilder();

        Link label = new Link(parent, SWT.NONE);
        sb.append("C-Helperを使って頂きありがとうございます！\n");
        sb.append("C-Helperについてのアンケートにご協力をお願いします。\n");
        sb.append("今後の研究に活用させていただきます。\n");
        sb.append("アンケートは<a href=\"https://docs.google.com/forms/d/1bdGMk4v-gFTAPq7C6s--pkpHi-5227vU4bZO1pjZ8hQ/viewform\">アンケートフォーム</a>から。");
        label.setText(sb.toString());

        label.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                try {
                    PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser().openURL(new URL(e.text));
                } catch (PartInitException e1) {
                    e1.printStackTrace();
                } catch (MalformedURLException e1) {
                    e1.printStackTrace();
                }
            }
        });

        return parent;
    }
}
