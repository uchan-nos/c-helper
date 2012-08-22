package com.github.uchan_nos.c_helper.suggest;

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.cdt.core.dom.ast.*;

import com.github.uchan_nos.c_helper.util.ASTFilter;
import com.github.uchan_nos.c_helper.util.Util;

public class CastSuppressingErrorSuggester extends Suggester {

    @Override
    public Collection<Suggestion> suggest(final SuggesterInput input,
            AssumptionManager assumptionManager) {
        ArrayList<Suggestion> suggestions = new ArrayList<Suggestion>();

        Collection<IASTNode> castExpressions =
            new ASTFilter(input.getAst()).filter(
                new ASTFilter.Predicate() {
                    @Override
                    public boolean pass(IASTNode node) {
                        return node instanceof IASTCastExpression;
                    }
                });

        for (IASTNode node : castExpressions) {
            Collection<Suggestion> s = suggest((IASTCastExpression) node, input);
            if (s != null) {
                suggestions.addAll(s);
            }
        }
        return suggestions;
    }

    private Collection<Suggestion> suggest(
            IASTCastExpression castExpression, final SuggesterInput input) {
        ArrayList<Suggestion> suggestions = new ArrayList<Suggestion>();

        IType castedType = castExpression.getExpressionType();
        IType operandType = castExpression.getOperand().getExpressionType();

        if (castedType instanceof IPointerType
                && operandType instanceof IBasicType) {
            String suggestion;

            IASTFunctionCallExpression fce =
                    getParentFunctionCallExpression(castExpression);
            IASTName name = Util.
            suggestions.add(new Suggestion(input.getSource(), castExpression,
                    "整数型からポインタ型へのキャストは危険です。",
                    ""));
        }
        return null;
    }

    private IASTFunctionCallExpression getParentFunctionCallExpression(
            IASTExpression expression) {
        IASTNode parent = expression;

        while (parent != null && !(parent instanceof IASTFunctionCallExpression)) {
            parent = parent.getParent();
        }

        return (IASTFunctionCallExpression) parent;
    }

}
