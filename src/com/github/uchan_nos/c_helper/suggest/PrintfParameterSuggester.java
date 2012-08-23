package com.github.uchan_nos.c_helper.suggest;

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.cdt.core.dom.ast.*;
import org.eclipse.cdt.core.dom.ast.IBasicType.Kind;
import org.eclipse.jface.text.BadLocationException;

import com.github.uchan_nos.c_helper.util.ASTFilter;
import com.github.uchan_nos.c_helper.util.PrintfFormatAnalyzer;
import com.github.uchan_nos.c_helper.util.PrintfFormatAnalyzer.Type;
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
                IASTInitializerClause arg = printfCall.getArguments()[i + 1];
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

        type = Util.removeQualifier(type);

        final boolean typeIsBasicType = type instanceof IBasicType;
        final boolean typeIsPointer = type instanceof IPointerType;
        final IBasicType basicType = typeIsBasicType ?
                (IBasicType) type : null;

        // type が指す先の型
        final IType pointerToType = typeIsPointer ?
                Util.removeQualifier(((IPointerType) type).getType()) : null;
        // type が指す先の型が基本型かどうか
        final boolean pointerToTypeIsBasicType = pointerToType instanceof IBasicType;
        // type が指す先の型が基本型の場合はその型
        final IBasicType pointerToBasicType = pointerToTypeIsBasicType ?
                (IBasicType) pointerToType : null;

        final boolean typeIsInteger = typeIsBasicType ?
                (basicType.getKind() == Kind.eChar
                || basicType.getKind() == Kind.eInt) : false;
        final boolean typeIsFloating = typeIsBasicType ?
                (basicType.getKind() == Kind.eFloat
                || basicType.getKind() == Kind.eDouble) : false;
        final boolean typeIsIntegerPointer =
                typeIsPointer
                && pointerToTypeIsBasicType && pointerToBasicType.getKind() == Kind.eInt
                && !pointerToBasicType.isShort()
                && !pointerToBasicType.isLong()
                && !pointerToBasicType.isLongLong();
        final boolean typeIsVoidPointer =
                typeIsPointer
                && pointerToTypeIsBasicType && pointerToBasicType.getKind() == Kind.eVoid;

        final boolean specTypeIsInteger =
                PrintfFormatAnalyzer.EXPECTED_TYPE.get(spec.type) == Type.INT
                || PrintfFormatAnalyzer.EXPECTED_TYPE.get(spec.type) == Type.UINT
                || PrintfFormatAnalyzer.EXPECTED_TYPE.get(spec.type) == Type.CHAR;
        final boolean specTypeIsFloating =
                PrintfFormatAnalyzer.EXPECTED_TYPE.get(spec.type) == Type.DOUBLE;

        final boolean typeIsShort = typeIsBasicType ?
                basicType.isShort() : false;
        final boolean typeIsLong = typeIsBasicType ?
                basicType.isLong() : false;
        final boolean typeIsLongLong = typeIsBasicType ?
                basicType.isLongLong() : false;
        final boolean typeIsNormalLength =
                !typeIsShort && !typeIsLong && !typeIsLongLong;

        final boolean typeIsUnsigned = typeIsBasicType ?
                basicType.isUnsigned() : false;

        if ((typeIsInteger && !specTypeIsInteger)
                || (typeIsFloating && !specTypeIsFloating)
                || (typeIsIntegerPointer && specType != Type.INTPTR)
                || (typeIsPointer && !typeIsIntegerPointer && specType != Type.VOIDPTR)
                || (typeIsUnsigned && specType != Type.UINT)) {
            // 型が合わない場合
            String suggestion = "";
            if (typeIsInteger) {
                if (typeIsUnsigned) {
                    if (typeIsShort || typeIsNormalLength) {
                        suggestion = "符号なし整数の表示には %u, %x などが使えます。";
                    } else if (typeIsLong) {
                        suggestion = "long 符号なし整数の表示には %lu, %lx などが使えます。";
                    } else if (typeIsLongLong) {
                        suggestion = "long long 符号なし整数の表示には %llu, %llx などが使えます。";
                    }
                } else {
                    if (typeIsShort || typeIsNormalLength) {
                        suggestion = "符号付き整数の表示には %d が使えます。";
                    } else if (typeIsLong) {
                        suggestion = "long 符号付き整数の表示には %ld が使えます。";
                    } else if (typeIsLongLong) {
                        suggestion = "long long 符号付き整数の表示には %lld が使えます。";
                    }
                }
            } else if (typeIsFloating) {
                if (typeIsNormalLength) {
                    suggestion = "浮動小数点数の表示には %f, %e, %g などが使えます。";
                }
            } else if (typeIsPointer) {
                suggestion = "ポインタ値は %p で表示できます。";
            }
            if (specTypeIsFloating) {
                return new MessageSuggestion("引数は整数型ですが %" + spec.type + " は浮動小数点数型を期待しています。", suggestion);
            } else {
                return new MessageSuggestion("引数と %変換の型が合いません。", suggestion);
            }
        } else if ( (typeIsShort || typeIsNormalLength)
                && !(spec.length.equals("") || spec.length.equals("h") || spec.length.equals("hh"))
                || (typeIsLong && !spec.length.equals("l"))
                || (typeIsLongLong && !spec.length.equals("ll"))) {
            String suggestion = "";
            return new MessageSuggestion("引数の型のサイズと %変換の型のサイズが合いません。", suggestion);
        }
        return null;
    }
}
