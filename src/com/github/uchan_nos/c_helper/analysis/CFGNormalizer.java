package com.github.uchan_nos.c_helper.analysis;

import java.util.ArrayList;
import java.util.Set;

public class CFGNormalizer {

    public static class Edge {
        public final CFG.Vertex from, to;
        public Edge(CFG.Vertex from, CFG.Vertex to) {
            this.from = from;
            this.to = to;
        }
    }

    public static void normalize(CFG cfg) {
        CFGUtil util = new CFGUtil(cfg);

        // どこからも指されておらず、ASTノードを含まないような頂点を削除
        ArrayList<CFG.Vertex> removeVertices = new ArrayList<CFG.Vertex>();
        for (CFG.Vertex v : cfg.getVertices()) {
            if (v != cfg.entryVertex() && v.getASTNode() == null && cfg.getConnectedVerticesTo(v).size() == 0) {
                removeVertices.add(v);
            }
        }
        for (CFG.Vertex v : removeVertices) {
            cfg.remove(v);
        }

        boolean modified;

        do {
            modified = false;

            Edge edgeToMerge = null;
            if ((edgeToMerge = util.getMergableEdgeDeletingEndVertex(cfg)) != null) {
                modified = true;
                util.mergeIntoFrom(edgeToMerge);
                util.remove(edgeToMerge.to);
                if (cfg.exitVertex() == edgeToMerge.to) {
                    cfg.setExitVertex(edgeToMerge.from);
                }
            } else if ((edgeToMerge = util.getMergableEdgeDeletingStartVertex(cfg)) != null) {
                modified = true;
                util.mergeIntoTo(edgeToMerge);
                util.remove(edgeToMerge.from);
                if (cfg.entryVertex() == edgeToMerge.from) {
                    cfg.setEntryVertex(edgeToMerge.to);
                }
            }

        } while (modified == true);
    }
}

class CFGUtil {
    // 指定されたノードに入ってくる辺の集合
    // entryEdges.get(v) -> vに入ってくる辺の集合
    //private Map<CFG.Vertex, ArrayList<CFG.Edge>> entryEdges;

    // 指定されたノードから出ていく辺の集合
    // exitEdges.get(v) -> vから出ていく辺の集合
    //private Map<CFG.Vertex, ArrayList<CFG.Edge>> exitEdges;

    private CFG cfg;

    public CFGUtil(CFG cfg) {
        this.cfg = cfg;
        //this.entryEdges = getEntryEdges(cfg);
        //this.exitEdges = getExitEdges(cfg);
    }

    /**
     * 指定された辺の前後の頂点をマージする.
     * 辺edgeが与えられたとき、edge.toをedge.fromにマージする.
     * @param edge マージする辺
     */
    public void mergeIntoFrom(CFGNormalizer.Edge edge) {
        CFG.Vertex from = edge.from;
        CFG.Vertex to = edge.to;

        // toをfromに統合
        from.setLabel(from.label() + to.label());
        if (from.getASTNode() != null && to.getASTNode() != null) {
            throw new RuntimeException("cannot merge because both from and to have ast nodes");
        } else if (from.getASTNode() == null) {
            from.setASTNode(to.getASTNode());
        }

        for (CFG.Vertex toto : this.cfg.getConnectedVerticesFrom(to)) {
            this.cfg.connect(from, toto);
        }
    }

    public void mergeIntoTo(CFGNormalizer.Edge edge) {
        CFG.Vertex from = edge.from;
        CFG.Vertex to = edge.to;

        // fromをtoに統合
        to.setLabel(from.label() + to.label());
        if (from.getASTNode() != null && to.getASTNode() != null) {
            throw new RuntimeException("cannot merge because both from and to have ast nodes");
        } else if (to.getASTNode() == null) {
            to.setASTNode(from.getASTNode());
        }

        for (CFG.Vertex tofrom : this.cfg.getConnectedVerticesTo(from)) {
            this.cfg.connect(tofrom, to);
        }
    }

    /**
     * 指定された頂点と、その頂点に接続されている辺を削除する.
     * @param v 削除する頂点
     */
    public void remove(CFG.Vertex v) {
        this.cfg.remove(v);
    }

    public boolean canMergeDeletingEndVertex(CFG.Vertex from, CFG.Vertex to) {
        Set<CFG.Vertex> exitVerticesOfFrom = this.cfg.getConnectedVerticesFrom(from);
        Set<CFG.Vertex> entryVerticesOfTo = this.cfg.getConnectedVerticesTo(to);
        Set<CFG.Vertex> exitVerticesOfTo = this.cfg.getConnectedVerticesFrom(to);
        return (exitVerticesOfFrom.size() == 1
                && entryVerticesOfTo.size() == 1
                && to.getASTNode() == null)
                || (entryVerticesOfTo.size() == 1
                    && exitVerticesOfTo.size() <= 1
                    && to.getASTNode() == null);
    }

    public boolean canMergeDeletingStartVertex(CFG.Vertex from, CFG.Vertex to) {
        Set<CFG.Vertex> exitVerticesOfFrom = this.cfg.getConnectedVerticesFrom(from);
        return (from.getASTNode() == null &&
                exitVerticesOfFrom.size() == 1);
    }

    /*
    public static Map<CFG.Vertex, ArrayList<CFG.Edge>> getEntryEdges(CFG cfg) {
        // 指定されたノードに入ってくる辺の集合
        // entryEdges.get(v) -> vに入ってくる辺の集合
        Map<CFG.Vertex, ArrayList<CFG.Edge>> entryEdges = new HashMap<CFG.Vertex, ArrayList<CFG.Edge>>();

        for (CFG.Edge edge : cfg.edges()) {
            ArrayList<CFG.Edge> entry = entryEdges.get(edge.to());
            if (entry == null) {
                entry = new ArrayList<CFG.Edge>();
                entryEdges.put(edge.to(), entry);
            }
            entry.add(edge);
        }

        return entryEdges;
    }

    public static Map<CFG.Vertex, ArrayList<CFG.Edge>> getExitEdges(CFG cfg) {

        // 指定されたノードから出ていく辺の集合
        // exitEdges.get(v) -> vから出ていく辺の集合
        Map<CFG.Vertex, ArrayList<CFG.Edge>> exitEdges = new HashMap<CFG.Vertex, ArrayList<CFG.Edge>>();

        for (CFG.Vertex v : cfg.vertices()) {
        	exitEdges.put(v, new ArrayList<CFG.Edge>());
        }

        for (CFG.Edge edge : cfg.edges()) {
            ArrayList<CFG.Edge> exit = exitEdges.get(edge.from());
            exit.add(edge);
        }

        return exitEdges;
    }
    */

    public CFGNormalizer.Edge getMergableEdgeDeletingEndVertex(CFG cfg) {
        for (CFG.Vertex from : cfg.getVertices()) {
            for (CFG.Vertex to : cfg.getConnectedVerticesFrom(from)) {
                if (canMergeDeletingEndVertex(from, to)) {
                    return new CFGNormalizer.Edge(from, to);
                }
            }
        }
        return null;
    }

    public CFGNormalizer.Edge getMergableEdgeDeletingStartVertex(CFG cfg) {
        for (CFG.Vertex from : cfg.getVertices()) {
            for (CFG.Vertex to : cfg.getConnectedVerticesFrom(from)) {
                if (canMergeDeletingStartVertex(from, to)) {
                    return new CFGNormalizer.Edge(from, to);
                }
            }
        }
        return null;
    }
}
