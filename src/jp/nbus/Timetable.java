package jp.nbus;

/***** 構造体っぽく使うためのクラス *****/
//APIから帰ってきたJSONからのデータを入れる

public class Timetable {
	public int arr_time;
    public int dep_time;
    public String via;
    public String detail;
    //Ash用拡張
    public String arr;
    public String dep;
    public String destination;
    
    public Timetable(){}
    public Timetable(int arr_time, int dep_time, String via, String detail, String arr, String dep, String destination){
        this.arr_time = arr_time;
        this.dep_time = dep_time;
        this.via = via;
        this.detail = detail;
        //Ash用拡張
        this.arr = arr;
        this.dep = dep;
        this.destination = destination;
    }
}
