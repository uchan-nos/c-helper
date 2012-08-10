package com.github.uchan_nos.c_helper.suggest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import org.eclipse.cdt.core.dom.ast.*;

import com.github.uchan_nos.c_helper.analysis.AnalysisEnvironment;
import com.github.uchan_nos.c_helper.analysis.AssignExpression;
import com.github.uchan_nos.c_helper.analysis.CFG;
import com.github.uchan_nos.c_helper.analysis.RD;
import com.github.uchan_nos.c_helper.util.ASTFilter;
import com.github.uchan_nos.c_helper.util.Util;

public class SizeofSuggester extends Suggester {

    @Override
    public Collection<Suggestion> suggest(SuggesterInput input) {
        ArrayList<Suggestion> suggestions = new ArrayList<Suggestion>();

        for (String proc : input.getProcToCFG().keySet()) {
            CFG cfg = input.getProcToCFG().get(proc);
            RD<CFG.Vertex> rd = input.getProcToRD().get(proc);

            for (CFG.Vertex v : cfg.getVertices()) {
                assert v.getASTNode() != null : "a vertex should include only one ast node";

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
                                        id.getName().toString());

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
                            message.append(AnalysisEnvironment.POINTER_BITS / AnalysisEnvironment.CHAR_BITS);
                            message.append(" を返します。それは本当に意図したことですか？");
                            Suggestion suggestion = new Suggestion(
                                    input.getFilePath(),
                                    node.getFileLocation().getStartingLineNumber(),
                                    Util.calculateColumnNumber(input.getSource(), node.getFileLocation().getNodeOffset()),
                                    node.getFileLocation().getNodeOffset(),
                                    node.getFileLocation().getNodeLength(),
                                    message.toString());
                            suggestions.add(suggestion);
                        }
                    }
                }
            }
        }

        return suggestions;
    }

}
