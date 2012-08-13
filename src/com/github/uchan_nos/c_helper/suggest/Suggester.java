package com.github.uchan_nos.c_helper.suggest;

import java.util.Collection;

/**
 * 1つのサジェスト項目に対応するクラス.
 * @author uchan
 *
 */
public abstract class Suggester {
    public abstract Collection<Suggestion> suggest(SuggesterInput input, AssumptionManager assumptionManager);
}
