package com.github.uchan_nos.c_helper.suggest;

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.cdt.core.dom.ast.IASTCompositeTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTCompoundStatement;
import org.eclipse.cdt.core.dom.ast.IASTDeclSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;

import com.github.uchan_nos.c_helper.util.DoNothingASTVisitor;
import com.github.uchan_nos.c_helper.util.Util;

public class SemicolonOblivionSuggester extends Suggester {

    @Override
    public Collection<Suggestion> suggest(final SuggesterInput input,
            AssumptionManager assumptionManager) {
        final ArrayList<Suggestion> suggestions =
                new ArrayList<Suggestion>();

        class Visitor extends DoNothingASTVisitor {
            @Override
            public int visit(IASTTranslationUnit tu) {
                for (IASTDeclaration decl : tu.getDeclarations()) {
                    decl.accept(this);
                }
                return super.visit(tu);
            }

            @Override
            public int visit(IASTDeclaration declaration) {
                for (IASTNode node : declaration.getChildren()) {
                    node.accept(this);
                }
                return super.visit(declaration);
            }

            @Override
            public int visit(IASTDeclSpecifier declSpec) {
                if (declSpec instanceof IASTCompositeTypeSpecifier) {
                    int line = 0, i = 0;
                    final int endingOffset = declSpec.getFileLocation().getNodeOffset() + declSpec.getFileLocation().getNodeLength() - 1;
                    final String lineDelimiter = input.getAnalysisEnvironment().LINE_DELIMITER;
                    while (line < 2) {
                        if (input.getSource().charAt(endingOffset + i) == ';') {
                            break;
                        } else if (input.getSource().substring(
                                endingOffset + i,
                                endingOffset + i + lineDelimiter.length())
                                .equals(lineDelimiter)) {
                            ++line;
                            i += lineDelimiter.length();
                        } else {
                            ++i;
                        }
                    }
                    if (line == 2) {
                        suggestions.add(new Suggestion(
                                input.getFilePath(),
                                declSpec.getFileLocation().getEndingLineNumber(),
                                0, -1, -1,
                                "構造体の宣言にはセミコロンが必要です。"
                                ));
                    }
                }
                return super.visit(declSpec);
            }
        }

        Visitor visitor = new Visitor();
        input.getAst().accept(visitor);
        return suggestions;
    }

}
