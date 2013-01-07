package com.github.uchan_nos.c_helper.util.test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

import org.eclipse.cdt.core.dom.ast.*;

import org.eclipse.core.runtime.CoreException;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import com.github.uchan_nos.c_helper.analysis.FileInfo;
import com.github.uchan_nos.c_helper.analysis.Parser;

import com.github.uchan_nos.c_helper.util.Util;

public class UtilTest {

    @Test
    public void getAllVariableNames() {
        final String source =
            "#include <stdlib.h>\n" +
            "extern int siz;\n" +
            "struct A { int a; };\n" +
            "int f(int var0) {\n" +
            "  char *var1;\n" +
            "  float var2;\n" +
            "  struct A var3;\n" +
            "  double g(double arg0);\n" +
            "  var1 = malloc(siz);\n" +
            "}\n";

        try {
            IASTTranslationUnit translationUnit =
                    new Parser(new FileInfo("dummy", false), source).parse();

            ArrayList<IASTFunctionDefinition> functionDefinitions =
                new ArrayList<IASTFunctionDefinition>();
            for (IASTDeclaration declaration : translationUnit.getDeclarations()) {
                if (declaration.isPartOfTranslationUnitFile()
                        && declaration instanceof IASTFunctionDefinition) {
                    functionDefinitions.add((IASTFunctionDefinition) declaration);
                }
            }

            assertEquals(1, functionDefinitions.size());

            final IASTFunctionDefinition fd_f = functionDefinitions.get(0);

            Collection<IBinding> variables =
                Util.map(
                    Util.getAllVariableNames(fd_f),
                    new Util.Function<IBinding, IASTName>() {
                        @Override public IBinding calc(IASTName arg) {
                            return arg.resolveBinding();
                        }},
                    new HashSet<IBinding>());

            Collection<String> variableNames =
                Util.map(
                    variables,
                    new Util.Function<String, IBinding>() {
                        @Override public String calc(IBinding arg) {
                            return arg.getName();
                        }},
                    new HashSet<String>());

            assertEquals(
                    new HashSet<String>(Arrays.asList(
                            "siz", "var0", "var1", "var2", "var3", "arg0")),
                    variableNames);
        } catch (CoreException e) {
            e.printStackTrace();
        }
    }

}
