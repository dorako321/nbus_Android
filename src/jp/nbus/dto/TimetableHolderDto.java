package jp.nbus.dto;

import java.util.ArrayList;

/**
 * 時刻表Dto
 * @author gomess
 *
 */
public class TimetableHolderDto {
	/**
	 * 企業Dto
	 */
	public CompanyDto co;
	/**
	 * 乗車停留所
	 */
	public BusStopDto fm;
	/**
	 * 降車停留所
	 */
	public BusStopDto to;
	/**
	 * memcachedを使用しているか
	 */
	public String cache;
	/**
	 * 時刻表
	 */
	public ArrayList<TimetableDto> timetable;
	/**
	 * エラー
	 */
	public String error;
	/**
	 * エラーID
	 */
	public String error_id;
	/**
	 * エラーメッセージ
	 */
	public String error_reason;
	/**
	 * 短縮URL
	 */
	public String tiny_url;
}
