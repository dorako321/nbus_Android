package jp.nbus;

import java.util.ArrayList;
import java.util.Arrays;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.net.Uri;
import android.util.Log;

public class SmartCardHistoryManager extends SQLiteOpenHelper{

	public SmartCardHistoryManager(Context context, int version) {
		//たいした抽象化クラスではないのでてきとう
		super(context, "SmartCardHistory", null, version);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		// TODO Auto-generated method stub
		db.beginTransaction();
		try{
			db.execSQL("create table bytehistories (id INTEGER PRIMARY KEY, bytehistory BLOB NOT NULL, idm TEXT NOT NULL);");
			db.execSQL("create table historiescache (byteid INTEGER NOT NULL, utiltype INTEGER NOT NULL, date TEXT, time TEXT, stop INTEGER, system INTEGER, balance INTEGER, fare INTEGER, idm TEXT NOT NULL);");
			db.execSQL("create table idm (id INTEGER PRIMARY KEY AUTOINCREMENT, idm TEXT NOT NULL, alias TEXT);");
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// TODO Auto-generated method stub
		
	}
	
	public void putSmartCardHistory(SQLiteDatabase db,SmartCardHistory[] smartCardHistories,String idm){
		int length = smartCardHistories.length;
	//	Log.i("HistoryManager", String.format("smartCardHistories=%d",length));
		
		//String idm = smartCardHistories[0].getIDm();

		Cursor c = db.rawQuery("SELECT id,bytehistory,idm FROM bytehistories WHERE idm LIKE '"+idm+"' ORDER BY id DESC LIMIT 1;", null);
		//該当するIDmの一番最後に追加された履歴を見つける
		byte[] byteLastHistory = {(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00};
		c.moveToFirst();
		if(c.getCount()>0){
			byteLastHistory = c.getBlob(1);//該当IDmの最後の履歴を格納。なければ0x00で埋まった状態になっている。
		}
		
		int i = 0;
		
		for (SmartCardHistory history : smartCardHistories){//今回カードから取得した履歴配列(最新のものから昇順に格納)を昇順に走査
			if(Arrays.equals(byteLastHistory, history.getByteHistory())){//DB上の該当IDmの最後の履歴と取得した履歴の要素を比較。マッチするとfor文を抜ける
				break;
			}else{
				i++;//DB上に存在しない履歴配列上の要素の位置を変数iに格納
			}
		}
		//Log.i("HistoryManager", String.format("i=%d",i));
		if (i>0){//追加すべき履歴配列上の要素が存在するときに以下の処理を実行
			c = db.rawQuery("SELECT id FROM bytehistories ORDER BY id DESC LIMIT 1;", null);//idというカラムの最大値をとる(MAXを使わないのは、レコードがないときは結果を一件も返さないようにしたいため)
			int currentId;
			c.moveToFirst();
			if (c.getCount()>0) {
				currentId = c.getInt(0);
			}else{
				//Cursor上に格納された結果が一つも無い場合(cのcountが0)。この処理はレコードのidを0から振るために必要
				currentId = -1;
			}
	//		Log.i("HistoryManager",String.format("currentRowid= %d",currentId));
			
			c = db.rawQuery("SELECT idm FROM idm WHERE idm LIKE '"+idm+"';", null);//IDmテーブルにIDmが存在するかどうか調べる
			boolean existIDm;//無い場合はトランザクションの最後のほうでIDmを追加
			if (c.getCount()>0) {
				existIDm = true;
			}else{
				existIDm = false;
			}
			
			db.beginTransaction();//トランザクションの開始
			SQLiteStatement stmtByte = db.compileStatement("INSERT INTO bytehistories (id,bytehistory, idm) VALUES (?, ?, ?);");
			SQLiteStatement stmtCache = db.compileStatement("INSERT INTO historiescache (byteid, utiltype, date, time, stop, system, balance, fare, idm) VALUES (?,?,?,?,?,?,?,?,?)");
			SQLiteStatement stmtIDm = db.compileStatement("INSERT INTO idm (idm) VALUES (?);");
			try{
				for (int j=i-1;j>=0;j--){//i:「履歴配列上の、DBに追加すべき履歴の“カウント”」 j:「履歴配列上でDBに追加すべき履歴の添え字」
					//無いレコードの分だけ繰り返す
					currentId++;//bytehistoriesテーブルとhistoriescacheテーブルで合わせる必要のあるid(byteid)で、存在するidに1加えたものを必ず使わなければいけない
					stmtByte.bindLong(1, currentId);
					stmtByte.bindBlob(2, smartCardHistories[j].getByteHistory());
					stmtByte.bindString(3, idm);
					
					stmtCache.bindLong(1, currentId);
					stmtCache.bindLong(2, smartCardHistories[j].getUtilType());
					stmtCache.bindString(3, smartCardHistories[j].getDate());
					stmtCache.bindString(4, smartCardHistories[j].getTime());
					stmtCache.bindLong(5, smartCardHistories[j].getStopId());
					stmtCache.bindLong(6, smartCardHistories[j].getSystemOfPath());
					stmtCache.bindLong(7, smartCardHistories[j].getBalance());
					stmtCache.bindLong(8, smartCardHistories[j].getFare());
					stmtCache.bindString(9, idm);
					
					stmtByte.executeInsert();
					stmtCache.executeInsert();
				}
				if(!existIDm){//IDmがIDmテーブルに存在しないとき
					stmtIDm.bindString(1, idm);
					stmtIDm.executeInsert();
				}
				db.setTransactionSuccessful();
			}finally{
				db.endTransaction();
			}
		}
	}
	
	public SmartCardHistory[] keepSmartCardHistory(SmartCardHistory[] smartCardHistories){
		//空の要素をチェックするメソッド。空の要素を含まない新しい配列を返す。(履歴を含まないカードでエラーを回避するための措置)
		//あと運賃の計算もやっている
		//下でfor文が二回連なっているのは、もともとの要素の個数から空の要素の個数を引いた数で新しい配列を作成する必要があるため。
		int length = smartCardHistories.length;
		int countEmpty = 0;//空の要素の個数。
		byte[] emptyByte = {(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00};
		for (SmartCardHistory history : smartCardHistories){
			if(Arrays.equals(history.getByteHistory(), emptyByte)){
				countEmpty++;//空の要素だった場合はインクリメント
			}
		}
		SmartCardHistory[] cleanedSmartCardHistory = new SmartCardHistory[length-countEmpty];//中身がある要素の数の分だけ配列を作成
		int i=0;//iは新しい配列の添え字
		for (int l = 0;l<length;l++){//lはもともとの配列の要素にアクセスするための添え字
			if(!Arrays.equals(smartCardHistories[l].getByteHistory(), emptyByte)){//空の要素ではないときに要素をコピー
				cleanedSmartCardHistory[i] = smartCardHistories[l];
				try{//運賃計算
					if(l+1<length){
						int fare = smartCardHistories[l+1].getBalance() - smartCardHistories[l].getBalance();//元々の配列で次の要素(一つ過去の履歴)の残高からいまの要素の残高を引く。運賃分は負、積み増し分は正になる。正負を逆転させるため*-1(変数がfareなので)
						cleanedSmartCardHistory[i].setFare(fare);
					}else{
						cleanedSmartCardHistory[i].setFare(0);//いま触っている要素が履歴配列で最古の履歴だったとき。次の要素を触るとヌルポがでるので回避するために分岐
					}
				}catch(Exception e){
					cleanedSmartCardHistory[i].setFare(0);
				}
				i++;
			}
		}
		return cleanedSmartCardHistory;
	}
	
	public String[][] idmList(SQLiteDatabase db){
		Cursor c = db.rawQuery("SELECT id,idm,alias FROM idm ORDER BY id ASC;", null);
		int count = c.getCount();
		String[][] idms = new String[count][3];
		c.moveToFirst();
		for(int i=0;i<count;i++){
			idms[i][0]=String.format("%d",c.getInt(0));
			idms[i][1]=c.getString(1);
			idms[i][2]=c.getString(2);
			c.moveToNext();
		}
		return idms;
	}

	public ArrayList<TinyHistory> getHistory(SQLiteDatabase db, String idm) {
		Cursor c = db.rawQuery("SELECT utiltype, date, time, balance, fare FROM historiescache WHERE idm LIKE '"+idm+"' ORDER BY byteid DESC;", null);
		//String[] utiltypestring = new String[]{Resources.getSystem().getString(R.string.card_util_undefined)};
		ArrayList<TinyHistory> histories = new ArrayList<TinyHistory>();
		c.moveToFirst();
		for(int i=0;i<c.getCount();i++){
			TinyHistory history = new TinyHistory();
			history.utilType = c.getInt(c.getColumnIndex("utiltype"));
			history.date = c.getString(c.getColumnIndex("date"));
			history.time = c.getString(c.getColumnIndex("time"));
			history.balance = c.getInt(c.getColumnIndex("balance"));
			history.fare = c.getInt(c.getColumnIndex("fare"));
			histories.add(history);
			c.moveToNext();
		}
		return histories;
	}
}
