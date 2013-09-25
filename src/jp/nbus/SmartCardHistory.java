package jp.nbus;

import java.nio.ByteBuffer;

public class SmartCardHistory {

	//変数として必要なもの
	//種別、系統、停留所(駅、電停)、残高、日付
	//もう変数としてはbyte[]だけを維持して動的解釈したほうが良さそうな気がするなあ。
	public String idm;
	public byte[] byteHistory;
	public int utilType;//利用種別::1byteの符号なし整数値 (定義: 1=登録、2=積み増し、3=乗車、4=降車)
	public String date;
	public String time;
	public int systemOfPath;
	public int stopId;
	public int balance;
	public int fare;//※utilTypeが降車のときのみ勘案すべきか→条件:系統、車番が一致していること、降車の1つ前か2つか3つ前に乗車が記録されていること。
	//これで構造体的に使えるか

	public SmartCardHistory(byte[] byteArgArr, String idmArg){
		byteHistory = byteArgArr;//publicな変数に代入

		idm = idmArg;//IDm

		//D([13])の利用種別が0x00のときのみ日付の処理方式が違うのでまず分岐(0x00はカードの作成時かエントリがない場合)
		if(byteArgArr[13] != (byte)0x00){
			//D([13])が0x10,0x20,0x30,0x40であることが想定される。

			//1.利用種別のパース
			//ローカライズはActivity側でやることなのでこっちではstatuscodeを出すだけ
			switch (byteArgArr[13]){
			case (byte)0x30:
				//乗車と判断
				utilType = 3;
				break;
			case (byte)0x40:
				//降車と判断
				utilType = 4;
				break;
			case (byte)0x20:
				//積増と判断
				utilType = 2;
				break;
			case (byte)0x10:
				//登録と判断
				utilType = 1;
				break;
			default:
				//何かよくわかんないもの・未定義
				break;
			}

			//以下のパースで使い回すByteBufferの準備
			ByteBuffer bf = ByteBuffer.allocate(4);
			//日付時刻のパース
			int month;
			int day;
			//月日のパース この辺、bf.wrap(byteArgArr[0])とか書いたほうが良さそう(未検証)
	        bf.put((byte)0x00);
	        bf.put((byte)0x00);
	        bf.put((byte)0x00);
	        bf.put(byteArgArr[0]);
	        month = bf.getInt(0);//1-12

	        bf.clear();
	        bf.put((byte)0x00);
	        bf.put((byte)0x00);
	        bf.put((byte)0x00);
	        bf.put(byteArgArr[1]);
	        day = bf.getInt(0);
	        date = String.format("%d/%d", month,day);// 月/日の形式で格納

	        int hour;
	        int minute;
	        int second;

	        bf.clear();
	        bf.put((byte)0x00);
	        bf.put((byte)0x00);
	        bf.put((byte)0x00);
	        bf.put(byteArgArr[2]);
	        hour = bf.getInt(0);

	        bf.clear();
	        bf.put((byte)0x00);
	        bf.put((byte)0x00);
	        bf.put((byte)0x00);
	        bf.put(byteArgArr[3]);
	        minute = bf.getInt(0);

	        bf.clear();
	        bf.put((byte)0x00);
	        bf.put((byte)0x00);
	        bf.put((byte)0x00);
	        bf.put(byteArgArr[4]);
	        second = bf.getInt(0);
	        time = String.format("%02d:%02d:%02d", hour,minute,second);//時:分:秒の形式で格納(JST)

	        //残高の変換
	        bf.clear();
	        bf.put((byte)0x00);
	        bf.put((byte)0x00);
	        bf.put(byteArgArr[14]);
	        bf.put(byteArgArr[15]);
	        balance = bf.getInt(0);

	        if (byteArgArr[13]==(byte)0x30||byteArgArr[13]==(byte)0x40){
		        //系統(?)10進の形でintへ
		        bf.clear();
		        bf.put((byte)0x00);
		        bf.put((byte)0x00);
		        bf.put(byteArgArr[8]);
		        bf.put(byteArgArr[9]);
		        systemOfPath = bf.getInt(0);

		        //停留所
		        bf.clear();
		        bf.put((byte)0x00);
		        bf.put(byteArgArr[5]);
		        bf.put(byteArgArr[6]);
		        bf.put(byteArgArr[7]);
		        stopId = bf.getInt(0);
	        }
	        //デバッグ用パース結果ログ
	      //  Log.i("SmartCardHistory:",String.format("UtilType:%d, %s,%s Stop:%d, System:%d, Balance:%d",utilType,date,time,stopId,systemOfPath,balance));
		} else {
			//不明と判断
			utilType = 0;

			//以下のパースで使い回すByteBufferの準備
			ByteBuffer bf = ByteBuffer.allocate(4);
			//日付時刻のパース
			int year;
			int month;
			int day;

	        bf.put((byte)0x00);
	        bf.put((byte)0x00);
	        bf.put((byte)0x00);
	        bf.put(byteArgArr[0]);
	        year = bf.getInt(0);

			//月日のパース この辺、bf.wrap(byteArgArr[0])とか書いたほうが良さそう(未検証)
	        bf.put((byte)0x00);
	        bf.put((byte)0x00);
	        bf.put((byte)0x00);
	        bf.put(byteArgArr[1]);
	        month = bf.getInt(0);//1-12

	        bf.clear();
	        bf.put((byte)0x00);
	        bf.put((byte)0x00);
	        bf.put((byte)0x00);
	        bf.put(byteArgArr[2]);
	        day = bf.getInt(0);
	        date = String.format("20%d/%d/%d", year,month,day);// 年/月/日の形式で格納

	        int hour;
	        int minute;
	        int second;

	        bf.clear();
	        bf.put((byte)0x00);
	        bf.put((byte)0x00);
	        bf.put((byte)0x00);
	        bf.put(byteArgArr[3]);
	        hour = bf.getInt(0);

	        bf.clear();
	        bf.put((byte)0x00);
	        bf.put((byte)0x00);
	        bf.put((byte)0x00);
	        bf.put(byteArgArr[4]);
	        minute = bf.getInt(0);

	        bf.clear();
	        bf.put((byte)0x00);
	        bf.put((byte)0x00);
	        bf.put((byte)0x00);
	        bf.put(byteArgArr[5]);
	        second = bf.getInt(0);
	        time = String.format("%02d:%02d:%02d", hour,minute,second);//時:分:秒の形式で格納(JST)

	        //残高の変換
	        bf.clear();
	        bf.put((byte)0x00);
	        bf.put((byte)0x00);
	        bf.put(byteArgArr[14]);
	        bf.put(byteArgArr[15]);
	        balance = bf.getInt(0);
		}
	}

	public byte[] getByteHistory(){
		return byteHistory;
	}
	public int getUtilType(){
		return utilType;
	}
	public String getDate(){
		return date;
	}
	public String getTime(){
		return time;
	}
	public int getStopId(){
		return stopId;
	}
	public int getSystemOfPath(){
		return systemOfPath;
	}
	public int getBalance(){
		return balance;
	}
	public void setFare(int argFare){
		fare = argFare;
	}
	public int getFare(){
		return fare;
	}
	public String getIDm(){
		return idm;
	}
}
