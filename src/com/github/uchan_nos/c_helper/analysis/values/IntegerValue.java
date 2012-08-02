package com.github.uchan_nos.c_helper.analysis.values;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;

import org.eclipse.cdt.core.dom.ast.IBasicType;
import org.eclipse.cdt.core.dom.ast.IEnumeration;
import org.eclipse.cdt.core.dom.ast.IPointerType;
import org.eclipse.cdt.core.dom.ast.IType;
import org.eclipse.cdt.core.dom.ast.IBasicType.Kind;
import org.eclipse.cdt.internal.core.dom.parser.c.CBasicType;

import com.github.uchan_nos.c_helper.analysis.AnalysisEnvironment;
import com.github.uchan_nos.c_helper.util.IntegerLimits;

public class IntegerValue extends Value {
    public static final BigInteger INT_MAX;
    public static final BigInteger INT_MIN;
    public static final BigInteger UINT_MAX;

    static {
        final BigInteger intMaxPlusOne =
                BigInteger.ONE.shiftLeft(AnalysisEnvironment.INT_BITS - 1);
        INT_MAX = intMaxPlusOne.subtract(BigInteger.ONE);
        INT_MIN = intMaxPlusOne.negate();
        UINT_MAX = BigInteger.ONE
                .shiftLeft(AnalysisEnvironment.INT_BITS)
                .subtract(BigInteger.ONE);
    }

    private IBasicType type;
    private BigInteger value;
    private int flag;

    public IntegerValue(BigInteger value, IType type, int flag) {
        this.type = (IBasicType)type;
        this.value = value;
        this.flag = flag;
    }

    @Override
    public IType getType() {
        return type;
    }

    @Override
    public int getFlag() {
        return flag;
    }

    public BigInteger getValue() {
        return value;
    }

    @Override
    public Value castTo(IType newType) {
        if (this.type.isSameType(newType)) {
            return this;
        } else if (newType instanceof IBasicType) {
            IBasicType t = (IBasicType)newType;
            if (t.getKind() == Kind.eChar || t.getKind() == Kind.eInt) {
                IntegerLimits thisLimit = IntegerLimits.create(this.type);
                IntegerLimits newLimit = IntegerLimits.create(t);
                BigInteger newValue = null;
                int newFlag = 0;

                if (this.type.isUnsigned()) {
                    if (t.isUnsigned()) {
                        if (thisLimit.bits <= newLimit.bits) {
                            newValue = this.value;
                        } else {
                            newValue = this.value.mod(
                                    newLimit.max.add(BigInteger.ONE));
                        }
                    } else { // signed
                        if (thisLimit.bits < newLimit.bits) {
                            newValue = this.value;
                        } else if ((this.value.signum() >= 0
                                    && this.value.bitLength() > newLimit.bits)
                                || (this.value.signum() < 0
                                    && (this.value.bitLength() + 1) > newLimit.bits)) {
                            // この変換は処理系依存である
                            newFlag |= Value.IMPLDEPENDENT;
                            // 変換先の型のビット数になるようにマスクする
                            BigInteger masked = this.value.and(newLimit.max);
                            if (masked.testBit(newLimit.bits - 1)) {
                                // 負数
                                newValue = masked.not().add(BigInteger.ONE).negate();
                            } else {
                                newValue = masked;
                            }
                        } else {
                            if (this.value.testBit(newLimit.bits - 1)) {
                                // 負数
                                newValue = this.value.not().add(BigInteger.ONE).negate();
                                newFlag |= Value.IMPLDEPENDENT;
                            } else {
                                newValue = this.value;
                            }
                        }
                    }
                }
                return new IntegerValue(newValue, newType, newFlag);
            }
        }
        return null;
    }

    /**
     * 汎整数拡張を行った後の値を返す.
     * @return
     */
    private IntegerValue promote() {
        if (type.getKind() == Kind.eChar
                || (type.getKind() == Kind.eInt && type.isShort())) {
            if (value.compareTo(INT_MIN) >= 0 && value.compareTo(INT_MAX) <= 0) {
                return new IntegerValue(value, new CBasicType(Kind.eInt, 0), 0);
            } else {
                return new IntegerValue(value, new CBasicType(Kind.eInt, IBasicType.IS_UNSIGNED), 0);
            }
        }
        return this;
    }

    private static int getNumericTypeOrder(IBasicType type) {
        switch (type.getKind()) {
        case eChar:
            return type.isUnsigned() ? 1 : 0;
        case eInt:
            if (type.isShort()) { return type.isUnsigned() ? 3 : 2; }
            else if (type.isLong()) { return type.isUnsigned() ? 7 : 6; }
            else if (type.isLongLong()) { return type.isUnsigned() ? 9 : 8; }
            else return type.isUnsigned() ? 5 : 4;
        case eFloat: return 10;
        case eDouble: return 11;
        default: throw new RuntimeException("cannot decide the order of type");
        }
    }

    private static IBasicType max(IBasicType a, IBasicType b) {
        int aOrder = getNumericTypeOrder(a);
        int bOrder = getNumericTypeOrder(b);
        return aOrder >= bOrder ? a : b;
    }

    public Value add(Value rhs) {
        IntegerValue l = this.promote();
        IntegerLimits lLimit = IntegerLimits.create(l.type);
        if (rhs instanceof IntegerValue) {
            IntegerValue r = ((IntegerValue)rhs).promote();
            IntegerLimits rLimit = IntegerLimits.create(r.type);

            // 精度の高い方を取る
            // TODO: 演算結果の型の決定は規格書を読むべき
            IBasicType resultType = max(l.type, r.type);
            IntegerLimits resultLimit = IntegerLimits.create(resultType);

            BigInteger result = l.value.add(r.value);
            if (!resultType.isUnsigned()
                    && (result.compareTo(resultLimit.min) < 0
                        || result.compareTo(resultLimit.max) > 0)) {
                // overflow
                return new IntegerValue(result, resultType, Value.OVERFLOWED);
            }
        }
        return null;
    }
}
