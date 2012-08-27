package com.github.uchan_nos.c_helper.suggest;

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.cdt.core.dom.ast.*;
import org.eclipse.cdt.core.dom.ast.IBasicType.Kind;
import org.eclipse.jface.text.BadLocationException;

import com.github.uchan_nos.c_helper.resource.StringResource;

import com.github.uchan_nos.c_helper.util.ASTFilter;
import com.github.uchan_nos.c_helper.util.PrintfFormatAnalyzer;
import com.github.uchan_nos.c_helper.util.PrintfFormatAnalyzer.Type;
import com.github.uchan_nos.c_helper.util.TypeUtil;
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
                            StringResource.get(
                                "printfに引数が1つも指定されていない。"),
                            StringResource.get(
                                "printf(hello)などと書く。")
                            ));
                    continue;
                }

                boolean arg0TypeIsValid = false;
                IASTExpression[] args = Util.getArguments(printfCall);
                IASTExpression arg0 = args[0];

                IType arg0Type = arg0.getExpressionType();
                if (arg0Type instanceof IPointerType) {
                    IPointerType ptr = (IPointerType) arg0Type;
                    IType ptrToType = TypeUtil.removeQualifiers(ptr.getType());
                    if (Util.isIBasicType(ptrToType, Kind.eChar)) {
                        arg0TypeIsValid = true;
                    }
                }

                if (arg0TypeIsValid
                        && !(arg0 instanceof IASTLiteralExpression
                                && ((IASTLiteralExpression) arg0).getKind()
                                == IASTLiteralExpression.lk_string_literal)) {
                    suggestions.add(new Suggestion(
                            input.getSource(), arg0,
                            StringResource.get(
                                "printfの第一引数は文字列定数であるべき。"),
                            ""));
                    continue;
                } else if (!arg0TypeIsValid) {
                    suggestions.add(new Suggestion(
                            input.getSource(), arg0,
                            StringResource.get(
                                "printfの第一引数には書式文字列を渡す。"),
                            StringResource.get(
                                "printf(hello)などと書く。")
                            ));
                    continue;
                }

                PrintfFormatAnalyzer.FormatSpecifier[] specs =
                        PrintfFormatAnalyzer.removePercentSpecifier(analyzer.analyze(
                                String.valueOf(((IASTLiteralExpression) arg0).getValue())));

                if (specs.length > argc - 1) {
                    suggestions.add(new Suggestion(
                            input.getSource(), arg0,
                            StringResource.get(
                                "printfの引数が足りない。"),
                            ""));
                    continue;
                } else if (specs.length < argc - 1) {
                    suggestions.add(new Suggestion(
                            input.getSource(), arg0,
                            StringResource.get(
                                "printfの引数が多すぎる。"),
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
                            StringResource.get(
                                "関数の引数は式でなければならない。"),
                            ""));
                    continue;
                }
                IASTExpression arg_ = (IASTExpression) arg;
                IType type = TypeUtil.removeQualifiers(arg_.getExpressionType());
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

        type = TypeUtil.removeQualifiers(type);

        final boolean typeIsBasicType = type instanceof IBasicType;
        final boolean typeIsPointer = type instanceof IPointerType;
        final IBasicType basicType = typeIsBasicType ?
                (IBasicType) type : null;

        // type が指す先の型
        final IType pointerToType = typeIsPointer ?
                TypeUtil.removeQualifiers(((IPointerType) type).getType()) : null;
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

        final boolean typeIsValid =
                (typeIsInteger && specTypeIsInteger && typeIsUnsigned == (specType == Type.UINT))
                || (typeIsFloating && specTypeIsFloating)
                || (pointerToTypeIsBasicType && pointerToBasicType.getKind() == Kind.eChar && specType == Type.STRING)
                || (typeIsPointer && typeIsVoidPointer && specType == Type.VOIDPTR);

        /*
        if ((typeIsInteger && !specTypeIsInteger)
                || (typeIsFloating && !specTypeIsFloating)
                || (typeIsIntegerPointer && specType != Type.INTPTR)
                || (pointerToTypeIsBasicType && pointerToBasicType.getKind() == Kind.eChar && specType != Type.STRING)
                || (typeIsPointer && !typeIsIntegerPointer && specType != Type.VOIDPTR)
                || (typeIsUnsigned && specType != Type.UINT)) {
                */
        if (!typeIsValid) {
            // 型が合わない場合
            String suggestion = "";
            if (typeIsInteger) {
                if (typeIsUnsigned) {
                    if (typeIsShort || typeIsNormalLength) {
                        suggestion = "符号なし整数の表示は%%u,%%xなど。";
                    } else if (typeIsLong) {
                        suggestion = "long符号なし整数の表示は%%lu,%%lxなど。";
                    } else if (typeIsLongLong) {
                        suggestion = "longlong符号なし整数の表示は%%llu,%%llxなど。";
                    }
                } else {
                    if (typeIsShort || typeIsNormalLength) {
                        suggestion = "符号付き整数の表示は%%d";
                    } else if (typeIsLong) {
                        suggestion = "long符号付き整数の表示は%%ld";
                    } else if (typeIsLongLong) {
                        suggestion = "longlong符号付き整数の表示は%%lld";
                    }
                }
            } else if (typeIsFloating) {
                if (typeIsNormalLength) {
                    suggestion = "浮動小数点数の表示は%%fなど";
                }
            } else if (typeIsPointer) {
                if (pointerToTypeIsBasicType && pointerToBasicType.getKind() == Kind.eChar) {
                    suggestion = "文字列の表示は%%s";
                } else {
                    suggestion = "ポインタ値の表示は%%p";
                }
            }
            if (specTypeIsFloating) {
                return new MessageSuggestion(
                        StringResource.get(
                            "引数は整数型だが%%%cは浮動小数点数型を期待している。", spec.type),
                        StringResource.get(suggestion));
            } else {
                return new MessageSuggestion(
                        StringResource.get(
                            "引数と%%変換の型が合わない。"),
                        StringResource.get(suggestion));
            }
        } else if ( (typeIsShort || typeIsNormalLength)
                && !(spec.length.equals("") || spec.length.equals("h") || spec.length.equals("hh"))
                || (typeIsLong && !spec.length.equals("l"))
                || (typeIsLongLong && !spec.length.equals("ll"))) {
            String suggestion = "";
            return new MessageSuggestion(
                    StringResource.get("引数型と%%変換のサイズが合わない。"),
                    StringResource.get(suggestion));
        }
        return null;
    }
}
