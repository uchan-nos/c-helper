package com.github.uchan_nos.c_helper.suggest;

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.cdt.core.dom.ast.*;
import org.eclipse.cdt.core.dom.ast.IBasicType.Kind;
import org.eclipse.jface.text.BadLocationException;

import com.github.uchan_nos.c_helper.util.ASTFilter;
import com.github.uchan_nos.c_helper.util.PrintfFormatAnalyzer;
import com.github.uchan_nos.c_helper.util.Util;

public class PrintfParameterSuggester extends Suggester {

    private static class MessageSuggestion {
        public final String message;
        public final String suggestion;
        public MessageSuggestion(String message, String suggestion) {
            this.message = message;
            this.suggestion = suggestion;
        }
    }

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
                } else {
                    suggestions.addAll(suggestDifferentType(input, printfCall, specs));
                }
            }
        } catch (BadLocationException e) {
            assert false : "must not be here";
            e.printStackTrace();
        }
        return suggestions;
    }

    // 書式指定子の型と引数の型が違う場合に警告する.
    // 書式指定子の数と引数の数が合っていることを前提とする.
    private Collection<Suggestion> suggestDifferentType(
            final SuggesterInput input,
            IASTFunctionCallExpression printfCall,
            PrintfFormatAnalyzer.FormatSpecifier[] formatSpecifiers) {
        ArrayList<Suggestion> suggestions = new ArrayList<Suggestion>();

        try {
            for (int i = 0; i < formatSpecifiers.length; ++i) {
                IASTInitializerClause arg = printfCall.getArguments()[i];
                if (!(arg instanceof IASTExpression)) {
                    suggestions.add(new Suggestion(
                            input.getSource(), arg,
                            "C89 (ANSI-C) では関数の引数は「式」でなければなりません。",
                            ""));
                    continue;
                }
                IASTExpression arg_ = (IASTExpression) arg;
                IType type = Util.removeQualifier(arg_.getExpressionType());
                MessageSuggestion ms = suggest(type, formatSpecifiers[i]);
                if (ms != null) {
                    suggestions.add(new Suggestion(input.getSource(), arg_, ms.message, ms.suggestion));
                }
            }
        } catch (BadLocationException e) {
            assert false : "must not be here";
            e.printStackTrace();
        }

        return suggestions;
    }

    private MessageSuggestion suggest(IType type, PrintfFormatAnalyzer.FormatSpecifier spec) {
        PrintfFormatAnalyzer.Type specType =
                PrintfFormatAnalyzer.EXPECTED_TYPE.get(spec.type);
        final boolean typeIsBasicType = type instanceof IBasicType;
        final boolean typeIsShort = typeIsBasicType ?
                ((IBasicType) type).isShort() : false;
        final boolean typeIsLong = typeIsBasicType ?
                ((IBasicType) type).isLong() : false;
        final boolean typeIsLongLong = typeIsBasicType ?
                ((IBasicType) type).isLongLong() : false;
        final boolean typeIsUnsigned = typeIsBasicType ?
                ((IBasicType) type).isUnsigned() : false;
        final IBasicType basicType = typeIsBasicType ?
                (IBasicType) type : null;
        switch (specType) {
        case INT:
            if (typeIsBasicType && !typeIsUnsigned && !typeIsLong && !typeIsLongLong) {
                return null;
            } break;
        case DOUBLE:
            if (typeIsBasicType
                    && (basicType.getKind() == Kind.eFloat
                    || basicType.getKind() == Kind.eDouble)) {
                return null;
            } break;
        case CHAR:
            if (typeIsBasicType && basicType.getKind() == Kind.eChar) {
                return null;
            } break;
        case UINT:
            if (typeIsBasicType && typeIsUnsigned && !typeIsLong && !typeIsLong) {
                return null;
            } break;
        case STRING:
            return null;
        case VOIDPTR:
            return null;
        case INTPTR:
            return null;
        }
        return new MessageSuggestion("%変換と引数は互換性がありません", "");
    }
}
