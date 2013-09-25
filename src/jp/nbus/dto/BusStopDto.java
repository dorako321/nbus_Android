package jp.nbus.dto;
/***** 構造体っぽく使うためのクラス *****/
//APIから帰ってきたJSONからのデータを入れる
/**
 * バスの経路情報Dto
 * @author gomess
 *
 */
public class BusStopDto {
	/**
	 * 企業ID
	 */
	public int companyId;
	/**
	 * 企業名
	 */
	public String companyName;
	/**
	 * 乗車停留所ID
	 */
	public int fmId;
	/**
	 * 乗車停留所名
	 */
	public String fmName;
	/**
	 * 乗車停留所座標ID
	 */
	public int fmPosId;
	/**
	 * 乗車停留所読み
	 */
	public String fmRuby;
	/**
	 * 降車停留所ID
	 */
	public int toId;
	/**
	 * 降車停留所名
	 */
	public String toName;
	/**
	 * 降車停留所座標ID
	 */
	public int toPosId;
	/**
	 * 降車停留所読み
	 */
	public String toRuby;

    public BusStopDto(){}

    /**
     *
     * @param companyId 企業ID
     * @param companyName 企業名
     * @param fromStopId 乗車停留所ID
     * @param fromStopName 乗車停留所名
     * @param fromStopPosId 乗車停留所座標ID
     * @param fromStopRuby 乗車停留所読み
     * @param toStopId 降車停留所ID
     * @param toStopName 降車停留所名
     * @param toStopPosId 降車停留所座標ID
     * @param toStopRuby 降車停留所読み
     */
    public BusStopDto(int companyId, String companyName,
    		int fromStopId, String fromStopName,
    		int fromStopPosId, String fromStopRuby,
    		int toStopId, String toStopName,
    		int toStopPosId, String toStopRuby){
        this.companyId = companyId;
        this.companyName = companyName;

        this.fmId = fromStopId;
        this.fmName = fromStopName;
        this.fmPosId = fromStopPosId;
        this.fmRuby = fromStopRuby;

        this.toId = toStopId;
        this.toName = toStopName;
        this.toPosId = toStopPosId;
        this.toRuby = toStopRuby;
    }

}
