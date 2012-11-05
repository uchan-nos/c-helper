package com.github.uchan_nos.c_helper.suggest;

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.cdt.core.dom.ast.*;
import org.eclipse.jface.text.BadLocationException;

import com.github.uchan_nos.c_helper.analysis.CFG;
import com.github.uchan_nos.c_helper.util.ASTFilter;
import com.github.uchan_nos.c_helper.util.DoNothingASTVisitor;
import com.github.uchan_nos.c_helper.util.Util;

public class ReturnOblivionSuggester extends Suggester {

    @Override
    public Collection<Suggestion> suggest(final SuggesterInput input,
            AssumptionManager assumptionManager) {
        final ArrayList<Suggestion> suggestions = new ArrayList<Suggestion>();

        class Visitor extends DoNothingASTVisitor {
            private void checkReturn(IASTFunctionDefinition fd) {
                final IASTName functionName = fd.getDeclarator().getName();
                final CFG cfg = input.getProcToCFG().get(
                        String.valueOf(functionName.getSimpleID()));

                final boolean typeIsVoid = fd.getDeclSpecifier() instanceof IASTSimpleDeclSpecifier
                        && ((IASTSimpleDeclSpecifier) fd.getDeclSpecifier()).getType()
                            == IASTSimpleDeclSpecifier.t_void;
                if (typeIsVoid) {
                    return;
                }

                // 戻り値が void 以外の時に処理する

                // 関数の出口

                try {
                    // 関数の出口に接続されている頂点
                    // 戻り値のある関数なら、すべて戻り値付きreturn文であるべき
                    if (cfg.entryVertex().getASTNode() == null // そもそも関数内に1つも文がない => return文もない
                            || cfg.exitVertex().getASTNode() != null // exitVertexに到達する文がある => return文を通らないパスがある
                            || cfg.getConnectedVerticesTo(cfg.exitVertex()).size() != 0) { // exitVertexに到達する文が複数ある => return文を通らないパスがある
                        suggestions.add(new Suggestion(
                                input.getFilePath(),
                                fd.getFileLocation().getStartingLineNumber() - 1,
                                Util.calculateColumnNumber(input.getSource(), fd.getFileLocation().getNodeOffset()),
                                fd.getFileLocation().getNodeOffset(),
                                fd.getFileLocation().getNodeLength(),
                                "return文がありません。",
                                ""
                                ));
                    } else {

                        Collection<IASTNode> returnStatementsWithNoArgument =
                                new ASTFilter(fd).filter(new ASTFilter.Predicate() {
                            @Override
                            public boolean pass(IASTNode node) {
                                return node instanceof IASTReturnStatement
                                        && ((IASTReturnStatement) node).getReturnArgument() == null;
                            }
                        });
                        for (IASTNode ret : returnStatementsWithNoArgument) {
                            suggestions.add(new Suggestion(
                                    input.getFilePath(),
                                    ret.getFileLocation().getStartingLineNumber() - 1,
                                    Util.calculateColumnNumber(input.getSource(), ret.getFileLocation().getNodeOffset()),
                                    ret.getFileLocation().getNodeOffset(),
                                    ret.getFileLocation().getNodeLength(),
                                    "戻り値がある関数では、return文に引数を指定してください。",
                                    ""
                                    ));
                        }
                    }
                } catch (BadLocationException e) {
                    assert false : "must not be here";
                    e.printStackTrace();
                }
            }

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
                if (declaration instanceof IASTFunctionDefinition) {
                    checkReturn((IASTFunctionDefinition) declaration);
                }
                return PROCESS_ABORT;
            }
        }

        input.getAst().accept(new Visitor());
        return suggestions;
    }

}
