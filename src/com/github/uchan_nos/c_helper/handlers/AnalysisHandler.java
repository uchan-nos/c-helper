package com.github.uchan_nos.c_helper.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.jface.dialogs.MessageDialog;

import com.github.uchan_nos.c_helper.analysis.Analyzer;
import com.github.uchan_nos.c_helper.exceptions.InvalidEditorPartException;

/**
 * Our sample handler extends AbstractHandler, an IHandler base class.
 *
 * @see org.eclipse.core.commands.IHandler
 * @see org.eclipse.core.commands.AbstractHandler
 */
public class AnalysisHandler extends AbstractHandler {
    /**
     * The constructor.
     */
    public AnalysisHandler() {
    }

    /**
     * the command has been executed, so extract extract the needed information
     * from the application context.
     */
    public Object execute(ExecutionEvent event) throws ExecutionException {
        Analyzer analyzer = new Analyzer();
        try {
            IEditorPart activeEditorPart = HandlerUtil.getActiveEditorChecked(event);
            analyzer.analyze(activeEditorPart, new Analyzer.RunOption());
        } catch (InvalidEditorPartException e) {
            e.printStackTrace();
            IWorkbenchWindow window = HandlerUtil
                    .getActiveWorkbenchWindowChecked(event);
            MessageDialog.openError(window.getShell(), "c-helper",
                    "Failed to analyze source code.");
        }
        return null;
    }
}
