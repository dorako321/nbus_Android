package jp.nbus.dto;

public class PathDto {
	/**
	 * 企業ID
	 */
	public int co;
	/**
	 * 企業名
	 */
	public String co_name;
	/**
	 * 乗車停留所情報
	 */
	public BusStopDto fm;
	/**
	 * 降車停留所情報
	 */
	public BusStopDto to;
}
