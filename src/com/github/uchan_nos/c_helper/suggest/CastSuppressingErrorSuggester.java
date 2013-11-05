package com.github.uchan_nos.c_helper.suggest;

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.cdt.core.dom.ast.*;
import org.eclipse.jface.text.BadLocationException;

import com.github.uchan_nos.c_helper.resource.StringResource;

import com.github.uchan_nos.c_helper.util.ASTFilter;
import com.github.uchan_nos.c_helper.util.PrintfFormatAnalyzer;
import com.github.uchan_nos.c_helper.util.TypeUtil;
import com.github.uchan_nos.c_helper.util.Util;

public class CastSuppressingErrorSuggester extends Suggester {

    @Override
    public Collection<Suggestion> suggest(final SuggesterInput input,
            AssumptionManager assumptionManager) {
        ArrayList<Suggestion> suggestions = new ArrayList<Suggestion>();

        Collection<IASTNode> castExpressions =
            new ASTFilter(input.getAst()).filter(
                new ASTFilter.Predicate() {
                    @Override
                    public boolean pass(IASTNode node) {
                        return node instanceof IASTCastExpression;
                    }
                });

        for (IASTNode node : castExpressions) {
            Collection<Suggestion> s = suggest((IASTCastExpression) node, input);
            if (s != null) {
                suggestions.addAll(s);
            }
        }
        return suggestions;
    }

    private Collection<Suggestion> suggest(
            IASTCastExpression castExpression, final SuggesterInput input) {
        ArrayList<Suggestion> suggestions = new ArrayList<Suggestion>();

        IType castedType = castExpression.getExpressionType();
        IType operandType = castExpression.getOperand().getExpressionType();

        if (castedType instanceof IPointerType && operandType instanceof IBasicType) {
            String message = "";
            String suggestion = "";

            IASTFunctionCallExpression fce =
                    getParentFunctionCallExpression(castExpression);
            if (fce != null) {
                IASTName name = Util.getName(fce.getFunctionNameExpression());
                IASTExpression[] args = Util.getArguments(fce);
                if (name != null && Util.equals(name.getSimpleID(), "printf")) {
                    String formatString = null;

                    // 書式指定文字列を取得する
                    IASTLiteralExpression le;
                    if ((le = TypeUtil.asIASTLiteralExpression(args[0],
                                IASTLiteralExpression.lk_string_literal)) != null) {
                        formatString = String.valueOf(le.getValue());
                    }

                    if (formatString != null) {
                        // castExpression が何番目の引数か検索
                        int indexOfCastExpression = -1;
                        for (int i = 1; i < args.length; ++i) {
                            if (args[i].contains(castExpression)) {
                                indexOfCastExpression = i;
                                break;
                            }
                        }

                        PrintfFormatAnalyzer.FormatSpecifier[] formatSpecifiers =
                                PrintfFormatAnalyzer.removePercentSpecifier(
                                        new PrintfFormatAnalyzer().analyze(formatString));

                        if (indexOfCastExpression >= 1 && (indexOfCastExpression - 1) < formatSpecifiers.length) {
                            PrintfFormatAnalyzer.FormatSpecifier spec =
                                    formatSpecifiers[indexOfCastExpression - 1];
                            if (spec.type == 's') {
                                suggestion = StringResource.get(
                                        "式%sが1文字を表しているなら%%cで表示可能。",
                                        castExpression.getOperand().getRawSignature());
                            }
                        }
                    }
                }
            }

            if (suggestion.length() == 0 && message.length() == 0) {
                IBasicType opt = (IBasicType) operandType;
                if (opt.getKind() == IBasicType.Kind.eChar
                        || opt.getKind() == IBasicType.Kind.eInt) {
                    if (castExpression.getOperand().getRawSignature().equals("0")
                            || castExpression.getRawSignature().equals("NULL")) {
                        // 整数リテラル0からポインタへのキャスト、NULLマクロの使用は例外として認める。
                    } else {
                        suggestion = StringResource.get(
                                "整数からポインタ型へのキャストは危険。");
                    }
                }
            }

            if (suggestion.length() > 0) {
                try {
                    suggestions.add(new Suggestion(
                            input.getSource(), castExpression,
                            suggestion, message));
                } catch (BadLocationException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }

        return suggestions;
    }

    private IASTFunctionCallExpression getParentFunctionCallExpression(
            IASTExpression expression) {
        IASTNode parent = expression;

        while (parent != null && !(parent instanceof IASTFunctionCallExpression)) {
            parent = parent.getParent();
        }

        return (IASTFunctionCallExpression) parent;
    }

}
