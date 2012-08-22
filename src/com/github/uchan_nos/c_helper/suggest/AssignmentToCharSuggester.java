package com.github.uchan_nos.c_helper.suggest;

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.cdt.core.dom.ast.*;
import org.eclipse.cdt.core.dom.ast.IBasicType.Kind;
import org.eclipse.jface.text.BadLocationException;

import com.github.uchan_nos.c_helper.analysis.AssignExpression;
import com.github.uchan_nos.c_helper.analysis.CFG;
import com.github.uchan_nos.c_helper.analysis.DummyAssignExpression;
import com.github.uchan_nos.c_helper.analysis.RD;
import com.github.uchan_nos.c_helper.util.DoNothingASTVisitor;
import com.github.uchan_nos.c_helper.util.Util;

public class AssignmentToCharSuggester extends Suggester {

    @Override
    public Collection<Suggestion> suggest(SuggesterInput input,
            AssumptionManager assumptionManager) {
        ArrayList<Suggestion> suggestions = new ArrayList<Suggestion>();

        for (String proc : input.getProcToCFG().keySet()) {
            RD<CFG.Vertex> rd = input.getProcToRD().get(proc);

            for (AssignExpression ae : rd.getAssigns()) {
                if (ae instanceof DummyAssignExpression) {
                    continue;
                }

                boolean lhsIsArrayElement = false;
                boolean lhsIsChar = false;
                if (ae.getLHS() instanceof IASTExpression) {
                    IASTExpression lhs = (IASTExpression) ae.getLHS();
                    if (Util.isIBasicType(lhs.getExpressionType(), Kind.eChar)) {
                        lhsIsChar = true;
                    }
                    if (lhs instanceof IASTArraySubscriptExpression) {
                        lhsIsArrayElement = true;
                    }
                }

                class RHSVisitor extends DoNothingASTVisitor {
                    public boolean rhsIsString = false;
                    @Override
                    public int visit(IASTExpression expression) {
                        if (expression instanceof IASTLiteralExpression) {
                            IASTLiteralExpression le = (IASTLiteralExpression) expression;
                            if (le.getKind() == IASTLiteralExpression.lk_string_literal) {
                                rhsIsString = true;
                            }
                        } else if (expression.getExpressionType() instanceof IPointerType) {
                            IPointerType type = (IPointerType) expression.getExpressionType();
                            IType typePointTo = type.getType();
                            while (typePointTo instanceof IQualifierType) {
                                typePointTo = ((IQualifierType) typePointTo).getType();
                            }
                            if (typePointTo instanceof IBasicType
                                    && ((IBasicType) typePointTo).getKind() == Kind.eChar) {
                                rhsIsString = true;
                            }
                        }
                        return PROCESS_ABORT;
                    }
                }

                RHSVisitor visitor = new RHSVisitor();
                ae.getRHS().accept(visitor);

                try {
                    if (lhsIsChar && lhsIsArrayElement && visitor.rhsIsString) {
                        suggestions.add(new Suggestion(
                                input.getSource(),
                                ae.getAST(),
                                "char型配列の1つの要素に文字列を格納できません。"
                                + " strcpy を使うことを検討してください。",
                                ""
                                        ));
                    } else if (lhsIsChar && !lhsIsArrayElement && visitor.rhsIsString) {
                        suggestions.add(new Suggestion(
                                input.getSource(),
                                ae.getAST(),
                                "char型変数に文字列を格納できません。"
                                + " char型配列を検討してください。",
                                ""
                                        ));
                    }
                } catch (BadLocationException e) {
                    assert false : "must not be here";
                    e.printStackTrace();
                }
            }
        }

        return suggestions;
    }

}
