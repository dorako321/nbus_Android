package jp.nbus;

import android.content.res.Resources;

/**
 * 履歴表示
 * @author gomess
 *
 */
public class TinyHistory {
	public int utilType;
	/**
	 * 日付
	 */
	public String date;
	/**
	 * 時間
	 */
	public String time;
	public int balance;
	public int fare;

	public String getUtilTypeInString(Resources resource) {
		String utilTypeStr;

		switch (utilType) {
		case 4:
			utilTypeStr = resource.getString(R.string.card_util_getoff);
			break;
		case 3:
			if (fare < 0) {
				utilTypeStr = resource.getString(R.string.card_util_charge);
			} else {
				utilTypeStr = resource.getString(R.string.card_util_geton);
			}
			break;
		case 2:
			utilTypeStr = resource.getString(R.string.card_util_charge);
			break;
		case 1:
			utilTypeStr = resource.getString(R.string.card_util_regist);
			break;
		default:
			utilTypeStr = resource.getString(R.string.card_util_undefined);
			break;
		}
		return utilTypeStr;

	}
}