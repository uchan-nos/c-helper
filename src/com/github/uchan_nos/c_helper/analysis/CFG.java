package com.github.uchan_nos.c_helper.analysis;

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
public class CFG extends DirectedGraph<CFG.Vertex> {
    /**
     * 制御フローグラフの1つの頂点を表す. 頂点は、0個以上のASTノードを含む.
     *
     * @author uchan
     *
     */
    static public class Vertex {
        private IASTNode astNode = null;
        private String label;

        /**
         * 指定されたラベルを持つ頂点を生成する. ラベルはIDとしては用いられないので、他の頂点と重複してもよい.
         *
         * @param label 頂点のラベル
         */
        public Vertex(String label) {
            this.label = label;
        }

        /**
         * 空の頂点を生成する. 生成された頂点のラベルは空文字列となる.
         *
         * @param label 頂点のラベル
         */
        public Vertex() {
            this.label = "";
        }

        /**
         * 指定されたASTノードを頂点に追加する.
         *
         * @param node 追加するASTノード
         */
        public void setASTNode(IASTNode node) {
            this.astNode = node;
        }

        /**
         * 頂点が含むASTノードを取得する.
         *
         * @return 頂点が含むASTノードのリスト
         */
        public IASTNode getASTNode() {
            return this.astNode;
        }

        /**
         * 頂点のラベルを取得する.
         *
         * @return 頂点のラベル
         */
        public String label() {
            return this.label;
        }

        /**
         * 頂点のラベルを更新する.
         *
         * @param label 新しいラベル
         */
        public void setLabel(String label) {
            this.label = label;
        }
    }

    private Vertex entryVertex;
    private Vertex exitVertex;
    private Set<Vertex> breakVertices = new HashSet<CFG.Vertex>();
    private Set<Vertex> continueVertices = new HashSet<CFG.Vertex>();
    private Set<Vertex> caseVertices = new HashSet<CFG.Vertex>();

    /**
     * 指定された入口ノードと出口ノードを持つ制御フローグラフを生成する.
     * entryVertex, exitVertexはこのコンストラクタ内で頂点集合に追加される.
     * ノードに null が指定された場合、そのノードは追加しない.
     * @param entryVertex 入口ノード
     * @param exitVertex 出口ノード
     */
    public CFG(Vertex entryVertex, Vertex exitVertex) {
        if (entryVertex != null) {
            this.add(entryVertex);
        }
        if (exitVertex != null) {
            this.add(exitVertex);
        }
        this.entryVertex = entryVertex;
        this.exitVertex = exitVertex;
    }

    /**
     * 指定されたグラフ全体をこのグラフに追加する.
     * グラフcfgのすべての頂点と辺を、グラフthisへ追加する.
     * グラフcfgの入口ノードと出口ノードは無視されるため、ユーザーの側で対処する必要がある.
     *
     * @param cfg 追加するグラフ
     */
    public void add(CFG cfg) {
        if (cfg != null) {
            Set<Vertex> vs = cfg.getVertices();

            // 頂点を追加
            add(vs);
            add(cfg.entryVertex());
            add(cfg.exitVertex());

            // 辺を追加
            for (Vertex v : vs) {
                for (Vertex to : cfg.getConnectedVerticesFrom(v)) {
                    connect(v, to);
                }
            }
            for (Vertex to : cfg.getConnectedVerticesFrom(cfg.entryVertex())) {
                connect(cfg.entryVertex(), to);
            }
            for (Vertex to : cfg.getConnectedVerticesFrom(cfg.exitVertex())) {
                connect(cfg.entryVertex(), to);
            }
        }
    }

    /**
     * 指定されたグラフ全体をこのグラフに追加し、追加したグラフとこのグラフを接続する.
     * グラフcfgのすべての頂点と辺を、グラフthisへ追加する.
     * connectFromからcfg.entryVertex()へ、
     * cfg.exitVertex()からconnectToへの辺をそれぞれ生成しグラフthisへ追加する.
     *
     * @param cfg 追加するグラフ
     * @param connectFrom 追加したグラフの入口ノードへ接続する本体側グラフのノード
     * @param connectTo 追加したグラフの出口ノードから接続される本体側グラフのノード
     */
    public void add(CFG cfg, Vertex connectFrom, Vertex connectTo) {
        add(cfg);
        if (connectFrom != null) {
            connect(connectFrom, cfg.entryVertex());
        }
        if (connectTo != null) {
            connect(cfg.exitVertex(), connectTo);
        }
    }

    /**
     * 指定された頂点をこのグラフの入口ノードとして設定する.
     *
     * @param v 入口ノードとして設定したい頂点
     */
    public void setEntryVertex(Vertex v) {
        this.entryVertex = v;
    }

    /**
     * 指定された頂点をこのグラフの出口ノードとして設定する.
     *
     * @param vs 出口ノードとして設定したい頂点集合
     */
    public void setExitVertex(Vertex v) {
        this.exitVertex = v;
    }

    public Vertex entryVertex() {
        return this.entryVertex;
    }

    public Vertex exitVertex() {
        return this.exitVertex;
    }

    /**
     * break文を含む頂点をbreak頂点集合に追加する.
     * break文に対応するASTノードを、ASTノード配列の一番後ろに持つような頂点を想定している.
     * @param v break文を含む頂点
     */
    public void addBreakVertex(Vertex v) {
        this.breakVertices.add(v);
    }

    public void addBreakVertex(Collection<Vertex> vs) {
        this.breakVertices.addAll(vs);
    }

    public Set<Vertex> breakVertices() {
        return this.breakVertices;
    }

    /**
     * continue文を含む頂点をcontinue頂点集合に追加する.
     * continue文に対応するASTノードを、ASTノード配列の一番後ろに持つような頂点を想定している.
     * @param v continue文を含む頂点
     */
    public void addContinueVertex(Vertex v) {
        this.continueVertices.add(v);
    }

    public void addContinueVertex(Collection<Vertex> vs) {
        this.continueVertices.addAll(vs);
    }

    public Set<Vertex> continueVertices() {
        return this.continueVertices;
    }

    /**
     * case文を含む頂点をcase頂点集合に追加する.
     * case文に対応するASTノードを、ASTノード配列の一番後ろに持つような頂点を想定している.
     * @param v case文を含む頂点
     */
    public void addCaseVertex(Vertex v) {
        this.caseVertices.add(v);
    }

    public void addCaseVertex(Collection<Vertex> vs) {
        this.caseVertices.addAll(vs);
    }

    public Set<Vertex> caseVertices() {
        return this.caseVertices;
    }
}
