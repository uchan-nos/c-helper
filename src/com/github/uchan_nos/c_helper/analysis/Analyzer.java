package com.github.uchan_nos.c_helper.analysis;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.cdt.core.dom.ast.*;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.texteditor.ITextEditor;

import com.github.uchan_nos.c_helper.Activator;
import com.github.uchan_nos.c_helper.exceptions.InvalidEditorPartException;
import com.github.uchan_nos.c_helper.suggest.*;

public class Analyzer {
    public static class RunOption {
        // 実行したいサジェスタ。すべて実行する場合は null
        public String suggester = null;
    }

    private IFile fileToAnalyze = null;

    public Analyzer() {
    }

    public void analyze(IEditorPart activeEditorPart, RunOption opt)
            throws InvalidEditorPartException {
        if (activeEditorPart instanceof ITextEditor) {
            ITextEditor textEditorPart = (ITextEditor) activeEditorPart;
            IEditorInput editorInput = textEditorPart.getEditorInput();
            IDocument documentToAnalyze = textEditorPart.getDocumentProvider()
                    .getDocument(editorInput);

            if (editorInput instanceof IFileEditorInput) {
                fileToAnalyze = ((IFileEditorInput) editorInput).getFile();
                analyze(new FileInfo(fileToAnalyze.getFullPath().toString(), true), documentToAnalyze, opt);
            } else {
                throw new RuntimeException("editor input isn't IFileEditorInput");
            }
        } else {
            throw new InvalidEditorPartException(
                    "We need a class implementing ITextEditor");
        }
    }

    public void analyze(FileInfo fileInfo, IDocument source, RunOption opt) {
        try {
            Suggester[] suggesters;
            if (opt.suggester == null) {
                suggesters = new Suggester[] {
                        new SizeofSuggester(),
                        new IndentationSuggester(),
                        new SemicolonOblivionSuggester(),
                        new SemicolonUnnecessarySuggester(),
                        new ReturnOblivionSuggester(),
                        new AssignmentToCharSuggester(),
                        new CastSuppressingErrorSuggester(),
                        new PrintfParameterSuggester(),
                        new MemoryLeakSuggester(),
                        new CompareCharStringSuggester(),
                        new DefinitionInHeaderSuggester(),
                        new HidingIdentifierSuggester(),
                        new ScanfCallByValueSuggester(),
                        new UndeclaredFunctionSuggester(),
                        new FreadBufferSizeSuggester()
                };
            } else {
                String suggesterName = "com.github.uchan_nos.c_helper.suggest." + opt.suggester;
                try {
                    Class<?> suggesterClass = Class.forName(suggesterName);
                    Object o = suggesterClass.newInstance();
                    if (o instanceof Suggester) {
                        suggesters = new Suggester[] {
                            (Suggester)o
                        };
                    } else {
                        System.err.println(suggesterName + " is not instanceof Suggester.");
                        return;
                    }
                } catch (ClassNotFoundException e) {
                    System.err.println(e);
                    return;
                } catch (InstantiationException e) {
                    System.err.println(e);
                    return;
                } catch (IllegalAccessException e) {
                    System.err.println(e);
                    return;
                }
            }

            AnalysisEnvironment analysisEnvironment = new AnalysisEnvironment();
            analysisEnvironment.CHAR_BIT = 8;
            analysisEnvironment.SHORT_BIT = 16;
            analysisEnvironment.INT_BIT = 32;
            analysisEnvironment.LONG_BIT = 32;
            analysisEnvironment.LONG_LONG_BIT = 64;
            analysisEnvironment.POINTER_BIT = analysisEnvironment.INT_BIT;
            analysisEnvironment.POINTER_BYTE = analysisEnvironment.POINTER_BIT / analysisEnvironment.CHAR_BIT;

            AssumptionManager assumptionManager = new AssumptionManager();

            Map<Assumption, String> assumptionDescriptions =
                    new EnumMap<Assumption, String>(Assumption.class);
            assumptionDescriptions.put(
                    Assumption.CHAR_BIT,
                    "char のサイズを " + analysisEnvironment.CHAR_BIT + " ビットと仮定しています。");
            assumptionDescriptions.put(
                    Assumption.SHORT_BIT,
                    "short int のサイズを " + analysisEnvironment.SHORT_BIT + " ビットと仮定しています。");
            assumptionDescriptions.put(
                    Assumption.INT_BIT,
                    "int のサイズを " + analysisEnvironment.INT_BIT + " ビットと仮定しています。");
            assumptionDescriptions.put(
                    Assumption.LONG_BIT,
                    "long int のサイズを " + analysisEnvironment.LONG_BIT + " ビットと仮定しています。");
            assumptionDescriptions.put(
                    Assumption.LONG_LONG_BIT,
                    "long long int のサイズを " + analysisEnvironment.LONG_LONG_BIT + " ビットと仮定しています。");
            assumptionDescriptions.put(
                    Assumption.POINTER_BIT,
                    "ポインタ変数のサイズを " + analysisEnvironment.POINTER_BIT + " ビットと仮定しています。");
            assumptionDescriptions.put(
                    Assumption.POINTER_BYTE,
                    "ポインタ変数のサイズを " + analysisEnvironment.POINTER_BYTE + " バイトと仮定しています。");

            IASTTranslationUnit translationUnit =
                    new Parser(fileInfo, source.get()).parse();
            Map<String, CFG> procToCFG =
                    new CFGCreator(translationUnit).create();
            Map<String, RD<CFG.Vertex>> procToRD =
                    new HashMap<String, RD<CFG.Vertex>>();
            for (Entry<String, CFG> entry : procToCFG.entrySet()) {
                CFG cfg = entry.getValue();
                RD<CFG.Vertex> rd =
                        new RDAnalyzer(translationUnit, cfg).analyze();
                procToRD.put(entry.getKey(), rd);
            }

            SuggesterInput input = new SuggesterInput(
                    fileInfo.getPath(), source, translationUnit, procToCFG, procToRD,
                    analysisEnvironment);
            ArrayList<Suggestion> suggestions = new ArrayList<Suggestion>();

            // 各種サジェストを生成
            for (Suggester suggester : suggesters) {
                Collection<Suggestion> s = suggester.suggest(input, assumptionManager);
                if (s != null && s.size() > 0) {
                    for (Suggestion suggestion : s) {
                        if (suggestion != null) {
                            suggestions.add(suggestion);
                        }
                    }
                }
            }

            // サジェストを行番号、列番号順にソート
            Collections.sort(suggestions, new Comparator<Suggestion>() {
                @Override
                public int compare(Suggestion o1, Suggestion o2) {
                    if (o1 == null && o2 == null) {
                        return 0;
                    } else if (o1 == null && o2 != null) {
                        return 1;
                    } else if (o1 != null && o2 == null) {
                        return -1;
                    }
                    int lineDiff = o1.getLineNumber() - o2.getLineNumber();
                    if (lineDiff != 0) {
                        return lineDiff;
                    } else {
                        return o1.getColumnNumber() - o2.getColumnNumber();
                    }
                }
            });

            // サジェストを表示
            if (fileToAnalyze != null) {

	            // 前回表示したマーカーを削除
	            Collection<IMarker> showingMarkers = Activator.getDefault().getShowingMarkers();
	            for (IMarker m : showingMarkers) {
	                m.delete();
	            }
	            showingMarkers.clear();
	
                for (Suggestion suggestion : suggestions) {
                    // サジェストするファイルを取得
                    IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(new Path(suggestion.getFilePath()));

                    // suggestionの内容を元にマーカーを生成
                    IMarker marker = file.createMarker(Activator.PLUGIN_ID + ".suggestionmarker");
                    showingMarkers.add(marker);
                    marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_WARNING);
                    if (suggestion.getOffset() >= 0 && suggestion.getLength() >= 0) {
                        marker.setAttribute(IMarker.CHAR_START, suggestion.getOffset());
                        marker.setAttribute(IMarker.CHAR_END, suggestion.getOffset() + suggestion.getLength());
                    } else {
                        marker.setAttribute(IMarker.LINE_NUMBER, suggestion.getLineNumber());
                    }
                    if (suggestion.getSuggestion() == null ||
                            suggestion.getSuggestion().length() == 0) {
                        marker.setAttribute(IMarker.MESSAGE, suggestion.getMessage());
                    } else {
                        marker.setAttribute(IMarker.MESSAGE,
                                suggestion.getMessage() + "（"
                                + suggestion.getSuggestion() + "）");
                    }
                }

                for (Assumption ass : assumptionManager.getReferredAssumptions()) {
                    IMarker marker = fileToAnalyze.createMarker(Activator.PLUGIN_ID + ".suggestionmarker");
                    marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_INFO);
                    marker.setAttribute(IMarker.MESSAGE,
                            "仮定" + ass.ordinal() + ": " + assumptionDescriptions.get(ass));
                }
            } else {
                for (Suggestion suggestion : suggestions) {
                    System.out.print(suggestion.getFilePath());
                    System.out.print(":");
                    System.out.print(suggestion.getLineNumber() + 1);
                    System.out.print(":");
                    System.out.print(suggestion.getColumnNumber() + 1);
                    System.out.print(":");
                    System.out.print(suggestion.getMessage());
                    System.out.print("（");
                    System.out.print(suggestion.getSuggestion());
                    System.out.print("）");
                    System.out.println();
                }

                for (Assumption ass : assumptionManager.getReferredAssumptions()) {
                    System.out.println("仮定" + ass.ordinal() + ": " + assumptionDescriptions.get(ass));
                }
            }

        } catch (CoreException e) {
            e.printStackTrace();
        }
    }

}
