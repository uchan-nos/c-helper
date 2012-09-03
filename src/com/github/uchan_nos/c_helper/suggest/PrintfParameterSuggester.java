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
                    if (TypeUtil.isIBasicType(ptrToType, Kind.eChar)) {
                        arg0TypeIsValid = true;
                    }
                }

                if (arg0TypeIsValid
                        && TypeUtil.asIASTLiteralExpression(arg0,
                            IASTLiteralExpression.lk_string_literal) == null) {
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
            IASTExpression[] args = Util.getArguments(printfCall);
            for (int i = 0; i < formatSpecifiers.length; ++i) {
                IASTExpression arg = args[i + 1];
                IType type = TypeUtil.removeQualifiers(arg.getExpressionType());
                MessageSuggestion ms = suggest(type, formatSpecifiers[i]);
                if (ms != null) {
                    suggestions.add(new Suggestion(input.getSource(), arg, ms.message, ms.suggestion));
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

        final IBasicType typeAsBasic = type instanceof IBasicType ?
            (IBasicType) type : null;
        final IPointerType typeAsPointer = type instanceof IPointerType ?
            (IPointerType) type : null;
        final IType pointerToType = typeAsPointer != null ?
            TypeUtil.removeQualifiers(typeAsPointer.getType()) : null;
        final IBasicType pointerToTypeAsBasic = pointerToType instanceof IBasicType ?
            (IBasicType) pointerToType : null;

        final boolean typeIsInteger = typeAsBasic != null
            && Util.contains(typeAsBasic.getKind(), Kind.eChar, Kind.eInt);
        final boolean typeIsFloating = typeAsBasic != null
            && Util.contains(typeAsBasic.getKind(), Kind.eFloat, Kind.eDouble);
        final boolean typeIsIntegerPointer =
            typeAsPointer != null
            && pointerToTypeAsBasic != null && pointerToTypeAsBasic.getKind() == Kind.eInt
            && !pointerToTypeAsBasic.isShort()
            && !pointerToTypeAsBasic.isLong()
            && !pointerToTypeAsBasic.isLongLong();
        final boolean typeIsVoidPointer =
            typeAsPointer != null
            && pointerToTypeAsBasic != null && pointerToTypeAsBasic.getKind() == Kind.eVoid;

        final boolean specTypeIsInteger =
                PrintfFormatAnalyzer.EXPECTED_TYPE.get(spec.type) == Type.INT
                || PrintfFormatAnalyzer.EXPECTED_TYPE.get(spec.type) == Type.UINT
                || PrintfFormatAnalyzer.EXPECTED_TYPE.get(spec.type) == Type.CHAR;
        final boolean specTypeIsFloating =
                PrintfFormatAnalyzer.EXPECTED_TYPE.get(spec.type) == Type.DOUBLE;

        final boolean typeIsShort = typeAsBasic != null && typeAsBasic.isShort();
        final boolean typeIsLong = typeAsBasic != null && typeAsBasic.isLong();
        final boolean typeIsLongLong = typeAsBasic != null && typeAsBasic.isLongLong();
        final boolean typeIsNormalLength = !typeIsShort && !typeIsLong && !typeIsLongLong;

        final boolean typeIsUnsigned = typeAsBasic != null && typeAsBasic.isUnsigned();

        final boolean typeIsValid =
                (typeIsInteger && specTypeIsInteger && typeIsUnsigned == (specType == Type.UINT))
                || (typeIsFloating && specTypeIsFloating)
                || (pointerToTypeAsBasic != null && pointerToTypeAsBasic.getKind() == Kind.eChar && specType == Type.STRING)
                || (typeAsPointer != null && typeIsVoidPointer && specType == Type.VOIDPTR);

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
            } else if (typeAsPointer != null) {
                if (pointerToTypeAsBasic != null && pointerToTypeAsBasic.getKind() == Kind.eChar) {
                    suggestion = "文字列の表示は%%s";
                } else {
                    suggestion = "ポインタ値の表示は%%p";
                }
            }

            String typeName = "", specTypeName = "";
            if (typeIsInteger) {
                typeName = typeIsUnsigned ? "符号なし整数型" : "符号付き整数型";
            } else if (typeIsFloating) {
                typeName = "浮動小数点数型";
            } else if (typeIsIntegerPointer) {
                typeName = "整数型へのポインタ";
            } else if (typeIsVoidPointer) {
                typeName = "voidポインタ";
            } else if (pointerToTypeAsBasic != null && pointerToTypeAsBasic.getKind() == Kind.eChar) {
                typeName = "文字列へのポインタ";
            }

            if (specTypeIsInteger) {
                specTypeName = PrintfFormatAnalyzer.EXPECTED_TYPE.get(spec.type) == Type.UINT ?
                    "符号なし整数型" : "符号付き整数型";
            } else if (specTypeIsFloating) {
                specTypeName = "浮動小数点数型";
            } else if (PrintfFormatAnalyzer.EXPECTED_TYPE.get(spec.type) == Type.INTPTR) {
                specTypeName = "整数型へのポインタ";
            } else if (PrintfFormatAnalyzer.EXPECTED_TYPE.get(spec.type) == Type.VOIDPTR) {
                specTypeName = "voidポインタ";
            } else if (PrintfFormatAnalyzer.EXPECTED_TYPE.get(spec.type) == Type.STRING) {
                specTypeName = "文字列へのポインタ";
            }

            return new MessageSuggestion(
                    StringResource.get("引数は%sだが%%%cは%sを期待している。",
                        StringResource.get(typeName),
                        spec.type,
                        StringResource.get(specTypeName)),
                    StringResource.get(suggestion));
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
