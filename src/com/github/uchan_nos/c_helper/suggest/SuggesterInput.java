package com.github.uchan_nos.c_helper.suggest;

import java.util.Map;

import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.jface.text.IDocument;

import com.github.uchan_nos.c_helper.analysis.AnalysisEnvironment;
import com.github.uchan_nos.c_helper.analysis.CFG;
import com.github.uchan_nos.c_helper.analysis.RD;

/**
 * 各種サジェスト機能で用いる共通の入力データ.
 * @author uchan
 *
 */
public class SuggesterInput {
    private final String filePath;
    private final IDocument source;
    private final IASTTranslationUnit ast;
    private final Map<String, CFG> procToCFG;
    private final Map<String, RD<CFG.Vertex>> procToRD;
    private final AnalysisEnvironment analysisEnvironment;

    public SuggesterInput(String filePath,
            IDocument source,
            IASTTranslationUnit ast,
            Map<String, CFG> procToCFG,
            Map<String, RD<CFG.Vertex>> procToRD,
            AnalysisEnvironment analysisEnvironment) {
        this.filePath = filePath;
        this.source = source;
        this.ast = ast;
        this.procToCFG = procToCFG;
        this.procToRD = procToRD;
        this.analysisEnvironment = analysisEnvironment;
    }

    public String getFilePath() {
        return filePath;
    }

    public IDocument getSource() {
        return source;
    }

    public IASTTranslationUnit getAst() {
        return ast;
    }

    public Map<String, CFG> getProcToCFG() {
        return procToCFG;
    }

    public Map<String, RD<CFG.Vertex>> getProcToRD() {
        return procToRD;
    }

    public AnalysisEnvironment getAnalysisEnvironment() {
        return analysisEnvironment;
    }
}
