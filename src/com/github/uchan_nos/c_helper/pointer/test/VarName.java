package com.github.uchan_nos.c_helper.pointer.test;

import org.eclipse.cdt.core.dom.ast.*;

import org.eclipse.cdt.core.dom.ILinkage;

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
