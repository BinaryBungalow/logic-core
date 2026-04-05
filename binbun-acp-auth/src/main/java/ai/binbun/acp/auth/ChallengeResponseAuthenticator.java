package ai.binbun.acp.auth;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ChallengeResponseAuthenticator implements AcpAuthenticator {
    private final DeviceTokenStore tokenStore;
    private final Map<String, DeviceIdentity> knownDevices = new ConcurrentHashMap<>();
    private final Map<String, String> pendingChallenges = new ConcurrentHashMap<>();
    private final String authToken;

    public ChallengeResponseAuthenticator(DeviceTokenStore tokenStore, String authToken) {
        this.tokenStore = tokenStore;
        this.authToken = authToken;
    }

    public ChallengeResponseAuthenticator(DeviceTokenStore tokenStore) {
        this(tokenStore, System.getenv().getOrDefault("PI_GATEWAY_TOKEN", "default-token"));
    }

    public String generateChallenge(String deviceId) {
        String nonce = UUID.randomUUID().toString();
        pendingChallenges.put(deviceId, nonce);
        return nonce;
    }

    public AcpAuthResult authenticateWithChallenge(String deviceId, String publicKey, String signature, String nonce, String token, String role) {
        String expectedNonce = pendingChallenges.get(deviceId);
        if (expectedNonce == null) {
            return new AcpAuthResult(false, null, "DEVICE_AUTH_NONCE_REQUIRED", "No pending challenge for device");
        }
        if (!expectedNonce.equals(nonce)) {
            return new AcpAuthResult(false, null, "DEVICE_AUTH_NONCE_MISMATCH", "Nonce mismatch");
        }

        byte[] challengeData = (deviceId + ":" + nonce + ":" + role).getBytes(StandardCharsets.UTF_8);
        if (!DeviceKeyPair.verify(publicKey, challengeData, signature)) {
            return new AcpAuthResult(false, null, "DEVICE_AUTH_SIGNATURE_INVALID", "Signature verification failed");
        }

        pendingChallenges.remove(deviceId);

        DeviceIdentity device = knownDevices.computeIfAbsent(deviceId, k ->
                DeviceIdentity.unpaired(publicKey, role));

        if (!device.paired()) {
            return new AcpAuthResult(false, null, "DEVICE_PAIRING_REQUIRED", "Device not yet paired");
        }

        if (token != null && !token.isEmpty()) {
            if (tokenStore.validate(token)) {
                DeviceTokenStore.DeviceToken dt = tokenStore.get(token);
                Set<String> scopes = dt.scopes();
                return new AcpAuthResult(true, new AcpPrincipal(deviceId, dt.role(), deviceId, scopes), null, null);
            }
        }

        if (authToken != null && token != null && authToken.equals(token)) {
            Set<String> scopes = Set.of("operator.read", "operator.write", "operator.admin");
            DeviceTokenStore.DeviceToken dt = tokenStore.issue(deviceId, role, scopes);
            return new AcpAuthResult(true, new AcpPrincipal(deviceId, role, deviceId, scopes), null, null);
        }

        return new AcpAuthResult(false, null, "DEVICE_AUTH_TOKEN_REQUIRED", "Valid token required");
    }

    public DeviceIdentity approveDevice(String deviceId) {
        DeviceIdentity device = knownDevices.get(deviceId);
        if (device == null) {
            throw new IllegalArgumentException("Unknown device: " + deviceId);
        }
        DeviceIdentity approved = device.approve();
        knownDevices.put(deviceId, approved);
        return approved;
    }

    public void denyDevice(String deviceId) {
        knownDevices.remove(deviceId);
        tokenStore.revokeByDevice(deviceId);
    }

    public Map<String, DeviceIdentity> knownDevices() {
        return Map.copyOf(knownDevices);
    }

    public Map<String, String> pendingChallenges() {
        return Map.copyOf(pendingChallenges);
    }

    @Override
    public Optional<AcpPrincipal> authenticate(String token) {
        if (authToken != null && authToken.equals(token)) {
            return Optional.of(new AcpPrincipal("gateway", "operator", null, Set.of("operator.admin")));
        }
        if (tokenStore.validate(token)) {
            DeviceTokenStore.DeviceToken dt = tokenStore.get(token);
            return Optional.of(new AcpPrincipal(dt.deviceId(), dt.role(), dt.deviceId(), dt.scopes()));
        }
        return Optional.empty();
    }
}
