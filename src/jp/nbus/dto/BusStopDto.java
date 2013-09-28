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
	 * 乗車停留所ID
	 */
	public int id;
	/**
	 * 乗車停留所名
	 */
	public String name;
	/**
	 * 乗車停留所座標ID
	 */
	public int pos_id;
	/**
	 * 乗車停留所読み
	 */
	public String ruby;


    public BusStopDto(){}

    /**
     *
     * @param busstopId 乗車停留所ID
     * @param busstopName 乗車停留所名
     * @param busstopPosId 乗車停留所座標ID
     * @param busstopRuby 乗車停留所読み
     */
    public BusStopDto(
    		int busstopId, String busstopName,
    		int busstopPosId, String busstopRuby){

        this.id = busstopId;
        this.name = busstopName;
        this.pos_id = busstopPosId;
        this.ruby = busstopRuby;

    }

}
