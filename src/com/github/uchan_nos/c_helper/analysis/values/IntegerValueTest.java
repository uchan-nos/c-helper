package com.github.uchan_nos.c_helper.analysis.values;

import static org.junit.Assert.*;

import java.math.BigInteger;

import org.eclipse.cdt.core.dom.ast.IBasicType;
import org.eclipse.cdt.core.dom.ast.IBasicType.Kind;
import org.eclipse.cdt.core.dom.ast.IType;
import org.eclipse.cdt.internal.core.dom.parser.c.CBasicType;
import org.junit.Test;

import com.github.uchan_nos.c_helper.analysis.AnalysisEnvironment;
import com.github.uchan_nos.c_helper.analysis.AssignExpression;
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

    @Test
    public void castTest1() {
        Value v1 = new IntegerValue(BigInteger.valueOf(1L), INT, 0);

        Value casted = v1.castTo(INT);
        assertTrue(casted instanceof IntegerValue);
        assertTrue(casted.getType().isSameType(INT));

        IntegerValue casted_ = (IntegerValue)casted;
        assertEquals(1, casted_.getValue().longValue());
        assertEquals(0, casted_.getFlag());
    }

    @Test
    public void castTest2() {
        Value v1 = new IntegerValue(BigInteger.valueOf(0xffffffffL), UINT, 0);

        Value casted = v1.castTo(INT);
        assertTrue(casted instanceof IntegerValue);
        assertTrue(casted.getType().isSameType(INT));

        IntegerValue casted_ = (IntegerValue)casted;
        assertEquals(-1, casted_.getValue().longValue());
        assertEquals(Value.IMPLDEPENDENT, casted_.getFlag());
    }

    @Test
    public void castTest3() {
        Value v1 = new IntegerValue(BigInteger.valueOf(0xffffL), USHORT, 0);

        Value casted = v1.castTo(INT);
        assertTrue(casted instanceof IntegerValue);
        assertTrue(casted.getType().isSameType(INT));

        IntegerValue casted_ = (IntegerValue)casted;
        assertEquals(0xffffL, casted_.getValue().longValue());
        assertEquals(0, casted_.getFlag());
    }

    @Test
    public void castTest4() {
        Value v1 = new IntegerValue(BigInteger.valueOf(-1L), INT, 0);

        Value casted = v1.castTo(UINT);
        assertTrue(casted instanceof IntegerValue);
        assertTrue(casted.getType().isSameType(UINT));

        IntegerValue casted_ = (IntegerValue)casted;
        assertEquals(0xffffffffL, casted_.getValue().longValue());
        assertEquals(0, casted_.getFlag());
    }

    @Test
    public void castTest5() {
        Value v1 = new IntegerValue(BigInteger.valueOf(1L), INT, 0);

        Value casted = v1.castTo(UINT);
        assertTrue(casted instanceof IntegerValue);
        assertTrue(casted.getType().isSameType(UINT));

        IntegerValue casted_ = (IntegerValue)casted;
        assertEquals(1L, casted_.getValue().longValue());
        assertEquals(0, casted_.getFlag());
    }

    @Test
    public void castTest6() {
        Value v1 = new IntegerValue(BigInteger.valueOf(-10L), SHORT, 0);

        Value casted = v1.castTo(UINT);
        assertTrue(casted instanceof IntegerValue);
        assertTrue(casted.getType().isSameType(UINT));

        IntegerValue casted_ = (IntegerValue)casted;
        assertEquals(IntegerLimits.create(UINT).max.add(BigInteger.ONE).add(BigInteger.valueOf(-10L)).longValue(),
                casted_.getValue().longValue());
        assertEquals(0, casted_.getFlag());
    }

    @Test
    public void castTest7() {
        Value v1 = new IntegerValue(BigInteger.valueOf(-10L), INT, 0);

        Value casted = v1.castTo(UINT);
        assertTrue(casted instanceof IntegerValue);
        assertTrue(casted.getType().isSameType(UINT));

        IntegerValue casted_ = (IntegerValue)casted;
        assertEquals(IntegerLimits.create(UINT).max.add(BigInteger.ONE).add(BigInteger.valueOf(-10L)).longValue(),
                casted_.getValue().longValue());
        assertEquals(0, casted_.getFlag());
    }

    @Test
    public void castTest8() {
        Value v1 = new IntegerValue(BigInteger.valueOf(-10L), INT, 0);

        IntegerValue casted = (IntegerValue)v1.castTo(SHORT);

        assertEquals(-10L, casted.getValue().longValue());
        assertEquals(0, casted.getFlag());
    }

    @Test
    public void castTest9() {
        Value v1 = new IntegerValue(BigInteger.valueOf(0x205a5L), INT, 0);

        IntegerValue casted = (IntegerValue)v1.castTo(SHORT);

        assertEquals(0x5a5L, casted.getValue().longValue());
        assertEquals(Value.IMPLDEPENDENT, casted.getFlag());
    }

    @Test
    public void castTest10() {
        Value v1 = new IntegerValue(BigInteger.valueOf(0x2a5a5L), INT, 0);

        IntegerValue casted = (IntegerValue)v1.castTo(SHORT);

        assertEquals(-0x5a5bL, casted.getValue().longValue());
        assertEquals(Value.IMPLDEPENDENT, casted.getFlag());
    }

    @Test
    public void canBeRepresentedTest() {
        assertTrue(
                new IntegerValue(BigInteger.valueOf(1L), CHAR, 0)
                .canBeRepresentedBy(CHAR));
        assertTrue(
                new IntegerValue(BigInteger.valueOf(1L), CHAR, 0)
                .canBeRepresentedBy(INT));
        assertTrue(
                new IntegerValue(BigInteger.valueOf(1L), UCHAR, 0)
                .canBeRepresentedBy(UCHAR));
        assertTrue(
                new IntegerValue(BigInteger.valueOf(1L), UCHAR, 0)
                .canBeRepresentedBy(UINT));

        assertTrue(
                new IntegerValue(BigInteger.valueOf(1L), INT, 0)
                .canBeRepresentedBy(CHAR));
        assertFalse(
                new IntegerValue(BigInteger.valueOf(1000L), INT, 0)
                .canBeRepresentedBy(CHAR));
        assertTrue(
                new IntegerValue(BigInteger.valueOf(-1L), INT, 0)
                .canBeRepresentedBy(CHAR));
        assertFalse(
                new IntegerValue(BigInteger.valueOf(-1000L), INT, 0)
                .canBeRepresentedBy(CHAR));
        assertTrue(
                new IntegerValue(BigInteger.valueOf(1L), INT, 0)
                .canBeRepresentedBy(UCHAR));
        assertFalse(
                new IntegerValue(BigInteger.valueOf(-1L), INT, 0)
                .canBeRepresentedBy(UCHAR));

        assertTrue(
                new IntegerValue(BigInteger.valueOf(1L), UINT, 0)
                .canBeRepresentedBy(CHAR));
        assertTrue(
                new IntegerValue(BigInteger.valueOf(127L), UINT, 0)
                .canBeRepresentedBy(CHAR));
        assertFalse(
                new IntegerValue(BigInteger.valueOf(128L), UINT, 0)
                .canBeRepresentedBy(CHAR));
        assertFalse(
                new IntegerValue(BigInteger.valueOf(1000L), UINT, 0)
                .canBeRepresentedBy(CHAR));
        assertTrue(
                new IntegerValue(BigInteger.valueOf(128L), UINT, 0)
                .canBeRepresentedBy(UCHAR));
    }
}
