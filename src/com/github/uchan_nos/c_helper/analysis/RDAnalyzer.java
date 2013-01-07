package com.github.uchan_nos.c_helper.analysis;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.DOMException;
import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTDeclarationStatement;
import org.eclipse.cdt.core.dom.ast.IASTDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTExpressionStatement;
import org.eclipse.cdt.core.dom.ast.IASTIdExpression;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.IBinding;
import org.eclipse.cdt.core.dom.ast.IFunctionType;
import org.eclipse.cdt.core.dom.ast.IScope;
import org.eclipse.cdt.core.dom.ast.IType;
import org.eclipse.core.runtime.CoreException;

import com.github.uchan_nos.c_helper.util.Util;

/**
 * 到達定義解析を行うクラス.
 * @author uchan
 */
public class RDAnalyzer {
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
    public RDAnalyzer(IASTTranslationUnit ast, CFG cfg) {
        this.cfg = cfg;
        this.assignList = createAssignExpressionList(ast);
        this.idExpressionList = createIdExpressionList(ast);
        this.dummyAssignList = createInitialAssigns(this.assignList.size(), this.idExpressionList);
        this.assignList.addAll(this.dummyAssignList);
    }

    public RD<CFG.Vertex> analyze() {
        // フローグラフ中の頂点数
        final int numVertex = cfg.getVertices().size();

        ArrayList<CFG.Vertex> vertices = new ArrayList<CFG.Vertex>(Util.sort(cfg.getVertices()));

        // 各頂点に対応するgen, killを生成
        ArrayList<GenKill> genkill = new ArrayList<RDAnalyzer.GenKill>(numVertex);
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

        //final int dummyAssignStartId = dummyAssignList.size() > 0 ?
        //                dummyAssignList.get(0).getId() : assignList.size();
        final Map<CFG.Vertex, BitSet> entrySets = new HashMap<CFG.Vertex, BitSet>();
        final Map<CFG.Vertex, BitSet> exitSets = new HashMap<CFG.Vertex, BitSet>();
        for (int i = 0; i < numVertex; ++i) {
            CFG.Vertex vertex = vertices.get(i);
            entrySets.put(vertex, entry.get(i));
            exitSets.put(vertex, exit.get(i));
            /*
            System.out.println();
            System.out.print("  entry  ");
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
            */
        }

        AssignExpression[] assigns = new AssignExpression[assignList.size()];
        return new RD<CFG.Vertex>(assignList.toArray(assigns), entrySets, exitSets);
    }

    /**
     * 指定された頂点のgen, kill集合を生成する.
     * @param v gen, kill集合を生成したい頂点
     * @return gen, kill集合
     */
    private GenKill createGenKill(CFG.Vertex v) {
        GenKill result = null;
        if (v.getASTNode() != null) {
            result = createGenKill(v.getASTNode());
        }
        if (result == null) {
            result = new GenKill(new BitSet(), new BitSet());
        }
        return result;
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
        IBinding nameBinding = name.resolveBinding();

        for (int i = 0; i < assignList.size(); ++i) {
            IASTNode lhs = assignList.get(i).getLHS();
            if (lhs instanceof IASTName) {
                IASTName n = (IASTName)lhs;
                if (n.resolveBinding().equals(nameBinding)) {
                    result.add(assignList.get(i));
                }
            } else if (lhs instanceof IASTIdExpression) {
                IASTIdExpression e = (IASTIdExpression)lhs;
                if (e.getName().resolveBinding().equals(nameBinding)) {
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
            IType type = idExpression.getExpressionType();
            if (!(type instanceof IFunctionType)) {
                DummyAssignExpression e = new DummyAssignExpression(id, idExpression);
                result.add(e);
                id++;
            }
        }
        return result;
    }

    private ArrayList<AssignExpression> createAssignExpressionList(IASTTranslationUnit ast) {
        final ArrayList<AssignExpression> result = new ArrayList<AssignExpression>();
        ast.accept(new ASTVisitor(true) {
            private int id = 0;
            @Override
            public int visit(IASTExpression expression) {
                if (expression instanceof IASTBinaryExpression) {
                    IASTBinaryExpression e = (IASTBinaryExpression)expression;
                    if (e.getOperator() == IASTBinaryExpression.op_assign) {
                        result.add(new AssignExpression(id, e));
                        id++;
                    }
                }
                return super.visit(expression);
            }
            @Override
            public int visit(IASTDeclaration declaration) {
                if (declaration instanceof IASTSimpleDeclaration) {
                    IASTSimpleDeclaration d = (IASTSimpleDeclaration)declaration;
                    for (IASTDeclarator decl : d.getDeclarators()) {
                        if (decl.getInitializer() != null) {
                            // 初期化付き変数宣言
                            result.add(new AssignExpression(id, decl));
                            id++;
                        }
                    }
                }
                return super.visit(declaration);
            }
        });
        return result;
    }

    private Set<IASTIdExpression> createIdExpressionList(IASTTranslationUnit ast) {
        final Set<IASTIdExpression> result = new TreeSet<IASTIdExpression>(new Comparator<IASTIdExpression>() {
            @Override
            public int compare(IASTIdExpression o1, IASTIdExpression o2) {
                return o1.getName().toString().compareTo(o2.getName().toString());
            }
        });
        ast.accept(new ASTVisitor(true) {
            @Override
            public int visit(IASTExpression expression) {
                if (expression instanceof IASTIdExpression) {
                    result.add((IASTIdExpression)expression);
                }
                return super.visit(expression);
            }
        });
        return result;
    }

    public static void main(String[] args) {
        class RDEntry {
            final public String name;
            final public int id;
            public RDEntry(String name, int id) {
                this.name = name;
                this.id = id;
            }
        }

        if (args.length == 1) {
            String inputFilename = args[0];
            File inputFile = new File(inputFilename);

            try {
                String fileContent = Util.readFileAll(inputFile, "UTF-8");
                IASTTranslationUnit translationUnit =
                        new Parser(new FileInfo(inputFilename, false), fileContent).parse();
                Map<String, CFG> procToCFG =
                        new CFGCreator(translationUnit).create();
                for (Entry<String, CFG> entry : procToCFG.entrySet()) {
                    CFG cfg = entry.getValue();
                    System.out.println("function " + entry.getKey());

                    long start = System.currentTimeMillis();
                    RD<CFG.Vertex> rd =
                            new RDAnalyzer(translationUnit, cfg).analyze();
                    long end = System.currentTimeMillis();
                    System.out.println("time ellapsed: " + (end - start) + "ms");

                    for (CFG.Vertex vertex : Util.sort(cfg.getVertices())) {
                        IASTNode node = vertex.getASTNode();
                        IScope[] nodeScopes = Util.getAllScopes(node).toArray(new IScope[] {});

                        ArrayList<RDEntry> rdTemp = new ArrayList<RDEntry>();
                        BitSet exitSet = rd.getExitSets().get(vertex);
                        for (int assid = 0; assid < rd.getAssigns().length; ++assid) {
                            if (exitSet.get(assid)) {
                                AssignExpression assign = rd.getAssigns()[assid];
                                IASTName name = Util.getName(assign.getLHS());

                                try {
                                    if (Util.contains(name.resolveBinding().getScope(), nodeScopes)) {
                                        /*
                                        System.out.println(
                                                name.toString() + ":"
                                                + name.getFileLocation().getStartingLineNumber() + " scope in "
                                                + node.getFileLocation().getStartingLineNumber() + " node scopes");
                                                */
                                        rdTemp.add(new RDEntry(
                                                    assign.getLHS().getRawSignature()
                                                    + ":"
                                                    + assign.getLHS().getFileLocation().getStartingLineNumber(),
                                                    assign.getRHS() == null ? -1 : assid));
                                    }
                                } catch (DOMException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                        Collections.sort(rdTemp, new Comparator<RDEntry>() {
                            @Override
                            public int compare(RDEntry o1, RDEntry o2) {
                                int diff = o1.name.compareTo(o2.name);
                                if (diff == 0) {
                                    return o1.id - o2.id;
                                }
                                return diff;
                            }
                        });
                        System.out.println("exit of " + vertex.label());
                        for (RDEntry rdEntry : rdTemp) {
                            System.out.print("(" + rdEntry.name + ","
                                    + (rdEntry.id == -1 ? "?" : String.valueOf(rdEntry.id)) + ")");
                        }
                        System.out.println();
                    }
                }
            } catch (CoreException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
