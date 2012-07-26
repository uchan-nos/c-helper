package com.github.uchan_nos.c_helper.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.cdt.core.dom.ast.IASTNode;

import com.github.uchan_nos.c_helper.analysis.CFG;

/**
 * 便利関数群.
 * @author uchan
 */
public class Util {
    /**
     * ファイル内容をすべて読み込み、文字列として返す.
     * @param file 読み込むファイル
     * @param charsetName ファイルのエンコーディング
     * @return ファイルの内容
     * @throws IOException
     */
    public static String readFileAll(File file, String charsetName) throws IOException {
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), charsetName));
        StringBuilder sb = new StringBuilder();
        int c;
        while ((c = reader.read()) != -1) {
            sb.append((char)c);
        }
        reader.close();
        return sb.toString();
    }

    /**
     * ファイル内容をすべて読み込み、文字列として返す.
     * ファイルはUTF-8でエンコーディングされていると仮定する.
     * @param file 読み込むファイル
     * @return ファイルの内容
     * @throws IOException
     */
    public static String readFileAll(File file) throws IOException {
        return readFileAll(file, "UTF-8");
    }

    /**
     * コントロールフローグラフの頂点集合をソートした集合を返す.
     * @param vertices 頂点集合
     * @return ソート済み頂点集合
     */
    public static Set<CFG.Vertex> sort(Set<CFG.Vertex> vertices) {
        /**
         * コントロールフローグラフの頂点の比較器.
         * @author uchan
         */
        class VertexComparator implements Comparator<CFG.Vertex> {
            @Override
            public int compare(CFG.Vertex o1, CFG.Vertex o2) {
                if (o1.getASTNodes().size() == 0) {
                    return -1;
                } else if (o2.getASTNodes().size() == 0) {
                    return 1;
                } else {
                    IASTNode n1 = o1.getASTNodes().get(0);
                    IASTNode n2 = o2.getASTNodes().get(0);
                    return n1.getFileLocation().getNodeOffset() - n2.getFileLocation().getNodeOffset();
                }
            }
        }

        TreeSet<CFG.Vertex> sorted = new TreeSet<CFG.Vertex>(new VertexComparator());
        sorted.addAll(vertices);
        return sorted;
    }
}
