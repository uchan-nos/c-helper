package com.github.uchan_nos.c_helper.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;

import com.github.uchan_nos.c_helper.questionnaire.QuestionnaireDialog;

public class QuestionnaireHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        showQuestionnaire(HandlerUtil.getActiveShell(event));
        return null;
    }

    private void showQuestionnaire(Shell shell) {
        QuestionnaireDialog dialog = new QuestionnaireDialog(shell);
        dialog.open();
    }
}
