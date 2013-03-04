package com.github.uchan_nos.c_helper.questionnaire;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.*;

import com.github.uchan_nos.c_helper.Activator;

public class QuestionnaireDialog extends Dialog {

    public QuestionnaireDialog(Shell parentShell) {
        super(parentShell);
        setShellStyle(getShellStyle() | SWT.RESIZE);
    }

    @Override
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);
        newShell.setText("C-Helper アンケート");
        newShell.setImage(
                Activator.getImageDescriptor("icons/analysis.png").createImage());
    }

    @Override
    protected Point getInitialSize() {
        return new Point(400, 300);
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        // OK, キャンセルボタンを除く、画面全体のレイアウト
        DialogArea dialogArea;
        {
            Composite c = parent;
            GridLayout l = new GridLayout();
            l.numColumns = 1;
            c.setLayout(l);
            dialogArea = new DialogArea(c, l);
        }

        // 選択式質問エリア
        DialogArea selectiveQuestionArea;
        {
            Composite c = new Composite(dialogArea.composite(), SWT.NONE);
            GridLayout l = new GridLayout();
            l.numColumns = 2;
            l.makeColumnsEqualWidth = false;
            c.setLayout(l);
            selectiveQuestionArea = new DialogArea(c, l);
        }

        // 自由記述質問エリア
        DialogArea freeFormArea;
        {
            Composite c = new Composite(dialogArea.composite(), SWT.NONE);
            GridLayout l = new GridLayout();
            l.numColumns = 1;
            c.setLayout(l);
            c.setLayoutData(new GridData(GridData.FILL_BOTH));
            freeFormArea = new DialogArea(c, l);
        }

        Question[] questions = new Question[] {
                new Question("質問1", "選択してください", "question1", "question2"),
                new Question("質問2", "選択してください", "question1", "question2"),
                new Question("質問3", "選択してください", "question1", "question2"),
        };
        Label[] questionLabels = new Label[questions.length];
        Combo[] questionComboBoxes = new Combo[questions.length];

        for (int i = 0; i < questions.length; ++i) {
            questionLabels[i] = new Label(selectiveQuestionArea.composite(), SWT.LEFT);
            questionLabels[i].setText(questions[i].getQuestion());

            questionComboBoxes[i] = new Combo(selectiveQuestionArea.composite(), SWT.READ_ONLY | SWT.RIGHT);
            questionComboBoxes[i].setItems(questions[i].getOptions());
            questionComboBoxes[i].select(0);
        }

        Label label1 = new Label(freeFormArea.composite(), SWT.LEFT);
        label1.setText("C-Helperを使ってみた感想を教えてください");

        Text comment = new Text(freeFormArea.composite(), SWT.MULTI | SWT.CENTER);
        comment.setLayoutData(new GridData(GridData.FILL_BOTH));
        comment.setText("感想");
        comment.selectAll();

        return parent;
    }

}
