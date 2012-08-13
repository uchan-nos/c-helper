package com.github.uchan_nos.c_helper.analysis.values;

import static org.junit.Assert.*;

import java.math.BigInteger;

import org.eclipse.cdt.core.dom.ast.IBasicType;
import org.eclipse.cdt.core.dom.ast.IBasicType.Kind;
import org.eclipse.cdt.internal.core.dom.parser.c.CBasicType;
import org.junit.Test;

import com.github.uchan_nos.c_helper.analysis.AnalysisEnvironment;
import com.github.uchan_nos.c_helper.util.IntegerLimits;

@SuppressWarnings("restriction")
public class IntegerValueTest {

    private static final IBasicType INT = new CBasicType(Kind.eInt, 0);
    private static final IBasicType SINT = new CBasicType(Kind.eInt, IBasicType.IS_SIGNED);
    private static final IBasicType UINT = new CBasicType(Kind.eInt, IBasicType.IS_UNSIGNED);
    private static final IBasicType SHORT = new CBasicType(Kind.eInt, IBasicType.IS_SHORT);
    private static final IBasicType SSHORT = new CBasicType(Kind.eInt, IBasicType.IS_SIGNED | IBasicType.IS_SHORT);
    private static final IBasicType USHORT = new CBasicType(Kind.eInt, IBasicType.IS_UNSIGNED | IBasicType.IS_SHORT);
    private static final IBasicType CHAR = new CBasicType(Kind.eChar, 0);
    private static final IBasicType SCHAR = new CBasicType(Kind.eChar, IBasicType.IS_SIGNED);
    private static final IBasicType UCHAR = new CBasicType(Kind.eChar, IBasicType.IS_UNSIGNED);

    private static final AnalysisEnvironment analysisEnvironment = new AnalysisEnvironment();

    @Test
    public void castTest1() {
        Value v1 = new IntegralValue(BigInteger.valueOf(1L), INT, 0, analysisEnvironment);

        Value casted = v1.castTo(INT);
        assertTrue(casted instanceof IntegralValue);
        assertTrue(casted.getType().isSameType(INT));

        IntegralValue casted_ = (IntegralValue)casted;
        assertEquals(1, casted_.getValue().longValue());
        assertEquals(0, casted_.getFlag());
    }

    @Test
    public void castTest2() {
        Value v1 = new IntegralValue(BigInteger.valueOf(0xffffffffL), UINT, 0, analysisEnvironment);

        Value casted = v1.castTo(INT);
        assertTrue(casted instanceof IntegralValue);
        assertTrue(casted.getType().isSameType(INT));

        IntegralValue casted_ = (IntegralValue)casted;
        assertEquals(-1, casted_.getValue().longValue());
        assertEquals(Value.IMPLDEPENDENT, casted_.getFlag());
    }

    @Test
    public void castTest3() {
        Value v1 = new IntegralValue(BigInteger.valueOf(0xffffL), USHORT, 0, analysisEnvironment);

        Value casted = v1.castTo(INT);
        assertTrue(casted instanceof IntegralValue);
        assertTrue(casted.getType().isSameType(INT));

        IntegralValue casted_ = (IntegralValue)casted;
        assertEquals(0xffffL, casted_.getValue().longValue());
        assertEquals(0, casted_.getFlag());
    }

    @Test
    public void castTest4() {
        Value v1 = new IntegralValue(BigInteger.valueOf(-1L), INT, 0, analysisEnvironment);

        Value casted = v1.castTo(UINT);
        assertTrue(casted instanceof IntegralValue);
        assertTrue(casted.getType().isSameType(UINT));

        IntegralValue casted_ = (IntegralValue)casted;
        assertEquals(0xffffffffL, casted_.getValue().longValue());
        assertEquals(0, casted_.getFlag());
    }

    @Test
    public void castTest5() {
        Value v1 = new IntegralValue(BigInteger.valueOf(1L), INT, 0, analysisEnvironment);

        Value casted = v1.castTo(UINT);
        assertTrue(casted instanceof IntegralValue);
        assertTrue(casted.getType().isSameType(UINT));

        IntegralValue casted_ = (IntegralValue)casted;
        assertEquals(1L, casted_.getValue().longValue());
        assertEquals(0, casted_.getFlag());
    }

    @Test
    public void castTest6() {
        Value v1 = new IntegralValue(BigInteger.valueOf(-10L), SHORT, 0, analysisEnvironment);

        Value casted = v1.castTo(UINT);
        assertTrue(casted instanceof IntegralValue);
        assertTrue(casted.getType().isSameType(UINT));

        IntegralValue casted_ = (IntegralValue)casted;
        assertEquals(IntegerLimits.create(UINT, analysisEnvironment).max.add(BigInteger.ONE).add(BigInteger.valueOf(-10L)).longValue(),
                casted_.getValue().longValue());
        assertEquals(0, casted_.getFlag());
    }

    @Test
    public void castTest7() {
        Value v1 = new IntegralValue(BigInteger.valueOf(-10L), INT, 0, analysisEnvironment);

        Value casted = v1.castTo(UINT);
        assertTrue(casted instanceof IntegralValue);
        assertTrue(casted.getType().isSameType(UINT));

        IntegralValue casted_ = (IntegralValue)casted;
        assertEquals(IntegerLimits.create(UINT, analysisEnvironment).max.add(BigInteger.ONE).add(BigInteger.valueOf(-10L)).longValue(),
                casted_.getValue().longValue());
        assertEquals(0, casted_.getFlag());
    }

    @Test
    public void castTest8() {
        Value v1 = new IntegralValue(BigInteger.valueOf(-10L), INT, 0, analysisEnvironment);

        IntegralValue casted = (IntegralValue)v1.castTo(SHORT);

        assertEquals(-10L, casted.getValue().longValue());
        assertEquals(0, casted.getFlag());
    }

    @Test
    public void castTest9() {
        Value v1 = new IntegralValue(BigInteger.valueOf(0x205a5L), INT, 0, analysisEnvironment);

        IntegralValue casted = (IntegralValue)v1.castTo(SHORT);

        assertEquals(0x5a5L, casted.getValue().longValue());
        assertEquals(Value.IMPLDEPENDENT, casted.getFlag());
    }

    @Test
    public void castTest10() {
        Value v1 = new IntegralValue(BigInteger.valueOf(0x2a5a5L), INT, 0, analysisEnvironment);

        IntegralValue casted = (IntegralValue)v1.castTo(SHORT);

        assertEquals(-0x5a5bL, casted.getValue().longValue());
        assertEquals(Value.IMPLDEPENDENT, casted.getFlag());
    }

    @Test
    public void castTest11() {
        Value v1 = new IntegralValue(BigInteger.valueOf(0x01a0L), SHORT, 0, analysisEnvironment);

        IntegralValue casted = (IntegralValue)v1.castTo(CHAR);

        assertEquals(-96, casted.getValue().longValue());
        assertEquals(Value.IMPLDEPENDENT, casted.getFlag());
    }

    private static void castHelper(IBasicType originalType, long original,
            IBasicType expectedType, long expected, int expectedFlag) {
        Value value = new IntegralValue(BigInteger.valueOf(original), originalType, 0, analysisEnvironment);
        IntegralValue casted = (IntegralValue) value.castTo(expectedType);
        assertEquals(expected, casted.getValue().longValue());
        assertEquals(expectedFlag, casted.getFlag());
    }

    @Test
    public void castGeneralTest() {
        castHelper(INT, 0x15L, INT, 0x15L, 0);
        castHelper(INT, 0x15L, CHAR, 0x15L, 0);
        castHelper(CHAR, 0x15L, INT, 0x15L, 0);
        castHelper(INT, 0x81L, CHAR, -127, Value.IMPLDEPENDENT);
        castHelper(INT, -0x81L, CHAR, 127, Value.IMPLDEPENDENT);
        castHelper(CHAR, -127, INT, -127, 0);

        castHelper(SHORT, 0x1234L, USHORT, 0x1234L, 0);
        castHelper(SHORT, -0x1234L, USHORT, 0xedccL, 0);
        castHelper(SHORT, 0x1234L, UCHAR, 0x34L, 0);
        castHelper(SHORT, -0x1234L, UCHAR, 0xccL, 0);

        castHelper(USHORT, 0x1234L, UINT, 0x1234L, 0);
        castHelper(USHORT, 0x1234L, USHORT, 0x1234L, 0);
        castHelper(USHORT, 0x1234L, UCHAR, 0x34L, 0);
        castHelper(USHORT, 0xf234L, USHORT, 0xf234L, 0);

        castHelper(USHORT, 0x1234L, INT, 0x1234L, 0);
        castHelper(USHORT, 0x1234L, SHORT, 0x1234L, 0);
        castHelper(USHORT, 0x1234L, CHAR, 0x34L, Value.IMPLDEPENDENT);
        castHelper(USHORT, 0x1284L, CHAR, -0x7cL, Value.IMPLDEPENDENT);
        castHelper(USHORT, 0xff34L, CHAR, 0x34L, Value.IMPLDEPENDENT);
        castHelper(USHORT, 0xf001L, SHORT, -4095, Value.IMPLDEPENDENT);

    }
}
