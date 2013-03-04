package com.github.uchan_nos.c_helper.questionnaire;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

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

        // 選択式質問エリア
        DialogArea selectiveQuestionArea = createSelectiveQuestionArea(dialogArea.composite());
        Question[] questions = createQuestions();
        createSelectiveQuestionWidgets(selectiveQuestionArea.composite(), questions);

        // 自由記述質問エリア
        DialogArea freeFormArea = createFreeFormArea(dialogArea.composite());
        createFreeFormQuestionWidgets(freeFormArea.composite());

        return parent;
    }

    private DialogArea createMainDialogArea(Composite parent) {
        Composite c = parent;
        GridLayout l = new GridLayout();
        l.numColumns = 1;
        c.setLayout(l);
        return new DialogArea(c, l);
    }

    private DialogArea createSelectiveQuestionArea(Composite parent) {
        Composite c = new Composite(parent, SWT.NONE);
        GridLayout l = new GridLayout();
        l.numColumns = 2;
        l.makeColumnsEqualWidth = false;
        c.setLayout(l);
        return new DialogArea(c, l);
    }

    private DialogArea createFreeFormArea(Composite parent) {
        Composite c = new Composite(parent, SWT.NONE);
        GridLayout l = new GridLayout();
        l.numColumns = 1;
        c.setLayout(l);
        c.setLayoutData(new GridData(GridData.FILL_BOTH));
        return new DialogArea(c, l);
    }

    private Question[] createQuestions() {
        return new Question[] {
                new Question("質問1", "選択してください", "option1-1", "option1-2"),
                new Question("質問2", "選択してください", "option2-1", "option2-2"),
                new Question("質問3", "選択してください", "option3-1", "option3-2"),
        };
    }

    private void createSelectiveQuestionWidgets(Composite container, Question[] questions) {
        for (int i = 0; i < questions.length; ++i) {
            Label label = new Label(container, SWT.LEFT);
            label.setText(questions[i].getQuestion());

            Combo combo = new Combo(container, SWT.READ_ONLY | SWT.RIGHT);
            combo.setItems(questions[i].getOptions());
            combo.select(0);
        }
    }

    private void createFreeFormQuestionWidgets(Composite container) {
        Label label1 = new Label(container, SWT.LEFT);
        label1.setText("C-Helperを使ってみた感想を教えてください");

        Text comment = new Text(container, SWT.MULTI | SWT.CENTER);
        comment.setLayoutData(new GridData(GridData.FILL_BOTH));
        comment.setText("感想");
        comment.selectAll();
    }
}
