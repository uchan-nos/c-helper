package com.github.uchan_nos.c_helper.suggest;

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.cdt.core.dom.ast.*;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;

import com.github.uchan_nos.c_helper.util.DoNothingASTVisitor;

public class SemicolonUnnecessarySuggester extends Suggester {

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
                return PROCESS_ABORT;
            }

            @Override
            public int visit(IASTDeclaration declaration) {
                /* グローバル領域での定義のみを調べる
                 * 例えば、以下の様なケースは無視
                 * void f() {
                 *     void func();
                 *     {
                 *     }
                 * }
                 */
                if (declaration instanceof IASTSimpleDeclaration) {
                    IASTSimpleDeclaration sd = (IASTSimpleDeclaration) declaration;
                    IASTDeclarator[] declarators = sd.getDeclarators();
                    if (declarators.length == 1
                            && declarators[0] instanceof IASTFunctionDeclarator) {
                        try {
                            final IDocument src = input.getSource();
                            final int endingOffset = declaration.getFileLocation().getNodeOffset()
                                    + declaration.getFileLocation().getNodeLength() - 1;
                            assert src.get(endingOffset, src.getLength() - endingOffset).trim().startsWith(";");
                            for (int i = endingOffset + 1; i < src.getLength(); ++i) {
                                final char c = src.getChar(i);
                                if (c == '{') {
                                    suggestions.add(new Suggestion(
                                            input.getFilePath(),
                                            src.getLineOfOffset(endingOffset),
                                            endingOffset - src.getLineOffset(src.getLineOfOffset(endingOffset)),
                                            endingOffset,
                                            1,
                                            "関数定義にはセミコロン ; を付けません。",
                                            ""
                                            ));
                                } else if (!Character.isWhitespace(c)) {
                                    break;
                                }
                            }
                        } catch (BadLocationException e) {
                            assert false : "must not be here";
                            e.printStackTrace();
                        }
                    }
                }
                return PROCESS_ABORT;
            }
        }

        Visitor visitor = new Visitor();
        input.getAst().accept(visitor);
        return suggestions;
    }

}
