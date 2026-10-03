package dev.yniyniyni.hidevpnicon;

import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedModuleInterface;

/**
 * Hides the VPN status-bar icon in Pixel SystemUI.
 *
 * <p>Android 17's Pixel SystemUI contains both the legacy signal-policy path and
 * the newer system-status-icons view-model path. The module hooks both so that
 * the icon stays hidden regardless of which pipeline is active.</p>
 */
public final class HideVpnIcon extends XposedModule {
    private static final String TAG = "HideVpnIcon";
    private static final String LEGACY_ICON_CONTROLLER =
            "com.android.systemui.statusbar.phone.ui.StatusBarIconControllerImpl";
    private static final String LEGACY_VPN_UPDATE =
            "com.android.systemui.statusbar.phone.StatusBarSignalPolicy$$ExternalSyntheticLambda0";
    private static final String MODERN_VPN_VIEW_MODEL =
            "com.android.systemui.statusbar.systemstatusicons.vpn.ui.viewmodel.VpnIconViewModel";

    @Override
    public void onModuleLoaded(XposedModuleInterface.ModuleLoadedParam param) {
        log(Log.INFO, "Module loaded in process " + param.getProcessName());
    }

    @Override
    public void onPackageReady(XposedModuleInterface.PackageReadyParam param) {
        if (!VpnIconVisibilityPolicy.isSystemUiPackage(param.getPackageName())) {
            return;
        }

        // Vector 2.x dispatches the ready callback for app processes. Use the
        // package-ready classloader, which is the one that actually owns the
        // SystemUI classes after AppComponentFactory initialization.
        ClassLoader classLoader = param.getClassLoader();
        hookLegacyIconVisibility(classLoader);
        hookLegacyVpnUpdate(classLoader);
        hookModernVpnViewModel(classLoader);
        log(Log.INFO, "Hooks installed for " + param.getPackageName());
    }

    private void hookLegacyIconVisibility(ClassLoader classLoader) {
        try {
            Class<?> controller = Class.forName(LEGACY_ICON_CONTROLLER, false, classLoader);
            Method setter = controller.getDeclaredMethod("setIconVisibility", String.class, boolean.class);
            hook(setter)
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        Object slot = chain.getArg(0);
                        boolean requestedVisible = Boolean.TRUE.equals(chain.getArg(1));
                        if (VpnIconVisibilityPolicy.shouldForceHidden(slot, requestedVisible)) {
                            // Force false even during the initial setIcon call. Simply
                            // skipping a true request would leave the default holder visible.
                            return chain.proceed(new Object[]{slot, false});
                        }
                        return chain.proceed();
                    });
            log(Log.INFO, "Hooked legacy setIconVisibility");
        } catch (Throwable t) {
            log(Log.WARN, "Legacy icon-controller hook unavailable", t);
        }
    }

    private void hookLegacyVpnUpdate(ClassLoader classLoader) {
        try {
            Class<?> runnable = Class.forName(LEGACY_VPN_UPDATE, false, classLoader);
            Method run = runnable.getDeclaredMethod("run");
            hook(run)
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        Object result = chain.proceed();
                        forceHideLegacyVpnSlot(chain.getThisObject());
                        return result;
                    });
            log(Log.INFO, "Hooked legacy VPN update runnable");
        } catch (Throwable t) {
            log(Log.WARN, "Legacy VPN runnable hook unavailable", t);
        }
    }

    private void hookModernVpnViewModel(ClassLoader classLoader) {
        try {
            Class<?> viewModel = Class.forName(MODERN_VPN_VIEW_MODEL, false, classLoader);
            Method visible = viewModel.getDeclaredMethod("getVisible");
            hook(visible)
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                    .intercept(chain -> false);

            Method icon = viewModel.getDeclaredMethod("getIcon");
            hook(icon)
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                    .intercept(chain -> null);
            log(Log.INFO, "Hooked modern VPN icon view model");
        } catch (Throwable t) {
            log(Log.WARN, "Modern VPN view-model hook unavailable", t);
        }
    }

    private void forceHideLegacyVpnSlot(Object runnable) {
        try {
            Field policyField = runnable.getClass().getDeclaredField("f$0");
            policyField.setAccessible(true);
            Object policy = policyField.get(runnable);

            Field slotField = policy.getClass().getDeclaredField("mSlotVpn");
            slotField.setAccessible(true);
            String vpnSlot = (String) slotField.get(policy);

            Field controllerField = policy.getClass().getDeclaredField("mIconController");
            controllerField.setAccessible(true);
            Object iconController = controllerField.get(policy);

            Method setter = iconController.getClass().getDeclaredMethod(
                    "setIconVisibility", String.class, boolean.class);
            setter.setAccessible(true);
            setter.invoke(iconController, vpnSlot, false);
        } catch (Throwable t) {
            log(Log.WARN, "Could not force-hide legacy VPN slot", t);
        }
    }

    private void log(int priority, String message) {
        Log.println(priority, TAG, message);
        log(priority, TAG, message);
    }

    private void log(int priority, String message, Throwable throwable) {
        Log.println(priority, TAG, message + "\n" + Log.getStackTraceString(throwable));
        log(priority, TAG, message, throwable);
    }
}
