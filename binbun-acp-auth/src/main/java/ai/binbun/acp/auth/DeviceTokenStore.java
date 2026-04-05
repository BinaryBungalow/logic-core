package ai.binbun.acp.auth;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public final class DeviceTokenStore {
    private final Map<String, DeviceToken> tokens = new ConcurrentHashMap<>();
    private final long ttlMillis;

    public DeviceTokenStore(long ttlMillis) {
        this.ttlMillis = ttlMillis;
    }

    public DeviceTokenStore() {
        this(TimeUnit.HOURS.toMillis(24));
    }

    public DeviceToken issue(String deviceId, String role, Set<String> scopes) {
        String token = UUID.randomUUID().toString();
        DeviceToken dt = new DeviceToken(token, deviceId, role, scopes, System.currentTimeMillis() + ttlMillis);
        tokens.put(token, dt);
        return dt;
    }

    public boolean validate(String token) {
        DeviceToken dt = tokens.get(token);
        if (dt == null) return false;
        if (System.currentTimeMillis() > dt.expiryMillis()) {
            tokens.remove(token);
            return false;
        }
        return true;
    }

    public DeviceToken get(String token) {
        return tokens.get(token);
    }

    public void revoke(String token) {
        tokens.remove(token);
    }

    public void revokeByDevice(String deviceId) {
        tokens.entrySet().removeIf(e -> e.getValue().deviceId().equals(deviceId));
    }

    public record DeviceToken(String token, String deviceId, String role, Set<String> scopes, long expiryMillis) {}
}
