package com.github.uchan_nos.c_helper.analysis;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.eclipse.cdt.core.dom.ast.IASTIdExpression;
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

/**
 * C言語パーサ.
 * @author uchan
 */
public class Parser {
    private String filePath;
    private String sourceCode;

    /**
     * C言語パーサを生成する.
     * ファイルではなくメモリ上の文字列をパースする場合は、ファイルパスに適当な文字列を与えればよい.
     * @param filePath ソースコードを含んでいるファイルへのパス
     * @param sourceCode ソースコード
     */
    public Parser(String filePath, String sourceCode) {
        this.filePath = filePath;
        this.sourceCode = sourceCode;
    }

    /**
     * ソースコードをパースし、ASTを返す.
     * @return ソースコード全体のAST
     * @throws CoreException
     */
    public IASTTranslationUnit parse() throws CoreException {
        ILanguage language = GCCLanguage.getDefault();

        FileContent reader = FileContent.create(filePath, sourceCode.toCharArray());

        Map<String, String> macroDefinitions = null;
        String[] includeSearchPath = null;
        IScannerInfo scanInfo = new ScannerInfo(macroDefinitions,
                includeSearchPath);

        IncludeFileContentProvider fileCreator = IncludeFileContentProvider
                .getEmptyFilesProvider();
        IIndex index = null;
        int options = ILanguage.OPTION_IS_SOURCE_UNIT;
        IParserLogService log = new DefaultLogService();

        IASTTranslationUnit translationUnit = language
                .getASTTranslationUnit(reader, scanInfo, fileCreator,
                        index, options, log);
        return translationUnit;
   }
}
