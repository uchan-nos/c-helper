package com.github.uchan_nos.c_helper.suggest;

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.cdt.core.dom.ast.*;


import com.github.uchan_nos.c_helper.resource.StringResource;

public class DefinitionInHeaderSuggester extends Suggester {

    @Override
    public Collection<Suggestion> suggest(SuggesterInput input,
            AssumptionManager assumptionManager) {
        ArrayList<Suggestion> suggestions = new ArrayList<Suggestion>();

        for (IASTDeclaration decl : input.getAst().getDeclarations()) {
            if (!decl.isPartOfTranslationUnitFile()) {
                // ソースファイルに含まれない、つまりヘッダファイルの宣言である
                if (decl instanceof IASTSimpleDeclaration) {
                    IASTSimpleDeclaration sd = (IASTSimpleDeclaration) decl;
                    IASTDeclSpecifier spec = sd.getDeclSpecifier();

                    // 関数プロトタイプ宣言ではなく、
                    // typedef, extern, staticなどがなく、
                    // デクラレータが存在する。
                    // （変数定義を伴わない構造体宣言はデクラレータがない）
                    if ((!isSimpleFunctionPrototypeDeclaration(sd)
                            && spec.getStorageClass() == IASTDeclSpecifier.sc_unspecified
                            && sd.getDeclarators().length > 0)) {
                        suggestions.add(new Suggestion(
                            decl.getContainingFilename(), decl.getFileLocation().getStartingLineNumber(), -1,
                            decl.getFileLocation().getNodeOffset(), decl.getFileLocation().getNodeLength(),
                            StringResource.get("ヘッダファイルには実体を定義すべきではない"),
                            StringResource.get("extern宣言する") + " (extern " + sd.getRawSignature() + ") "));
                    }
                } else if (decl instanceof IASTFunctionDefinition) {
                    suggestions.add(new Suggestion(
                        decl.getContainingFilename(), decl.getFileLocation().getStartingLineNumber(), -1,
                        decl.getFileLocation().getNodeOffset(), decl.getFileLocation().getNodeLength(),
                        StringResource.get("ヘッダファイルには実体を定義すべきではない"),
                        StringResource.get("ヘッダファイルにプロトタイプ宣言、ソースファイルに実体を書く")
                        + "\n" + StringResource.get("ヘッダファイルに") + ": "
                        + makePrototypeDeclaration((IASTFunctionDefinition) decl) + ";"
                        + "\n" + StringResource.get("ソースファイルに") + ": "
                        + makePrototypeDeclaration((IASTFunctionDefinition) decl) + " { ... }"));
                }
            }
        }


        return suggestions;
    }

    private static boolean isSimpleFunctionPrototypeDeclaration(IASTSimpleDeclaration sd) {
        IASTDeclarator[] declarators = sd.getDeclarators();
        if (declarators.length >= 2 || declarators.length <= 0) {
            return false;
        }

        if (!(declarators[0] instanceof IASTFunctionDeclarator)) {
            return false;
        }

        IASTFunctionDeclarator fd = (IASTFunctionDeclarator) declarators[0];

        if (fd.getNestedDeclarator() != null) {
            return false;
        }

        return true;
    }

    private static String makePrototypeDeclaration(IASTFunctionDefinition fd) {
        String beforeBody = fd.getRawSignature().substring(0,
                fd.getBody().getFileLocation().getNodeOffset()
                - fd.getFileLocation().getNodeOffset());
        return beforeBody.trim();
    }
}
