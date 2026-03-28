package ru.blps.lab_1.security.jaas;

import java.security.Principal;
import java.util.Objects;

public final class RolePrincipal implements Principal {

    private final String roleName;

    public RolePrincipal(String roleName) {
        this.roleName = Objects.requireNonNull(roleName);
    }

    @Override
    public String getName() {
        return roleName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RolePrincipal that = (RolePrincipal) o;
        return roleName.equals(that.roleName);
    }

    @Override
    public int hashCode() {
        return roleName.hashCode();
    }
}
