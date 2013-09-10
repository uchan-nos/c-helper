package com.github.uchan_nos.c_helper.suggest;

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.cdt.core.dom.ast.*;
import org.eclipse.jface.text.BadLocationException;

import com.github.uchan_nos.c_helper.analysis.CFG;
import com.github.uchan_nos.c_helper.resource.StringResource;
import com.github.uchan_nos.c_helper.util.ASTFilter;
import com.github.uchan_nos.c_helper.util.ConstantValueCalculator;
import com.github.uchan_nos.c_helper.util.TypeUtil;
import com.github.uchan_nos.c_helper.util.Util;

public class FreadBufferSizeSuggester extends Suggester {

    private static boolean isFreadCallExpression(IASTNode node) {
        if (node instanceof IASTFunctionCallExpression) {
            IASTFunctionCallExpression fce = (IASTFunctionCallExpression)node;
            if (fce.getFunctionNameExpression() instanceof IASTIdExpression) {
                String funcname = ((IASTIdExpression)fce.getFunctionNameExpression()).getName().toString();
                if (funcname.equals("fread")) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public Collection<Suggestion> suggest(SuggesterInput input, AssumptionManager assumptionManager) {
        ArrayList<Suggestion> suggestions = new ArrayList<Suggestion>();

        for (String proc : input.getProcToCFG().keySet()) {
            CFG cfg = input.getProcToCFG().get(proc);
            ConstantValueCalculator calc = new ConstantValueCalculator(
                    input.getProcToRD().get(proc), input.getAnalysisEnvironment());

            for (CFG.Vertex v : cfg.getVertices()) {
                if (v.getASTNode() == null) {
                    continue;
                }

                IASTNode ast = v.getASTNode();
                ASTFilter filter = new ASTFilter(ast);

                Collection<IASTNode> freadExpressions =
                        filter.filter(new ASTFilter.Predicate() {
                            @Override
                            public boolean pass(IASTNode node) {
                                return isFreadCallExpression(node);
                            }
                        });

                for (IASTNode node : freadExpressions) {
                    System.out.println("processing: " + node.getRawSignature());
                    IASTFunctionCallExpression fce = (IASTFunctionCallExpression)node;
                    IASTExpression[] arguments = Util.getArguments(fce);
                    if (arguments.length != 4) {
                        try {
                            suggestions.add(new Suggestion(
                                    input.getSource(), fce,
                                    StringResource.get("引数の数が不正"),
                                    StringResource.get("引数は%d個必要", 4)));
                        } catch (BadLocationException e) {
                            e.printStackTrace();
                        }
                        continue;
                    }

                    // 読み込み先バッファ名を取得
                    IASTExpression readBuffer = arguments[0];
                    IASTName readBufferName = Util.getName(readBuffer);
                    if (readBufferName == null) {
                        continue;
                    }

                    // 読み込み先バッファの要素型と要素数を取得
                    IBinding readBufferBinding = readBufferName.resolveBinding();
                    if (!(readBufferBinding instanceof IVariable)) {
                        continue;
                    }
                    if (!(((IVariable)readBufferBinding).getType() instanceof IArrayType)) {
                        continue;
                    }
                    IArrayType readBufferType = (IArrayType) ((IVariable)readBufferBinding).getType();

                    // 配列定義の型名、要素数式に名前をつける
                    // readBufferElementType array_name[ readBufferSize ];
                    IType readBufferElementType = readBufferType.getType();
                    IASTExpression readBufferSize = null;
                    try {
                        readBufferSize = readBufferType.getArraySizeExpression();
                    } catch (DOMException e1) {
                        continue;
                    }

                    // 読み込み先配列の要素数を計算（定数でなければcontinue）
                    int readBufferSizeValue = 0;
                    try {
                        readBufferSizeValue = calc.toInteger(readBufferSize, v);
                    } catch (Exception e1) {
                        continue;
                    }
                    // 読み込み先配列の要素サイズを計算
                    int readBufferElementBytes = TypeUtil.bytesOfType(readBufferElementType, input.getAnalysisEnvironment());

                    // freadの引数（要素サイズ、要素数）を計算（定数でなければcontinue）
                    IASTExpression readSize = arguments[1];
                    IASTExpression readNum = arguments[2];
                    int readSizeValue = 0;
                    int readNumValue = 0;
                    try {
                        readSizeValue = calc.toInteger(readSize, v);
                        readNumValue = calc.toInteger(readNum, v);
                    } catch (Exception e1) {
                        continue;
                    }

                    try {
                        if (readBufferElementBytes != readSizeValue || readBufferSizeValue != readNumValue) {
                            if (readBufferElementBytes * readBufferSizeValue < readSizeValue * readNumValue) {
                                // overflow
                                suggestions.add(new Suggestion(
                                        input.getSource(), readBuffer,
                                        StringResource.get("バッファからデータがあふれる可能性がある"),
                                        StringResource.get("第2引数には sizeof(%s) を、第3引数には%dを指定する", readBufferElementType.toString(), readBufferSizeValue)));
                                // TODO: 使った仮定を表示する仕組み
                            } else {
                                suggestions.add(new Suggestion(
                                        input.getSource(), readBuffer,
                                        StringResource.get("バッファの大きさと fread の引数が整合していない"),
                                        StringResource.get("第2引数には%dを、第3引数には%dを指定する", readBufferElementBytes, readBufferSizeValue)));
                            }
                        } else if ((readSize instanceof IASTLiteralExpression)
                            && ((IASTLiteralExpression)readSize).getKind() == IASTLiteralExpression.lk_integer_constant
                            && !(TypeUtil.isIBasicType(readBufferElementType, IBasicType.Kind.eChar) && readSizeValue == 1)) {
                            suggestions.add(new Suggestion(
                                    input.getSource(), readSize,
                                    StringResource.get("要素のサイズを固定値で指定している"),
                                    StringResource.get("sizeof(%s) を使う", readBufferElementType.toString())));
                        }
                    } catch (BadLocationException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        return suggestions;
    }

}
