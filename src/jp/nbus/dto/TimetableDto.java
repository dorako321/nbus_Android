package jp.nbus.dto;

import java.util.ArrayList;

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
	public int fmTime;
	/**
	 * 降車時刻
	 */
	public int toTime;
	/**
	 * 経由
	 */
	public String via;
	/**
	 * 目的地
	 */
	public String destination;
	/**
	 * 方向
	 */
	public String direction;
	/**
	 * 詳細
	 */
	public DetailDto detail;


	public TimetableDto() {
	}

	public TimetableDto(int fmtime, int totime, String via,
			String arr, String dep, String destination) {
		this.fmTime = fmtime;
		this.toTime = totime;
		this.via = via;
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
		return getTime(this.fmTime);
	}
	/**
	 * 降車時刻を時刻表記で取得
	 * @return
	 */
	public String getToTime(){
		return getTime(this.toTime);
	}

}
