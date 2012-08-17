package com.github.uchan_nos.c_helper.suggest;

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.cdt.core.dom.ast.IASTCompositeTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTDeclSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;

import com.github.uchan_nos.c_helper.util.DoNothingASTVisitor;
import com.github.uchan_nos.c_helper.util.Util;
import com.github.uchan_nos.c_helper.util.Util.CharPredicate;

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
                    if (decl.isPartOfTranslationUnitFile()) {
                        decl.accept(this);
                    }
                }
                return super.visit(tu);
            }

            @Override
            public int visit(IASTDeclaration declaration) {
                if (!(declaration instanceof IASTFunctionDefinition)) {
                    for (IASTNode node : declaration.getChildren()) {
                        node.accept(this);
                    }
                }
                return super.visit(declaration);
            }

            @Override
            public int visit(IASTDeclSpecifier declSpec) {
                class WhitespaceCharPredicate implements CharPredicate {
                    private final char key;
                    public WhitespaceCharPredicate(char key) {
                        this.key = key;
                    }
                    @Override
                    public boolean evaluate(char c) {
                        return !Character.isWhitespace(c) || key == c;
                    }
                }
                final CharPredicate semicolonPredicate = new WhitespaceCharPredicate(';');

                if (declSpec instanceof IASTCompositeTypeSpecifier) {
                    // 構造体に対する declaration specifier は閉じカッコ}までを指す
                    try {
                        final IDocument src = input.getSource();
                        final int endingLine = declSpec.getFileLocation().getEndingLineNumber() - 1;
                        final int endingOffset = declSpec.getFileLocation().getNodeOffset() + declSpec.getFileLocation().getNodeLength();

                        boolean foundSemicolon = false;
                        for (int line = 0; line < 2; ++line) {
                            final IRegion lineInfo = src.getLineInformation(endingLine + line);
                            final String lineString =
                                    line == 0 ? src.get(endingOffset, lineInfo.getOffset() + lineInfo.getLength() - endingOffset)
                                            : src.get(lineInfo.getOffset(), lineInfo.getLength());
                            final int pos = Util.indexOf(lineString, semicolonPredicate, 0, input.getAst().getComments(), -lineInfo.getOffset());
                            if (pos >= 0 && lineString.charAt(pos) == ';') {
                                foundSemicolon = true;
                                break;
                            }
                        }

                        int semicolonOffset = Util.indexOf(src.get(), semicolonPredicate, endingOffset, input.getAst().getComments(), 0);
                        if (src.get().charAt(semicolonOffset) != ';') {
                            semicolonOffset = -1;
                        }
                        //final int semicolonOffset = src.get().indexOf(';', src.getLineOffset(endingLine));
                        if (foundSemicolon == false && semicolonOffset >= 0) {
                            suggestions.add(new Suggestion(
                                    input.getFilePath(),
                                    src.getLineOfOffset(semicolonOffset),
                                    Util.calculateColumnNumbeer(src, semicolonOffset),
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
