package com.github.uchan_nos.c_helper.pointer.test;

import java.util.Arrays;
import java.util.HashSet;

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

