package ai.binbun.acp.auth;

import java.util.UUID;

public record DeviceIdentity(
        String id,
        String publicKey,
        String role,
        boolean paired,
        long createdAt
) {
    public static DeviceIdentity unpaired(String publicKey, String role) {
        return new DeviceIdentity(
                UUID.randomUUID().toString(),
                publicKey,
                role,
                false,
                System.currentTimeMillis()
        );
    }

    public DeviceIdentity approve() {
        return new DeviceIdentity(id, publicKey, role, true, createdAt);
    }
}
