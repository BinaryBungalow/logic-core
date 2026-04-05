package ai.binbun.gateway;

import java.util.Set;

public enum OperatorScope {
    READ("operator.read"),
    WRITE("operator.write"),
    ADMIN("operator.admin"),
    APPROVALS("operator.approvals"),
    PAIRING("operator.pairing");

    private final String name;

    OperatorScope(String name) {
        this.name = name;
    }

    public String value() {
        return name;
    }

    public static Set<OperatorScope> all() {
        return Set.of(values());
    }

    public static OperatorScope fromValue(String value) {
        for (OperatorScope scope : values()) {
            if (scope.name.equals(value)) return scope;
        }
        return null;
    }
}
