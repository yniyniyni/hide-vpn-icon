package dev.yniyniyni.hidevpnicon;

import java.util.Locale;

/** Pure decisions used by the SystemUI hooks. */
public final class VpnIconVisibilityPolicy {
    private VpnIconVisibilityPolicy() {
    }

    public static boolean isSystemUiPackage(String packageName) {
        return "com.android.systemui".equals(packageName);
    }

    public static boolean isVpnSlot(Object slot) {
        if (!(slot instanceof String)) {
            return false;
        }

        String normalized = ((String) slot).trim().toLowerCase(Locale.ROOT);
        return "vpn".equals(normalized) || normalized.endsWith(".vpn");
    }

    public static boolean shouldForceHidden(Object slot, boolean requestedVisible) {
        return requestedVisible && isVpnSlot(slot);
    }
}
