package jp.nbus.dto;

/**
 * 時刻表Dto
 *
 * @author gomess
 *
 */
public class TimetableDto {
	/**
	 * 乗車時刻
	 */
	public int fmtime;
	/**
	 * 降車時刻
	 */
	public int totime;
	/**
	 * 経由
	 */
	public String via;
	/**
	 * 詳細
	 */
	public String detail;

	public String fmName;
	public String toName;
	/**
	 * 目的地
	 */
	public String destination;

	public TimetableDto() {
	}

	public TimetableDto(int fmtime, int totime, String via, String detail,
			String arr, String dep, String destination) {
		this.fmtime = fmtime;
		this.totime = totime;
		this.via = via;
		this.detail = detail;
		// Ash用拡張
		this.fmName = arr;
		this.toName = dep;
		this.destination = destination;
	}

	/**
	 * 時刻を時刻表記で取得
	 */
	protected String getTime(int time) {
		String fmHour = String.valueOf(time / 60);
		if (fmHour.length() == 1) {
			fmHour = "  " + fmHour;
		}
		String fmMinute = String.valueOf(time % 60);
		if (fmMinute.length() == 1) {
			fmMinute = "0" + fmMinute;
		}
		return fmHour + ":" + fmMinute;
	}

	/**
	 * 乗車時刻を時刻表記で取得
	 * @return
	 */
	public String getFmTime(){
		return getTime(this.fmtime);
	}
	/**
	 * 降車時刻を時刻表記で取得
	 * @return
	 */
	public String getToTime(){
		return getTime(this.totime);
	}

}
