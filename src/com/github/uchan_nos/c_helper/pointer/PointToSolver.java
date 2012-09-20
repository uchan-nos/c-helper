package com.github.uchan_nos.c_helper.pointer;

import com.github.uchan_nos.c_helper.analysis.CFG;
import com.github.uchan_nos.c_helper.analysis.IGraph;

import com.github.uchan_nos.c_helper.dataflow.Solver;

public abstract class PointToSolver extends Solver<CFG.Vertex, MemoryStatus> {
    public PointToSolver(IGraph<CFG.Vertex> cfg, CFG.Vertex entryVertex) {
        super(cfg, entryVertex);
    }
}
