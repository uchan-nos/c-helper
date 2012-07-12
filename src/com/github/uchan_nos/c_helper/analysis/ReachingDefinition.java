package com.github.uchan_nos.c_helper.analysis;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Set;

import org.eclipse.cdt.core.dom.ast.*;

/**
 * 到達定義解析を行うクラス.
 * @author uchan
 */
public class ReachingDefinition {
    // gen, kill集合を保持する構造体
    private static class GenKill {
        final public BitSet gen;
        final public BitSet kill;
        public GenKill(BitSet gen, BitSet kill) {
            this.gen = gen;
            this.kill = kill;
        }
    }

    private CFG cfg; // 解析すべきソースのフローグラフ
    private ArrayList<AssignExpression> assignList; // cfgに含まれる代入文のリスト（DummyAssignExpressionを含む）
    private Set<IASTIdExpression> idExpressionList; // cfgに含まれるID式のリスト
    private ArrayList<DummyAssignExpression> dummyAssignList; // cfgに含まれるダミー変数定義のリスト

    /**
     * 到達定義実行器を生成する.
     * @param cfg 解析するソースコードの制御フローグラフ
     * @param assignList ソースコード中の代入文のリスト
     */
    public ReachingDefinition(CFG cfg, ArrayList<AssignExpression> assignList, Set<IASTIdExpression> idExpressionList) {
        this.cfg = cfg;
        this.assignList = new ArrayList<AssignExpression>(assignList);
        this.idExpressionList = idExpressionList;
        this.dummyAssignList = createInitialAssigns(this.assignList.size(), this.idExpressionList);
        this.assignList.addAll(this.dummyAssignList);
    }

    public void analyze() {
        // フローグラフ中の頂点数
        final int numVertex = cfg.getVertices().size();

        ArrayList<CFG.Vertex> vertices = new ArrayList<CFG.Vertex>(CFGPrinter.sort(cfg.getVertices()));

        // 各頂点に対応するgen, killを生成
        ArrayList<GenKill> genkill = new ArrayList<ReachingDefinition.GenKill>(numVertex);
        for (int i = 0; i < numVertex; ++i) {
            genkill.add(createGenKill(vertices.get(i)));
        }

        // 各頂点の入口と出口の到達定義集合
        ArrayList<BitSet> entry = new ArrayList<BitSet>(numVertex);
        ArrayList<BitSet> exit = new ArrayList<BitSet>(numVertex);
        // exitに変更が加わったか調べるために過去の値を保存しておく
        ArrayList<BitSet> exitPrev = new ArrayList<BitSet>(numVertex);
        for (int i = 0; i < numVertex; ++i) {
            entry.add(new BitSet(assignList.size()));
            exit.add(new BitSet(assignList.size()));
            exitPrev.add(new BitSet(assignList.size()));
        }

        final BitSet entryOfEntryVertex = entry.get(vertices.indexOf(cfg.entryVertex()));
        for (DummyAssignExpression e : this.dummyAssignList) {
            entryOfEntryVertex.set(e.getId());
        }

        boolean modified = true;
        while (modified) {
            for (int i = 0; i < numVertex; ++i) {
                for (CFG.Vertex leading : cfg.getConnectedVerticesTo(vertices.get(i))) {
                    int leadingIndex = vertices.indexOf(leading);
                    entry.get(i).or(exit.get(leadingIndex));
                }
                // exit, entryは単調増加であるので、clearしなくてよい
                exit.get(i).or(entry.get(i));
                exit.get(i).andNot(genkill.get(i).kill);
                exit.get(i).or(genkill.get(i).gen);
            }
            if (exit.equals(exitPrev)) {
                modified = false;
            } else {
                for (int i = 0; i < exit.size(); ++i) {
                    // exit, entryは単調増加であるので、clearしなくてよい
                    exitPrev.get(i).or(exit.get(i));
                }
            }
        }

        final int dummyAssignStartId = dummyAssignList.size() > 0 ?
                        dummyAssignList.get(0).getId() : assignList.size();
        for (int i = 0; i < numVertex; ++i) {
            System.out.println(vertices.get(i).label());
            System.out.print("  entry ");
            for (int b = 0; b < assignList.size(); ++b) {
                if (entry.get(i).get(b)) {
                    System.out.print("(" + assignList.get(b).getLHS().getRawSignature()
                            + "," + (b >= dummyAssignStartId ? "?" : String.valueOf(b)) + ")");
                }
            }
            System.out.println();
            System.out.print("  exit  ");
            for (int b = 0; b < assignList.size(); ++b) {
                if (exit.get(i).get(b)) {
                    System.out.print("(" + assignList.get(b).getLHS().getRawSignature()
                            + "," + (b >= dummyAssignStartId ? "?" : String.valueOf(b)) + ")");
                }
            }
            System.out.println();
        }
    }

    /**
     * 指定された頂点のgen, kill集合を生成する.
     * @param v gen, kill集合を生成したい頂点
     * @return gen, kill集合
     */
    private GenKill createGenKill(CFG.Vertex v) {
        BitSet kill = new BitSet(assignList.size());
        BitSet gen = new BitSet(assignList.size());
        for (int i = v.getASTNodes().size() - 1; i >= 0; --i) {
            GenKill sub = createGenKill(v.getASTNodes().get(i));
            if (sub != null) {
                if (sub.gen != null) {
                    sub.gen.andNot(kill);
                    gen.or(sub.gen);
                }
                if (sub.kill != null) {
                    kill.or(sub.kill);
                }
            }
        }
        return new GenKill(gen, kill);
    }

    // 指定されたASTノードのgen, kill集合を生成する.
    private GenKill createGenKill(IASTNode ast) {
        if (ast instanceof IASTExpressionStatement) {
            return createGenKill((IASTExpressionStatement)ast);
        } else if (ast instanceof IASTDeclarationStatement) {
            return createGenKill((IASTDeclarationStatement)ast);
        }
        return null;
    }

    private GenKill createGenKill(IASTExpressionStatement ast) {
        // 代入に対応したgen, kill
        AssignExpression assign = getAssignExpressionOfNode(ast.getExpression());
        ArrayList<AssignExpression> killedAssigns = null;

        if (assign != null) {
            if (assign.getLHS() instanceof IASTIdExpression) {
                killedAssigns = getAssignExpressionsOfName(
                        ((IASTIdExpression)assign.getLHS()).getName());
            }
        }

        if (assign != null && killedAssigns != null) {
            BitSet gen = new BitSet(assignList.size());
            BitSet kill = new BitSet(assignList.size());
            for (int i = 0; i < killedAssigns.size(); ++i) {
                kill.set(killedAssigns.get(i).getId());
            }
            gen.set(assign.getId());
            return new GenKill(gen, kill);
        }
        return null;

    }

    private GenKill createGenKill(IASTDeclarationStatement ast) {
        // 初期化付き変数定義に対応したgen, kill
        if (ast.getDeclaration() instanceof IASTSimpleDeclaration) {
            IASTDeclarator[] declarators =
                    ((IASTSimpleDeclaration)ast.getDeclaration()).getDeclarators();
            BitSet gen = new BitSet(assignList.size());
            BitSet kill = new BitSet(assignList.size());
            for (int i = 0; i < declarators.length; ++i) {
                if (declarators[i].getInitializer() != null) {
                    AssignExpression assign = getAssignExpressionOfNode(declarators[i]);
                    // assignがnullになるのは、初期化なし変数定義の場合
                    gen.set(assign.getId());
                    IASTName nameToBeKilled = null;
                    if (assign.getLHS() instanceof IASTName) {
                        nameToBeKilled = (IASTName)assign.getLHS();
                    } else if (assign.getLHS() instanceof IASTIdExpression) {
                        nameToBeKilled = ((IASTIdExpression)assign.getLHS()).getName();
                    }
                    if (nameToBeKilled != null) {
                        ArrayList<AssignExpression> killedAssigns =
                                getAssignExpressionsOfName(nameToBeKilled);
                        for (int j = 0; j < killedAssigns.size(); ++j) {
                            kill.set(killedAssigns.get(j).getId());
                        }
                    }
                }
            }

            return new GenKill(gen, kill);
        }
        return null;
    }

    /**
     * 指定されたASTノードに対応する変数定義を取得する.
     * 指定するASTノードは初期化付き変数定義のIASTDeclaratorか
     * 代入文のIASTBinaryExpressionに対応している.
     * @param ast
     * @return
     */
    private AssignExpression getAssignExpressionOfNode(IASTNode ast) {
        for (int i = 0; i < assignList.size(); ++i) {
            if (assignList.get(i).getAST() == ast) {
                return assignList.get(i);
            }
        }
        return null;
    }

    /**
     * 指定された名前を左辺値として持つ変数定義の一覧を取得する.
     * @param name 抽出する名前
     * @return 抽出された変数定義一覧
     */
    private ArrayList<AssignExpression> getAssignExpressionsOfName(IASTName name) {
        ArrayList<AssignExpression> result = new ArrayList<AssignExpression>();
        for (int i = 0; i < assignList.size(); ++i) {
            IASTNode lhs = assignList.get(i).getLHS();
            if (lhs instanceof IASTName) {
                IASTName n = (IASTName)lhs;
                if (Arrays.equals(n.getSimpleID(), name.getSimpleID())) {
                    result.add(assignList.get(i));
                }
            } else if (lhs instanceof IASTIdExpression) {
                IASTIdExpression e = (IASTIdExpression)lhs;
                if (Arrays.equals(e.getName().getSimpleID(), name.getSimpleID())) {
                    result.add(assignList.get(i));
                }
            }
        }
        return result;
    }

    private static ArrayList<DummyAssignExpression> createInitialAssigns(int startId, Collection<IASTIdExpression> idExpressionList) {
        int id = startId;
        ArrayList<DummyAssignExpression> result = new ArrayList<DummyAssignExpression>();
        for (IASTIdExpression idExpression : idExpressionList) {
            DummyAssignExpression e = new DummyAssignExpression(id, idExpression);
            result.add(e);
            id++;
        }
        return result;
    }
}
