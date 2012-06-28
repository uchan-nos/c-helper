package com.github.uchan_nos.c_helper.analysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.cdt.core.dom.ast.IASTNode;

public class CFGNormalizer {
    
	public static void normalize(CFG cfg) {
        Util util = new Util(cfg);
                
        boolean modified;
        
        do {
            modified = false;
            CFG.Edge edgeToMerge = null;
            
            for (CFG.Edge edge : cfg.edges()) {
                if (util.canMerge(edge)) {
                    // まとめられる
                    edgeToMerge = edge;
                    break;
                }
            }
            
            if (edgeToMerge != null) {
                modified = true;
                
                // toをfromに統合
                util.merge(edgeToMerge);
                
                // toとtoに接続されている辺を削除
                util.remove(edgeToMerge.to());
                // 一つまとめたらまた最初から。
            }
        } while (modified == true);
    }
}

class Util {
    // 指定されたノードに入ってくる辺の集合
    // entryEdges.get(v) -> vに入ってくる辺の集合
    private Map<CFG.Vertex, ArrayList<CFG.Edge>> entryEdges;
    
    // 指定されたノードから出ていく辺の集合
    // exitEdges.get(v) -> vから出ていく辺の集合
    private Map<CFG.Vertex, ArrayList<CFG.Edge>> exitEdges;
    
    private CFG cfg;
    
    public Util(CFG cfg) {
        this.cfg = cfg;
        this.entryEdges = getEntryEdges(cfg);
        this.exitEdges = getExitEdges(cfg);
    }
    
    /**
     * 指定された辺の前後の頂点をマージする.
     * 辺edgeが与えられたとき、edge.to()をedge.from()にマージする.
     * @param edge マージする辺
     */
    public void merge(CFG.Edge edge) {
        CFG.Vertex from = edge.from();
        CFG.Vertex to = edge.to();
        
        // toをfromに統合
        from.setLabel(from.label() + to.label());
        for (IASTNode node : to.getASTNodes()) {
            from.addASTNode(node);
        }
        
        for (CFG.Edge e : this.exitEdges.get(to)) {
            CFG.Edge newEdge = new CFG.Edge(from, e.to());
            this.cfg.add(newEdge);
            this.exitEdges.get(from).add(newEdge);
            this.entryEdges.get(e.to()).add(newEdge);
        }
    }
    
    /**
     * 指定された頂点と、その頂点に接続されている辺を削除する.
     * @param v 削除する頂点
     */
    public void remove(CFG.Vertex v) {
        this.cfg.remove(v);
        
        for (CFG.Edge e : this.entryEdges.get(v)) {
            // e.to()は必ずvと一致する
            this.cfg.remove(e);
            this.exitEdges.get(e.from()).remove(e);
        }
        for (CFG.Edge e : this.exitEdges.get(v)) {
            // e.from()は必ずvと一致する
            this.cfg.remove(e);
            this.entryEdges.get(e.to()).remove(e);
        }
        this.cfg.remove(v);
        this.entryEdges.remove(v);
        this.exitEdges.remove(v);
    }
    
    public boolean canMerge(CFG.Edge edge) {
        return this.exitEdges.get(edge.from()).size() == 1 &&
                this.entryEdges.get(edge.to()).size() == 1;
    }
    
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
}
