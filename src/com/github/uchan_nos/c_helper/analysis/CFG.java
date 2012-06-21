package com.github.uchan_nos.c_helper.analysis;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.cdt.core.dom.ast.IASTNode;

/**
 * 制御フローグラフの実装.
 * このクラスは、制御フローグラフ本体と、そのグラフの入口ノード、出口ノードを記憶する。
 * @author uchan
 *
 */
public class CFG {
	static public class Vertex {
		private ArrayList<IASTNode> astNodes = new ArrayList<IASTNode>();
		private String label = "";
		
		public Vertex(String label) {
		    this.label = label;
		}
		
		public void addASTNode(IASTNode node) {
			this.astNodes.add(node);
		}
		
		public ArrayList<IASTNode> getASTNodes() {
			return this.astNodes;
		}
		
		public String label() {
		    return this.label;
		}
	}
	
	static public class Edge {
		private Vertex from, to;
		public Edge(Vertex from, Vertex to) {
			this.from = from;
			this.to = to;
		}
		
		public Vertex from() {
			return this.from;
		}
		
		public Vertex to() {
			return this.to;
		}
	}
	
	private ArrayList<Vertex> vertices = new ArrayList<CFG.Vertex>();
	private ArrayList<Edge> edges = new ArrayList<CFG.Edge>();
	private Vertex entryVertex = null;
	private ArrayList<Vertex> exitVertices = null;
	
	/**
	 * 指定された頂点をグラフに追加する.
	 * @param v 追加する頂点
	 */
	public void add(Vertex v) {
		this.vertices.add(v);
	}
	
	/**
	 * 指定された辺をグラフに追加する.
	 * @param e 追加する辺
	 */
	public void add(Edge e) {
		this.edges.add(e);
	}
	
	/**
	 * 指定されたグラフ全体をこのグラフに追加する.
	 * @param cfg 追加するグラフ
	 */
	public void add(CFG cfg) {
		this.vertices.addAll(cfg.vertices());
		this.edges.addAll(cfg.edges());
	}
	
	/**
	 * 指定されたグラフ全体をこのグラフに追加し、追加したグラフとこのグラフを接続する.
	 * @param cfg 追加するグラフ
	 * @param connectFrom 追加したグラフの入口ノードへ接続する本体側グラフのノード
	 * @param connectTo 追加したグラフの出口ノードから接続される本体側グラフのノード
	 */
	public void add(CFG cfg, ArrayList<Vertex> connectFrom, Vertex connectTo) {
		add(cfg);
		if (connectFrom != null) {
			for (Vertex v : connectFrom) {
				add(new Edge(v, cfg.entryVertex()));
			}
		}
		if (connectTo != null) {
			for (Vertex v : cfg.exitVertices()) {
				add(new Edge(v, connectTo));
			}
		}
	}
	
	public void setEntryVertex(Vertex v) {
		this.entryVertex = v;
	}
	
	public void setExitVertices(ArrayList<Vertex> vs) {
		this.exitVertices = vs;
	}
	
	public void setExitVertices(Vertex v1) {
	    this.exitVertices = new ArrayList<CFG.Vertex>(1);
	    this.exitVertices.add(v1);
	}
    
    public void setExitVertices(Vertex v1, Vertex v2) {
        this.exitVertices = new ArrayList<CFG.Vertex>(2);
        this.exitVertices.add(v1);
        this.exitVertices.add(v2);
    }
	
	public ArrayList<Vertex> vertices() {
		return this.vertices;
	}
	
	public ArrayList<Edge> edges() {
		return this.edges;
	}
	
	public Vertex entryVertex() {
		return this.entryVertex;
	}
	
	public ArrayList<Vertex> exitVertices() {
		return this.exitVertices;
	}
}
