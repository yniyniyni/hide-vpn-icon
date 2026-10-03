package dev.yniyniyni.hidevpnicon;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class VpnIconVisibilityPolicyTest {
    @Test
    public void recognizesSystemUiPackage() {
        assertTrue(VpnIconVisibilityPolicy.isSystemUiPackage("com.android.systemui"));
    }

    @Test
    public void rejectsOtherPackages() {
        assertFalse(VpnIconVisibilityPolicy.isSystemUiPackage("com.android.settings"));
        assertFalse(VpnIconVisibilityPolicy.isSystemUiPackage(null));
    }

    @Test
    public void recognizesVpnSlots() {
        assertTrue(VpnIconVisibilityPolicy.isVpnSlot("vpn"));
        assertTrue(VpnIconVisibilityPolicy.isVpnSlot(" VPN "));
        assertTrue(VpnIconVisibilityPolicy.isVpnSlot("status.vpn"));
    }

    @Test
    public void doesNotMatchOtherSlots() {
        assertFalse(VpnIconVisibilityPolicy.isVpnSlot("wifi"));
        assertFalse(VpnIconVisibilityPolicy.isVpnSlot("vpn_status"));
        assertFalse(VpnIconVisibilityPolicy.isVpnSlot(null));
    }

    @Test
    public void onlyOverridesVisibleVpnRequests() {
        assertTrue(VpnIconVisibilityPolicy.shouldForceHidden("vpn", true));
        assertFalse(VpnIconVisibilityPolicy.shouldForceHidden("vpn", false));
        assertFalse(VpnIconVisibilityPolicy.shouldForceHidden("wifi", true));
    }
}
