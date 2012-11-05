package com.github.uchan_nos.c_helper.suggest;

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.cdt.core.dom.ast.IASTCompositeTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTDeclSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;

import com.github.uchan_nos.c_helper.util.DoNothingASTVisitor;
import com.github.uchan_nos.c_helper.util.Util;

public class SemicolonOblivionSuggester extends Suggester {

    @Override
    public Collection<Suggestion> suggest(final SuggesterInput input,
            AssumptionManager assumptionManager) {
        final ArrayList<Suggestion> suggestions =
                new ArrayList<Suggestion>();

        class Visitor extends DoNothingASTVisitor {
            private boolean parentIsSimpleDeclaration = false;
            private int semicolonOffset = -1;

            @Override
            public int visit(IASTTranslationUnit tu) {
                for (IASTDeclaration decl : tu.getDeclarations()) {
                    if (decl.isPartOfTranslationUnitFile()) {
                        decl.accept(this);
                    }
                }
                return super.visit(tu);
            }

            @Override
            public int visit(IASTDeclaration declaration) {
                if (declaration instanceof IASTSimpleDeclaration) {
                    IASTSimpleDeclaration sd = (IASTSimpleDeclaration) declaration;
                    parentIsSimpleDeclaration = true;

                    try {
                        if (input.getSource().getChar(
                                sd.getFileLocation().getNodeOffset()
                                + sd.getFileLocation().getNodeLength() - 1) == ';') {
                            semicolonOffset = sd.getFileLocation().getNodeOffset()
                                    + sd.getFileLocation().getNodeLength() - 1;
                        }
                    }
                    catch (BadLocationException e) {
                        assert false : "must not be here";
                        e.printStackTrace();
                    }

                    sd.getDeclSpecifier().accept(this);
                    parentIsSimpleDeclaration = false;
                    semicolonOffset = -1;
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

                        if (parentIsSimpleDeclaration && semicolonOffset >= 0 && input.getSource().getLineOfOffset(semicolonOffset) >= endingLine + 2) {
                            suggestions.add(new Suggestion(
                                    input.getFilePath(),
                                    src.getLineOfOffset(semicolonOffset),
                                    Util.calculateColumnNumber(src, semicolonOffset),
                                    semicolonOffset, 1,
                                    "構造体の宣言のセミコロンは、最後の閉じ括弧 } の直後に書くと見やすくなります。",
                                    ""
                                    ));
                        } else if (parentIsSimpleDeclaration && semicolonOffset == -1) {
                            suggestions.add(new Suggestion(
                                    input.getFilePath(),
                                    declSpec.getFileLocation().getEndingLineNumber() - 1,
                                    0, -1, -1,
                                    "構造体の宣言にはセミコロンが必要です。",
                                    ""
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
