package jp.nbus;
/***** 構造体っぽく使うためのクラス *****/
//APIから帰ってきたJSONからのデータを入れる

public class BusStop {
	public int company;
	public String companyName;
	
	public int fromStopId;
	public String fromStopName;
	public int fromStopPosId;
	public String fromStopRuby;
	
	public int toStopId;
	public String toStopName;
	public int toStopPosId;
	public String toStopRuby;
    
    public BusStop(){}
    public BusStop(int company, String companyName, int fromStopId, String fromStopName, int fromStopPosId, String fromStopRuby, int toStopId, String toStopName, int toStopPosId, String toStopRuby){
        this.company = company;
        this.companyName = companyName;
        
        this.fromStopId = fromStopId;
        this.fromStopName = fromStopName;
        this.fromStopPosId = fromStopPosId;
        this.fromStopRuby = fromStopRuby;
        
        this.toStopId = toStopId;
        this.toStopName = toStopName;
        this.toStopPosId = toStopPosId;
        this.toStopRuby = toStopRuby;
    }
}
