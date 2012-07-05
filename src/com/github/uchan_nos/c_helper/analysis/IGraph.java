package com.github.uchan_nos.c_helper.analysis;
import java.util.Collection;
import java.util.Set;

/**
 * グラフ操作のインターフェース.
 * @author uchan
 *
 * @param <Vertex>
 */
public interface IGraph<Vertex> {
    /**
     * 指定された頂点をグラフに追加する.
     * 頂点 v が既にグラフに含まれる場合は何もしない.
     * @param v 追加する頂点
     */
    void add(Vertex v);

    /**
     * 指定された頂点集合をグラフに追加する.
     * 頂点集合に含まれる頂点のうち、既にグラフに含まれる頂点は追加しない.
     * @param vs 追加する頂点の集合
     */
    void add(Collection<Vertex> vs);

    /**
     * 指定された頂点をグラフから削除する.
     * 頂点 v に接続されている辺（v -> x, x -> v）もすべて削除される.
     * 頂点 v がグラフに存在しない場合は NoSuchElementException を投げる.
     * @param v 削除する頂点
     */
    void remove(Vertex v);

    /**
     * 2つの頂点を結ぶ辺をグラフに追加する.
     * 2つの頂点 from と to がグラフに存在しない場合は NoSuchElementException を投げる.
     * @param from 開始点
     * @param to 終了点
     */
    void connect(Vertex from, Vertex to);

    /**
     * 2つの頂点を結ぶ辺をグラフから削除する.
     * 2つの頂点 from と to がグラフに存在しない場合は NoSuchElementException を投げる.
     * 辺が存在しない場合は何もしない.
     * @param from 開始点
     * @param to 終了点
     */
    void disconnect(Vertex from, Vertex to);

    /**
     * 指定された頂点がグラフに含まれるかを返す.
     * @param v 調べる頂点
     * @return 含まれるなら true, 含まれないなら false
     */
    boolean contains(Vertex v);

    /**
     * 2つの頂点を結ぶ辺がグラフに含まれるかを返す.
     * @param from 開始点
     * @param to 終了点
     * @return 含まれるなら true, 含まれないなら false
     */
    boolean isConnected(Vertex from, Vertex to);

    /**
     * 指定された頂点から出ている辺に接続している頂点集合を取得する.
     * { x | v -> x } を返す.
     * @param v 辺の開始点
     * @return 辺の終了点の集合
     */
    Set<Vertex> getConnectedVerticesFrom(Vertex v);

    /**
     * 指定された頂点へ入っている辺に接続している頂点集合を取得する.
     * { x | x -> v } を返す.
     * @param v 辺の終了点
     * @return 辺の開始点の集合
     */
    Set<Vertex> getConnectedVerticesTo(Vertex v);

    /**
     * グラフに含まれるすべての頂点の集合を返す.
     * @return すべての頂点
     */
    Set<Vertex> getVertices();
}
