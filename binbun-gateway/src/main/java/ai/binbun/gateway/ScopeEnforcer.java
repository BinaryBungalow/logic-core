package ai.binbun.gateway;

import java.util.Set;

public final class ScopeEnforcer {
    public static boolean hasScope(Set<OperatorScope> granted, Set<OperatorScope> required) {
        if (required.isEmpty()) return true;
        return granted.containsAll(required);
    }

    public static String missingScope(Set<OperatorScope> granted, Set<OperatorScope> required) {
        if (required.isEmpty()) return null;
        return required.stream()
                .filter(s -> !granted.contains(s))
                .map(OperatorScope::name)
                .toList()
                .toString();
    }
}
