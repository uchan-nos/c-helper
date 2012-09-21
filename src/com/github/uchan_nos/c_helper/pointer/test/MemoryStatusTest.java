package com.github.uchan_nos.c_helper.pointer.test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.cdt.core.dom.ILinkage;
import org.eclipse.cdt.core.dom.ast.ASTNodeProperty;
import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.ExpansionOverlapsBoundaryException;
import org.eclipse.cdt.core.dom.ast.IASTCompletionContext;
import org.eclipse.cdt.core.dom.ast.IASTFileLocation;
import org.eclipse.cdt.core.dom.ast.IASTImageLocation;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTNodeLocation;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.IBinding;
import org.eclipse.cdt.core.parser.IToken;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import com.github.uchan_nos.c_helper.pointer.Address;
import com.github.uchan_nos.c_helper.pointer.MemoryStatus;
import com.github.uchan_nos.c_helper.pointer.Variable;

public class MemoryStatusTest {

    @Before
    public void setUp() throws Exception {
    }

    @Test
    public void equalityTest() {
        ArrayList<IASTName> names = new ArrayList<IASTName>();
        map(
                Arrays.asList("p", "q"),
                new Function<IASTName, String>() {
                    @Override
                    public IASTName calc(String arg) {
                        return new VarName(arg);
                    }
                },
                names
            );

        MemoryStatus s0 = new MemoryStatus(names);
        assertEquals(new HashSet<IASTName>(names), s0.variables().keySet());

        MemoryStatus s1 = new MemoryStatus(s0);
        assertEquals(s0, s1);
        assertEquals(s1, s0);

        s1.update(new Variable(new VarName("p"), Variable.States.POINTING, new DummyAddress(12)));
        assertFalse(s0.equals(s1));
        assertFalse(s1.equals(s0));

        s0.update(new Variable(new VarName("p"), Variable.States.POINTING, new DummyAddress(12)));
        assertEquals(s0, s1);
        assertEquals(s1, s0);
    }

    @Test
    public void collectionTest() {
        ArrayList<IASTName> names = new ArrayList<IASTName>();
        map(
                Arrays.asList("p", "q"),
                new Function<IASTName, String>() {
                    @Override
                    public IASTName calc(String arg) {
                        return new VarName(arg);
                    }
                },
                names
            );

        MemoryStatus ms0 = new MemoryStatus(names);
        ms0.update(
                new Variable(new VarName("p"), Variable.States.POINTING, new DummyAddress(1)),
                new Variable(new VarName("q"), Variable.States.POINTING, new DummyAddress(2))
                );
        MemoryStatus ms1 = new MemoryStatus(ms0);

        assertEquals(1, (new HashSet<MemoryStatus>(Arrays.asList(ms0, ms1))).size());

        ms1.update(new Variable(new VarName("p"), Variable.States.NULL, null));
        assertEquals(2, (new HashSet<MemoryStatus>(Arrays.asList(ms0, ms1))).size());

        ms1.update(new Variable(new VarName("p"), Variable.States.POINTING, new DummyAddress(3)));
        assertEquals(2, (new HashSet<MemoryStatus>(Arrays.asList(ms0, ms1))).size());

        ms0.update(new Variable(new VarName("p"), Variable.States.POINTING, new DummyAddress(3)));
        assertEquals(1, (new HashSet<MemoryStatus>(Arrays.asList(ms0, ms1))).size());
    }

    private static <Ret, Arg> void map(Collection<Arg> arg, Function<Ret, Arg> f, Collection<Ret> ret) {
        ret.clear();
        for (Arg elem : arg) {
            ret.add(f.calc(elem));
        }
    }
}

interface Function<Ret, Arg> {
    Ret calc(Arg a);
}

class DummyAddress extends Address {
    private int id;
    public DummyAddress(int id) {
        this.id = id;
    }
    public int id() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof DummyAddress)) {
            return false;
        }
        DummyAddress a = (DummyAddress) o;
        return this.id == a.id;
    }

    @Override
    public int hashCode() {
        return this.id;
    }

    @Override
    public String toString() {
        return "addr(" + id + ")";
    }
}

class VarName implements IASTName {

    private final String name;
    public VarName(String name) {
        this.name = name;
    }

    @Override
    public IASTTranslationUnit getTranslationUnit() {
        return null;
    }

    @Override
    public IASTNodeLocation[] getNodeLocations() {
        return null;
    }

    @Override
    public IASTFileLocation getFileLocation() {
        return null;
    }

    @Override
    public String getContainingFilename() {
        return null;
    }

    @Override
    public boolean isPartOfTranslationUnitFile() {
        return false;
    }

    @Override
    public IASTNode getParent() {
        return null;
    }

    @Override
    public IASTNode[] getChildren() {
        return null;
    }

    @Override
    public void setParent(IASTNode node) {
    }

    @Override
    public ASTNodeProperty getPropertyInParent() {
        return null;
    }

    @Override
    public void setPropertyInParent(ASTNodeProperty property) {
    }

    @Override
    public boolean accept(ASTVisitor visitor) {
        return false;
    }

    @Override
    public String getRawSignature() {
        return name;
    }

    @Override
    public boolean contains(IASTNode node) {
        return false;
    }

    @Override
    public IToken getLeadingSyntax() throws ExpansionOverlapsBoundaryException,
            UnsupportedOperationException {
        return null;
    }

    @Override
    public IToken getTrailingSyntax()
            throws ExpansionOverlapsBoundaryException,
            UnsupportedOperationException {
        return null;
    }

    @Override
    public IToken getSyntax() throws ExpansionOverlapsBoundaryException {
        return null;
    }

    @Override
    public boolean isFrozen() {
        return false;
    }

    @Override
    public boolean isActive() {
        return false;
    }

    @Override
    public IASTNode getOriginalNode() {
        return null;
    }

    @Override
    public char[] getSimpleID() {
        return name.toCharArray();
    }

    @Override
    public boolean isDeclaration() {
        return false;
    }

    @Override
    public boolean isReference() {
        return false;
    }

    @Override
    public boolean isDefinition() {
        return false;
    }

    @Override
    public char[] toCharArray() {
        return getSimpleID();
    }

    @Override
    public IBinding getBinding() {
        return null;
    }

    @Override
    public IBinding resolveBinding() {
        return null;
    }

    @Override
    public int getRoleOfName(boolean allowResolution) {
        return 0;
    }

    @Override
    public IASTCompletionContext getCompletionContext() {
        return null;
    }

    @Override
    public ILinkage getLinkage() {
        return null;
    }

    @Override
    public IASTImageLocation getImageLocation() {
        return null;
    }

    @Override
    public IASTName getLastName() {
        return null;
    }

    @Override
    public IASTName copy() {
        return null;
    }

    @Override
    public IASTName copy(CopyStyle style) {
        return null;
    }

    @Override
    public void setBinding(IBinding binding) {
    }

    @Override
    public char[] getLookupKey() {
        return null;
    }

    @Override
    public IBinding getPreBinding() {
        return null;
    }

    @Override
    public IBinding resolvePreBinding() {
        return null;
    }

    @Override
    public boolean isQualified() {
        return false;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof VarName)) {
            return false;
        }
        VarName n = (VarName) o;
        return this.name.equals(n.name);
    }

    @Override
    public int hashCode() {
        return this.name.hashCode();
    }
}
