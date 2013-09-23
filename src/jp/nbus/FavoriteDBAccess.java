package jp.nbus;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.R.integer;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class FavoriteDBAccess{

	static int DB_VERSION = 1;
	static String DB_NAME = "favorites.db";
	private Context context;

	public FavoriteDBAccess(Context context){
		this.context = context;
	}
	/*
	protected void onHandleIntent(Intent intent){
		FavoriteDBAccessHelper dbHelper = new FavoriteDBAccessHelper(context, DB_NAME, null,DB_VERSION);
		dbHelper.getReadableDatabase();
		dbHelper.close();
	}*/
	public long addFavorite(int fm_id,
			String fm_name,
			String fm_ruby,
			int to_id,
			String to_name,
			String to_ruby,
			int co,
			String co_name){
		//SQLiteHelperを作成
		FavoriteDBAccessHelper helper = new FavoriteDBAccessHelper(context,DB_NAME,null,DB_VERSION);
		//SQLiteDatabaseを取得
		SQLiteDatabase db = helper.getWritableDatabase();
		//DBに追加する値の入れ物(ContentValues)を作成
		ContentValues values = new ContentValues();
		//ContentValuesの中身を準備
		values.put("fm_id", fm_id);
		values.put("fm_name", fm_name);
		values.put("fm_ruby", fm_ruby);
		values.put("to_id", to_id);
		values.put("to_name", to_name);
		values.put("to_ruby", to_ruby);
		values.put("co", co);
		values.put("co_name", co_name);

		//db.insertで値を挿入
		long result = db.insert("favorites", "", values);
		//失敗した場合は-1が出力
		return result;
	}

	public FavoriteRoutesAsh[] readFavorites(){
		//SQLiteHelperを作成
		FavoriteDBAccessHelper helper = new FavoriteDBAccessHelper(context,DB_NAME,null,DB_VERSION);
		//SQLiteDatabaseを取得
		SQLiteDatabase db = helper.getReadableDatabase();
		Log.i("FavoriteDBAccess","ReadableDBPath="+db.getPath());

		//selectで読み出すcolumnの名前(このColumnsIndexはあとでgetString/getIntする際に必要だから変更は慎重に)
		String[] columnsName = {"id","fm_id","fm_name","fm_ruby","to_id","to_name","to_ruby","co","co_name"};
		//db.queryの返り値はCursor
		Cursor cursor = null;
		try{
			cursor = db.query("favorites", columnsName,null, null, null, null, null);
		} catch(NullPointerException e){
			e.printStackTrace();
		}
		//cursorの先頭に移動
		cursor.moveToFirst();
		int count = cursor.getCount();
		//cursorの数だけ結果を格納する配列を用意
		FavoriteRoutesAsh[] favoriteRoutes = new FavoriteRoutesAsh[count];
		for (int i = 0; i < count; i++) {
			FavoriteRoutesAsh favoriteRoute;
			//fm(またはto)_idもしくはfm(またはto)_rubyもしくはcoもしくはco_nameのいずれかがnullのときはtrueになる変数(※DB上はINTEGERのカラムでもNULLが入る)
			boolean isSearchFavorite = cursor.isNull(1)||cursor.isNull(3)||cursor.isNull(4)||cursor.isNull(6)||cursor.isNull(7)||cursor.isNull(8);
			if(isSearchFavorite){
				//停留所名のみのコンストラクタ
				favoriteRoute = new FavoriteRoutesAsh(cursor.getInt(0), cursor.getString(2), cursor.getString(5));
			}else{
				//停留所名、id、ルビ、社局名、社局idを含むコンストラクタ
				favoriteRoute = new FavoriteRoutesAsh(cursor.getInt(0), cursor.getInt(1), cursor.getString(2), cursor.getString(3), cursor.getInt(4), cursor.getString(5), cursor.getString(6), cursor.getInt(7), cursor.getString(8));
			}
			favoriteRoutes[i] = favoriteRoute;
			//次の項目にcursorを移動
			cursor.moveToNext();
		}
		cursor.close();
		db.close();

		return favoriteRoutes;
	}

	public void deleteFavorite(int rowId){
		Log.i("favorites","id="+String.valueOf(rowId));
		SQLiteDatabase db = null;
		try{
			//SQLiteHelperを作成
			FavoriteDBAccessHelper helper = new FavoriteDBAccessHelper(context,DB_NAME,null,DB_VERSION);
			//SQLiteDatabaseを取得
			db = helper.getWritableDatabase();

			int rows = db.delete("favorites","id=?", new String[]{ String.valueOf( rowId)});
			Log.i("deleteFavorite", "delete " + rows + " record(s).");
		}catch(Exception e){
			Log.e ("db.delete", e.getStackTrace().toString());
		}finally{
			if(db != null) db.close();
		}
	}

	public boolean IsDepulicate(int fmId, int toId, int co){
		//SQLiteHelperを作成
		FavoriteDBAccessHelper helper = new FavoriteDBAccessHelper(context,DB_NAME,null,DB_VERSION);
		//SQLiteDatabaseを取得
		SQLiteDatabase db = helper.getReadableDatabase();
		Log.i("FavoriteDBAccess","ReadableDBPath="+db.getPath());

		//selectで読み出すcolumnの名前(このColumnsIndexはあとでgetString/getIntする際に必要だから変更は慎重に)
		String[] columnsName = {"id"};
		//db.queryの返り値はCursor
		Cursor cursor = null;
		try{
			cursor = db.query("favorites", columnsName, "fm_id=? and to_id=? and co=?", new String[]{ String.valueOf(fmId), String.valueOf(toId), String.valueOf(co)}, null, null, null);
		} catch(NullPointerException e){
			e.printStackTrace();
		}
		//cursorの先頭に移動
		cursor.moveToFirst();
		int count = cursor.getCount();

		cursor.close();
		db.close();

		if(count == 0){
			return false;
		}
		return true;
	}

	private class FavoriteDBAccessHelper extends SQLiteOpenHelper{

		public FavoriteDBAccessHelper(Context context, String name,
				CursorFactory factory,int version) {
			super(context, name, factory, version);

			// TODO Auto-generated constructor stub
		}

		@Override
		public void onCreate(SQLiteDatabase dbarg) {
			//はじめてこのクラスのコンストラクタが呼ばれた際に呼ばれるメソッド
			//favoritesというテーブルをSQLiteDBに作成

			dbarg.beginTransaction();//トランザクションを開始(必ずあとでendTransaction()すること)
			dbarg.execSQL("create table favorites (id INTEGER PRIMARY KEY AUTOINCREMENT,fm_id INTEGER,fm_name TEXT,fm_ruby TEXT,to_id INTEGER,to_name TEXT,to_ruby TEXT,co INTEGER,co_name TEXT);");



			//SharedPreferencesに格納されたFavoriteのJSONObjectをSQLiteに格納し直す。
			//SharedPreferencesにFavoriteが格納されていない場合はSQLiteDBを作成して終わり
			SharedPreferences sharedpreference = context.getSharedPreferences("Nbus_Android", Context.MODE_PRIVATE);



			try{
				//SharedPreferencesにFavoriteのJSONが格納されているかどうか
				if(sharedpreference.contains("JSON")){
					//"JSON"というキーで何かがが格納されている場合
					String str_json = sharedpreference.getString("JSON","");//valueがなにもないときはnullを返す
					JSONObject jsonObj = new JSONObject(str_json);
					JSONArray jsonArr = jsonObj.getJSONArray("route");
					int routeArrLength = jsonArr.length();//route配列の要素数
					for(int i=0; i<routeArrLength; i++){
						JSONObject route = jsonArr.getJSONObject(i);
						//値を格納するvaluesというオブジェクトを作成
						ContentValues values = new ContentValues();
						//JSOObjのroute内のarr,depを取り出す(なぜかarr=発地、dep=着地になっている?)
						values.put("fm_name",route.getString("arr"));
						values.put("to_name",route.getString("dep"));
						//dbへinsertする
						if(dbarg.insert("favorites",null,values)==-1){
							//insertがエラーになったときの処理
							Log.i("SQLite initial insert","i="+String.valueOf(i));
						}
					}
				}
				dbarg.setTransactionSuccessful();//トランザクションが正常に実行されたということにする
				//ここでSharedPreferencesを削除?無効化?
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally{

				//トランザクションを終了
				dbarg.endTransaction();
			}



		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {


		}
	}

}
