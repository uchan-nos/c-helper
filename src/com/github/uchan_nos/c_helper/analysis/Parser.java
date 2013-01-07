package com.github.uchan_nos.c_helper.analysis;

import java.util.HashMap;

import java.util.logging.Logger;
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

import com.github.uchan_nos.c_helper.Activator;

/**
 * C言語パーサ.
 * @author uchan
 */
public class Parser {
    private final Logger logger = Activator.getLogger();

    private FileInfo fileInfo;
    private String sourceCode;

    /**
     * C言語パーサを生成する.
     * ファイルではなくメモリ上の文字列をパースする場合は、ファイルパスに適当な文字列を与えればよい.
     * @param filePath ソースコードを含んでいるファイルへのパス
     * @param sourceCode ソースコード
     */
    public Parser(FileInfo fileInfo, String sourceCode) {
        this.fileInfo = fileInfo;
        this.sourceCode = sourceCode;
    }

    /**
     * ソースコードをパースし、ASTを返す.
     * @return ソースコード全体のAST
     * @throws CoreException
     */
    public IASTTranslationUnit parse() throws CoreException {
        logger.finest("Parser#parse()");

        ILanguage language = GCCLanguage.getDefault();

        FileContent reader = FileContent.create(fileInfo.getPath(), sourceCode.toCharArray());

        Map<String, String> macroDefinitions = new HashMap<String, String>();
        macroDefinitions.put("__STDC__", "100");

        String stdheaderDirPath = "";

        logger.finest("  setting include search path: " + stdheaderDirPath);

        /*
         * includeSearchPathに指定したディレクトリにヘッダファイル名を付加したパス名が
         * MyFileContentProvider#getContentForInclusionに渡される
         */
        String[] includeSearchPath = new String[] { stdheaderDirPath };
        IScannerInfo scanInfo = new ScannerInfo(macroDefinitions, includeSearchPath);

        logger.finest("  creating include file content provider");

        IncludeFileContentProvider fileCreator =
                //IncludeFileContentProvider.getSavedFilesProvider();
                //IncludeFileContentProvider.getEmptyFilesProvider();
                //new MyFileContentProvider(stdheaderDirPath);
                new MyFileContentProvider("stdheaders", fileInfo);
        IIndex index = null;
        int options = ILanguage.OPTION_IS_SOURCE_UNIT;
        IParserLogService log = new DefaultLogService();

        logger.finest("  getting ast translation unit");

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
