package com.github.uchan_nos.c_helper.analysis;

import org.eclipse.cdt.core.dom.ast.IASTName;

public class NamedVertex<Vertex> {
    private Vertex vertex;
    private IASTName name;

    public NamedVertex(Vertex vertex, IASTName name) {
        this.vertex = vertex;
        this.name = name;
    }

    public Vertex vertex() {
        return this.vertex;
    }

    public IASTName name() {
        return this.name;
    }
}
