package com.github.uchan_nos.c_helper.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.cdt.core.dom.ast.*;
import org.eclipse.cdt.internal.core.pdom.indexer.IndexerASTVisitor;

import com.github.uchan_nos.c_helper.analysis.AssignExpression;
import com.github.uchan_nos.c_helper.analysis.CFG;
import com.github.uchan_nos.c_helper.analysis.RD;

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
                if (o1.getASTNode() == null && o2.getASTNode() == null) {
                    return 0;
                } else if (o1.getASTNode() == null && o2.getASTNode() != null) {
                    return -1;
                } else if (o1.getASTNode() != null && o2.getASTNode() == null) {
                    return 1;
                } else {
                    IASTNode n1 = o1.getASTNode();
                    IASTNode n2 = o2.getASTNode();
                    return n1.getFileLocation().getNodeOffset() - n2.getFileLocation().getNodeOffset();
                }
            }
        }

        TreeSet<CFG.Vertex> sorted = new TreeSet<CFG.Vertex>(new VertexComparator());
        sorted.addAll(vertices);
        return sorted;
    }

    /**
     * 指定された名前の到達定義を取得する.
     */
    public static Set<AssignExpression> getAssigns(AssignExpression[] assigns, BitSet rd, String name) {
        Set<AssignExpression> result = new HashSet<AssignExpression>();
        for (AssignExpression ae : assigns) {
            if (rd.get(ae.getId())) {
                IASTNode lhs = ae.getLHS();
                String nameOfLhs = null;
                if (lhs instanceof IASTIdExpression) {
                    nameOfLhs = ((IASTIdExpression)lhs).getName().toString();
                } else if (lhs instanceof IASTName) {
                    nameOfLhs = ((IASTName)lhs).toString();
                }

                if (name.equals(nameOfLhs)) {
                    result.add(ae);
                }
            }
        }
        return result;
    }

    /**
     * 指定されたビット数だけを取り出す.
     * @return ビットマスク後の値（非負整数）
     */
    public static BigInteger maskBits(BigInteger value, int bits) {
        BigInteger mask = BigInteger.ONE.shiftLeft(bits).subtract(BigInteger.ONE);
        return value.and(mask);
    }

    /**
     * 与えられた整数の指定されたbits以上のビットを切り落とす.
     * 切り落とした後の整数の bits-1 ビット目が1なら、負数に変換する.
     *
     * value = 416 (0x01a0), bits = 8
     * のとき、結果は
     * -96 (0xffa0)
     * となる.
     *
     * @param value 切り落とし対象の整数
     * @param bits 切り落とす位置
     * @return bits 以上のビットを切り落とした整数
     */
    public static BigInteger cutBits(BigInteger value, int bits) {
        BigInteger mask = BigInteger.ONE.shiftLeft(bits).subtract(BigInteger.ONE);
        value = value.and(mask);
        if (value.testBit(bits - 1)) {
            value = value.or(mask.not());
        }
        return value;
    }

    /**
     * 行頭からのオフセットを返す.
     * @param source ソースコード
     * @param offset ソースコード先頭からのオフセット
     * @return 行頭からのオフセット
     */
    public static int calculateColumnNumber(String source, int offset) {
        int prevLF = source.lastIndexOf('\n', offset);
        return offset - prevLF - 1; // prevLFが-1でもきちんと動く
    }
}
