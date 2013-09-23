package jp.nbus.util;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.provider.Settings.Secure;
import android.util.Log;

public class Utility {
	/**
	 * デバッグかリリースかの判定
	 * @param context
	 * @return 1:デバッグ　0:リリース
	 */
	public static Boolean isDebug(Context context){
		try {
			ApplicationInfo appInfo = context.getPackageManager().getApplicationInfo(context.getPackageName(), 0);
			return ((appInfo.flags & ApplicationInfo.FLAG_DEBUGGABLE) == ApplicationInfo.FLAG_DEBUGGABLE);
		} catch (Exception e) {
			e.getStackTrace();
			return false;
		}
	}

	/**
	 * エミュレータか実機かの判定
	 * @return 1:エミュレータ 0:実機
	 */
	public static Boolean isEmulator(Context context) {
		String android_id = Secure.getString(context.getContentResolver(),
				Secure.ANDROID_ID);

		if (android_id == null) {
			// エミュレータ
			Log.d("LOG : Secure.ANDROID_ID = ", "null");
			return true;
		}

		Log.d("LOG : Secure.ANDROID_ID = ", android_id);

		// Android 2.2
		final String EMULATOR_ANDROID_ID_8 = "9774d56d682e549c";
		if (android.os.Build.VERSION.SDK_INT == 8) {
			if (android_id.equals(EMULATOR_ANDROID_ID_8)) {
				return true;
			}
		}

		// Android 2.3
		final String EMULATOR_ANDROID_ID_9 = "c77cf6d661a8080e";
		if (android.os.Build.VERSION.SDK_INT == 9) {
			if (android_id.equals(EMULATOR_ANDROID_ID_9)) {
				return true;
			}
		}

		// Android 2.3.3
		final String EMULATOR_ANDROID_ID_10 = "8432796b7be7bd7c";
		if (android.os.Build.VERSION.SDK_INT == 10) {
			if (android_id.equals(EMULATOR_ANDROID_ID_10)) {
				return true;
			}
		}

		// Android 3.0
		final String EMULATOR_ANDROID_ID_11 = "de36860dc021280e";
		if (android.os.Build.VERSION.SDK_INT == 11) {
			if (android_id.equals(EMULATOR_ANDROID_ID_11)) {
				return true;
			}
		}

		// 未知のバージョンが存在するかも・・・return false;
		return false;
	}

	/**
	 * Version Code の取得
	 */
	public static Integer GetVersionCode(PackageManager packageManager,
			String packageName) {
		try {
			PackageInfo packageInfo = packageManager.getPackageInfo(
					packageName, PackageManager.GET_ACTIVITIES);
			return packageInfo.versionCode;
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Version Name の取得
	 */
	public static String GetVersionName(PackageManager packageManager,
			String packageName) {
		try {
			PackageInfo packageInfo = packageManager.getPackageInfo(
					packageName, PackageManager.GET_ACTIVITIES);
			return packageInfo.versionName;
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
		return null;
	}

}
