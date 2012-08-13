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

            /**
             * ソースコード中でoffsetが示す行の行頭からoffset-1までの部分文字列を返す.
             * @param offset ソースコード先頭からのオフセット
             * @return 行頭から offset-1 までの文字からなる部分文字列
             */
            private String getHeadString(int offset) {
                int prevLF = input.getSource().lastIndexOf('\n', offset);
                return input.getSource().substring(prevLF + 1, offset);
            }

            /**
             * 指定されたオフセットが示す行のインデントが乱れていたらサジェストを生成して返す.
             * @param offset ソースコード先頭からのオフセット
             * @return 生成したサジェスト. インデント乱れがなければ null を返す.
             */
            private Collection<Suggestion> createSuggestionIfIndentIsWrong(int offset, int lineNumber) {
                assert shiftWidth > 0 && nestDepth >= 0;

                ArrayList<Suggestion> result = new ArrayList<Suggestion>();
                String head = getHeadString(offset);
                String indentation = getIndentation(head);
                int width = getSpaceWidth(indentation);

                if (width != shiftWidth * nestDepth) {
                    result.add(new Suggestion(
                            input.getFilePath(),
                            lineNumber,
                            0,
                            offset - head.length(),
                            indentation.length(),
                            "インデントが乱れています。スペース "
                            + (shiftWidth * nestDepth)
                            + " 個分インデントすべきです。"));
                } else {
                    int pos = head.indexOf(indentChar == ' ' ? '\t' : ' ');
                    if (pos != -1) {
                        result.add(new Suggestion(
                                input.getFilePath(),
                                lineNumber,
                                pos,
                                offset - head.length() + pos,
                                1,
                                "インデントに用いる文字は統一すべきです。前方では"
                                + (indentChar == ' ' ? "スペース" : "タブ")
                                + "が、ここでは"
                                + (indentChar == ' ' ?  "タブ" : "スペース")
                                + "が用いられています。"
                                ));
                    }
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

                String nodeHead = getHeadString(location.getNodeOffset());

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
                                    0,
                                    location.getNodeOffset(),
                                    location.getNodeLength(),
                                    "行頭から書き始めるのは分かりにくいため、スペース "
                                    + (4 * nestDepth) + " 個分インデントすべきです。"
                                    ));
                            shiftWidth = 4;
                        }
                    }

                    suggestions.addAll(createSuggestionIfIndentIsWrong(
                            location.getNodeOffset(), location.getStartingLineNumber()));

                }

                if (statement instanceof IASTCompoundStatement) {
                    ++nestDepth;
                    for (IASTStatement sub : ((IASTCompoundStatement) statement).getStatements()) {
                        sub.accept(this);
                    }
                    --nestDepth;

                    suggestions.addAll(createSuggestionIfIndentIsWrong(
                            location.getNodeOffset() + location.getNodeLength() - 1,
                            location.getEndingLineNumber()));

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
