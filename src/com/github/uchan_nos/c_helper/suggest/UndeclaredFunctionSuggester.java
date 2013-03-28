package com.github.uchan_nos.c_helper.suggest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.eclipse.cdt.core.dom.ast.*;
import org.eclipse.cdt.internal.core.dom.parser.c.ICInternalFunction;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;

import com.github.uchan_nos.c_helper.resource.StringResource;

import com.github.uchan_nos.c_helper.util.ASTFilter;
import com.github.uchan_nos.c_helper.util.EscapeSequenceDecoder;
import com.github.uchan_nos.c_helper.util.Util;

/**
 * 未宣言の関数の呼び出しを検出し、可能ならインクルードすべきヘッダを提案
 * @author uchan
 *
 */
public class UndeclaredFunctionSuggester extends Suggester {

    private static final HashMap<String, HashSet<String>> functionNames;

    static {
        functionNames = new HashMap<String, HashSet<String>>();
        functionNames.put("stdio", new HashSet<String>(Arrays.asList(
                "printf", "scanf")));
        functionNames.put("stdlib", new HashSet<String>(Arrays.asList(
            "malloc", "free")));
    }

    private static String findCorrespondingHeader(String functionName) {
        for (Map.Entry<String, HashSet<String>> e : functionNames.entrySet()) {
            if (e.getValue().contains(functionName)) {
                return e.getKey();
            }
        }
        return null;
    }

    @Override
    public Collection<Suggestion> suggest(SuggesterInput input, AssumptionManager assumptionManager) {
        ArrayList<Suggestion> suggestions = new ArrayList<Suggestion>();

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
                        if (f.getDeclarations().length == 0) {
                            // 宣言が見つからない

                            String suggestionString = null;
                            String header = findCorrespondingHeader(
                                    fce.getFunctionNameExpression().getRawSignature());
                            if (header != null) {
                                suggestionString = StringResource.get(
                                        "%s.hをインクルードする", header);
                            }

                            suggestions.add(new Suggestion(input.getSource(), fce.getFunctionNameExpression(),
                                    StringResource.get("宣言されていない関数名"),
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
}
