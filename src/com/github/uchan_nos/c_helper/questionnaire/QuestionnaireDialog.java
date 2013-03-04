package com.github.uchan_nos.c_helper.questionnaire;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.jface.dialogs.Dialog;

import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import com.github.uchan_nos.c_helper.Activator;

public class QuestionnaireDialog extends Dialog {

    public QuestionnaireDialog(Shell parentShell) {
        super(parentShell);
        // ダイアログをリサイズ可能にする
        setShellStyle(getShellStyle() | SWT.RESIZE);
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
    protected Control createDialogArea(Composite parent) {
        // OK, キャンセルボタンを除く、画面全体のレイアウト
        DialogArea dialogArea = createMainDialogArea(parent);

        StringBuilder sb = new StringBuilder();

        Link label = new Link(dialogArea.composite(), SWT.NONE);
        sb.append("以下の質問に答え、\n");
        sb.append("Twitter: <a href=\"https://twitter.com/uchan_nos\">@uchan_nos</a>\n");
        sb.append("Github: <a href=\"https://github.com/uchan-nos\">uchan-nos</a>\n");
        sb.append("E-mail: uchan0@gmail.com\n");
        sb.append("のいずれかにお知らせください。\n");
        sb.append("今後の研究に活用させていただきます。");
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

        sb = new StringBuilder();
        Text questionText = new Text(dialogArea.composite(), SWT.MULTI);
        questionText.setLayoutData(new GridData(GridData.FILL_BOTH));

        sb.append("質問1:\n");
        sb.append("質問2:\n");
        sb.append("その他ご意見・ご感想:\n");

        questionText.setText(sb.toString());
        questionText.setFocus();

        return parent;
    }

    private DialogArea createMainDialogArea(Composite parent) {
        Composite c = parent;
        GridLayout l = new GridLayout();
        l.numColumns = 1;
        c.setLayout(l);
        return new DialogArea(c, l);
    }
}
