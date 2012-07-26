package com.github.uchan_nos.c_helper.analysis;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.cdt.core.dom.ast.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.texteditor.ITextEditor;

import com.github.uchan_nos.c_helper.exceptions.InvalidEditorPartException;
import com.github.uchan_nos.c_helper.util.ASTFilter;
import com.github.uchan_nos.c_helper.util.Util;

public class Analyzer {
    public Analyzer() {
    }

    public void analyze(IEditorPart activeEditorPart)
            throws InvalidEditorPartException {
        if (activeEditorPart instanceof ITextEditor) {
            ITextEditor textEditorPart = (ITextEditor) activeEditorPart;
            IDocument documentToAnalyze = textEditorPart.getDocumentProvider()
                    .getDocument(textEditorPart.getEditorInput());
            analyze(textEditorPart.getTitle(), documentToAnalyze.get());
        } else {
            throw new InvalidEditorPartException(
                    "We need a class implementing ITextEditor");
        }
    }

    public void analyze(String filePath, String source) {
        try {
            IASTTranslationUnit translationUnit =
                    new Parser(filePath, source).parse();
            Map<String, CFG> procToCFG =
                    new CFGCreator(translationUnit).create();
            for (Entry<String, CFG> entry : procToCFG.entrySet()) {
                CFG cfg = entry.getValue();
                RD<CFG.Vertex> rd =
                        new RDAnalyzer(translationUnit, cfg).analyze();

                for (CFG.Vertex v : cfg.getVertices()) {
                    if (v.getASTNodes().size() != 1) {
                        throw new RuntimeException("only one ast node should be contained");
                    }
                    IASTNode ast = v.getASTNodes().get(0);
                    ASTFilter filter = new ASTFilter(ast);

                    Collection<IASTNode> sizeofExpressions =
                            filter.filter(new ASTFilter.Predicate() {
                                @Override
                                public boolean pass(IASTNode node) {
                                    if (node instanceof IASTUnaryExpression) {
                                        IASTUnaryExpression ue = (IASTUnaryExpression)node;
                                        if (ue.getOperator() == IASTUnaryExpression.op_sizeof) {
                                            return true;
                                        }
                                    }
                                    return false;
                                }
                            });

                    for (IASTNode node : sizeofExpressions) {
                        IASTUnaryExpression ue = (IASTUnaryExpression)node;
                        while (ue.getOperand() instanceof IASTUnaryExpression
                                && ((IASTUnaryExpression)ue.getOperand()).getOperator()
                                    == IASTUnaryExpression.op_bracketedPrimary) {
                            ue = (IASTUnaryExpression)ue.getOperand();
                        }
                        if (ue.getOperand() instanceof IASTIdExpression) {
                            IASTIdExpression id = (IASTIdExpression)ue.getOperand();
                            Set<AssignExpression> assigns =
                                    Util.getAssigns(
                                            rd.getAssigns(),
                                            rd.getEntrySets().get(v),
                                            id.getName().toString());
                            for (AssignExpression assign : assigns) {
                                IASTNode rhs = assign.getRHS();
                                while (rhs instanceof IASTCastExpression) {
                                    rhs = ((IASTCastExpression)rhs).getOperand();
                                }
                                if (rhs instanceof IASTFunctionCallExpression) {
                                    IASTFunctionCallExpression fce = (IASTFunctionCallExpression)rhs;
                                    if (fce.getFunctionNameExpression() instanceof IASTIdExpression) {
                                        String funcname = ((IASTIdExpression)fce.getFunctionNameExpression()).getName().toString();
                                        if (funcname.equals("malloc")) {
                                            System.out.println("variable `" + id.getName().toString()
                                                    + "' was assigned a heap memory (at line "
                                                    + rhs.getFileLocation().getStartingLineNumber() + ").");
                                            System.out.println("But " + node.getRawSignature()
                                                    + " (at line "
                                                    + node.getFileLocation().getStartingLineNumber()
                                                    + ") always returns the same value, not "
                                                    + fce.getArguments()[0].getRawSignature() + ".");
                                            System.out.println("Is it OK?");
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (CoreException e) {
            e.printStackTrace();
        }
    }

}
