package com.github.uchan_nos.c_helper.suggest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.eclipse.cdt.core.dom.ast.*;

import org.eclipse.cdt.core.dom.ast.IBasicType.Kind;
import org.eclipse.jface.text.BadLocationException;

import com.github.uchan_nos.c_helper.resource.StringResource;

import com.github.uchan_nos.c_helper.util.ASTFilter;
import com.github.uchan_nos.c_helper.util.ScanfFormatAnalyzer;
import com.github.uchan_nos.c_helper.util.TypeUtil;
import com.github.uchan_nos.c_helper.util.Util;

/**
 * scanf系関数への値渡し警告.
 * scanf系関数へ値渡ししている場合に警告する.
 * パラメタが足りないとか、型が合わない問題には対応しない.
 */
public class ScanfCallByValueSuggester extends Suggester {

    @Override
    public Collection<Suggestion> suggest(SuggesterInput input,
            AssumptionManager assumptionManager) {
        ArrayList<Suggestion> suggestions = new ArrayList<Suggestion>();

        // scanf系関数の一覧。vargs系は対応しない
        final List<String> scanfLikeFunctionNames = Arrays.asList(
                "scanf", "fscanf", "sscanf");

        // scanf系関数の呼び出し式の一覧
        Collection<IASTNode> scanfLikeFunctionCallExpressions =
            new ASTFilter(input.getAst()).filter(new ASTFilter.Predicate() {
                @Override public boolean pass(IASTNode node) {
                    if (node instanceof IASTFunctionCallExpression) {
                        IASTFunctionCallExpression fce = (IASTFunctionCallExpression) node;
                        IASTExpression nameExpression = fce.getFunctionNameExpression();
                        IASTName name = Util.getName(nameExpression);
                        if (name != null) {
                            String nameString = String.valueOf(name.getSimpleID());
                            if (scanfLikeFunctionNames.contains(nameString)) {
                                return true;
                            }
                        }
                    }
                    return false;
                }});

        try {
            for (IASTNode node : scanfLikeFunctionCallExpressions) {
                Suggestion s = checkCallByValue(input, (IASTFunctionCallExpression) node);
                if (s != null) {
                    suggestions.add(s);
                }
            }
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
        return suggestions;
    }

    // scanf系関数の呼び出し式を検査
    private Suggestion checkCallByValue(SuggesterInput input,
            IASTFunctionCallExpression scanfLikeFunctionCallExpression) throws BadLocationException {
        IASTExpression[] args = Util.getArguments(scanfLikeFunctionCallExpression);
        String functionName = getFunctionName(scanfLikeFunctionCallExpression);
        int startingIndexOfFormatArgument = 0;

        IASTExpression formatStringExpression = null;
        if (functionName.equals("fscanf") || functionName.equals("sscanf")) {
            if (args.length <= 1) {
                return null;
            }
            formatStringExpression = args[1];
            startingIndexOfFormatArgument = 2;
        } else if (functionName.equals("scanf")) {
            if (args.length <= 0) {
                return null;
            }
            formatStringExpression = args[0];
            startingIndexOfFormatArgument = 1;
        } else {
            return null;
        }

        if (formatStringExpression != null) {
            if (TypeUtil.asIASTLiteralExpression(formatStringExpression,
                        IASTLiteralExpression.lk_string_literal) == null) {
                // 書式文字列が文字列定数以外には対応しない
                return null;
            }

            ScanfFormatAnalyzer.FormatSpecifier[] specs
                = new ScanfFormatAnalyzer().analyze(
                        String.valueOf(((IASTLiteralExpression) formatStringExpression).getValue()));

            for (int i = 0; i < specs.length; ++i) {
                IASTExpression arg = args[i + startingIndexOfFormatArgument];
                IType type = TypeUtil.removeQualifiers(arg.getExpressionType());
                if (!(TypeUtil.resolveOuterTypedef(type) instanceof IPointerType)) {
                    String suggest = null;
                    if (arg instanceof IASTIdExpression) {
                        IASTName argName = ((IASTIdExpression) arg).getName();
                        suggest = StringResource.get(
                                "&を付けて変数へのポインタを取得する(&%s)",
                                String.valueOf(argName.getSimpleID()));
                    }
                    return new Suggestion(input.getSource(), arg,
                            StringResource.get(
                                "ポインタを渡す必要がある"),
                            suggest);
                }
            }
        }

        return null;
    }

    private static String getFunctionName(IASTFunctionCallExpression fce) {
        IASTName name = Util.getName(fce.getFunctionNameExpression());
        if (name != null) {
            return String.valueOf(name.getSimpleID());
        }
        return null;
    }
}
