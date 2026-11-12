package com.globalr.inventory.infrastructure.tenant;

import com.globalr.inventory.domain.enums.Role;

import java.util.UUID;

public final class TenantContext {

    public record PrincipalInfo(
            UUID userId,
            String username,
            String fullName,
            Role role,
            UUID countryId,
            String countryCode,
            boolean multiCountryAccess,
            String sourceChannel,
            String clientIp
    ) {}

    private static final ThreadLocal<PrincipalInfo> HOLDER = new ThreadLocal<>();

    private TenantContext() {}

    public static void set(PrincipalInfo info) {
        HOLDER.set(info);
    }

    public static PrincipalInfo get() {
        return HOLDER.get();
    }

    public static UUID getCountryId() {
        PrincipalInfo p = HOLDER.get();
        return p != null ? p.countryId() : null;
    }

    public static String getCountryCode() {
        PrincipalInfo p = HOLDER.get();
        return p != null ? p.countryCode() : null;
    }

    public static String getUsername() {
        PrincipalInfo p = HOLDER.get();
        return p != null ? p.username() : "system";
    }

    public static void clear() {
        HOLDER.remove();
    }
}
