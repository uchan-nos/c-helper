package com.github.uchan_nos.c_helper.util;

import org.eclipse.cdt.core.dom.ast.*;

import com.github.uchan_nos.c_helper.analysis.AnalysisEnvironment;

/**
 * 型関係の便利関数群.
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
     * @param kinds 基本型の種類（OR結合）
     * @return 指定された型が指定された種類の基本型なら type、そうでなければ null
     */
    public static IBasicType asIBasicType(IType type, IBasicType.Kind... kinds) {
        if (type instanceof IBasicType) {
            IBasicType t = (IBasicType) type;
            if (kinds.length == 0) {
                return t;
            }
            for (int i = 0; i < kinds.length; ++i) {
                if (t.getKind() == kinds[i]) {
                    return t;
                }
            }
        }
        return null;
    }

    /**
     * 指定されたASTノードが指定された種類のリテラル式なら、指定されたASTノードをキャストして返す.
     * @param node ASTノード
     * @param kinds リテラル式の種類
     * @return 指定されたASTノードが指定された種類のリテラル式なら node、そうでなければ null
     */
    public static IASTLiteralExpression asIASTLiteralExpression(IASTNode node, int... kinds) {
        if (node instanceof IASTLiteralExpression) {
            IASTLiteralExpression e = (IASTLiteralExpression) node;
            if (kinds.length == 0) {
                return e;
            }
            for (int i = 0; i < kinds.length; ++i) {
                if (e.getKind() == kinds[i]) {
                    return e;
                }
            }
        }
        return null;
    }

    /**
     * 指定された型がtypedef型の場合、本当の型を解決して返す.
     * 一番外側がtypedefでなくなるまで解決する.
     * すなわち、戻り値がIPointerTypeであり、かつ指し示す先がtypedef型である場合などがある.
     */
    public static IType resolveOuterTypedef(IType type) {
        while (type instanceof ITypedef) {
            type = ((ITypedef) type).getType();
        }
        return type;
    }

    /**
     * 与えられた型のバイト数を返す.
     * @param type バイト数を計算する型
     * @param assumptions 計算の前提となる基本型のビット数情報
     * @return typeのバイト数
     */
    public static int bytesOfType(IType type, AnalysisEnvironment assumptions) {
        if (type instanceof IBasicType) {
            IBasicType t = (IBasicType) type;
            int bits = 0;
            switch (t.getKind()) {
            case eInt:
                if (t.isShort()) {
                    bits = assumptions.SHORT_BIT;
                } else if (t.isLong()) {
                    bits = assumptions.LONG_BIT;
                } else if (t.isLongLong()) {
                    bits = assumptions.LONG_LONG_BIT;
                } else {
                    bits = assumptions.INT_BIT;
                }
                return bits / assumptions.CHAR_BIT;
            case eChar:
                return 1;
            default:
                // TODO: assumptions に float や double を加える
                throw new RuntimeException("not supported basic type");
            }
        } else if (type instanceof ICompositeType) {
            ICompositeType t = (ICompositeType) type;
            int bytes = 0;
            for (IField field : t.getFields()) {
                bytes += bytesOfType(field.getType(), assumptions);
            }
            return bytes;
        }
        throw new RuntimeException("not supported type: " + type.toString());
    }

    /**
     * 与えられた型のバイト数を返す.
     * @param spec バイト数を計算する型が使用された宣言指定子
     * @param assumptions 計算の前提となる基本型のビット数情報
     * @return specで使用されている型のバイト数
     */
    public static int bytesOfType(IASTDeclSpecifier spec, AnalysisEnvironment assumptions) {
        if (spec instanceof IASTSimpleDeclSpecifier) {
            int bits = 0;
            IASTSimpleDeclSpecifier sds = (IASTSimpleDeclSpecifier) spec;
            switch (sds.getType()) {
            case IASTSimpleDeclSpecifier.t_unspecified:
            case IASTSimpleDeclSpecifier.t_int:
                if (sds.isShort()) {
                    bits = assumptions.SHORT_BIT;
                } else if (sds.isLong()) {
                    bits = assumptions.LONG_BIT;
                } else if (sds.isLongLong()) {
                    bits = assumptions.LONG_LONG_BIT;
                } else {
                    bits = assumptions.INT_BIT;
                }
                return bits / assumptions.CHAR_BIT;
            case IASTSimpleDeclSpecifier.t_char:
                return 1;
            default:
                // TODO: assumptions に float や double を加える
                throw new RuntimeException("not supported basic type");
            }
        } else if (spec instanceof IASTCompositeTypeSpecifier) {
            IASTCompositeTypeSpecifier cts = (IASTCompositeTypeSpecifier) spec;
            int bytes = 0;
            for (IASTDeclaration decl : cts.getMembers()) {
                if (decl instanceof IASTSimpleDeclaration) {
                    IASTSimpleDeclaration sd = (IASTSimpleDeclaration) decl;
                    bytes += bytesOfType(sd.getDeclSpecifier(), assumptions);
                }
            }
            return bytes;
        }
        throw new RuntimeException("not supported declaration specifier: " + spec.toString());
    }
}
