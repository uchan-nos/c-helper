package com.github.uchan_nos.c_helper.suggest;

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTLiteralExpression;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IBasicType;
import org.eclipse.cdt.core.dom.ast.IPointerType;
import org.eclipse.cdt.core.dom.ast.IType;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;

import com.github.uchan_nos.c_helper.resource.StringResource;

import com.github.uchan_nos.c_helper.util.ASTFilter;
import com.github.uchan_nos.c_helper.util.EscapeSequenceDecoder;
import com.github.uchan_nos.c_helper.util.Util;

/**
 * メモリリーク，2重freeなどを指摘する.
 * @author uchan
 *
 */
public class CompareCharStringSuggester extends Suggester {

    @Override
    public Collection<Suggestion> suggest(SuggesterInput input, AssumptionManager assumptionManager) {
        ArrayList<Suggestion> suggestions = new ArrayList<Suggestion>();

        // ==による比較の式をすべて取得
        Collection<IASTNode> equalityExpressions = new ASTFilter(input.getAst()).filter(
                new ASTFilter.Predicate() {
                    @Override public boolean pass(IASTNode node) {
                        return Util.isIASTBinaryExpression(node, IASTBinaryExpression.op_equals);
                    }
                });

        try {
            for (IASTNode equalityExpression : equalityExpressions) {
                IASTBinaryExpression be = (IASTBinaryExpression) equalityExpression;
                IASTExpression e1 = be.getOperand1();
                IASTExpression e2 = be.getOperand2();

                Suggestion s = null;
                if (isIntegral(e1.getExpressionType()) && isStringLiteral(e2)) {
                    // 整数 == 文字列リテラル
                    s = createSuggestion(input.getSource(), be, e1, e2);
                } else if (isIntegral(e2.getExpressionType()) && isStringLiteral(e1)) {
                    // 文字列リテラル == 整数
                    s = createSuggestion(input.getSource(), be, e2, e1);
                }

                suggestions.add(s);
            }
        } catch (BadLocationException e) {
            e.printStackTrace();
        }

        return suggestions;
    }

    /**
     * 指定された型が整数型かどうかを返す.
     * char, short, intなど
     */
    private static boolean isIntegral(IType t) {
        if (t instanceof IBasicType) {
            IBasicType.Kind k = ((IBasicType) t).getKind();
            if (k == IBasicType.Kind.eChar
                    || k == IBasicType.Kind.eWChar
                    || k == IBasicType.Kind.eInt) {
                return true;
            }
        }
        return false;
    }

    /**
     * 指定された型が整数型へのポインタかどうかを返す.
     */
    private static boolean isPointerToIntegral(IType t) {
        if (t instanceof IPointerType) {
            IPointerType t_ = (IPointerType) t;
            IType pointToType = t_.getType();
            if (isIntegral(pointToType)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 指定された式が文字列リテラルかどうかを返す.
     */
    private static boolean isStringLiteral(IASTExpression e) {
        if (e instanceof IASTLiteralExpression) {
            IASTLiteralExpression le = (IASTLiteralExpression) e;
            if (le.getKind() == IASTLiteralExpression.lk_string_literal) {
                return true;
            }
        }
        return false;
    }

    private static int calculateQuotedStringLength(String stringConstant) {
        if (stringConstant.length() < 2) {
            return -1;
        }
        if (stringConstant.charAt(0) == '"' && stringConstant.charAt(stringConstant.length() - 1) == '"') {
            // 普通の文字列
            return calculateStringLength(
                    stringConstant.substring(1, stringConstant.length() - 1));
        }
        return -1;
    }

    private static int calculateStringLength(String stringConstant) {
        EscapeSequenceDecoder.Result r = EscapeSequenceDecoder.decode(stringConstant);
        if (r.errorLocation() < 0) {
            return stringConstant.length();
        } else {
            return -1;
        }
    }

    /**
     * 文字と文字列リテラルの比較の警告を生成する.
     * @param integralExpression 整数型の式
     * @param stringExpression 文字列リテラルの式
     */
    private static Suggestion createSuggestion(IDocument inputSource, IASTNode location, IASTExpression integralExpression, IASTExpression stringExpression) throws BadLocationException {
        String suggestion = null;

        IASTLiteralExpression stringConstant = (IASTLiteralExpression) stringExpression;
        char[] value = stringConstant.getValue();
        int valueLength = calculateQuotedStringLength(new String(value));
        if (valueLength == 0) {
            suggestion = "ナル文字か確認したいなら'\\0'と比較する";
        } else if (valueLength == 1) {
            suggestion = "1文字を表すには\"ではなく\'で囲む";
        } else if (valueLength > 1) {
            suggestion = "";
        }

        return new Suggestion(
                inputSource,
                location,
                StringResource.get("文字と文字列は比較できない"),
                StringResource.get(suggestion));
    }
}
