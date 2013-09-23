package jp.nbus.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import android.content.Context;

public class Nfc {

	/**
	 * NFC対応端末かを判定する
	 * @param context
	 * @return 1:NFC対応端末 0:NFC非対応端末
	 */
	static public boolean isNfc(Context context){
		Class<?> clazz;
		try {
			clazz = Class.forName("android.nfc.NfcAdapter");
			if(clazz!=null){
				Method method = clazz.getMethod("getDefaultAdapter", new Class[]{Context.class});
				Object obj = method.invoke(null, context);

				if(obj!=null){
					return true;
				}
			}
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return false;
	}
}
