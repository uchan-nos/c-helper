package com.github.uchan_nos.c_helper.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Logger;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.cdt.core.dom.ast.*;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;

import com.github.uchan_nos.c_helper.analysis.AssignExpression;
import com.github.uchan_nos.c_helper.analysis.CFG;

/**
 * 便利関数群.
 * @author uchan
 */
public class Util {
    /**
     * 入力ストリームの内容をすべて読み込み、文字列として返す.
     * @param inputStream 読み込むコンテンツ
     * @param charsetName ファイルのエンコーディング
     * @return 入力ストリームの内容
     * @throws IOException
     */
    public static String readInputStreamAll(InputStream inputStream, String charsetName) throws IOException {
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, charsetName));
        StringBuilder sb = new StringBuilder();
        int c;
        while ((c = reader.read()) != -1) {
            sb.append((char)c);
        }
        reader.close();
        return sb.toString();
    }

    /**
     * 入力ストリームの内容をすべて読み込み、文字列として返す.
     * ファイルはUTF-8でエンコーディングされていると仮定する.
     * @param inputStream 読み込むコンテンツ
     * @return 入力ストリームの内容
     * @throws IOException
     */
    public static String readInputStreamAll(InputStream inputStream) throws IOException {
        return readInputStreamAll(inputStream, "UTF-8");
    }

    /**
     * ファイル内容をすべて読み込み、文字列として返す.
     * @param file 読み込むファイル
     * @param charsetName ファイルのエンコーディング
     * @return ファイルの内容
     * @throws IOException
     */
    public static String readFileAll(File file, String charsetName) throws IOException {
        return readInputStreamAll(new FileInputStream(file), charsetName);
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
                    if (o1.equals(o2)) {
                        return 0;
                    } else {
                        return -1;
                    }
                } else if (o1.getASTNode() == null && o2.getASTNode() != null) {
                    return -1;
                } else if (o1.getASTNode() != null && o2.getASTNode() == null) {
                    return 1;
                } else {
                    IASTNode n1 = o1.getASTNode();
                    IASTNode n2 = o2.getASTNode();
                    IASTFileLocation l1 = n1.getFileLocation();
                    IASTFileLocation l2 = n2.getFileLocation();
                    if (l1.getNodeOffset() != l2.getNodeOffset()) {
                        return l1.getNodeOffset() - l2.getNodeOffset();
                    } else if (l1.getNodeLength() != l2.getNodeLength()) {
                        return l1.getNodeLength() - l2.getNodeLength();
                    } else {
                        return -1;
                    }
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
    public static Set<AssignExpression> getAssigns(AssignExpression[] assigns, BitSet rd, IASTName name) {
        return getAssigns(assigns, rd, name.resolveBinding());
    }

    /**
     * 指定された変数の到達定義を取得する.
     */
    public static Set<AssignExpression> getAssigns(AssignExpression[] assigns, BitSet rd, IBinding var) {
        Set<AssignExpression> result = new HashSet<AssignExpression>();
        for (AssignExpression ae : assigns) {
            if (rd.get(ae.getId())) {
                IASTNode lhs = ae.getLHS();
                IASTName nameOfLhs = Util.getName(lhs);

                if (var.equals(nameOfLhs.resolveBinding())) {
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
    public static int calculateColumnNumber(String source, int offset, String lineDelimiter) {
        int prevLF = source.lastIndexOf(lineDelimiter, offset);
        if (prevLF >= 0) {
            return offset - prevLF - lineDelimiter.length();
        }
        return offset;
    }

    /**
     * 行頭からのオフセットを返す.
     * @param source ソースコード
     * @param offset ソースコード先頭からのオフセット
     * @return 行頭からのオフセット
     */
    public static int calculateColumnNumber(IDocument source, int offset) throws BadLocationException {
        return offset - source.getLineOffset(source.getLineOfOffset(offset));
    }

    /**
     * 文字列にパターンが出現する回数を返す.
     * @param s 検索対象の文字列
     * @param pattern 検索するパターン
     * @return パターンの出現回数
     */
    public static int countMatches(String s, Pattern pattern) {
        int count = 0;
        Matcher m = pattern.matcher(s);
        while (m.find()) {
            ++count;
        }
        return count;
    }

    /**
     * 指定されたノードの raw signature を返す.
     * もし指定されたノードがnullなら空文字列""を返す.
     * @param node raw signature を取得したいノード
     * @return raw signature
     */
    public static String getRawSignature(IASTNode node) {
        return node != null ? node.getRawSignature() : "";
    }

    public interface CharPredicate {
        boolean evaluate(char c);
    }

    /**
     * 指定された文字列から指定された述語が成り立つ文字を探し、そのインデックスを返す.
     * 述語を渡せる String#indexOf と考えればよい.
     * @param s
     * @param p
     * @param fromIndex
     * @return
     */
    public static int indexOf(String s, CharPredicate p, int fromIndex) {
        for (int i = fromIndex >= 0 ? fromIndex : 0; i < s.length(); ++i) {
            if (p.evaluate(s.charAt(i))) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 指定された文字列から指定された述語が成り立つ文字を探し、そのインデックスを返す.
     * 述語を渡せる String#indexOf と考えればよい.
     * commentsにnull以外が渡された場合、
     * commentsに含まれるコメント以外で初めて現れた文字のインデックスを返す.
     * @param s
     * @param p
     * @param fromIndex
     * @param comments
     * @param commentOffset
     * @return
     */
    public static int indexOf(String s, CharPredicate p, int fromIndex, IASTComment[] comments, final int commentOffset) {
        if (comments == null) {
            return indexOf(s, p, fromIndex);
        }

        // コメントの位置順にソート
        Arrays.sort(comments, new Comparator<IASTComment>() {
            @Override
            public int compare(IASTComment o1, IASTComment o2) {
                IASTFileLocation l1 = o1.getFileLocation();
                IASTFileLocation l2 = o2.getFileLocation();
                if (l1.getNodeOffset() == l2.getNodeOffset()) {
                    return l1.getNodeLength() - l2.getNodeLength();
                } else {
                    return l1.getNodeOffset() - l2.getNodeOffset();
                }
            }
        });

        final TwoComparator<IASTComment, Integer> comparator = new TwoComparator<IASTComment, Integer>() {
            @Override
            public int compare(IASTComment a, Integer b) {
                final int offset = commentOffset + a.getFileLocation().getNodeOffset();
                final int length = a.getFileLocation().getNodeLength();
                if (offset <= b && b < offset + length) {
                    return 0;
                } else if (offset + length <= b) {
                    return -1;
                } else {
                    return 1;
                }
            }
        };

        while (true) {
            int pos = indexOf(s, p, fromIndex);
            if (pos < 0) {
                return pos;
            }

            int commentPos = binarySearch(comments, pos, comparator);
            if (commentPos < 0) {
                return pos;
            }
            fromIndex = pos + 1;
        }
    }

    public interface TwoComparator<T, U> {
        int compare(T a, U b);
    }

    public static <T,U> int binarySearch(T[] a, U key, TwoComparator<T,U> c) {
        int low = 0;
        int high = a.length - 1;
        int mid = 0;

        while (low <= high) {
            mid = (high + low) / 2;
            int res = c.compare(a[mid], key);
            if (res == 0) {
                return mid;
            } else if (res > 0) {
                high = mid - 1;
            } else {
                low = mid + 1;
            }
        }
        return -low - 1;
    }

    /**
     * 与えられたノードから IASTName を探して返す.
     * 与えられたノードが IASTIdExpression または IASTName 以外なら null を返す.
     * @param node 検索対象のノード
     * @return 検索された IASTName. ヒットしなければ null.
     */
    public static IASTName getName(IASTNode node) {
        if (node instanceof IASTIdExpression) {
            return ((IASTIdExpression) node).getName();
        } else if (node instanceof IASTName) {
            return (IASTName) node;
        }
        return null;
    }

    public static boolean equals(char[] s1, String s2) {
        return String.valueOf(s1).equals(s2);
    }

    public static boolean equals(String s1, char[] s2) {
        return equals(s2, s1);
    }

    /**
     * 関数の引数を Expression の配列として取得.
     * @param fce 関数呼び出し式
     * @return 引数の配列
     */
    public static IASTExpression[] getArguments(IASTFunctionCallExpression fce)
    {
        IASTExpression[] args = new IASTExpression[fce.getArguments().length];
        for (int i = 0; i < args.length; ++i) {
            IASTInitializerClause arg = fce.getArguments()[i];
            args[i] = (IASTExpression) arg;
        }
        return args;
    }

    public static <T> int indexOf(T value, T... collection) {
        if (collection == null) {
            return -1;
        }
        for (int i = 0; i < collection.length; ++i) {
            if (value.equals(collection[i])) {
                return i;
            }
        }
        return -1;
    }

    public static <T> boolean contains(T value, T... collection) {
        return indexOf(value, collection) >= 0;
    }

    /**
     * 指定されたASTノードが属しているスコープを返す.
     * @param node ASTノード
     * @return nodeが属しているスコープ
     */
    public static IScope getScope(IASTNode node) {
        while (node != null && !(node instanceof IASTCompoundStatement)) {
            node = node.getParent();
        }
        if (node != null) {
            return ((IASTCompoundStatement) node).getScope();
        }
        return null;
    }

    /**
     * 指定されたASTノードが属しているスコープを、グローバルスコープまでさかのぼってすべて取得する.
     * @param node ASTノード
     * @return nodeが属する一番深いスコープが先頭要素となったスコープのリスト
     */
    public static List<IScope> getAllScopes(IASTNode node) {
        ArrayList<IScope> scopes = new ArrayList<IScope>();
        while (node != null) {
            if (node instanceof IASTCompoundStatement) {
                scopes.add(((IASTCompoundStatement) node).getScope());
            }
            node = node.getParent();
        }
        return scopes;
    }

    /**
     * 指定されたASTノードが指定された二項演算子を使った二項式かどうかを調べる.
     * @param node ASTノード
     * @param operator 期待する二項演算子
     * @return node が operator を使った二項式なら true
     */
    public static boolean isIASTBinaryExpression(IASTNode node, int operator) {
        return node instanceof IASTBinaryExpression
            && ((IASTBinaryExpression) node).getOperator() == operator;
    }

    /**
     * 指定された2つのオブジェクトが等しいか調べる.
     * 2つのオブジェクトが共にnullなら true.
     * そうでなければequalsを呼び出す.
     */
    public static boolean equalsOrBothNull(Object a, Object b) {
        return (a == null ? b == null : a.equals(b));
    }

    /**
     * 指定されたASTノードに含まれるすべての変数名を取得する.
     * すべてのIASTNameのうち、resolveBinding() instanceof IVariable
     * であるIASTNameのコレクションを返す.
     */
    public static Collection<IASTName> getAllVariableNames(IASTNode node) {
        ASTFilter filter = new ASTFilter(node);
        Collection<IASTNode> names = filter.filter(
                new ASTFilter.Predicate() {
                    @Override
                    public boolean pass(IASTNode n) {
                        return n instanceof IASTName;
                    }
                });

        Set<IASTName> variableNames = new HashSet<IASTName>();
        for (IASTNode n : names) {
            IASTName name = (IASTName) n;
            IBinding binding = name.resolveBinding();
            if (binding instanceof IVariable) {
                variableNames.add(name);
            }
        }

        return variableNames;
    }

    /**
     * 汎用の関数インターフェース.
     */
    public interface Function<Ret, Arg> {
        Ret calc(Arg arg);
    }

    /**
     * 汎用の述語インターフェース.
     */
    public interface Predicate<T> {
        boolean calc(T arg);
    }

    /**
     * arg の要素すべてに f を適用した結果を ret に格納する.
     */
    public static <Ret, Arg> Collection<Ret> map(Collection<Arg> arg, Function<Ret, Arg> f, Collection<Ret> ret) {
        ret.clear();
        for (Arg elem : arg) {
            ret.add(f.calc(elem));
        }
        return ret;
    }

    /**
     * arg の要素のうち p が成り立つ要素のみ ret に格納する.
     */
    public static <T> Collection<T> filter(Collection<T> arg, Predicate<T> p, Collection<T> ret) {
        ret.clear();
        for (T elem : arg) {
            if (p.calc(elem)) {
                ret.add(elem);
            }
        }
        return ret;
    }

    /**
     * 指定されたASTノードの子要素を、指定されたインデックスでたどる.
     * インデックスが複数指定された場合、指定された個数だけ奥に潜っていく.
     * nodeが持つ子要素の階層より指定されたインデックスの個数が多いならnullを返す.
     */
    public static IASTNode getChildNode(IASTNode node, int... indices) {
        for (int i : indices) {
            if (node == null) {
                return null;
            } else {
                node = node.getChildren()[i];
            }
        }
        return node;
    }

    /**
     * ロガーに登録されたコンソールハンドラを返す.
     * 登録されてなければ、新規作成したハンドラを登録してから返す.
     */
    public static ConsoleHandler GetConsoleHandler(Logger logger) {
        for (Handler handler : logger.getHandlers()) {
            if (handler instanceof ConsoleHandler) {
                return (ConsoleHandler) handler;
            }
        }

        ConsoleHandler consoleHandler = new ConsoleHandler();
        logger.addHandler(consoleHandler);

        return consoleHandler;
    }
}
