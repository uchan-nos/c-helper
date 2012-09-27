package com.github.uchan_nos.c_helper.pointer.test;

import java.util.Arrays;
import java.util.HashSet;

import static org.junit.Assert.*;

import org.junit.Test;

import com.github.uchan_nos.c_helper.pointer.Variable;
import com.github.uchan_nos.c_helper.pointer.VariableManager;

public class VariableManagerTest {

    @Test
    public void putTest() {
        VariableManager m0 = new VariableManager();

        VarName p = new VarName("p");
        VarName q = new VarName("q");

        assertEquals(Variable.States.UNDEFINED, m0.getVariableStatus(p));
        assertEquals(Variable.States.UNDEFINED, m0.getVariableStatus(q));

        m0.put(new Variable(p, Variable.States.NULL, null));
        assertEquals(Variable.States.NULL, m0.getVariableStatus(p));
        assertEquals(Variable.States.UNDEFINED, m0.getVariableStatus(q));

        m0.put(new Variable(p, Variable.States.UNDEFINED, null));
        assertEquals(Variable.States.UNDEFINED, m0.getVariableStatus(p));
        assertEquals(Variable.States.UNDEFINED, m0.getVariableStatus(q));

        m0.put(new Variable(q, Variable.States.NULL, null));
        assertEquals(Variable.States.UNDEFINED, m0.getVariableStatus(p));
        assertEquals(Variable.States.NULL, m0.getVariableStatus(q));

    }

    @Test
    public void collectionTest() {
        VariableManager m0 = new VariableManager();

        VarName p = new VarName("p");
        VarName q = new VarName("q");

        assertEquals(
                new HashSet<Variable>(),
                new HashSet<Variable>(m0.getContainingVariables()));

        m0.put(new Variable(p, Variable.States.UNDEFINED, null));
        assertEquals(
                new HashSet<Variable>(Arrays.asList(
                        new Variable(p, Variable.States.UNDEFINED, null))),
                new HashSet<Variable>(m0.getContainingVariables()));

        m0.put(new Variable(q, Variable.States.NULL, null));
        assertEquals(
                new HashSet<Variable>(Arrays.asList(
                        new Variable(p, Variable.States.UNDEFINED, null),
                        new Variable(q, Variable.States.NULL, null))),
                new HashSet<Variable>(m0.getContainingVariables()));

    }
}

