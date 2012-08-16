package com.github.uchan_nos.c_helper.suggest;

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.cdt.core.dom.ast.IASTCompositeTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTDeclSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;

import com.github.uchan_nos.c_helper.util.DoNothingASTVisitor;

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
                    if (node.isPartOfTranslationUnitFile()) {
                        node.accept(this);
                    }
                }
                return super.visit(declaration);
            }

            @Override
            public int visit(IASTDeclSpecifier declSpec) {
                if (declSpec instanceof IASTCompositeTypeSpecifier) {
                    // 構造体に対する declaration specifier は閉じカッコ}までを指す
                    try {
                        final IDocument src = input.getSource();
                        final int endingLine = declSpec.getFileLocation().getEndingLineNumber() - 1;

                        boolean foundSemicolon = false;
                        for (int line = 0; line < 2; ++line) {
                            final IRegion lineInfo = src.getLineInformation(endingLine + line);
                            final String lineString = src.get(lineInfo.getOffset(), lineInfo.getLength());
                            if (lineString.indexOf(';') >= 0) {
                                foundSemicolon = true;
                                break;
                            }
                        }

                        final int semicolonOffset = src.get().indexOf(';', src.getLineOffset(endingLine));
                        if (foundSemicolon == false && semicolonOffset >= 0) {
                            suggestions.add(new Suggestion(
                                    input.getFilePath(),
                                    src.getLineOfOffset(semicolonOffset),
                                    0,
                                    semicolonOffset, 1,
                                    "構造体の宣言のセミコロンは、最後の閉じ括弧 } の直後に書くと見やすくなります。"
                                    ));
                        } else if (foundSemicolon == false) {
                            suggestions.add(new Suggestion(
                                    input.getFilePath(),
                                    declSpec.getFileLocation().getEndingLineNumber() - 1,
                                    0, -1, -1,
                                    "構造体の宣言にはセミコロンが必要です。"
                                    ));
                        }
                    } catch (BadLocationException e) {
                        assert false : "must not be here";
                        e.printStackTrace();
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
