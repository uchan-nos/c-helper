package com.github.uchan_nos.c_helper.suggest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import org.eclipse.cdt.core.dom.ast.*;
import org.eclipse.jface.text.BadLocationException;

import com.github.uchan_nos.c_helper.analysis.AssignExpression;
import com.github.uchan_nos.c_helper.analysis.CFG;
import com.github.uchan_nos.c_helper.analysis.RD;
import com.github.uchan_nos.c_helper.util.ASTFilter;
import com.github.uchan_nos.c_helper.util.Util;

public class SizeofSuggester extends Suggester {

    @Override
    public Collection<Suggestion> suggest(SuggesterInput input, AssumptionManager assumptionManager) {
        ArrayList<Suggestion> suggestions = new ArrayList<Suggestion>();

        for (String proc : input.getProcToCFG().keySet()) {
            CFG cfg = input.getProcToCFG().get(proc);
            RD<CFG.Vertex> rd = input.getProcToRD().get(proc);

            for (CFG.Vertex v : cfg.getVertices()) {
                if (v.getASTNode() == null) {
                    continue;
                }

                IASTNode ast = v.getASTNode();
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
                                        id.getName());

                        ArrayList<String> beginnerExpectingValues =
                                new ArrayList<String>();
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
                                        beginnerExpectingValues.add(
                                                fce.getArguments()[0].getRawSignature());
                                    }
                                }
                            }
                        }

                        if (beginnerExpectingValues.size() > 0) {
                            StringBuilder message = new StringBuilder();
                            message.append(node.getRawSignature());
                            message.append(" は ");
                            if (beginnerExpectingValues.size() == 1) {
                                message.append(beginnerExpectingValues.get(0));
                            } else if (beginnerExpectingValues.size() >= 2) {
                                message.append(beginnerExpectingValues.get(0));
                                message.append(" や ");
                                message.append(beginnerExpectingValues.get(1));
                            }
                            message.append(" ではなく ");
                            message.append(input.getAnalysisEnvironment().POINTER_BYTE);
                            message.append(" を返します (仮定");
                            message.append(assumptionManager.ref(Assumption.POINTER_BYTE));
                            message.append(")。それは本当に意図したことですか？");
                            try {
                                suggestions.add(new Suggestion(
                                        input.getFilePath(),
                                        node.getFileLocation().getStartingLineNumber() - 1,
                                        /*
                                        Util.calculateColumnNumber(input.getSource(), node.getFileLocation().getNodeOffset(),
                                                input.getAnalysisEnvironment().LINE_DELIMITER),
                                                */
                                        input.getSource().getLineInformationOfOffset(node.getFileLocation().getNodeOffset()).getOffset(),
                                        node.getFileLocation().getNodeOffset(),
                                        node.getFileLocation().getNodeLength(),
                                        message.toString(),
                                        ""));
                            } catch (BadLocationException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
        }

        return suggestions;
    }

}
