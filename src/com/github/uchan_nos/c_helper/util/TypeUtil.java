package com.github.uchan_nos.c_helper.util;

import org.eclipse.cdt.core.dom.ast.*;

/**
 * IType 向け便利関数群.
 * @author uchan
 */
public class TypeUtil {
    /**
     * 指定された型がポインタ型の場合、指している先の型を返す.
     * 指定された型が非ポインタ型なら null を返す.
     * 指定された型がCV修飾子付きのポインタ型なら、CV修飾子を無視する.
     * ex) const char * const : const char
     *
     * @param type ポインタ型
     * @return ポインタが指している型（CV修飾子は残ったまま）
     */
    public static IType getPointerToType(IType type) {
        type = removeQualifiers(type);
        if (type instanceof IPointerType) {
            return ((IPointerType) type).getType();
        } else {
            return null;
        }
    }

    /**
     * 指定された型からCV修飾子を取り除いた型を返す.
     * @param type 修飾子を取り除きたい型
     * @return 修飾子を取り除いた型
     */
    public static IType removeQualifiers(IType type) {
        while (type instanceof IQualifierType) {
            type = ((IQualifierType) type).getType();
        }
        return type;
    }

    /**
     * 指定された型が指定された種類の基本型かを調べる.
     * @param type 調べる型
     * @param kind 基本型の種類
     * @return 指定された型が指定された種類の基本型なら true
     */
    public static boolean isIBasicType(IType type, IBasicType.Kind kind) {
        return type instanceof IBasicType
            && ((IBasicType) type).getKind() == kind;
    }

    /**
     * 指定された型が指定された種類の基本型なら、指定された型をキャストして返す.
     * @param type 調べる型
     * @param kind 基本型の種類
     * @return 指定された型が指定された種類の基本型なら type、そうでなければ null
     */
    public static IBasicType asIBasicType(IType type, IBasicType.Kind kind) {
        if (type instanceof IBasicType) {
            IBasicType t = (IBasicType) type;
            if (t.getKind() == kind) {
                return t;
            }
        }
        return null;
    }

    /**
     * 指定されたASTノードが指定された種類のリテラル式なら、指定されたASTノードをキャストして返す.
     * @param node ASTノード
     * @param kind リテラル式の種類
     * @return 指定されたASTノードが指定された種類のリテラル式なら node、そうでなければ null
     */
    public static IASTLiteralExpression asIASTLiteralExpression(IASTNode node, int kind) {
        if (node instanceof IASTLiteralExpression) {
            IASTLiteralExpression e = (IASTLiteralExpression) node;
            if (e.getKind() == kind) {
                return e;
            }
        }
        return null;
    }
}
