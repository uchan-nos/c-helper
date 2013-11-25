package com.github.uchan_nos.c_helper.suggest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

import java.util.logging.Logger;

import java.util.Map;

import org.eclipse.cdt.core.dom.ast.*;

import org.eclipse.jface.text.BadLocationException;

import com.github.uchan_nos.c_helper.Activator;

import com.github.uchan_nos.c_helper.analysis.MyFileContentProvider;
import com.github.uchan_nos.c_helper.resource.StringResource;
import com.github.uchan_nos.c_helper.util.Util;

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
                if (node.isPartOfTranslationUnitFile()) {
                    this.suggestions.add(new Suggestion(input.getSource(), node, message, suggestion));
                } else {
                    this.suggestions.add(new Suggestion(node.getContainingFilename(),
                            node.getFileLocation().getStartingLineNumber() - 1, -1,
                            node.getFileLocation().getNodeOffset(), node.getFileLocation().getNodeLength(),
                            message, suggestion));
                }
            } catch (BadLocationException e) {
                logger.info(e.toString());
                e.printStackTrace();
            }
        }
    }

    private SuggestionAppender suggestionAppender = null;

    // プログラムの構造に従った宣言の一覧
    private static class DeclarationTree {
        // この木が対応するプログラムのスコープ
        private IScope scope;

        // この木のルートにある宣言の一覧
        private Map<String, Collection<IASTDeclarator>> definitions = new HashMap<String, Collection<IASTDeclarator>>();

        // 親木
        private DeclarationTree supertree;

        // 部分木（直下にあるサブスコープに対応）
        private Collection<DeclarationTree> subtrees = new ArrayList<DeclarationTree>();

        /**
         * 変数定義木を生成する.
         * @param scope 木の根に相当するスコープ
         */
        public DeclarationTree(IScope scope, DeclarationTree supertree) {
            this.scope = scope;
            this.supertree = supertree;
        }

        public IScope getScope() {
            return scope;
        }

        public DeclarationTree getSuperTree() {
            return supertree;
        }

        // このスコープに含まれる宣言の一覧を返す.
        public Map<String, Collection<IASTDeclarator>> getDefinitions() {
            return definitions;
        }
        public void addDefinition(IASTDeclarator declarator) {
            String simpleID = String.valueOf(declarator.getName().getSimpleID());

            if (!definitions.containsKey(simpleID)) {
                definitions.put(simpleID, new ArrayList<IASTDeclarator>());
            }

            definitions.get(simpleID).add(declarator);
        }

        // 部分木を返す.
        public Collection<DeclarationTree> getSubTrees() {
            return subtrees;
        }
        public void addSubTree(DeclarationTree subtree) {
            subtrees.add(subtree);
        }
    }

    private static class DefinitionExtractor {
        public static DeclarationTree extract(IASTTranslationUnit tu) {
            // プログラムのルートに対応する木を生成（親を持たない）
            DeclarationTree tree = new DeclarationTree(tu.getScope(), null);

            for (IASTDeclaration declaration : tu.getDeclarations()) {
                if (declaration instanceof IASTFunctionDefinition) {
                    // 関数定義のボディーに含まれる変数定義の木構造を取得
                    IASTFunctionDefinition fd = (IASTFunctionDefinition) declaration;
                    DeclarationTree subtree = extract((IASTCompoundStatement) fd.getBody(), tree);

                    // 取得した木構造を部分木として追加
                    tree.addSubTree(subtree);
                } else if (declaration instanceof IASTSimpleDeclaration) {
                    // グローバル領域の宣言はそのままtreeに追加
                    addAllDefinitions(tree, (IASTSimpleDeclaration) declaration);
                }
            }

            return tree;
        }

        // 複文中の変数定義の木構造を抜き出して返す
        private static DeclarationTree extract(IASTCompoundStatement cs, DeclarationTree supertree) {
            DeclarationTree tree = new DeclarationTree(cs.getScope(), supertree);

            for (IASTStatement statement : cs.getStatements()) {
                if (statement instanceof IASTDeclarationStatement) {
                    // 変数定義を見つけた
                    IASTDeclarationStatement ds = (IASTDeclarationStatement) statement;
                    IASTDeclaration declaration = ds.getDeclaration();
                    if (declaration instanceof IASTSimpleDeclaration) {
                        addAllDefinitions(tree, (IASTSimpleDeclaration) declaration);
                    }
                } else if (statement instanceof IASTCompoundStatement) {
                    // 複文を見つけた
                    DeclarationTree subtree = extract((IASTCompoundStatement) statement, tree);

                    // 複文の変数定義を部分木として追加
                    tree.addSubTree(subtree);
                }
            }

            return tree;
        }

        // sdに含まれるすべての定義をtreeに追加する
        private static void addAllDefinitions(DeclarationTree tree, IASTSimpleDeclaration sd) {
            final int storageClass = sd.getDeclSpecifier().getStorageClass();
            if (!Util.contains(storageClass, IASTDeclSpecifier.sc_extern, IASTDeclSpecifier.sc_typedef)) {
                for (IASTDeclarator declarator : sd.getDeclarators()) {
                    tree.addDefinition(declarator);
                }
            }
        }
    }

    private static class DuplicatedIdentifierFinder {
        public static Collection<IASTDeclarator> find(DeclarationTree tree) {
            Collection<IASTDeclarator> duplicatedDefinitions = new HashSet<IASTDeclarator>();
            find(tree, duplicatedDefinitions);
            return duplicatedDefinitions;
        }

        private static void find(DeclarationTree tree, Collection<IASTDeclarator> duplicatedDefinitions) {
            // この階層にある識別子のうち、重複しているものを探す
            for (Map.Entry<String, Collection<IASTDeclarator>> e : tree.getDefinitions().entrySet()) {
                // key of e = simple id, value of e = declarators

                if (e.getValue().size() >= 2) {
                    // 識別子が重複している
                    duplicatedDefinitions.addAll(e.getValue());
                } else if (e.getValue().size() == 1) {
                    // 親スコープで同じ識別子があるかをチェックする
                    DeclarationTree supertree = tree.getSuperTree();
                    while (supertree != null) {
                        Collection<IASTDeclarator> decls =
                                supertree.getDefinitions().get(e.getKey());
                        if (decls != null && decls.size() >= 1) {
                            // supertreeにe.getKey()と等しい識別子を持つ宣言があった
                            duplicatedDefinitions.addAll(e.getValue());
                            duplicatedDefinitions.addAll(decls);
                        }
                        supertree = supertree.getSuperTree();
                    }
                }
            }

            // 下の階層を再帰的に調べる
            for (DeclarationTree subtree : tree.getSubTrees()) {
                find(subtree, duplicatedDefinitions);
            }
        }
    }

    @Override
    public Collection<Suggestion> suggest(final SuggesterInput input,
            AssumptionManager assumptionManager) {
        final ArrayList<Suggestion> suggestions = new ArrayList<Suggestion>();

        suggestionAppender = new SuggestionAppender(suggestions, input);

        // 変数宣言、関数宣言を抜き出す
        DeclarationTree definitionTree = DefinitionExtractor.extract(input.getAst());
        Collection<IASTDeclarator> duplicatedDefinitions = DuplicatedIdentifierFinder.find(definitionTree);

        for (IASTDeclarator decl : duplicatedDefinitions) {
            suggestionAppender.append(decl,
                    StringResource.get("識別子%sが重複している",
                        String.valueOf(decl.getName().getSimpleID())),
                    null);
        }

        return suggestions;
    }

}
