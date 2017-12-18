package com.google.zxing.client.android.utils;

import android.content.Context;
import android.os.Process;

/**
 * @author Cazaea
 * @time 2017/12/18 13:38
 * @mail wistorm@sina.com
 */

public class PermissionUtil {

    /**
     * 判断权限是否开通
     * @param context
     * @param permission
     * @return
     */
    public static int checkSelfPermission(Context context, String permission) {
        if (permission == null) {
            throw new IllegalArgumentException("permission is null");
        }

        return context.checkPermission(permission, android.os.Process.myPid(), Process.myUid());
    }

}
