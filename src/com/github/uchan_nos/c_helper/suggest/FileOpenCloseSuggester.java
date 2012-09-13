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
import com.github.uchan_nos.c_helper.util.DoNothingASTVisitor;
import com.github.uchan_nos.c_helper.util.Util;

public class FileOpenCloseSuggester extends Suggester {

    private static class InconsistentFopenFinder {
        public Collection<IASTFunctionCallExpression> find(IASTCompoundStatement cs) {
            ArrayList<IASTFunctionCallExpression> inconsistentFopenCalls =
                new ArrayList<IASTFunctionCallExpression>();

            for (IASTStatement s : cs.getStatements()) {
                if (s instanceof IASTCompoundStatement) {
                    InconsistentFopenFinder finder = new InconsistentFopenFinder();
                    finder.find((IASTCompoundStatement) s);
                } else {
                    // sに含まれるすべての fopen 呼び出しを取得
                    ASTFilter filter = new ASTFilter(s);
                    Collection<IASTNode> fopenCalls = filter.filter(new ASTFilter.Predicate() {
                        public boolean pass(IASTNode node) {
                            if (node instanceof IASTFunctionCallExpression) {
                                IASTFunctionCallExpression fce = (IASTFunctionCallExpression) node;
                                return Util.equals(
                                    Util.getName(fce.getFunctionNameExpression()).getSimpleID(),
                                    "fopen");
                            }
                            return false;
                        }
                    });

                    for (IASTNode fopenCall : fopenCalls) {
                        IASTName varName = getAssignVariableName((IASTFunctionCallExpression) fopenCall);
                        System.out.println(varName.getSimpleID());
                    }
                }
            }

            return inconsistentFopenCalls;
        }

        // 関数呼び出し式 e の戻り値を代入している場合、被代入変数名を返す
        private IASTName getAssignVariableName(IASTFunctionCallExpression e) {
            IASTNode parent = e.getParent();
            if (Util.isIASTBinaryExpression(parent, IASTBinaryExpression.op_assign)) {
                IASTBinaryExpression be = (IASTBinaryExpression) parent;
                if (be.getOperand2().contains(e)
                        && be.getOperand1() instanceof IASTIdExpression) {
                    return ((IASTIdExpression) be.getOperand1()).getName();
                }
            } else if (parent instanceof IASTEqualsInitializer) {
                IASTEqualsInitializer init = (IASTEqualsInitializer) parent;
                if (init.getParent() instanceof IASTDeclarator) {
                    return ((IASTDeclarator) init.getParent()).getName();
                }
            }
            return null;
        }
    }

    @Override
    public Collection<Suggestion> suggest(SuggesterInput input, AssumptionManager assumptionManager) {
        ArrayList<Suggestion> suggestions = new ArrayList<Suggestion>();

        /*
        for (String proc : input.getProcToCFG().keySet()) {
            CFG cfg = input.getProcToCFG().get(proc);
            RD<CFG.Vertex> rd = input.getProcToRD().get(proc);

            InconsistentFopenFinder finder = new InconsistentFopenFinder();
            finder.find((IASTCompoundStatement) cfg.entryVertex().getASTNode());
        }
        */

        for (IASTDeclaration declaration : input.getAst().getDeclarations()) {
            if (declaration.isPartOfTranslationUnitFile()
                    && declaration instanceof IASTFunctionDefinition) {

                InconsistentFopenFinder finder = new InconsistentFopenFinder();
                finder.find((IASTCompoundStatement) ((IASTFunctionDefinition) declaration).getBody());
            }
        }

        return suggestions;
    }

}
