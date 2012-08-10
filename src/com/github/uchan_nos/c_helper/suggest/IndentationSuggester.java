package com.github.uchan_nos.c_helper.suggest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.cdt.core.dom.ast.*;

import com.github.uchan_nos.c_helper.util.DoNothingASTVisitor;
import com.github.uchan_nos.c_helper.util.Util;

public class IndentationSuggester extends Suggester {

    @Override
    public Collection<Suggestion> suggest(final SuggesterInput input) {

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

            @Override
            public int visit(IASTExpression expression) {
                throw new RuntimeException();
            }

            @Override
            public int visit(IASTStatement statement) {
                IASTFileLocation location = statement.getFileLocation();

                // 行頭からのオフセット
                int columnNumber = Util.calculateColumnNumber(
                        input.getSource(), location.getNodeOffset());

                // 行頭からcolumnNumberまでの部分文字列
                String nodeHead = input.getSource().substring(
                        location.getNodeOffset() - columnNumber,
                        location.getNodeOffset());

                if (nodeHead.trim().length() == 0) {
                    // ステートメントの開始位置から行頭の間に非空白文字がない
                    int spaceWidth = getSpaceWidth(nodeHead);

                    if (shiftWidth == -1) {
                        if (spaceWidth > 0) {
                            shiftWidth = spaceWidth;

                            // インデントに用いられている文字を推定
                            indentChar = nodeHead.charAt(0);
                        } else if (spaceWidth == 0 && nestDepth > 0) {
                            suggestions.add(new Suggestion(
                                    input.getFilePath(),
                                    location.getStartingLineNumber(),
                                    columnNumber,
                                    location.getNodeOffset(),
                                    location.getNodeLength(),
                                    "行頭から書き始めるのは分かりにくいため、スペース "
                                    + (4 * nestDepth) + " 個分インデントすべきです。"
                                    ));
                            shiftWidth = 4;
                        }
                    } else if (spaceWidth != shiftWidth * nestDepth) {
                        suggestions.add(new Suggestion(
                                input.getFilePath(),
                                location.getStartingLineNumber(),
                                columnNumber,
                                location.getNodeOffset() - columnNumber,
                                columnNumber,
                                "インデントが乱れています。スペース "
                                + (shiftWidth * nestDepth)
                                + " 個分インデントすべきです。"));
                    }

                    if (nodeHead.indexOf(indentChar == ' ' ? '\t' : ' ') != -1) {
                        suggestions.add(new Suggestion(
                                input.getFilePath(),
                                location.getStartingLineNumber(),
                                columnNumber,
                                location.getNodeOffset() - columnNumber,
                                columnNumber,
                                "インデントに用いる文字は統一すべきです。ソースコード先頭では"
                                + (indentChar == ' ' ? "スペース" : "タブ")
                                + "が、ここでは"
                                + (indentChar == ' ' ?  "タブ" : "スペース")
                                + "が用いられています。"
                                ));
                    }
                }

                if (statement instanceof IASTCompoundStatement) {
                    ++nestDepth;
                    for (IASTStatement sub : ((IASTCompoundStatement) statement).getStatements()) {
                        sub.accept(this);
                    }
                    --nestDepth;

                    // 行頭からのオフセット
                    int lastColumnNumber = Util.calculateColumnNumber(
                            input.getSource(), location.getNodeOffset() + location.getNodeLength() - 1);

                    // 行頭からlastColumnNumberまでの部分文字列
                    String lastNodeHead = input.getSource().substring(
                            location.getNodeOffset() + location.getNodeLength() - lastColumnNumber - 1,
                            location.getNodeOffset() + location.getNodeLength());

                    if (lastNodeHead.length() > 0) {
                        int spaceWidth = getSpaceWidth(lastNodeHead.substring(0, lastNodeHead.length() - 1));

                        assert lastNodeHead.charAt(lastNodeHead.length() - 1) == '}';
                        if (lastNodeHead.indexOf(indentChar == ' ' ? '\t' : ' ') != -1) {
                            suggestions.add(new Suggestion(
                                    input.getFilePath(),
                                    location.getEndingLineNumber(),
                                    lastColumnNumber,
                                    location.getNodeOffset() + location.getNodeLength() - lastColumnNumber - 1,
                                    lastColumnNumber,
                                    "インデントに用いる文字は統一すべきです。ソースコード先頭では"
                                    + (indentChar == ' ' ? "スペース" : "タブ")
                                    + "が、ここでは"
                                    + (indentChar == ' ' ?  "タブ" : "スペース")
                                    + "が用いられています。"
                                    ));
                        }

                        if (spaceWidth != shiftWidth * nestDepth) {
                            suggestions.add(new Suggestion(
                                    input.getFilePath(),
                                    location.getStartingLineNumber(),
                                    lastColumnNumber,
                                    location.getNodeOffset() + location.getNodeLength() - lastColumnNumber - 1,
                                    lastColumnNumber,
                                    "インデントが乱れています。スペース "
                                    + (shiftWidth * nestDepth)
                                    + " 個分インデントすべきです。"));
                        }
                    }

                } else {
                    for (IASTNode sub : statement.getChildren()) {
                        if (sub instanceof IASTStatement) {
                            sub.accept(this);
                        }
                    }
                }

                return PROCESS_ABORT;
            }

            @Override
            public int visit(IASTTranslationUnit tu) {
                for (IASTNode node : tu.getChildren()) {
                    node.accept(this);
                }

                return PROCESS_ABORT;
            }

            @Override
            public int visit(IASTDeclaration declaration) {
                if (declaration instanceof IASTFunctionDefinition) {
                    IASTFunctionDefinition fd = (IASTFunctionDefinition) declaration;
                    int columnNumber = Util.calculateColumnNumber(
                            input.getSource(), fd.getFileLocation().getNodeOffset());
                    if (columnNumber != 0) {
                        suggestions.add(new Suggestion(
                                input.getFilePath(),
                                fd.getFileLocation().getStartingLineNumber(),
                                columnNumber,
                                fd.getFileLocation().getNodeOffset(),
                                fd.getFileLocation().getNodeLength(),
                                "関数の定義は行頭から書き始めると綺麗です"));
                    }
                    fd.getBody().accept(this);
                }

                return PROCESS_ABORT;
            }
        }
        IndentCheckingVisitor visitor = new IndentCheckingVisitor();
        input.getAst().accept(visitor);
        return visitor.getSuggestions();
    }

    private static Pattern indentationPattern = Pattern.compile("^([ \\t])+.*$");
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
