package com.github.uchan_nos.c_helper.analysis;

import java.util.Map;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.gnu.c.GCCLanguage;
import org.eclipse.cdt.core.index.IIndex;
import org.eclipse.cdt.core.model.ILanguage;
import org.eclipse.cdt.core.parser.DefaultLogService;
import org.eclipse.cdt.core.parser.FileContent;
import org.eclipse.cdt.core.parser.IParserLogService;
import org.eclipse.cdt.core.parser.IScannerInfo;
import org.eclipse.cdt.core.parser.IncludeFileContentProvider;
import org.eclipse.cdt.core.parser.ScannerInfo;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.texteditor.ITextEditor;

import com.github.uchan_nos.c_helper.exceptions.InvalidEditorPartException;

public class Analyzer {
    public void analyze(IEditorPart activeEditorPart) throws InvalidEditorPartException {
        if (activeEditorPart instanceof ITextEditor) {
            ITextEditor textEditorPart = (ITextEditor)activeEditorPart;
            IDocument documentToAnalyze =
                    textEditorPart.getDocumentProvider().getDocument(
                            textEditorPart.getEditorInput());
            analyze(textEditorPart.getTitle(), documentToAnalyze.get().toCharArray());
        } else {
            throw new InvalidEditorPartException("We need a class implementing ITextEditor");
        }
    }

    public void analyze(String filePath, char[] source) {
        ILanguage language = GCCLanguage.getDefault();

        FileContent reader = FileContent.create(filePath, source);

        Map<String, String> macroDefinitions = null;
        String[] includeSearchPath = null;
        IScannerInfo scanInfo = new ScannerInfo(macroDefinitions, includeSearchPath );

        IncludeFileContentProvider fileCreator = IncludeFileContentProvider.getEmptyFilesProvider();
        IIndex index = null;
        int options = 0;
        IParserLogService log = new DefaultLogService();

        try {
            IASTTranslationUnit translationUnit = language.getASTTranslationUnit(reader, scanInfo, fileCreator, index, options, log);
            createCFG(translationUnit);
        } catch (CoreException e) {
            e.printStackTrace();
        }
    }

    private void createCFG(IASTTranslationUnit ast) {

    }
}
