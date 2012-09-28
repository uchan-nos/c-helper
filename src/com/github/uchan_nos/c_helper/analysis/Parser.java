package com.github.uchan_nos.c_helper.analysis;

import java.util.HashMap;
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

        Map<String, String> macroDefinitions = new HashMap<String, String>();
        macroDefinitions.put("__STDC__", "100");

        String[] includeSearchPath = new String[] { "/Users/uchan/git/c-helper/stdheaders" };
        IScannerInfo scanInfo = new ScannerInfo(macroDefinitions,
                includeSearchPath);

        IncludeFileContentProvider fileCreator =
                //IncludeFileContentProvider.getSavedFilesProvider();
                //IncludeFileContentProvider.getEmptyFilesProvider();
                new MyFileContentProvider("/Users/uchan/git/c-helper/stdheaders");
        IIndex index = null;
        int options = ILanguage.OPTION_IS_SOURCE_UNIT;
        IParserLogService log = new DefaultLogService();

        IASTTranslationUnit translationUnit = language
                .getASTTranslationUnit(reader, scanInfo, fileCreator,
                        index, options, log);
        return translationUnit;
   }

    /**
     * ソースコードをパースし、ASTを返す.
     * 内部でparse()を呼び出す.
     * パースに失敗した場合はnullを返す.
     * @return ソースコード全体のAST, またはnull.
     */
    public IASTTranslationUnit parseOrNull() {
       try {
           IASTTranslationUnit tu = parse();
           return tu;
       } catch (CoreException e) {
           return null;
       }
   }
}
