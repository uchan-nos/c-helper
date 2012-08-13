package com.github.uchan_nos.c_helper.util;

import java.math.BigInteger;

import org.eclipse.cdt.core.dom.ast.IBasicType;
import org.eclipse.cdt.core.dom.ast.IType;
import org.eclipse.cdt.core.dom.ast.IBasicType.Kind;

import com.github.uchan_nos.c_helper.analysis.AnalysisEnvironment;

/**
 * ITypeに対する各特徴値を取得する.
 * @author uchan
 */
public class IntegerLimits {
    public final BigInteger max;
    public final BigInteger min;
    public final int bits;

    private IntegerLimits(BigInteger max, BigInteger min, int bits) {
        this.max = max;
        this.min = min;
        this.bits = bits;
    }

    private static IntegerLimits createSigned(int bits) {
        BigInteger maxPlusOne =
                BigInteger.ONE.shiftLeft(bits - 1);
        return new IntegerLimits(
                maxPlusOne.subtract(BigInteger.ONE),
                maxPlusOne.negate(),
                bits);
    }

    private static IntegerLimits createUnsigned(int bits) {
        return new IntegerLimits(
                BigInteger.ONE.shiftLeft(bits).subtract(BigInteger.ONE),
                BigInteger.ZERO,
                bits);
    }

    public static IntegerLimits create(IType type, AnalysisEnvironment analysisEnvironment) {
        if (type instanceof IBasicType) {
            IBasicType t = (IBasicType)type;

            int bits = 0;
            if (t.getKind() == Kind.eChar) {
                bits = analysisEnvironment.CHAR_BIT;
            } else if (t.getKind() == Kind.eInt) {
                if (t.isShort()) {
                    bits = analysisEnvironment.SHORT_BIT;
                } else if (t.isLong()) {
                    bits = analysisEnvironment.LONG_BIT;
                } else if (t.isLongLong()) {
                    bits = analysisEnvironment.LONG_LONG_BIT;
                } else {
                    bits = analysisEnvironment.INT_BIT;
                }
            } else {
                throw new RuntimeException("IntegerLimits.create needs type to be an integer type.");
            }

            if (bits > 0 && (t.isSigned() || !t.isUnsigned())) {
                return createSigned(bits);
            } else if (bits > 0 && t.isUnsigned()) {
                return createUnsigned(bits);
            } else {
                throw new RuntimeException("must not be here");
            }
        }
        throw new RuntimeException("IntegerLimits.create needs type to be an integer type.");
    }
}
