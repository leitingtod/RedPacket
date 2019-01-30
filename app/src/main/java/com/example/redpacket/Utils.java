package com.example.redpacket;

import android.accessibilityservice.AccessibilityService;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.os.SystemClock;
import android.view.accessibility.AccessibilityNodeInfo;

import com.orhanobut.logger.Logger;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;

public class Utils {
    public static Optional<AccessibilityNodeInfo> findNodeInfoById(AccessibilityNodeInfo nodeInfo, String resId) {
        return Optional.ofNullable(nodeInfo).map(node -> {
            return Optional.ofNullable(node.findAccessibilityNodeInfosByViewId(resId)).map(list -> {
                return list.isEmpty() ? null : list.get(0);
            }).orElse(null);
        });
    }

    public static Optional<List<AccessibilityNodeInfo>> findNodeInfosById(AccessibilityNodeInfo nodeInfo, String resId) {
        return Optional.ofNullable(nodeInfo).map(node -> {
            return Optional.ofNullable(node.findAccessibilityNodeInfosByViewId(resId)).map(list -> {
                return list.isEmpty() ? null : list;
            }).orElse(Collections.emptyList());
        });
    }

    public static Optional<AccessibilityNodeInfo> findNodeInfoByText(AccessibilityNodeInfo nodeInfo, String text) {
        return Optional.ofNullable(nodeInfo).map(node -> {
            return Optional.ofNullable(node.findAccessibilityNodeInfosByText(text)).map(list -> {
                return list.isEmpty() ? null : list.get(0);
            }).orElse(null);
        });
    }

    public static Optional<AccessibilityNodeInfo> findNodeInfosByTexts(AccessibilityNodeInfo nodeInfo, String... texts) {
        return Arrays.stream(texts).map(text -> {
            return Optional.ofNullable(nodeInfo).map(node -> {
                return findNodeInfoByText(nodeInfo, text).orElse(null);
            }).orElse(null);
        }).filter(Objects::nonNull).findFirst();
    }

    public static boolean performClick(AccessibilityNodeInfo nodeInfo) {
        return Optional.ofNullable(nodeInfo).map(node -> {
            if (!nodeInfo.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                return performClick(nodeInfo.getParent());
            }
            return false;
        }).orElse(false);
    }

    public static boolean performClick(AccessibilityNodeInfo nodeInfo, int delay) {
        boolean ok = performClick(nodeInfo);
        delay = Utils.random(delay);
        Logger.d( "延时 " + delay + " 毫秒, 等待程序加载完成");
        SystemClock.sleep(delay);
        return ok;
    }

    public static boolean performBack(AccessibilityService service) {
        return Optional.ofNullable(service).map(s -> {
            return s.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);
        }).orElse(false);
    }

    public static boolean performHome(AccessibilityService service) {
        return Optional.ofNullable(service).map(s -> {
            return s.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME);
        }).orElse(false);
    }

    public static void performHome(AccessibilityService service, boolean intent) {
        Optional.ofNullable(service).ifPresent(s -> {
            Intent home = new Intent(Intent.ACTION_MAIN);
            home.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            home.addCategory(Intent.CATEGORY_HOME);
            s.startActivity(home);
        });
    }

    public static int random(int range) {
        return new Random(25).nextInt(Math.abs(range) + 1) + 300;
    }

    public static boolean isScreenOn(AccessibilityService service) {
        return Optional.ofNullable(service).map(s -> {
            return Optional.ofNullable((PowerManager) s.getSystemService(Context.POWER_SERVICE))
                    .map(PowerManager::isInteractive).orElse(false);
        }).orElse(false);
    }

    public static void wakeUpScreen(AccessibilityService service) {
        Optional.ofNullable(service).ifPresent(s -> {
            Optional.ofNullable((PowerManager) s.getSystemService(Context.POWER_SERVICE)).ifPresent(pm -> {
                PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.ACQUIRE_CAUSES_WAKEUP |
                        PowerManager.FULL_WAKE_LOCK, "red:packet");

                //点亮屏幕
                wl.acquire(3000);

                //得到键盘锁管理器
                Optional.ofNullable((KeyguardManager) s.getSystemService(Context.KEYGUARD_SERVICE)).ifPresent(km -> {
                    KeyguardManager.KeyguardLock keyguardLock = km.newKeyguardLock("unlock");
                    //解锁
                    keyguardLock.disableKeyguard();
                });
            });
        });
    }
}
