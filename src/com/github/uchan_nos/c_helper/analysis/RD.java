package com.github.uchan_nos.c_helper.analysis;

import java.util.BitSet;
import java.util.Map;

public class RD<Vertex> {
    final private AssignExpression[] assigns;
    final private Map<Vertex, BitSet> entrySets;
    final private Map<Vertex, BitSet> exitSets;

    public RD(AssignExpression[] assigns, Map<Vertex, BitSet> entrySets, Map<Vertex, BitSet> exitSets) {
        this.assigns = assigns;
        this.entrySets = entrySets;
        this.exitSets = exitSets;
    }

    public AssignExpression[] getAssigns() {
        return assigns;
    }

    public Map<Vertex, BitSet> getEntrySets() {
        return entrySets;
    }

    public Map<Vertex, BitSet> getExitSets() {
        return exitSets;
    }
}
