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
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.texteditor.ITextEditor;

import com.github.uchan_nos.c_helper.Activator;
import com.github.uchan_nos.c_helper.exceptions.InvalidEditorPartException;
import com.github.uchan_nos.c_helper.suggest.AssumptionManager;
import com.github.uchan_nos.c_helper.suggest.Assumption;
import com.github.uchan_nos.c_helper.suggest.IndentationSuggester;
import com.github.uchan_nos.c_helper.suggest.SizeofSuggester;
import com.github.uchan_nos.c_helper.suggest.Suggester;
import com.github.uchan_nos.c_helper.suggest.SuggesterInput;
import com.github.uchan_nos.c_helper.suggest.Suggestion;

public class Analyzer {
    private IFile fileToAnalyze = null;

    public Analyzer() {
    }

    public void analyze(IEditorPart activeEditorPart)
            throws InvalidEditorPartException {
        if (activeEditorPart instanceof ITextEditor) {
            ITextEditor textEditorPart = (ITextEditor) activeEditorPart;
            IEditorInput editorInput = textEditorPart.getEditorInput();
            IDocument documentToAnalyze = textEditorPart.getDocumentProvider()
                    .getDocument(editorInput);

            if (editorInput instanceof IFileEditorInput) {
                fileToAnalyze = ((IFileEditorInput) editorInput).getFile();
                analyze(fileToAnalyze.getFullPath().toString(), documentToAnalyze.get());
            } else {
                throw new RuntimeException("editor input isn't IFileEditorInput");
            }
        } else {
            throw new InvalidEditorPartException(
                    "We need a class implementing ITextEditor");
        }
    }

    public void analyze(String filePath, String source) {
        Suggester[] suggesters = {
                new SizeofSuggester(),
                new IndentationSuggester()
        };
        AnalysisEnvironment analysisEnvironment = new AnalysisEnvironment();
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

        try {
            IASTTranslationUnit translationUnit =
                    new Parser(filePath, source).parse();
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
                    filePath, source, translationUnit, procToCFG, procToRD,
                    analysisEnvironment);
            ArrayList<Suggestion> suggestions = new ArrayList<Suggestion>();

            // 各種サジェストを生成
            for (Suggester suggester : suggesters) {
                Collection<Suggestion> s = suggester.suggest(input, assumptionManager);
                if (s != null && s.size() > 0) {
                    suggestions.addAll(s);
                }
            }

            // サジェストを行番号、列番号順にソート
            Collections.sort(suggestions, new Comparator<Suggestion>() {
                @Override
                public int compare(Suggestion o1, Suggestion o2) {
                    int lineDiff = o1.getLineNumber() - o2.getLineNumber();
                    if (lineDiff != 0) {
                        return lineDiff;
                    } else {
                        return o1.getColumnNumber() - o2.getColumnNumber();
                    }
                }
            });

            IResource resource = fileToAnalyze;

            // サジェストを表示
            if (resource != null) {
                resource.deleteMarkers(Activator.PLUGIN_ID + ".suggestionmarker", false, IResource.DEPTH_ZERO);
                for (Suggestion suggestion : suggestions) {
                    IMarker marker = resource.createMarker(Activator.PLUGIN_ID + ".suggestionmarker");
                    marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_WARNING);
                    marker.setAttribute(IMarker.LOCATION, suggestion.getFilePath());
                    marker.setAttribute(IMarker.CHAR_START, suggestion.getOffset());
                    marker.setAttribute(IMarker.CHAR_END, suggestion.getOffset() + suggestion.getLength());
                    marker.setAttribute(IMarker.MESSAGE, suggestion.getMessage());
                }

                for (Assumption ass : assumptionManager.getReferredAssumptions()) {
                    IMarker marker = resource.createMarker(Activator.PLUGIN_ID + ".suggestionmarker");
                    marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_INFO);
                    marker.setAttribute(IMarker.MESSAGE,
                            "仮定" + ass.ordinal() + ": " + assumptionDescriptions.get(ass));
                }
            } else {
                for (Suggestion suggestion : suggestions) {
                    System.out.print(suggestion.getFilePath());
                    System.out.print(":");
                    System.out.print(suggestion.getLineNumber());
                    System.out.print(":");
                    System.out.print(suggestion.getColumnNumber());
                    System.out.print(":");
                    System.out.print(suggestion.getMessage());
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
