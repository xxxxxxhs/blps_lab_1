package ru.blps.lab_1.security.jaas;

import org.springframework.security.authentication.jaas.AuthorityGranter;
import ru.blps.lab_1.security.OrderPrivileges;

import java.security.Principal;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class RoleToPrivilegesAuthorityGranter implements AuthorityGranter {

    private static final Map<String, Set<String>> ROLE_TO_PRIVILEGES;

    static {
        Map<String, Set<String>> m = new HashMap<>();
        m.put("CLIENT", setOf(
            OrderPrivileges.ORDER_CREATE,
            OrderPrivileges.ORDER_READ,
            OrderPrivileges.ORDER_CANCEL
        ));
        m.put("COURIER", setOf(
            OrderPrivileges.ORDER_READ,
            OrderPrivileges.ORDER_ACCEPT,
            OrderPrivileges.ORDER_REJECT,
            OrderPrivileges.ORDER_PICKUP,
            OrderPrivileges.ORDER_COMPLETE
        ));
        m.put("RESTAURANT", setOf(
            OrderPrivileges.ORDER_READ,
            OrderPrivileges.ORDER_COOK
        ));
        ROLE_TO_PRIVILEGES = Collections.unmodifiableMap(m);
    }

    private static Set<String> setOf(String... names) {
        Set<String> set = new HashSet<>();
        Collections.addAll(set, names);
        return Collections.unmodifiableSet(set);
    }

    @Override
    public Set<String> grant(Principal principal) {
        if (!(principal instanceof RolePrincipal)) {
            return Set.of();
        }
        return ROLE_TO_PRIVILEGES.getOrDefault(principal.getName(), Set.of());
    }
}
