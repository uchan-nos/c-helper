package com.github.uchan_nos.c_helper.analysis;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.cdt.core.dom.ast.IASTNode;

/**
 * 制御フローグラフの実装. このクラスは、制御フローグラフ本体と、グラフへの入口ノード、グラフからの出口ノードを記憶する.
 * 
 * @author uchan
 * 
 */
public class CFG {
    /**
     * 制御フローグラフの1つの頂点を表す. 頂点は、0個以上のASTノードを含む.
     * 
     * @author uchan
     * 
     */
    static public class Vertex {
        private ArrayList<IASTNode> astNodes = new ArrayList<IASTNode>();
        private String label = "";

        /**
         * 指定されたラベルを持つ頂点を生成する. ラベルはIDとしては用いられないので、他の頂点と重複してもよい.
         * 
         * @param label
         *            頂点のラベル
         */
        public Vertex(String label) {
            this.label = label;
        }

        /**
         * 頂点のラベルを更新する.
         * 
         * @param label
         *            新しいラベル
         */
        public void setLabel(String label) {
            this.label = label;
        }

        /**
         * 指定されたASTノードを頂点に追加する.
         * 
         * @param node
         *            追加するASTノード
         */
        public void addASTNode(IASTNode node) {
            this.astNodes.add(node);
        }

        /**
         * 頂点が含むすべてのASTノードを取得する.
         * 
         * @return 頂点が含むASTノードのリスト
         */
        public ArrayList<IASTNode> getASTNodes() {
            return this.astNodes;
        }

        /**
         * 頂点のラベルを取得する.
         * 
         * @return 頂点のラベル
         */
        public String label() {
            return this.label;
        }
    }

    /**
     * 制御フローグラフの1つの有向辺を表す. 辺は、出発点(from)と目標点(to)を含む.
     * 
     * @author uchan
     * 
     */
    static public class Edge {
        private Vertex from, to;

        /**
         * 指定された出発点と目標点を結ぶ辺を生成する.
         * 
         * @param from
         *            出発点
         * @param to
         *            目標点
         */
        public Edge(Vertex from, Vertex to) {
            this.from = from;
            this.to = to;
        }

        /**
         * 辺の出発点を取得する.
         * 
         * @return 出発点
         */
        public Vertex from() {
            return this.from;
        }

        /**
         * 辺の目標点を取得する.
         * 
         * @return 目標点
         */
        public Vertex to() {
            return this.to;
        }
    }

    private Set<Vertex> vertices = new HashSet<CFG.Vertex>();
    private Set<Edge> edges = new HashSet<CFG.Edge>();
    private Vertex entryVertex = null;
    private Set<Vertex> exitVertices = null;

    /**
     * 指定された頂点をグラフに追加する.
     * 
     * @param v
     *            追加する頂点
     */
    public void add(Vertex v) {
        this.vertices.add(v);
    }

    /**
     * 指定された頂点をグラフから取り除く.
     * 
     * @param v
     *            取り除く頂点
     * @return TODO: ArrayList.removeの戻り値を調べる
     */
    public boolean remove(Vertex v) {
        return this.vertices.remove(v);
    }

    /**
     * 指定された辺をグラフに追加する.
     * 
     * @param e
     *            追加する辺
     */
    public void add(Edge e) {
        this.edges.add(e);
    }

    /**
     * 指定された辺をグラフから取り除く.
     * 
     * @param e
     *            取り除く辺
     * @return TODO: ArrayList.removeの戻り値を調べる
     */
    public boolean remove(Edge e) {
        return this.edges.remove(e);
    }

    /**
     * 指定されたグラフ全体をこのグラフに追加する. グラフcfgのすべての頂点と辺を、グラフthisへ追加する.
     * グラフcfgの入口ノードと出口ノードは無視されるため、ユーザーの側で対処する必要がある.
     * 
     * @param cfg
     *            追加するグラフ
     */
    public void add(CFG cfg) {
        if (cfg != null) {
            this.vertices.addAll(cfg.vertices());
            this.edges.addAll(cfg.edges());
        }
    }

    /**
     * 指定されたグラフ全体をこのグラフに追加し、追加したグラフとこのグラフを接続する. グラフcfgのすべての頂点と辺を、グラフthisへ追加する.
     * connectFromからcfg
     * .entryVertex()へ、cfg.exitVertices()からconnectToへの辺をそれぞれ生成しグラフthisへ追加する.
     * 
     * @param cfg
     *            追加するグラフ
     * @param connectFrom
     *            追加したグラフの入口ノードへ接続する本体側グラフのノード
     * @param connectTo
     *            追加したグラフの出口ノードから接続される本体側グラフのノード
     */
    public void add(CFG cfg, Set<Vertex> connectFrom, Vertex connectTo) {
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

    /**
     * 指定された頂点をこのグラフの入口ノードとして設定する.
     * 
     * @param v
     *            入口ノードとして設定したい頂点
     */
    public void setEntryVertex(Vertex v) {
        this.entryVertex = v;
    }

    /**
     * 指定された頂点をこのグラフの出口ノードとして設定する.
     * 
     * @param vs
     *            出口ノードとして設定したい頂点集合
     */
    public void setExitVertices(Collection<Vertex> vs) {
        if (vs instanceof Set) {
            this.exitVertices = (Set<Vertex>) vs;
        } else {
            this.exitVertices = new HashSet<CFG.Vertex>(vs); // TODO:
                                                             // TreeSetのコンストラクタ(Collection)を調べる
        }
    }

    /**
     * 指定された1つの頂点だけをこのグラフの出口ノードとして設定する. 便利メソッド.
     * 
     * @param v1
     *            出口ノードとして設定したい頂点
     */
    public void setExitVertices(Vertex v1) {
        this.exitVertices = new HashSet<CFG.Vertex>();
        this.exitVertices.add(v1);
    }

    /**
     * 指定された2つの頂点だけをこのグラフの出口ノードとして設定する. 便利メソッド.
     * 
     * @param v1
     *            出口ノードとして設定したい頂点その1
     * @param v2
     *            出口ノードとして設定したい頂点その2
     */
    public void setExitVertices(Vertex v1, Vertex v2) {
        this.exitVertices = new HashSet<CFG.Vertex>();
        this.exitVertices.add(v1);
        this.exitVertices.add(v2);
    }

    public Set<Vertex> vertices() {
        return this.vertices;
    }

    public Set<Edge> edges() {
        return this.edges;
    }

    public Vertex entryVertex() {
        return this.entryVertex;
    }

    public Set<Vertex> exitVertices() {
        return this.exitVertices;
    }
}
