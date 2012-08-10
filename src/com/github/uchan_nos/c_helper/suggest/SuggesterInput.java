package com.github.uchan_nos.c_helper.suggest;

import java.util.Map;

import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;

import com.github.uchan_nos.c_helper.analysis.CFG;
import com.github.uchan_nos.c_helper.analysis.RD;

/**
 * 各種サジェスト機能で用いる共通の入力データ.
 * @author uchan
 *
 */
public class SuggesterInput {
    private final String filePath;
    private final String source;
    private final IASTTranslationUnit ast;
    private final Map<String, CFG> procToCFG;
    private final Map<String, RD<CFG.Vertex>> procToRD;

    public SuggesterInput(String filePath,
            String source,
            IASTTranslationUnit ast,
            Map<String, CFG> procToCFG,
            Map<String, RD<CFG.Vertex>> procToRD) {
        this.filePath = filePath;
        this.source = source;
        this.ast = ast;
        this.procToCFG = procToCFG;
        this.procToRD = procToRD;
    }

    public String getFilePath() {
        return filePath;
    }

    public String getSource() {
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
}
