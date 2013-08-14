package com.github.uchan_nos.c_helper.suggest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.cdt.core.dom.ast.*;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;

import com.github.uchan_nos.c_helper.resource.StringResource;

import com.github.uchan_nos.c_helper.util.DoNothingASTVisitor;

public class IndentationSuggester extends Suggester {

    @Override
    public Collection<Suggestion> suggest(final SuggesterInput input, AssumptionManager assumptionManager) {

        class IndentCheckingVisitor extends DoNothingASTVisitor {
            private int shiftWidth = -1; // インデントの1段のスペース換算幅（推定中=-1）
            private int nestDepth = 0; // ブレースによるネストの段数
            private char indentChar = ' '; // インデントに用いられる文字（タブかスペース）
            private ArrayList<Suggestion> suggestions =
                    new ArrayList<Suggestion>();

            public IndentCheckingVisitor() {
                shouldVisitTranslationUnit = true;
            }

            public Collection<Suggestion> getSuggestions() {
                return suggestions;
            }

            /**
             * ソースコード中でoffsetが示す行の行頭からoffset-1までの部分文字列を返す.
             * @param offset ソースコード先頭からのオフセット
             * @return 行頭から offset-1 までの文字からなる部分文字列
             */
            private String getHeadString(int offset) {
                IDocument src = input.getSource();
                try {
                    final int lineOffset = src.getLineOffset(src.getLineOfOffset(offset));
                    return src.get(lineOffset, offset - lineOffset);
                } catch (BadLocationException e) {
                    e.printStackTrace();
                    throw new RuntimeException("must not be here");
                }
            }

            /**
             * 指定されたオフセットが示す行のインデントが乱れていたらサジェストを生成して返す.
             * オフセットはその行での最初の非空白文字のオフセットを表す.
             * @param offset ソースコード先頭からのオフセット
             * @return 生成したサジェスト. インデント乱れがなければ空リストを返す.
             */
            private Collection<Suggestion> createSuggestionIfIndentIsWrong(int offset) {
                ArrayList<Suggestion> result = new ArrayList<Suggestion>();

                final String head = getHeadString(offset);
                if (head.trim().length() != 0) {
                    return result;
                }

                final String indentation = getIndentation(head);
                final int width = getSpaceWidth(indentation);

                try {
                    if (shiftWidth == -1) {
                        if (width > 0) {
                            shiftWidth = width;
                            // インデントに用いられている文字を推定
                            indentChar = head.charAt(0);
                        } else if (width == 0 && nestDepth > 0) {
                            suggestions.add(new Suggestion(
                                    input.getFilePath(),
                                    input.getSource().getLineOfOffset(offset),
                                    0,
                                    -1,
                                    -1,
                                    StringResource.getInstance().getString(
                                        "行頭から書き始めるのは分かりにくい。%d個分インデントすべき。",
                                        4 * nestDepth),
                                    ""
                                    ));
                            shiftWidth = 4;
                        }
                    } else if (width != shiftWidth * nestDepth) {
                        result.add(new Suggestion(
                                input.getFilePath(),
                                input.getSource().getLineOfOffset(offset),
                                0,
                                offset - head.length(),
                                indentation.length(),
                                StringResource.get(
                                    "インデントが乱れている。%d個分インデントすべき。",
                                    shiftWidth * nestDepth),
                                ""
                                ));
                    } else {
                        int pos = head.indexOf(indentChar == ' ' ? '\t' : ' ');
                        if (pos != -1) {
                            result.add(new Suggestion(
                                    input.getFilePath(),
                                    input.getSource().getLineOfOffset(offset),
                                    pos,
                                    offset - head.length() + pos,
                                    1,
                                    StringResource.get(
                                        "インデントに用いる文字は統一すべき。前方では%sここでは%sが用いられている。",
                                        (indentChar == ' ' ? "スペース" : "タブ"),
                                        (indentChar == ' ' ?  "タブ" : "スペース")),
                                    ""
                                    ));
                        }
                    }
                } catch (BadLocationException e) {
                    assert false : "must not be here";
                    e.printStackTrace();
                }

                return result;
            }

            @Override
            public int visit(IASTExpression expression) {
                throw new RuntimeException();
            }

            @Override
            public int visit(IASTStatement statement) {
                IASTFileLocation location = statement.getFileLocation();

                if (statement instanceof IASTCompoundStatement) {
                    suggestions.addAll(createSuggestionIfIndentIsWrong(
                            location.getNodeOffset()));

                    ++nestDepth;
                    for (IASTStatement sub : ((IASTCompoundStatement) statement).getStatements()) {
                        sub.accept(this);
                    }
                    --nestDepth;

                    suggestions.addAll(createSuggestionIfIndentIsWrong(
                            location.getNodeOffset() + location.getNodeLength() - 1));

                } else if (statement.getParent() instanceof IASTIfStatement
                		|| statement.getParent() instanceof IASTForStatement
                		|| statement.getParent() instanceof IASTWhileStatement
                		|| statement.getParent() instanceof IASTDoStatement
                		) {
                	++nestDepth;
                    suggestions.addAll(createSuggestionIfIndentIsWrong(
                            location.getNodeOffset()));
                	--nestDepth;
                } else {
                    suggestions.addAll(createSuggestionIfIndentIsWrong(
                            location.getNodeOffset()));

                    for (IASTNode sub : statement.getChildren()) {
                        if (sub instanceof IASTStatement) {
                            sub.accept(this);
                        }
                    }
                }

                return PROCESS_ABORT;
            }

            @Override
            public int visit(IASTDeclSpecifier declSpec) {
                if (declSpec instanceof IASTCompositeTypeSpecifier) {
                    IASTCompositeTypeSpecifier typeSpec = (IASTCompositeTypeSpecifier) declSpec;
                    ++nestDepth;
                    for (IASTDeclaration memberDecl : typeSpec.getDeclarations(false)) {
                        memberDecl.accept(this);
                    }
                    --nestDepth;
                }
                return super.visit(declSpec);
            }

            @Override
            public int visit(IASTTranslationUnit tu) {
                for (IASTNode node : tu.getChildren()) {
                    if (node.isPartOfTranslationUnitFile()) {
                        node.accept(this);
                    }
                }

                return PROCESS_ABORT;
            }

            @Override
            public int visit(IASTDeclaration declaration) {
                if (declaration instanceof IASTFunctionDefinition) {
                    IASTFunctionDefinition fd = (IASTFunctionDefinition) declaration;
                    try {
                        final int columnNumber = fd.getFileLocation().getNodeOffset()
                                - input.getSource().getLineOffset(fd.getFileLocation().getStartingLineNumber() - 1);
                        if (columnNumber != 0) {
                            suggestions.add(new Suggestion(
                                    input.getFilePath(),
                                    fd.getFileLocation().getStartingLineNumber() - 1,
                                    columnNumber,
                                    fd.getFileLocation().getNodeOffset(),
                                    fd.getFileLocation().getNodeLength(),
                                    StringResource.get(
                                        "関数の定義は行頭から書き始めると綺麗"),
                                    ""
                                    ));
                        }
                    } catch (BadLocationException e) {
                        assert false : "must not be here";
                        e.printStackTrace();
                    }
                    fd.getBody().accept(this);
                } else if (declaration instanceof IASTSimpleDeclaration) {
                    suggestions.addAll(createSuggestionIfIndentIsWrong(
                            declaration.getFileLocation().getNodeOffset()));
                    IASTSimpleDeclaration sd = (IASTSimpleDeclaration) declaration;
                    sd.getDeclSpecifier().accept(this);
                }

                return PROCESS_ABORT;
            }
        }
        IndentCheckingVisitor visitor = new IndentCheckingVisitor();
        input.getAst().accept(visitor);
        return visitor.getSuggestions();
    }

    private static Pattern indentationPattern = Pattern.compile("^([ \\t]+).*$");
    private static String getIndentation(String line) {
        Matcher m = indentationPattern.matcher(line);
        if (m.matches()) {
            return m.group(1);
        } else {
            return "";
        }
    }

    private static int getSpaceWidth(String indentation) {
        int width = 0;
        for (int i = 0; i < indentation.length(); ++i) {
            char c = indentation.charAt(i);
            assert c == ' ' || c == '\t';
            switch (c) {
            case ' ': ++width; break;
            case '\t': width += 4; break;
            }
        }
        return width;
    }

}
