package com.github.uchan_nos.c_helper.suggest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.eclipse.cdt.core.dom.ast.*;

import com.github.uchan_nos.c_helper.util.ASTFilter;
import com.github.uchan_nos.c_helper.util.Util;

public class ScanfCallByValueSuggester extends Suggester {

    @Override
    public Collection<Suggestion> suggest(SuggesterInput input,
            AssumptionManager assumptionManager) {
        ArrayList<Suggestion> suggestions = new ArrayList<Suggestion>();

        // scanf系関数の一覧。vargs系は対応しない
        final List<String> scanfLikeFunctionNames = Arrays.asList(
                "scanf", "fscanf", "sscanf");

        // scanf系関数の呼び出し式の一覧
        Collection<IASTNode> scanfLikeFunctionCallExpressions =
            new ASTFilter(input.getAst()).filter(new ASTFilter.Predicate() {
                @Override public boolean pass(IASTNode node) {
                    if (node instanceof IASTFunctionCallExpression) {
                        IASTFunctionCallExpression fce = (IASTFunctionCallExpression) node;
                        IASTExpression nameExpression = fce.getFunctionNameExpression();
                        IASTName name = Util.getName(nameExpression);
                        if (name != null) {
                            String nameString = String.valueOf(name.getSimpleID());
                            if (scanfLikeFunctionNames.contains(nameString)) {
                                return true;
                            }
                        }
                    }
                    return false;
                }});

        for (IASTNode node : scanfLikeFunctionCallExpressions) {
            Suggestion s = checkCallByValue((IASTFunctionCallExpression) node);
            if (s != null) {
                suggestions.add(s);
            }
        }
        return suggestions;
    }

    private Suggestion checkCallByValue(IASTFunctionCallExpression scanfLikeFunctionCallExpression) {
        return null;
    }
}
