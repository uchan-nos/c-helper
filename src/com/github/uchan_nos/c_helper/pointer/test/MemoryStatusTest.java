package com.github.uchan_nos.c_helper.pointer.test;

import java.util.Arrays;
import java.util.HashSet;

import org.eclipse.cdt.core.dom.ILinkage;
import org.eclipse.cdt.core.dom.ast.DOMException;
import org.eclipse.cdt.core.dom.ast.IBinding;
import org.eclipse.cdt.core.dom.ast.IScope;
import org.eclipse.cdt.core.dom.ast.IType;
import org.eclipse.cdt.core.dom.ast.IValue;
import org.eclipse.cdt.core.dom.ast.IVariable;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import com.github.uchan_nos.c_helper.pointer.Address;
import com.github.uchan_nos.c_helper.pointer.MemoryStatus;
import com.github.uchan_nos.c_helper.pointer.Variable;

public class MemoryStatusTest {

    @Before
    public void setUp() throws Exception {
    }

    @Test
    public void equalityTest() {

        MemoryStatus s0 = new MemoryStatus();
        assertEquals(new HashSet<Variable>(), new HashSet<Variable>(s0.variableManager().getContainingVariables()));

        MemoryStatus s1 = new MemoryStatus(s0);
        assertEquals(s0, s1);
        assertEquals(s1, s0);

        final VarName p = new VarName("p");
        final VarName p_ = new VarName("p");

        s1.update(new Variable(p, Variable.States.POINTING, new DummyAddress(12)));
        assertFalse(s0.equals(s1));
        assertFalse(s1.equals(s0));

        s0.update(new Variable(p, Variable.States.POINTING, new DummyAddress(12)));
        assertEquals(s0, s1);
        assertEquals(s1, s0);

        s1.update(new Variable(p_, Variable.States.POINTING, new DummyAddress(12)));
        assertFalse(s0.equals(s1));
        assertFalse(s1.equals(s0));

        s0.update(new Variable(p_, Variable.States.POINTING, new DummyAddress(12)));
        assertEquals(s0, s1);
        assertEquals(s1, s0);
    }

    @Test
    public void collectionTest() {
        final VarName p = new VarName("p");
        final VarName q = new VarName("q");

        MemoryStatus ms0 = new MemoryStatus();
        ms0.update(
                new Variable(p, Variable.States.POINTING, new DummyAddress(1)),
                new Variable(q, Variable.States.POINTING, new DummyAddress(2))
                );
        MemoryStatus ms1 = new MemoryStatus(ms0);

        assertEquals(1, (new HashSet<MemoryStatus>(Arrays.asList(ms0, ms1))).size());

        ms1.update(new Variable(p, Variable.States.NULL, null));
        assertEquals(2, (new HashSet<MemoryStatus>(Arrays.asList(ms0, ms1))).size());

        ms1.update(new Variable(p, Variable.States.POINTING, new DummyAddress(3)));
        assertEquals(2, (new HashSet<MemoryStatus>(Arrays.asList(ms0, ms1))).size());

        ms0.update(new Variable(p, Variable.States.POINTING, new DummyAddress(3)));
        assertEquals(1, (new HashSet<MemoryStatus>(Arrays.asList(ms0, ms1))).size());
    }
}

interface Function<Ret, Arg> {
    Ret calc(Arg a);
}

class DummyAddress extends Address {
    private int id;
    public DummyAddress(int id) {
        this.id = id;
    }
    public int id() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof DummyAddress)) {
            return false;
        }
        DummyAddress a = (DummyAddress) o;
        return this.id == a.id;
    }

    @Override
    public int hashCode() {
        return this.id;
    }

    @Override
    public String toString() {
        return "addr(" + id + ")";
    }
}

class VarName implements IVariable {

    private final String name;
    public VarName(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public char[] getNameCharArray() {
        return name.toCharArray();
    }

    @Override
    public ILinkage getLinkage() {
        return null;
    }

    @Override
    public IBinding getOwner() {
        return null;
    }

    @Override
    public IScope getScope() throws DOMException {
        return null;
    }

    @Override
    public Object getAdapter(Class adapter) {
        return null;
    }

    @Override
    public IType getType() {
        return null;
    }

    @Override
    public IValue getInitialValue() {
        return null;
    }

    @Override
    public boolean isStatic() {
        return false;
    }

    @Override
    public boolean isExtern() {
        return false;
    }

    @Override
    public boolean isAuto() {
        return false;
    }

    @Override
    public boolean isRegister() {
        return false;
    }
}
