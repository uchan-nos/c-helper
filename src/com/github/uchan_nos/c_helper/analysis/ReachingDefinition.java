package com.github.uchan_nos.c_helper.analysis;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.NoSuchElementException;

import org.eclipse.cdt.core.dom.ast.*;

public class ReachingDefinition {
    private static class GenKill {
        final public BitSet gen;
        final public BitSet kill;
        public GenKill(BitSet gen, BitSet kill) {
            this.gen = gen;
            this.kill = kill;
        }
    }

    private CFG cfg;
    private ArrayList<AssignExpression> assignList;
    public ReachingDefinition(CFG cfg, ArrayList<AssignExpression> assignList) {
        this.cfg = cfg;
        this.assignList = assignList;
    }

    public void analyze() {
        final int numVertex = cfg.getVertices().size();
        ArrayList<GenKill> genkill = new ArrayList<ReachingDefinition.GenKill>(numVertex);
        ArrayList<CFG.Vertex> vertices = new ArrayList<CFG.Vertex>(CFGPrinter.sort(cfg.getVertices()));
        for (int i = 0; i < numVertex; ++i) {
            genkill.add(createGenKill(vertices.get(i)));
        }

        ArrayList<BitSet> entry = new ArrayList<BitSet>(numVertex);
        ArrayList<BitSet> exit = new ArrayList<BitSet>(numVertex);
        ArrayList<BitSet> exitPrev = null;
        for (int i = 0; i < numVertex; ++i) {
            entry.add(new BitSet(assignList.size()));
            exit.add(new BitSet(assignList.size()));
        }

        boolean modified = true;
        while (modified) {
            for (int i = 0; i < numVertex; ++i) {
                for (CFG.Vertex leading : cfg.getConnectedVerticesTo(vertices.get(i))) {
                    int leadingIndex = vertices.indexOf(leading);
                    entry.get(i).or(exit.get(leadingIndex));
                }
                exit.get(i).clear();
                exit.get(i).or(entry.get(i));
                exit.get(i).andNot(genkill.get(i).kill);
                exit.get(i).or(genkill.get(i).gen);
            }
            if (exit.equals(exitPrev)) {
                modified = false;
            } else {
                exitPrev = (ArrayList<BitSet>)exit.clone();
            }
        }

        for (int i = 0; i < numVertex; ++i) {
            System.out.println(vertices.get(i).label());
            System.out.print("  entry ");
            for (int b = 0; b < assignList.size(); ++b) {
                if (entry.get(i).get(b)) {
                    System.out.print("(" + assignList.get(b).getLHS().getRawSignature()
                            + "," + b + ")");
                }
            }
            System.out.println();
            System.out.print("  exit  ");
            for (int b = 0; b < assignList.size(); ++b) {
                if (exit.get(i).get(b)) {
                    System.out.print("(" + assignList.get(b).getLHS().getRawSignature()
                            + "," + b + ")");
                }
            }
            System.out.println();
        }
    }

    private GenKill createGenKill(CFG.Vertex v) {
        BitSet kill = new BitSet(assignList.size());
        BitSet gen = new BitSet(assignList.size());
        for (int i = v.getASTNodes().size() - 1; i >= 0; --i) {
            GenKill sub = createGenKill(v.getASTNodes().get(i));
            if (sub != null) {
                sub.gen.andNot(kill);
                gen.or(sub.gen);
                kill.or(sub.kill);
            }
        }
        return new GenKill(gen, kill);
    }

    private GenKill createGenKill(IASTNode ast) {
        if (ast instanceof IASTExpressionStatement) {
            return createGenKill((IASTExpressionStatement)ast);
        } else if (ast instanceof IASTDeclarationStatement) {
            return createGenKill((IASTDeclarationStatement)ast);
        }
        return null;
    }

    private GenKill createGenKill(IASTExpressionStatement ast) {
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
        if (ast.getDeclaration() instanceof IASTSimpleDeclaration) {
            IASTDeclarator[] declarators =
                    ((IASTSimpleDeclaration)ast.getDeclaration()).getDeclarators();
            BitSet gen = new BitSet(assignList.size());
            for (int i = 0; i < declarators.length; ++i) {
                AssignExpression assign = getAssignExpressionOfNode(declarators[i]);
                if (assign != null) {
                    gen.set(assign.getId());
                }
            }
            return new GenKill(gen, new BitSet());
        }
        return null;
    }

    private AssignExpression getAssignExpressionOfNode(IASTNode ast) {
        for (int i = 0; i < assignList.size(); ++i) {
            if (assignList.get(i).getAST() == ast) {
                return assignList.get(i);
            }
        }
        return null;
    }

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
}
