package ai.binbun.acp.auth;

import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public final class DeviceKeyPair {
    private final KeyPair keyPair;

    public DeviceKeyPair() {
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
            kpg.initialize(256);
            this.keyPair = kpg.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public DeviceKeyPair(KeyPair keyPair) {
        this.keyPair = keyPair;
    }

    public String fingerprint() {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] encoded = keyPair.getPublic().getEncoded();
            byte[] digest = md.digest(encoded);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public String publicKeyBase64() {
        return Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
    }

    public String sign(byte[] data) {
        try {
            Signature sig = Signature.getInstance("SHA256withECDSA");
            sig.initSign(keyPair.getPrivate());
            sig.update(data);
            return Base64.getEncoder().encodeToString(sig.sign());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean verify(String publicKeyBase64, byte[] data, String signatureBase64) {
        try {
            byte[] pubKeyBytes = Base64.getDecoder().decode(publicKeyBase64);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(pubKeyBytes);
            KeyFactory kf = KeyFactory.getInstance("EC");
            PublicKey pubKey = kf.generatePublic(spec);
            Signature sig = Signature.getInstance("SHA256withECDSA");
            sig.initVerify(pubKey);
            sig.update(data);
            return sig.verify(Base64.getDecoder().decode(signatureBase64));
        } catch (Exception e) {
            return false;
        }
    }
}
