package jp.nbus.dto;

/**
* お気に入りを構造体っぽく使うためのクラス
* Preferencesから取得したお気に入りの経路を入れる
* @author kerokawa55
*
*/
public class FavoriteRouteDto {
    public String fmName;
    public String toName;

    public FavoriteRouteDto(){}
    /**
     * FavoriteRoutes
     * @param arr 乗車停留所名
     * @param dep 降車停留所名
     */
    public FavoriteRouteDto(String arr, String dep){
        this.fmName = arr;
        this.toName = dep;
    }
}
