package jp.nbus;

import java.io.ByteArrayOutputStream;
import java.util.Calendar;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

public class Child2_bookmark extends Activity{

	private ListView listview;


	private ProgressDialog dialog;	//通信中ダイアログ

	//通信中に発生するエラーについて
	private Boolean net_error = false;	//エラーが起きているか
	private String error_message = "";	//エラーメッセージ

	//nbus.jpから帰って来るエラーについて
	private int json_error;	//エラー起きてないか
	private String json_error_reason;	//起きてた場合、理由は何か

	private SmartCardAccess smartCardAccess;

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.child2_bookmark);

        //タイトルバー
        LinearLayout titlebar = (LinearLayout)findViewById(R.id.child2_bookmark_titlebar);
        titlebar.setLayoutParams(new LinearLayout.LayoutParams(Parent1.disp_width, (int)(Parent1.disp_height/10)));

        //Editボタン
        Button btn_edit = (Button)findViewById(R.id.child2_bookmark_edit);
        btn_edit.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				if(Parent1.favoriteroutes.length==0){	//お気に入りが無い場合
					Toast.makeText(getApplicationContext(), "時刻表の検索結果画面からお気に入りの経路を登録してください。", Toast.LENGTH_LONG).show();
				}else{					//お気に入りがある場合、Edit画面に遷移
					Parent2 parentActivity = ((Parent2)getParent());
			        parentActivity.showChild_edit();
				}
			}
        });

        //リストビューが入るレイアウト
        LinearLayout content = (LinearLayout)findViewById(R.id.child2_bookmark_content);
        content.setLayoutParams(new LinearLayout.LayoutParams(Parent1.disp_width, (int)(Parent1.disp_height/10*6)));

        //リストビュー
        listview = (ListView)findViewById(R.id.child2_bookmark_listview);

        //SharedPreferencesからアダプタを作ってリストビューにセットする
        make_adapter();

        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // クリックされたアイテムを取得します
            	if(Parent1.favoriteroutes.length == 0){	//お気に入りが0件の時は何もしない
            	}else{				//お気に入りがあれば時刻表取得
            		if (Parent1.favoriteroutes[position].isSearchFavorite) {
						//停留所名のみ保持されているとき
            			//Child1_selectに遷移
            			Parent1.geton_name = Parent1.favoriteroutes[position].fm_name;
            			Parent1.getoff_name = Parent1.favoriteroutes[position].to_name;

            			send_neighbor();
					} else {
						//停留所idなどが保持されているとき
						//Child1_resultに遷移
	            		Parent1.geton_id = Parent1.favoriteroutes[position].fm_id;
	            		Parent1.getoff_id = Parent1.favoriteroutes[position].to_id;
	            		Parent1.company_id = Parent1.favoriteroutes[position].co;
            			Parent1.geton_name = Parent1.favoriteroutes[position].fm_name;
            			Parent1.getoff_name = Parent1.favoriteroutes[position].to_name;
	            		Parent1.route = Parent1.geton_name+"→"+Parent1.getoff_name;	//タイトルバー用経路文字列
	            		Parent1.result_all = false;
	            		Calendar calendar = Calendar.getInstance();
	            		int week = calendar.get(Calendar.DAY_OF_WEEK)-1;	//曜日取得
	            		if(week==0){	//日曜
	            			Parent1.result_week = 2;
	            		}else if(week==6){	//土曜
	            			Parent1.result_week = 1;
	            		}else{	//平日
	            			Parent1.result_week = 0;
	            		}
	            		//実装省略のためにChild1_selectのメソッドを呼び出してみる。
	                	//Child1_select child1_select = new Child1_select();
	            		//child1_select.send_url();	//通信、エラー無く終わったら画面遷移される

		                send_ttm();//リクエストを送る
					}

            	}

            }
        });

        //NFC
        smartCardAccess = new SmartCardAccess();
		smartCardAccess.initialize(this);

	}


	@Override	//他アクティビティから戻ってきた時とかに呼ばれる、アダプタを作り直す
	protected void onResume() {
		Log.d("Child2_bookmark", "onResume");
		super.onResume();
		make_adapter();

		smartCardAccess.enableDetection(this);
	}

	//バックキーイベントをParent2じゃなくてこっちが受け取る場合がある
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if(keyCode==KeyEvent.KEYCODE_BACK){
			Nbus_AndroidActivity.tabHost.setCurrentTab(0);	//タブを切り替え
			return true;
		}
		return false;
	}


	//SharedPreferencesからアダプタを作ってリストビューにセットする
	public void make_adapter(){
		//アダプタを初期化
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.row, R.id.row_textview1);
		/*
		//SharedPreferencesから情報を読み込む
        SharedPreferences sharedpreference = getSharedPreferences("Nbus_Android", Activity.MODE_PRIVATE);
        favorite_sum = sharedpreference.getInt("DataSum", 0);	//データ数を取得
        String str_json = "";
        */

		FavoriteDBAccess dbAccess = new FavoriteDBAccess(this);
		Parent1.favoriteroutes = dbAccess.readFavorites();


        //Log.e("Child2_bookmark", String.valueOf(favorite_sum));

        if(Parent1.favoriteroutes.length == 0){	//データが無い時
        	adapter.add("お気に入りの経路が登録されていません。時刻表の検索結果画面からお気に入りを登録してください。");
        }else{
        	//データがある時
        	/*str_json = sharedpreference.getString("JSON", "");
        	try {
				JSONObject root = new JSONObject(str_json);//文字列を元にJSONObjectを作る
				JSONArray array_route = root.getJSONArray("route");
				Parent1.favoriteroutes = new FavoriteRoutes[array_route.length()];
				for(int i=0; i<array_route.length(); i++){
					JSONObject route = array_route.getJSONObject(i);
					Parent1.favoriteroutes[i] = new FavoriteRoutes(route.getString("arr"), route.getString("dep"));
					adapter.add(route.getString("arr")+"→"+route.getString("dep"));
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}*/
        	int length=Parent1.favoriteroutes.length;
        	for (int i = 0; i < length; i++) {
				FavoriteRoutesAsh favRoute= Parent1.favoriteroutes[i];
				//ホントは独自のArrayAdapterを実装したい。そのほうがきれいに仕上げられる。
				String co_name = " ";
				if (!TextUtils.isEmpty(favRoute.co_name)){
					co_name = " ["+favRoute.co_name+"]";
				}
				adapter.add(favRoute.fm_name+"→"+favRoute.to_name+co_name);
			}
        }
        listview.setAdapter(adapter);
	}

	public void send_url(){
		Calendar calendar = Calendar.getInstance();
		int week = calendar.get(Calendar.DAY_OF_WEEK)-1;	//曜日取得
		if(week==0){	//日曜
			Parent1.result_week = 2;
		}else if(week==6){	//土曜
			Parent1.result_week = 1;
		}else{	//平日
			Parent1.result_week = 0;
		}

		//-----[ダイアログの設定]
        dialog = new ProgressDialog(getParent());	//通常はnew ProgressDialog(this)だがタブ内なのでgetParent()
        dialog.setTitle("通信中");
        dialog.setMessage("時刻表取得中");
        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        dialog.show();

        //-----[ローディングの描画は別スレッドで行う]
        Thread thread = new Thread(new Progress());
        thread.start();
	}

	//ダイアログが出てる間に通信
	private class Progress implements Runnable {
        public void run() {

        	String str_json = "";	//通信して取得したJSONな文字列、後でJSONObjectに変換される
        	JSONObject rootObject;
        	JSONArray json_timetables;

        	HttpClient httpClient = new DefaultHttpClient();

    		//URLを生成
            StringBuilder uri = new StringBuilder("http://nbus.jp/path_maker.php?from="
            										+Parent1.geton_name+"&to="
            										+Parent1.getoff_name+"&week="
            										+String.valueOf(Parent1.result_week));

            HttpGet request = new HttpGet(uri.toString());
            HttpResponse httpResponse = null;

            int status = -1;

            try {
                httpResponse = httpClient.execute(request);
                status = httpResponse.getStatusLine().getStatusCode();
            } catch (Exception e) {
                Log.d("Child2_bookmark", "network_error");
                net_error = true;
                error_message = "Error:ネットワークに接続できません。";
                //return;	returnすると繋がるまで何回でも繰り返してダイアログが終わらない
            }

            if (HttpStatus.SC_OK == status) {
                try {
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    httpResponse.getEntity().writeTo(outputStream);
                    str_json = outputStream.toString(); // JSONデータ

                    //取得したJSONな文字列の先頭3文字(3バイト)をスキップして保存し直す
                    //BOMをスキップしたいので
                    //str_json = str_json.substring(3, str_json.length());

                } catch (Exception e) {
                      Log.d("Child2_bookmark", "error_httpResponse");
                }


                //文字列からJSONObjectに変換
                try {
					rootObject = new JSONObject(str_json);
	                json_error = rootObject.getInt("error");	//エラー起きてないか取得
	                if(json_error == 0){	//エラー無し
	                	json_timetables = rootObject.getJSONArray("timetable");

	                	int lenght = json_timetables.length();
	                	Parent1.timetables = new Timetable[lenght];	//JSONArrayのサイズで配列を作り直す
	                	//構造体っぽいクラスにJSONObject Parent1.json_timetablesからデータを格納していく
	                	for(int i=0; i<lenght; i++){
	                		JSONObject time = json_timetables.getJSONObject(i);
	                		String via = null;
	                		try{	//JSONObjectから各項目を取得
	                			if(time.has("via")){
	                				via = time.getString("via");
	                			}else{
	                				via = " ";//経由地がない場合のエラー回避
	                			}
		                		Parent1.timetables[i] = new Timetable(time.getInt("arr_time"),
										time.getInt("dep_time"),
										via,
										time.getString("detail"),
										time.getString("arr"),
										time.getString("dep"),
										time.getString("destination"));
	                		}catch(JSONException e){	//経由地が無くてエラーが出た場合はこっちでキャッチ
		                		/*Parent1.timetables[i] = new Timetable(time.getInt("arr_time"),
										time.getInt("dep_time"),
										"",	//viaの文字列を空に
										time.getString("detail"));*/
	                			e.printStackTrace();
	        					Log.d("Child1_result", "error_timeTableJSONObject");
	                		}

	                	}
	                }else{	//エラー有り
	                	json_error_reason = rootObject.getString("error_reason");
	                	//Log.e(String.valueOf(Parent1.json_error_id), Parent1.json_error_reason);
	                }

				} catch (JSONException e) {
					e.printStackTrace();
					Log.d("Child2_bookmark", "error_JSONObject");
				}
            } else {
                Log.d("Child2_bookmark", "Status" + status);
                //return;	returnすると繋がるまで何回でも繰り返してダイアログが終わらない
            }

            //-----[読み込み終了の通知]
            handler.sendEmptyMessage(0);
        }
    }

	//通信終了後に呼ばれる
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            //-----[ダイアログを閉じる]
            dialog.dismiss();
            if(net_error){	//通信に失敗していた場合
              	net_error = false;	//フラグ下ろす
            	Toast.makeText(getApplicationContext(), error_message, Toast.LENGTH_LONG).show();
            }else{			//通信には成功していた場合
                if(json_error==0){	//正常に時刻表が取得できてたら
                	Nbus_AndroidActivity.select_bookmark = true;	//ブックマーク選択フラグを立ててから
                	Nbus_AndroidActivity.tabHost.setCurrentTab(0);	//タブを切り替え
                }else{				//エラーが帰ってきてたら
                	//エラーダイアログ
                	new AlertDialog.Builder(getParent())
                		.setTitle("エラー")
                		.setMessage(json_error_reason)
                		.setCancelable(false)
                		.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                			public void onClick(DialogInterface dialog, int id) {
                				dialog.cancel();
                			}
                		})
                		.show();
                }
            }


        }
    };

	public void send_neighbor() {
		/*
		 * 停留所名検索メソッド(Ash API対応)
		 */

		// -----[ダイアログの設定]
		dialog = new ProgressDialog(getParent()); // 通常はnew
													// ProgressDialog(this)だがタブ内なのでgetParent()
		dialog.setTitle("通信中"); // TODO string.xmlで文字列を出せるように
		dialog.setMessage("停留所名取得中");
		dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		dialog.show();

		// -----[ローディングの描画は別スレッドで行う]
		Thread thread = new Thread(new AshProgress());
		thread.start();

	}

	// ダイアログが出てる間に通信
	private class AshProgress implements Runnable {
		public void run() {
			Log.d("Child1_search", "progress");
			String str_json = ""; // 通信して取得したJSONな文字列、後でJSONObjectに変換される
			HttpClient httpClient = new DefaultHttpClient();
			// URLを生成
			StringBuilder uri = new StringBuilder("http://nbus.jp/ng.php?fm="
					+ Parent1.geton_name + "&to=" + Parent1.getoff_name);

			HttpGet request = new HttpGet(uri.toString());
			HttpResponse httpResponse = null;

			int status = -1;

			try {
				httpResponse = httpClient.execute(request);
				status = httpResponse.getStatusLine().getStatusCode();
			} catch (Exception e) {
				Log.d("Child1_search", "network_error(ng)");
				net_error = true;
				error_message = "Error:ネットワークに接続できません。";
				// return; returnすると繋がるまで何回でも繰り返してダイアログが終わらない
			}

			if (HttpStatus.SC_OK == status) {
				try {
					ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
					httpResponse.getEntity().writeTo(outputStream);
					str_json = outputStream.toString(); // JSONデータ

					// 取得したJSONな文字列の先頭3文字(3バイト)をスキップして保存し直す
					// BOMをスキップしたいので
					// str_json = str_json.substring(3, str_json.length());

				} catch (Exception e) {
					Log.d("Child1_search", "error_httpResponse(ng)");
				}

				// 文字列からJSONObjectに変換
				try {
					JSONArray rootArrayObject = new JSONArray(str_json);
					// json_error = rootObject.getInt("error"); //エラー起きてないか取得
					// Ashだとここでerrorがでないようだ
					// json_timetables = rootObject.getJSONArray("timetable");

					int length = rootArrayObject.length();
					Parent1.busstops = new BusStop[length]; // JSONArrayのサイズで配列を作り直す
					// 構造体っぽいクラスにJSONObject Parent1.json_timetablesからデータを格納していく
					for (int i = 0; i < length; i++) {

						try {
							JSONObject stop = rootArrayObject.getJSONObject(i);
							JSONObject from = stop.getJSONObject("fm");
							JSONObject to = stop.getJSONObject("to");
							Parent1.busstops[i] = new BusStop(
									stop.getInt("co"),
									stop.getString("co_name"),
									from.getInt("id"), from.getString("name"),
									from.getInt("pos_id"),
									from.getString("ruby"), to.getInt("id"),
									to.getString("name"), to.getInt("pos_id"),
									to.getString("ruby"));
						} catch (JSONException e) {
							// TODO 該当するバス停がないときの処理
						}
						/*
						 * try{ //JSONObjectから各項目を取得 Parent1.timetables[i] = new
						 * Timetable(time.getInt("arr_time"),
						 * time.getInt("dep_time"), time.getString("via"),
						 * time.getString("detail")); }catch(JSONException e){
						 * //経由地が無くてエラーが出た場合はこっちでキャッチ Parent1.timetables[i] =
						 * new Timetable(time.getInt("arr_time"),
						 * time.getInt("dep_time"), "", //viaの文字列を空に
						 * time.getString("detail")); }
						 */
					}
				} catch (JSONException e) {
					e.printStackTrace();
					Log.d("Child1_search", "error_JSONObject");
				}
			} else {
				// Log.d("Child1_search", "Status" + status);
				// return; returnすると繋がるまで何回でも繰り返してダイアログが終わらない
			}

			// -----[読み込み終了の通知]
			busStopHandler.sendEmptyMessage(0);
		}
	}

	// 通信終了後に呼ばれる
	private Handler busStopHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			// -----[ダイアログを閉じる]
			dialog.dismiss();
			if (net_error) { // 通信に失敗していた場合
				net_error = false; // フラグ下ろす
				Toast.makeText(getApplicationContext(), error_message,
						Toast.LENGTH_LONG).show();
			} else { // 通信には成功していた場合
				if (json_error == 0) { // 正常に停留所検索結果が取得できてたら
					Nbus_AndroidActivity.select_stopname = true; // ブックマーク選択フラグを立ててから
					Nbus_AndroidActivity.tabHost.setCurrentTab(0); // タブを切り替え
				} else { // エラーが帰ってきてたら
					// エラーダイアログ
					new AlertDialog.Builder(getParent())
							.setTitle("エラー")
							.setMessage(json_error_reason)
							.setCancelable(false)
							.setPositiveButton("OK",
									new DialogInterface.OnClickListener() {
										public void onClick(
												DialogInterface dialog, int id) {
											dialog.cancel();
										}
									}).show();
				}
			}
		}
	};

	public void send_ttm() {
		/*
		 * 停留所名検索メソッド(Ash API対応)
		 */

		// -----[ダイアログの設定]
		dialog = new ProgressDialog(getParent()); // 通常はnew
													// ProgressDialog(this)だがタブ内なのでgetParent()
		dialog.setTitle("通信中"); // TODO string.xmlで文字列を出せるように
		dialog.setMessage("停留所名取得中");
		dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		dialog.show();

		// -----[ローディングの描画は別スレッドで行う]
		Thread thread = new Thread(new AshTtmProgress());
		thread.start();

	}

	// ダイアログが出てる間に通信
	//ダイアログが出てる間に通信
	public class AshTtmProgress implements Runnable {
        public void run() {

        	String str_json = "";	//通信して取得したJSONな文字列、後でJSONObjectに変換される
        	JSONObject rootObject;
        	JSONArray json_timetables;

        	HttpClient httpClient = new DefaultHttpClient();

    		//URLを生成
            StringBuilder uri = new StringBuilder("http://nbus.jp/ttm.php?fm="
            										+Parent1.geton_id+"&to="
            										+Parent1.getoff_id+"&wk="
            										+String.valueOf(Parent1.result_week)+"&co="
            										+String.valueOf(Parent1.company_id));

            HttpGet request = new HttpGet(uri.toString());
            HttpResponse httpResponse = null;

            int status = -1;

            try {
                httpResponse = httpClient.execute(request);
                status = httpResponse.getStatusLine().getStatusCode();
            } catch (Exception e) {
                Log.d("Child1_select", "network_error");
                net_error = true;
                error_message = "Error:ネットワークに接続できません。";
                //return;	returnすると繋がるまで何回でも繰り返してダイアログが終わらない
            }



            if (HttpStatus.SC_OK == status) {
                try {
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    httpResponse.getEntity().writeTo(outputStream);
                    str_json = outputStream.toString(); // JSONデータ

                    //取得したJSONな文字列の先頭3文字(3バイト)をスキップして保存し直す
                    //BOMをスキップしたいので
                    //str_json = str_json.substring(3, str_json.length());

                } catch (Exception e) {
                      Log.d("Child1_result", "error_httpResponse");
                }


                //文字列からJSONObjectに変換
                try {
					rootObject = new JSONObject(str_json);
	                json_error = rootObject.getInt("error");	//エラー起きてないか取得
	                if(json_error == 0){	//エラー無し
	                	json_timetables = rootObject.getJSONArray("timetable");

	                	int lenght = json_timetables.length();
	                	Parent1.timetables = new Timetable[lenght];	//JSONArrayのサイズで配列を作り直す
	                	//構造体っぽいクラスにJSONObject Parent1.json_timetablesからデータを格納していく

	                	for(int i=0; i<lenght; i++){
	                		JSONObject time = json_timetables.getJSONObject(i);
	                		String via = null;
	                		try{	//JSONObjectから各項目を取得
	                			if(time.has("via")){
	                				via = time.getString("via");
	                			}else{
	                				via = " ";//経由地がない場合のエラー回避
	                			}
		                		Parent1.timetables[i] = new Timetable(time.getInt("arr_time"),
										time.getInt("dep_time"),
										via,
										time.getString("detail"),
										time.getString("arr"),
										time.getString("dep"),
										time.getString("destination"));
	                		}catch(JSONException e){	//経由地が無くてエラーが出た場合はこっちでキャッチ
		                		/*Parent1.timetables[i] = new Timetable(time.getInt("arr_time"),
										time.getInt("dep_time"),
										"",	//viaの文字列を空に
										time.getString("detail"));*/
	                			e.printStackTrace();
	        					Log.d("Child1_result", "error_timeTableJSONObject");
	                		}

	                	}
	                	JSONObject from = rootObject.getJSONObject("fm");
	                	Parent1.result_geton_name = from.getString("name");
	                	Parent1.result_geton_ruby = from.getString("ruby");
	                	JSONObject to = rootObject.getJSONObject("to");
	                	Parent1.result_getoff_name = to.getString("name");
	                	Parent1.result_getoff_ruby = to.getString("ruby");

	                }else{	//エラー有り
	                	json_error_reason = rootObject.getString("error_reason");
	                	//Log.e(String.valueOf(Parent1.json_error_id), Parent1.json_error_reason);
	                }

				} catch (JSONException e) {
					e.printStackTrace();
					Log.d("Child1_result", "error_JSONObject");
				}
            } else {
                Log.d("Child1_result", "Status" + status);
                //return;	returnすると繋がるまで何回でも繰り返してダイアログが終わらない
            }

            //-----[読み込み終了の通知]
            handler.sendEmptyMessage(0);
        }
    }


	@Override
    public void onPause(){
		super.onPause();
		//本当にActivityがPauseしているのか判定
		if (this.isFinishing()){
			smartCardAccess.disableDetection(this);
		}
	}

}
