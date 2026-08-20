package com.vaadin.plugin.hotswap;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.eclipse.jdt.launching.IVMInstall;

/**
 * Pure selection algorithm for choosing the best JetBrains Runtime (or fall-back JDK) for a project's required Java
 * version. The logic only operates on plain descriptors so it can be unit-tested without an Eclipse runtime.
 *
 * Selection rules, in order:
 * <ol>
 * <li>Drop broken and incompatible (major &lt; required) candidates.</li>
 * <li>Prefer JBR over non-JBR — fall back to non-JBR JDKs only if no compatible JBR exists.</li>
 * <li>Within the chosen kind, exact major match wins; otherwise smallest major &gt;= required.</li>
 * <li>Tie-break by latest patch version (highest full version compared numerically).</li>
 * </ol>
 */
public final class JbrSelector {

    public record JbrCandidate(IVMInstall vm, int majorVersion, boolean isJbr, boolean broken, String fullVersion) {
    }

    private JbrSelector() {
    }

    public static Optional<JbrCandidate> select(List<JbrCandidate> candidates, int requiredMajor) {
        List<JbrCandidate> compatible = candidates.stream().filter(c -> !c.broken())
                .filter(c -> c.majorVersion() >= requiredMajor).toList();

        if (compatible.isEmpty()) {
            return Optional.empty();
        }

        List<JbrCandidate> jbrs = compatible.stream().filter(JbrCandidate::isJbr).toList();
        List<JbrCandidate> pool = jbrs.isEmpty() ? compatible : jbrs;

        Comparator<JbrCandidate> byExactMatch = Comparator.comparingInt(c -> c.majorVersion() == requiredMajor ? 0 : 1);
        Comparator<JbrCandidate> bySmallestMajor = Comparator.comparingInt(JbrCandidate::majorVersion);
        Comparator<JbrCandidate> byLatestPatch = (a, b) -> compareVersions(b.fullVersion(), a.fullVersion());

        return pool.stream().min(byExactMatch.thenComparing(bySmallestMajor).thenComparing(byLatestPatch));
    }

    /**
     * Parse a Java version string ("1.8", "21", "21.0.4", "25-ea") into its major version. Returns 0 when the input
     * cannot be parsed.
     */
    public static int parseMajor(String javaVersion) {
        if (javaVersion == null || javaVersion.isEmpty()) {
            return 0;
        }
        String[] parts = javaVersion.split("[.\\-+_]");
        if (parts.length == 0) {
            return 0;
        }
        try {
            int first = Integer.parseInt(parts[0]);
            if (first == 1 && parts.length > 1) {
                return Integer.parseInt(parts[1]);
            }
            return first;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public static int compareVersions(String a, String b) {
        String[] aParts = a == null ? new String[0] : a.split("[^0-9]+");
        String[] bParts = b == null ? new String[0] : b.split("[^0-9]+");
        int n = Math.max(aParts.length, bParts.length);
        for (int i = 0; i < n; i++) {
            int aPart = parseIntOrZero(i < aParts.length ? aParts[i] : "");
            int bPart = parseIntOrZero(i < bParts.length ? bParts[i] : "");
            if (aPart != bPart) {
                return Integer.compare(aPart, bPart);
            }
        }
        return 0;
    }

    private static int parseIntOrZero(String s) {
        if (s == null || s.isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
