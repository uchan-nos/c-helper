package com.github.uchan_nos.c_helper.analysis.values;

import java.math.BigInteger;

import org.eclipse.cdt.core.dom.ast.IBasicType;
import org.eclipse.cdt.core.dom.ast.IBasicType.Kind;
import org.eclipse.cdt.core.dom.ast.IType;
import org.eclipse.cdt.internal.core.dom.parser.c.CBasicType;

import com.github.uchan_nos.c_helper.analysis.AnalysisEnvironment;
import com.github.uchan_nos.c_helper.util.IntegerLimits;
import com.github.uchan_nos.c_helper.util.Util;

public class IntegralValue extends ArithmeticValue {
    public final BigInteger INT_MAX;
    public final BigInteger INT_MIN;
    public final BigInteger UINT_MAX;

    private final IBasicType type;
    private final BigInteger value;
    private final int flag;
    private final AnalysisEnvironment analysisEnvironment;

    public IntegralValue(BigInteger value, IType type, int flag, AnalysisEnvironment analysisEnvironment) {
        this.type = (IBasicType)type;
        this.value = value;
        this.flag = flag;
        this.analysisEnvironment = analysisEnvironment;

        final BigInteger intMaxPlusOne =
                BigInteger.ONE.shiftLeft(this.analysisEnvironment.INT_BIT - 1);
        INT_MAX = intMaxPlusOne.subtract(BigInteger.ONE);
        INT_MIN = intMaxPlusOne.negate();
        UINT_MAX = BigInteger.ONE
                .shiftLeft(this.analysisEnvironment.INT_BIT)
                .subtract(BigInteger.ONE);
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
            return castTo((IBasicType) newType);
        }
        return null;
    }

    private Value castTo(IBasicType newType) {
        if (newType.getKind() == Kind.eChar || newType.getKind() == Kind.eInt) {
            IntegerLimits thisLimit = IntegerLimits.create(this.type, this.analysisEnvironment);
            IntegerLimits newLimit = IntegerLimits.create(newType, this.analysisEnvironment);
            BigInteger newValue = null;
            int newFlag = 0;

            if (this.type.isUnsigned()) {
                if (newType.isUnsigned()) {
                    if (thisLimit.bits <= newLimit.bits) {
                        newValue = this.value;
                    } else {
                        newValue = this.value.mod(
                                newLimit.max.add(BigInteger.ONE));
                    }
                } else { // newType isn't unsigned
                    if (thisLimit.bits < newLimit.bits) {
                        newValue = this.value;
                    } else if (this.value.bitLength() > newLimit.bits - 1) {
                        // this.valueが新しい型で表せない。
                        // この変換は処理系依存である
                        newFlag |= Value.IMPLDEPENDENT;
                        newValue = Util.cutBits(this.value, newLimit.bits);
                    } else {
                        newValue = this.value;
                    }
                }
            } else { // this.type isn't unsigned
                if (newType.isUnsigned()) {
                    if (thisLimit.bits <= newLimit.bits) {
                        if (this.value.signum() >= 0) {
                            newValue = this.value;
                        } else {
                            IntegralValue intermediateValue;
                            if (thisLimit.bits < newLimit.bits) {
                                IBasicType intermediateType =
                                        new CBasicType(newType.getKind(),
                                                (newType.getModifiers() & ~IBasicType.IS_UNSIGNED) | IBasicType.IS_SIGNED);
                                intermediateValue = (IntegralValue)this.castTo(intermediateType);
                            } else {
                                intermediateValue = this;
                            }
                            newValue = intermediateValue.value.add(newLimit.max.add(BigInteger.ONE));
                        }
                    } else if ((this.value.signum() >= 0
                                && this.value.bitLength() > newLimit.bits)
                            || this.value.signum() < 0) {
                        // this.valueが新しい型で表せない。
                        newValue = this.value.mod(newLimit.max.add(BigInteger.ONE));
                    } else {
                        newValue = this.value;
                    }
                } else { // newType isn't unsigned
                    if (thisLimit.bits <= newLimit.bits) {
                        newValue = this.value;
                    } else if ((this.value.signum() >= 0
                                && this.value.bitLength() > newLimit.bits - 1)
                            || (this.value.signum() < 0
                                && this.value.bitLength() > newLimit.bits - 1)) {
                        // this.valueが新しい型で表せない。
                        // この変換は処理系依存である
                        newFlag |= Value.IMPLDEPENDENT;
                        newValue = Util.cutBits(this.value, newLimit.bits);
                    } else {
                        newValue = this.value;
                    }
                }
            }
            return new IntegralValue(newValue, newType, newFlag, this.analysisEnvironment);
        } else {
            throw new RuntimeException("Not Implemented");
        }
    }

    /**
     * 汎整数拡張を行った後の値を返す.
     * @return
     */
    private IntegralValue promote() {
        if (type.getKind() == Kind.eChar
                || (type.getKind() == Kind.eInt && type.isShort())) {
            if (value.compareTo(INT_MIN) >= 0 && value.compareTo(INT_MAX) <= 0) {
                return new IntegralValue(value, new CBasicType(Kind.eInt, 0), 0, this.analysisEnvironment);
            } else {
                return new IntegralValue(value, new CBasicType(Kind.eInt, IBasicType.IS_UNSIGNED), 0, this.analysisEnvironment);
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
        IntegralValue l = this.promote();
        IntegerLimits lLimit = IntegerLimits.create(l.type, this.analysisEnvironment);
        if (rhs instanceof IntegralValue) {
            IntegralValue r = ((IntegralValue)rhs).promote();
            IntegerLimits rLimit = IntegerLimits.create(r.type, this.analysisEnvironment);

            // 精度の高い方を取る
            // TODO: 演算結果の型の決定は規格書を読むべき
            IBasicType resultType = max(l.type, r.type);
            IntegerLimits resultLimit = IntegerLimits.create(resultType, this.analysisEnvironment);

            BigInteger result = l.value.add(r.value);
            if (!resultType.isUnsigned()
                    && (result.compareTo(resultLimit.min) < 0
                        || result.compareTo(resultLimit.max) > 0)) {
                // overflow
                return new IntegralValue(result, resultType, Value.OVERFLOWED, this.analysisEnvironment);
            }
        }
        return null;
    }

    @Override
    public boolean isTrue() {
        return this.value.compareTo(BigInteger.ZERO) == 0;
    }
}
