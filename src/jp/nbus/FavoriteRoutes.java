package jp.nbus;

/**
* お気に入りを構造体っぽく使うためのクラス
* Preferencesから取得したお気に入りの経路を入れる
* @author kerokawa55
*
*/
public class FavoriteRoutes {
    public String arr;
    public String dep;

    public FavoriteRoutes(){}
    /**
     * FavoriteRoutes
     * @param arr 乗車停留所名
     * @param dep 降車停留所名
     */
    public FavoriteRoutes(String arr, String dep){
        this.arr = arr;
        this.dep = dep;
    }
}
