package com.github.uchan_nos.c_helper.suggest;

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.cdt.core.dom.ast.*;
import org.eclipse.cdt.core.dom.ast.IBasicType.Kind;
import org.eclipse.core.runtime.preferences.IExportedPreferences;
import org.eclipse.jface.text.BadLocationException;

import com.github.uchan_nos.c_helper.util.ASTFilter;
import com.github.uchan_nos.c_helper.util.Util;

public class PrintfParameterSuggester extends Suggester {

    @Override
    public Collection<Suggestion> suggest(SuggesterInput input,
            AssumptionManager assumptionManager) {
        ArrayList<Suggestion> suggestions =
                new ArrayList<Suggestion>();

        Collection<IASTNode> printfCalls =
            new ASTFilter(input.getAst()).filter(
                new ASTFilter.Predicate() {
                    @Override
                    public boolean pass(IASTNode node) {
                        if (node instanceof IASTFunctionCallExpression) {
                            IASTFunctionCallExpression fce = (IASTFunctionCallExpression) node;
                            IASTName name = Util.getName(fce.getFunctionNameExpression());
                            if (name != null && Util.equals(name.getSimpleID(), "printf")) {
                                return true;
                            }
                        }
                        return false;
                    }
                });

        PrintfFormatAnalyzer analyzer = new PrintfFormatAnalyzer();

        try {
            for (IASTNode node : printfCalls) {
                IASTFunctionCallExpression printfCall = (IASTFunctionCallExpression) node;
                int argc = printfCall.getArguments().length;

                if (argc == 0) {
                    suggestions.add(new Suggestion(
                            input.getSource(), printfCall,
                            "printf 関数に引数が1つも指定されていません。",
                            "printf(\"hello, world\\n\") などと書きます。"));
                    continue;
                }
                IASTInitializerClause arg0 = printfCall.getArguments()[0];
                boolean arg0TypeIsValid = false;
                if (arg0 instanceof IASTExpression) {
                    IASTExpression e = (IASTExpression) arg0;
                    IType arg0Type = e.getExpressionType();
                    if (arg0Type instanceof IPointerType) {
                        IPointerType ptr = (IPointerType) e.getExpressionType();
                        IType ptrToType = ptr.getType();
                        while (ptrToType instanceof IQualifierType) {
                            ptrToType = ((IQualifierType) ptrToType).getType();
                        }
                        if (ptrToType instanceof IBasicType
                                && ((IBasicType) ptrToType).getKind() == Kind.eChar) {
                            arg0TypeIsValid = true;
                        }
                    }
                }
                if (arg0TypeIsValid
                        && !(arg0 instanceof IASTLiteralExpression
                                && ((IASTLiteralExpression) arg0).getKind()
                                == IASTLiteralExpression.lk_string_literal)) {
                    suggestions.add(new Suggestion(
                            input.getSource(), arg0,
                            "printf 関数の第一引数に渡す文字列は、変数ではなく文字列定数であることが推奨されます。",
                            ""));
                    continue;
                } else if (!arg0TypeIsValid) {
                    suggestions.add(new Suggestion(
                            input.getSource(), arg0,
                            "printf 関数の第一引数には書式指定文字列を渡します。",
                            "printf(\"hello, world\\n\") などと書きます。"));
                    continue;
                }

                PrintfFormatAnalyzer.FormatSpecifier[] specs =
                        PrintfFormatAnalyzer.removePercentSpecifier(analyzer.analyze(
                                String.valueOf(((IASTLiteralExpression) arg0).getValue())));

                if (specs.length > argc - 1) {
                    suggestions.add(new Suggestion(
                            input.getSource(), arg0,
                            "printf 関数の書式指定文字列の%変換に用いる引数の数が足りません。",
                            ""));
                    continue;
                } else if (specs.length < argc - 1) {
                    suggestions.add(new Suggestion(
                            input.getSource(), arg0,
                            "printf 関数の書式指定文字列の%変換に対し、引数が多すぎます。",
                            ""));
                    continue;
                }
            }
        } catch (BadLocationException e) {
            assert false : "must not be here";
            e.printStackTrace();
        }
        return suggestions;
    }
}
