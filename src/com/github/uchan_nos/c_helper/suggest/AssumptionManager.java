package com.github.uchan_nos.c_helper.suggest;

import java.util.HashSet;
import java.util.Set;

/**
 * 引用した仮定を記録する.
 * @author uchan
 *
 */
public class AssumptionManager {
    private final HashSet<Assumption> referred;

    public AssumptionManager() {
        this.referred = new HashSet<Assumption>();
    }

    public Set<Assumption> getReferredAssumptions() {
        return referred;
    }

    public int ref(Assumption ass) {
        referred.add(ass);
        return ass.ordinal();
    }
}
