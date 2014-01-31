package com.github.uchan_nos.c_helper.analysis;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.cdt.core.dom.ast.IASTBreakStatement;
import org.eclipse.cdt.core.dom.ast.IASTCaseStatement;
import org.eclipse.cdt.core.dom.ast.IASTCompoundStatement;
import org.eclipse.cdt.core.dom.ast.IASTContinueStatement;
import org.eclipse.cdt.core.dom.ast.IASTDefaultStatement;
import org.eclipse.cdt.core.dom.ast.IASTDoStatement;
import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTGotoStatement;
import org.eclipse.cdt.core.dom.ast.IASTIfStatement;
import org.eclipse.cdt.core.dom.ast.IASTLabelStatement;
import org.eclipse.cdt.core.dom.ast.IASTReturnStatement;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IASTSwitchStatement;
import org.eclipse.cdt.core.dom.ast.IASTWhileStatement;

import com.github.uchan_nos.c_helper.util.Util;

public class FunctionCFGCreator {
    private IASTFunctionDefinition functionDefinition;

    private Set<NamedVertex<CFG.Vertex>> labelVertices;
    private Set<NamedVertex<CFG.Vertex>> gotoVertices;
    private Set<CFG.Vertex> returnVertices;

    public FunctionCFGCreator(IASTFunctionDefinition functionDefinition) {
        this.functionDefinition = functionDefinition;
        this.labelVertices = new HashSet<NamedVertex<CFG.Vertex>>();
        this.gotoVertices = new HashSet<NamedVertex<CFG.Vertex>>();
        this.returnVertices = new HashSet<CFG.Vertex>();
    }

    public CFG create() {
        CFG cfg = create(functionDefinition.getBody());
        applyGotoInfo(cfg);
        applyReturnInfo(cfg);
        CFGNormalizer.normalize(cfg);
        return cfg;
    }

    private CFG create(IASTStatement stmt) {
        CFG cfg = null;
        if (stmt instanceof IASTCompoundStatement) {
            cfg = create((IASTCompoundStatement)stmt);
        } else if (stmt instanceof IASTIfStatement) {
            cfg = create((IASTIfStatement)stmt);
        } else if (stmt instanceof IASTWhileStatement) {
            cfg = create((IASTWhileStatement)stmt);
        } else if (stmt instanceof IASTDoStatement) {
            cfg = create((IASTDoStatement)stmt);
        } else if (stmt instanceof IASTForStatement) {
            cfg = create((IASTForStatement)stmt);
        } else if (stmt instanceof IASTSwitchStatement) {
            cfg = create((IASTSwitchStatement)stmt);
        } else if (stmt instanceof IASTCaseStatement) {
            cfg = create((IASTCaseStatement)stmt);
        } else if (stmt instanceof IASTDefaultStatement) {
            cfg = create((IASTDefaultStatement)stmt);
        } else if (stmt instanceof IASTBreakStatement) {
            cfg = create((IASTBreakStatement)stmt);
        } else if (stmt instanceof IASTContinueStatement) {
            cfg = create((IASTContinueStatement)stmt);
        } else if (stmt instanceof IASTReturnStatement) {
            cfg = create((IASTReturnStatement)stmt);
        } else if (stmt instanceof IASTLabelStatement) {
            cfg = create((IASTLabelStatement)stmt);
        } else if (stmt instanceof IASTGotoStatement) {
            cfg = create((IASTGotoStatement)stmt);
        } else {
            CFG.Vertex v = new CFG.Vertex(stmt.getRawSignature() + "\\l");
            v.setASTNode(stmt);
            cfg = new CFG(v, v);
        }

        if (cfg.entryVertex() == null || cfg.exitVertex() == null) {
            System.out.println("entry or exit vertex is null");
        }
        return cfg;
    }

    private CFG create(IASTCompoundStatement stmt) {
        CFG.Vertex entryVertex = new CFG.Vertex();
        CFG.Vertex exitVertex = new CFG.Vertex();
        CFG cfg = new CFG(entryVertex, exitVertex);
        IASTStatement[] sub = stmt.getStatements();

        CFG.Vertex prevVertex = entryVertex;
        for (int i = 0; i < sub.length; ++i) {
            CFG subcfg = create(sub[i]);
            cfg.add(subcfg);
            cfg.addBreakVertex(subcfg.breakVertices());
            cfg.addContinueVertex(subcfg.continueVertices());
            cfg.addCaseVertex(subcfg.caseVertices());
            cfg.connect(prevVertex, subcfg.entryVertex());
            prevVertex = subcfg.exitVertex();
        }
        cfg.connect(prevVertex, exitVertex);

        return cfg;
    }

    private CFG create(IASTIfStatement stmt) {
        CFG.Vertex entryVertex = new CFG.Vertex("if (" + stmt.getConditionExpression().getRawSignature() + ")\\l");
        CFG.Vertex exitVertex = new CFG.Vertex();
        entryVertex.setASTNode(stmt);

        CFG cfg = new CFG(entryVertex, exitVertex);
        CFG thencfg = create(stmt.getThenClause());
        CFG elsecfg = stmt.getElseClause() == null ? null : create(stmt.getElseClause());

        cfg.add(thencfg);
        cfg.addBreakVertex(thencfg.breakVertices());
        cfg.addContinueVertex(thencfg.continueVertices());
        cfg.addCaseVertex(thencfg.caseVertices());
        cfg.connect(entryVertex, thencfg.entryVertex());
        cfg.connect(thencfg.exitVertex(), exitVertex);

        if (elsecfg != null) {
            cfg.add(elsecfg);
            cfg.addBreakVertex(elsecfg.breakVertices());
            cfg.addContinueVertex(elsecfg.continueVertices());
            cfg.addCaseVertex(elsecfg.caseVertices());
            cfg.connect(entryVertex, elsecfg.entryVertex());
            cfg.connect(elsecfg.exitVertex(), exitVertex);
        } else {
            cfg.connect(entryVertex, exitVertex);
        }
        return cfg;
    }

    private CFG create(IASTWhileStatement stmt) {
        CFG.Vertex entryVertex = new CFG.Vertex("while (" + stmt.getCondition().getRawSignature() + ")\\l");
        CFG.Vertex exitVertex = new CFG.Vertex();
        CFG.Vertex bodyendVertex = new CFG.Vertex();
        entryVertex.setASTNode(stmt);

        CFG cfg = new CFG(entryVertex, exitVertex);
        CFG subcfg = create(stmt.getBody());

        cfg.add(subcfg);
        cfg.add(bodyendVertex);
        cfg.addCaseVertex(subcfg.caseVertices());
        cfg.connect(entryVertex, subcfg.entryVertex());
        cfg.connect(subcfg.exitVertex(), bodyendVertex);
        cfg.connect(bodyendVertex, entryVertex);
        cfg.connect(entryVertex, exitVertex);

        applyJumps(cfg, subcfg.continueVertices(), bodyendVertex, subcfg.breakVertices(), exitVertex);

        return cfg;
    }

    private CFG create(IASTDoStatement stmt) {
        CFG.Vertex exitVertex = new CFG.Vertex();
        CFG.Vertex condVertex = new CFG.Vertex("do-while (" + stmt.getCondition().getRawSignature() + ")\\l");
        CFG.Vertex bodyendVertex = new CFG.Vertex();
        condVertex.setASTNode(stmt);

        CFG subcfg = create(stmt.getBody());
        CFG cfg = new CFG(subcfg.entryVertex(), exitVertex);

        cfg.add(subcfg);
        cfg.add(condVertex);
        cfg.add(bodyendVertex);
        cfg.addCaseVertex(subcfg.caseVertices());
        cfg.connect(subcfg.exitVertex(), bodyendVertex);
        cfg.connect(bodyendVertex, condVertex);
        cfg.connect(condVertex, subcfg.entryVertex());
        cfg.connect(condVertex, exitVertex);

        applyJumps(cfg, subcfg.continueVertices(), bodyendVertex, subcfg.breakVertices(), exitVertex);

        return cfg;
    }

    private CFG create(IASTForStatement stmt) {
        final String initializationStatementSignature =
                Util.getRawSignature(stmt.getInitializerStatement());
        final String conditionExpressionSignature =
                Util.getRawSignature(stmt.getConditionExpression());
        final String iterationExpressionSignature =
                Util.getRawSignature(stmt.getIterationExpression());
        CFG.Vertex entryVertex = new CFG.Vertex(
                "for (" + initializationStatementSignature + " " +
                //stmt.getConditionExpression().getRawSignature() + "; " +
                conditionExpressionSignature + "; " +
                //stmt.getIterationExpression().getRawSignature() + ")\\l");
                iterationExpressionSignature + ")\\l");
        CFG.Vertex exitVertex = new CFG.Vertex();
        CFG.Vertex initVertex = new CFG.Vertex(initializationStatementSignature);
        CFG.Vertex condVertex = new CFG.Vertex(conditionExpressionSignature);
        CFG.Vertex iterVertex = new CFG.Vertex(iterationExpressionSignature);
        CFG.Vertex bodyendVertex = new CFG.Vertex();
        entryVertex.setASTNode(stmt);
        initVertex.setASTNode(stmt.getInitializerStatement());
        condVertex.setASTNode(stmt.getConditionExpression());
        iterVertex.setASTNode(stmt.getIterationExpression());

        CFG cfg = new CFG(entryVertex, exitVertex);
        CFG subcfg = create(stmt.getBody());

        cfg.add(subcfg);
        cfg.add(initVertex);
        cfg.add(condVertex);
        cfg.add(iterVertex);
        cfg.add(bodyendVertex);
        cfg.addCaseVertex(subcfg.caseVertices());
        cfg.connect(entryVertex, initVertex);
        cfg.connect(initVertex, condVertex);
        cfg.connect(condVertex, exitVertex);
        cfg.connect(condVertex, subcfg.entryVertex());
        cfg.connect(subcfg.exitVertex(), bodyendVertex);
        cfg.connect(bodyendVertex, iterVertex);
        cfg.connect(iterVertex, condVertex);

        applyJumps(cfg, subcfg.continueVertices(), bodyendVertex, subcfg.breakVertices(), exitVertex);

        return cfg;
    }

    private CFG create(IASTSwitchStatement stmt) {
        CFG.Vertex entryVertex = new CFG.Vertex("switch (" + stmt.getControllerExpression().getRawSignature() + ")\\l");
        CFG.Vertex exitVertex = new CFG.Vertex();
        entryVertex.setASTNode(stmt);

        CFG cfg = new CFG(entryVertex, exitVertex);

        CFG subcfg = create(stmt.getBody());

        cfg.add(subcfg);
        cfg.addContinueVertex(subcfg.continueVertices());
        //cfg.connect(entryVertex, subcfg.entryVertex());
        cfg.connect(subcfg.exitVertex(), exitVertex);

        for (CFG.Vertex caseVertex : subcfg.caseVertices()) {
            cfg.connect(entryVertex, caseVertex);
        }

        applyUnconditionalJumps(cfg, subcfg.breakVertices(), exitVertex);

        return cfg;
    }

    private CFG create(IASTCaseStatement stmt) {
        CFG.Vertex v = new CFG.Vertex("case " + stmt.getExpression().getRawSignature() + ":\\l");
        v.setASTNode(stmt);
        CFG cfg = new CFG(v, v);
        cfg.addCaseVertex(v);
        return cfg;
    }

    private CFG create(IASTDefaultStatement stmt) {
        CFG.Vertex v = new CFG.Vertex("default:\\l");
        v.setASTNode(stmt);
        CFG cfg = new CFG(v, v);
        cfg.addCaseVertex(v);
        return cfg;
    }

    private CFG create(IASTBreakStatement stmt) {
        CFG.Vertex v = new CFG.Vertex(stmt.getRawSignature() + "\\l");
        v.setASTNode(stmt);
        CFG cfg = new CFG(v, v);
        cfg.addBreakVertex(v);
        return cfg;
    }

    private CFG create(IASTContinueStatement stmt) {
        CFG.Vertex v = new CFG.Vertex(stmt.getRawSignature() + "\\l");
        v.setASTNode(stmt);
        CFG cfg = new CFG(v, v);
        cfg.addContinueVertex(v);
        return cfg;
    }

    private CFG create(IASTReturnStatement stmt) {
        CFG.Vertex v = new CFG.Vertex(stmt.getRawSignature() + "\\l");
        v.setASTNode(stmt);
        CFG cfg = new CFG(v, v);
        this.returnVertices.add(v);
        return cfg;
    }

    private CFG create(IASTLabelStatement stmt) {
        CFG.Vertex entryVertex = new CFG.Vertex(
                String.valueOf(stmt.getName().getSimpleID()) + ":\\l");
        entryVertex.setASTNode(stmt);
        this.labelVertices.add(new NamedVertex<CFG.Vertex>(entryVertex, stmt.getName()));
        CFG cfg = create(stmt.getNestedStatement());
        cfg.add(entryVertex);
        cfg.connect(entryVertex, cfg.entryVertex());
        cfg.setEntryVertex(entryVertex);
        return cfg;
    }

    private CFG create(IASTGotoStatement stmt) {
        CFG.Vertex v = new CFG.Vertex(stmt.getRawSignature() + "\\l");
        v.setASTNode(stmt);
        this.gotoVertices.add(new NamedVertex<CFG.Vertex>(v, stmt.getName()));
        CFG cfg = new CFG(v, v);
        return cfg;
    }

    private void applyGotoInfo(CFG cfg) {
        for (NamedVertex<CFG.Vertex> v : gotoVertices) {
            for (CFG.Vertex to : cfg.getConnectedVerticesFrom(v.vertex())) {
                cfg.disconnect(v.vertex(), to);
            }
            for (NamedVertex<CFG.Vertex> l : labelVertices) {
                if (Arrays.equals(v.name().getLookupKey(), l.name().getLookupKey())) {
                    cfg.connect(v.vertex(), l.vertex());
                    break;
                }
            }
        }
    }

    private void applyReturnInfo(CFG cfg) {
        for (CFG.Vertex v : returnVertices) {
            for (CFG.Vertex to : cfg.getConnectedVerticesFrom(v)) {
                cfg.disconnect(v, to);
            }
        }
    }

    private static void applyUnconditionalJumps(CFG cfg, Set<CFG.Vertex> jumpFromVertices, CFG.Vertex jumpToVertex) {
        for (CFG.Vertex from : jumpFromVertices) {
            // まずジャンプ元頂点から出ている辺を削除
            for (CFG.Vertex to : cfg.getConnectedVerticesFrom(from)) {
                cfg.disconnect(from, to);
            }
            // ジャンプ元からジャンプ先への辺を追加
            cfg.connect(from, jumpToVertex);
        }
    }

    private static void applyJumps(CFG cfg,
            Set<CFG.Vertex> continueVertices, CFG.Vertex continueToVertex,
            Set<CFG.Vertex> breakVertices, CFG.Vertex breakToVertex) {
        applyUnconditionalJumps(cfg, continueVertices, continueToVertex);
        applyUnconditionalJumps(cfg, breakVertices, breakToVertex);
    }
}
