package com.github.uchan_nos.c_helper.suggest;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Logger;

import org.eclipse.cdt.core.dom.ast.*;
import org.eclipse.cdt.internal.core.dom.parser.c.ICInternalFunction;
import org.eclipse.core.runtime.CoreException;

import org.eclipse.jface.text.BadLocationException;

import com.github.uchan_nos.c_helper.Activator;
import com.github.uchan_nos.c_helper.analysis.FileInfo;
import com.github.uchan_nos.c_helper.analysis.Parser;
import com.github.uchan_nos.c_helper.resource.StringResource;

import com.github.uchan_nos.c_helper.util.ASTFilter;
import com.github.uchan_nos.c_helper.util.FileLoader;
import com.github.uchan_nos.c_helper.util.Util;

/**
 * 未宣言の関数の呼び出しを検出し、可能ならインクルードすべきヘッダを提案
 * @author uchan
 *
 */
public class UndeclaredFunctionSuggester extends Suggester {
    private Map<String, IASTTranslationUnit> parsedStdHeaderMap = null;

    /**
     * 指定された名前の関数の宣言が含まれる標準ヘッダ名を返す.
     */
    private String findCorrespondingHeader(char[] functionName) {
        for (Map.Entry<String, IASTTranslationUnit> e : parsedStdHeaderMap.entrySet()) {
            for (IASTDeclaration d : e.getValue().getDeclarations()) {
                do {
                    if (!(d instanceof IASTSimpleDeclaration)) break;
                    if (((IASTSimpleDeclaration) d).getDeclarators().length != 1) break;
                    IASTDeclarator declarator = ((IASTSimpleDeclaration) d).getDeclarators()[0];
                    if (!(declarator instanceof IASTFunctionDeclarator)) break;

                    // 関数宣言が見つかったので、関数名を比較
                    IASTName name = ((IASTFunctionDeclarator) declarator).getName();
                    if (Arrays.equals(name.getSimpleID(), functionName)) {
                        return e.getKey();
                    }
                } while (false);
            }
        }

        return null;
    }

    @Override
    public Collection<Suggestion> suggest(SuggesterInput input, AssumptionManager assumptionManager) {
        ArrayList<Suggestion> suggestions = new ArrayList<Suggestion>();

        if (parsedStdHeaderMap == null) {
            parsedStdHeaderMap = parseStdHeaders();
        }

        // 関数呼び出し式をすべて取得
        Collection<IASTNode> functionCallExpressions = new ASTFilter(input.getAst()).filter(
                new ASTFilter.Predicate() {
                    @Override public boolean pass(IASTNode node) {
                        return node instanceof IASTFunctionCallExpression;
                    }
                });

        try {
            for (IASTNode functionCallExpression : functionCallExpressions) {
                IASTFunctionCallExpression fce = (IASTFunctionCallExpression) functionCallExpression;

                IASTName functionName = Util.getName(fce.getFunctionNameExpression());
                if (functionName != null) {
                    IBinding b = functionName.resolveBinding();
                    if (b instanceof ICInternalFunction) {
                        ICInternalFunction f = (ICInternalFunction) b;
                        if (f.getDeclarations() == null || f.getDeclarations().length == 0) {
                            // 宣言が見つからない

                            String suggestionString = null;
                            String header = findCorrespondingHeader(
                                    functionName.getSimpleID());
                            if (header != null) {
                                suggestionString = StringResource.get(
                                        "%s.hをインクルードする", header);
                            }

                            suggestions.add(new Suggestion(input.getSource(), fce.getFunctionNameExpression(),
                                    StringResource.get("%sは宣言されていない関数名",
                                        String.valueOf(functionName.getSimpleID())),
                                    suggestionString));

                        }
                    }
                }

            }
        } catch (BadLocationException e) {
            e.printStackTrace();
        }

        return suggestions;
    }

    private static class StdHeaderParser implements Util.Function<IASTTranslationUnit, String> {
    	private final Logger logger = Activator.getLogger();
        /**
         * 指定された標準ヘッダをパースする.
         * @param headerName 標準ヘッダ名（"stdio", "stdlib"など）
         */
        @Override
        public IASTTranslationUnit calc(String headerName) {
            String filePath = "stdheaders/" + headerName + ".h";
            try {
                InputStream is = FileLoader.getInstance().openStreamForEmbeddedFile(filePath);
                if (is == null) {
                    return null;
                }
                String sourceCode = Util.readInputStreamAll(is);
                Parser parser = new Parser(new FileInfo(filePath, false), sourceCode);
                IASTTranslationUnit tu = parser.parse();
                return tu;
            } catch (FileNotFoundException e) {
                logger.info(e.getMessage());
            } catch (IOException e) {
                logger.warning(e.getMessage());
            } catch (CoreException e) {
                logger.warning(e.getMessage());
            }
            return null;
        }
    }

    // すべての標準ヘッダをパースして返す.
    private static Map<String, IASTTranslationUnit> parseStdHeaders() {
        Map<String, IASTTranslationUnit> result = new HashMap<String, IASTTranslationUnit>();

        // 標準ヘッダ名一覧
        Collection<String> headerNames = Arrays.asList(
                "assert", "ctype", "locale", "math", "setjmp", "signal",
                "stdarg", "stdio", "stdlib", "string", "time");

        // 標準ヘッダをすべてパースする
        Collection<IASTTranslationUnit> parsed = Util.map(
                headerNames,
                new StdHeaderParser(),
                new ArrayList<IASTTranslationUnit>(headerNames.size()));

        // 標準ヘッダ名をキー、パース結果を値とする辞書を作る
        Iterator<String> it0 = headerNames.iterator();
        Iterator<IASTTranslationUnit> it1 = parsed.iterator();
        while (it0.hasNext()) {
            String key = it0.next();
            IASTTranslationUnit value = it1.next();
            if (value != null) {
                result.put(key, value);
            }
        }

        return result;
    }
}
