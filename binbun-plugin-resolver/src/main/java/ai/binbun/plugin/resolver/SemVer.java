package ai.binbun.plugin.resolver;

public final class SemVer {
    private final int major;
    private final int minor;
    private final int patch;

    public SemVer(int major, int minor, int patch) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
    }

    public static SemVer parse(String version) {
        String clean = version.startsWith("v") ? version.substring(1) : version;
        String[] parts = clean.split("\\.");
        int major = parts.length > 0 ? parseInt(parts[0]) : 0;
        int minor = parts.length > 1 ? parseInt(parts[1]) : 0;
        int patch = parts.length > 2 ? parseInt(parts[2]) : 0;
        return new SemVer(major, minor, patch);
    }

    public int major() { return major; }
    public int minor() { return minor; }
    public int patch() { return patch; }

    public int compareTo(SemVer other) {
        if (this.major != other.major) return Integer.compare(this.major, other.major);
        if (this.minor != other.minor) return Integer.compare(this.minor, other.minor);
        return Integer.compare(this.patch, other.patch);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SemVer semVer)) return false;
        return major == semVer.major && minor == semVer.minor && patch == semVer.patch;
    }

    @Override
    public int hashCode() {
        return 31 * (31 * major + minor) + patch;
    }

    @Override
    public String toString() {
        return major + "." + minor + "." + patch;
    }

    private static int parseInt(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
