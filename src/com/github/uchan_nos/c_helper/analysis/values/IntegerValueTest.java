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

@SuppressWarnings("restriction")
public class IntegerValueTest {

    private static final IType INT = new CBasicType(Kind.eInt, 0);
    private static final IType SINT = new CBasicType(Kind.eInt, IBasicType.IS_SIGNED);
    private static final IType UINT = new CBasicType(Kind.eInt, IBasicType.IS_UNSIGNED);
    private static final IType SHORT = new CBasicType(Kind.eInt, IBasicType.IS_SHORT);
    private static final IType SSHORT = new CBasicType(Kind.eInt, IBasicType.IS_SIGNED | IBasicType.IS_SHORT);
    private static final IType USHORT = new CBasicType(Kind.eInt, IBasicType.IS_UNSIGNED | IBasicType.IS_SHORT);
    private static final IType CHAR = new CBasicType(Kind.eChar, 0);
    private static final IType SCHAR = new CBasicType(Kind.eChar, IBasicType.IS_SIGNED);
    private static final IType UCHAR = new CBasicType(Kind.eChar, IBasicType.IS_UNSIGNED);

    @Test
    public void castTest1() {
        Value v1 = new IntegerValue(BigInteger.valueOf(1), INT, 0);

        Value casted = v1.castTo(INT);
        assertTrue(casted instanceof IntegerValue);
        assertTrue(casted.getType().isSameType(INT));

        IntegerValue casted_ = (IntegerValue)casted;
        assertEquals(1, casted_.getValue().longValue());
        assertEquals(0, casted_.getFlag());
    }


    @Test
    public void castTest2() {
        Value v1 = new IntegerValue(BigInteger.valueOf(0xffffffff), UINT, 0);

        Value casted = v1.castTo(INT);
        assertTrue(casted instanceof IntegerValue);
        assertTrue(casted.getType().isSameType(INT));

        IntegerValue casted_ = (IntegerValue)casted;
        assertEquals(-1, casted_.getValue().longValue());
        assertEquals(Value.IMPLDEPENDENT, casted_.getFlag());
    }

}
