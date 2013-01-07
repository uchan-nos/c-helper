package com.github.uchan_nos.c_helper.suggest;

import java.util.ArrayList;
import java.util.Collection;

import java.util.logging.Logger;

import org.eclipse.cdt.core.dom.ast.*;
import org.eclipse.cdt.core.dom.ast.DOMException;

import org.eclipse.jface.text.BadLocationException;

import com.github.uchan_nos.c_helper.Activator;

import com.github.uchan_nos.c_helper.resource.StringResource;

import com.github.uchan_nos.c_helper.util.ASTFilter;
import com.github.uchan_nos.c_helper.util.DoNothingASTVisitor;

public class HidingIdentifierSuggester extends Suggester {
    private static final Logger logger = Activator.getLogger();

    /**
     * サジェスチョンをリストに追加する機能を提供する.
     * Suggestionのコンストラクタに渡すソースコードを固定するためのクラス.
     */
    private static class SuggestionAppender {
        private Collection<Suggestion> suggestions;
        private SuggesterInput input;

        public SuggestionAppender(Collection<Suggestion> suggestions, SuggesterInput input) {
            this.suggestions = suggestions;
            this.input = input;
        }

        public void append(IASTNode node, String message, String suggestion) {
            try {
                this.suggestions.add(new Suggestion(input.getSource(), node, message, suggestion));
            } catch (BadLocationException e) {
                logger.info(e.toString());
                e.printStackTrace();
            }
        }
    }

    private SuggestionAppender suggestionAppender = null;

    // デクラレータを探す述語
    private final ASTFilter.Predicate declaratorPredicate = new ASTFilter.Predicate() {
        @Override public boolean pass(IASTNode node) {
            return node instanceof IASTDeclarator;
        }
    };

    private static class Definition {
        public final IScope scope;
        public final IASTDeclarator declarator;

        public Definition(IScope scope, IASTDeclarator declarator) {
            this.scope = scope;
            this.declarator = declarator;
        }
    }

    // 変数定義を抽出するビジタ
    private static class DefinitionVisitor extends DoNothingASTVisitor {
        private Collection<Definition> definitions = new ArrayList<Definition>();
        public Collection<Definition> getDefinitions() {
            return definitions;
        }

        private IScope tuGlobalScope = null;
        @Override public int visit(IASTTranslationUnit tu) {
            for (IASTDeclaration declaration : tu.getDeclarations()) {
                declaration.accept(this);
            }
            tuGlobalScope = tu.getScope();
            return PROCESS_ABORT;
        }

        @Override public int visit(IASTDeclaration declaration) {
            if (declaration instanceof IASTFunctionDefinition) {
                IASTFunctionDefinition fd = (IASTFunctionDefinition) declaration;
                DefinitionInCSVisitor visitor = new DefinitionInCSVisitor();
                fd.getBody().accept(visitor);
                definitions.addAll(visitor.getDefinitions());
            } else if (declaration instanceof IASTSimpleDeclaration) {
                IASTSimpleDeclaration sd = (IASTSimpleDeclaration) declaration;
                addAllDeclarators(sd, tuGlobalScope, definitions);
            }
            return PROCESS_ABORT;
        }

        public static void addAllDeclarators(IASTSimpleDeclaration sd,
                IScope currentScope, Collection<Definition> definitions) {
            for (IASTDeclarator declarator : sd.getDeclarators()) {
                definitions.add(new Definition(currentScope, declarator));
            }
        }
    }

    // 複文において変数定義を抽出するビジタ
    private static class DefinitionInCSVisitor extends DoNothingASTVisitor {
        private Collection<Definition> definitions = new ArrayList<Definition>();
        public Collection<Definition> getDefinitions() {
            return definitions;
        }

        private IScope currentScope = null;
        @Override public int visit(IASTStatement statement) {
            if (statement instanceof IASTCompoundStatement) {
                IASTCompoundStatement cs = (IASTCompoundStatement) statement;
                currentScope = cs.getScope();
                for (IASTStatement s : cs.getStatements()) {
                    s.accept(this);
                }
            } else if (statement instanceof IASTDeclarationStatement) {
                IASTDeclarationStatement ds = (IASTDeclarationStatement) statement;
                ds.getDeclaration().accept(this);
            }
            return PROCESS_ABORT;
        }

        @Override public int leave(IASTStatement statement) {
            if (statement instanceof IASTCompoundStatement) {
                IASTCompoundStatement cs = (IASTCompoundStatement) statement;
                try {
                    currentScope = cs.getScope().getParent();
                } catch (DOMException e) {
                    logger.info("Cannot get parent scope");
                    currentScope = null;
                }
            }
            return PROCESS_ABORT;
        }

        @Override public int visit(IASTDeclaration declaration) {
            if (declaration instanceof IASTSimpleDeclaration) {
                IASTSimpleDeclaration sd = (IASTSimpleDeclaration) declaration;
                DefinitionVisitor.addAllDeclarators(sd, currentScope, definitions);
            }
            return PROCESS_ABORT;
        }
    }

    @Override
    public Collection<Suggestion> suggest(final SuggesterInput input,
            AssumptionManager assumptionManager) {
        final ArrayList<Suggestion> suggestions = new ArrayList<Suggestion>();

        suggestionAppender = new SuggestionAppender(suggestions, input);

        System.out.println("visiting");

        DefinitionVisitor visitor = new DefinitionVisitor();
        input.getAst().accept(visitor);

        System.out.println("definitions are:");
        for (Definition d : visitor.getDefinitions()) {
            System.out.println("  " + String.valueOf(d.declarator.getName().getSimpleID())
                    + " at " + d.declarator.getFileLocation().getStartingLineNumber());
        }

        /*
        // 複文を抽出
        Collection<IASTNode> compoundStatements =
            new ASTFilter(input.getAst()).filter(new ASTFilter.Predicate() {
                @Override public boolean pass(IASTNode node) {
                    return node instanceof IASTCompoundStatement;
                }});

        // すべての複文を調べる
        for (IASTNode node : compoundStatements) {
            IASTCompoundStatement cs = (IASTCompoundStatement) node;
            check(cs);
        }
        */

        return suggestions;
    }

    /**
     * 指定された複文直下の宣言について、名前の衝突をチェックする.
     */
    private void check(IASTCompoundStatement stmt) {
        try {
            Collection<IASTNode> declarators =
                new ASTFilter(stmt).filter(declaratorPredicate);
            IScope parentScope = stmt.getScope().getParent();

            for (IASTNode declarator : declarators) {
                IASTDeclarator decl = (IASTDeclarator) declarator;
                IASTName id = decl.getName();
                String idString = String.valueOf(id.getSimpleID());
                IBinding[] hiddenBindings = getHiddenBindings(parentScope, id);
                if (hiddenBindings != null) {
                    suggestionAppender.append(decl,
                            StringResource.get("識別子%sが重複している",
                                idString),
                            null);
                }
            }
        } catch (DOMException e) {
            logger.info(e.toString());
            e.printStackTrace();
        }
    }

    /**
     * 指定された名前により隠されているバインディングを返す.
     * @return nameがparentScope以上のスコープに存在していれば、そのバインディング
     */
    private IBinding[] getHiddenBindings(IScope parentScope, IASTName name) {
        String nameString = String.valueOf(name.getLookupKey());

        try {
            while (parentScope != null) {
                IBinding[] foundBindings = parentScope.find(nameString);
                if (foundBindings != null && foundBindings.length > 0) {
                    return foundBindings;
                }
                parentScope = parentScope.getParent();
            }
        } catch (DOMException e) {
            logger.info(e.toString());
            e.printStackTrace();
        }

        return null;
    }
}
