package jp.nbus;

import java.io.ByteArrayOutputStream;

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
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.format.Time;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class Child1_result extends Activity{

	private ArrayAdapter<String> adapter;
	private ProgressDialog dialog;	//通信中ダイアログ
	private ListView listview;
	private TextView title;

	private Button btn_weekday
					,btn_saturday
					,btn_holiday
					,btn_all
			        ,btn_bytime;

	private Boolean bus_end = false;	//今日のバス、もう終わってない？

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
        requestWindowFeature(Window.FEATURE_NO_TITLE);	//アプリのタイトルバーを表示しない
        setContentView(R.layout.child1_result);
        Log.d("Child1_result", "onCreate");

        //タイトルバー
        LinearLayout titlebar = (LinearLayout)findViewById(R.id.child1_result_titlebar);
        titlebar.setLayoutParams(new LinearLayout.LayoutParams(Parent1.disp_width, (int)(Parent1.disp_height/10*1.2)));
        title = (TextView)findViewById(R.id.child1_result_title_text);
        title.setText(Parent1.route);

        //曜日設定ボタン
        btn_weekday = (Button)findViewById(R.id.button_weekday);	//平日
        btn_saturday = (Button)findViewById(R.id.button_saturday);	//土曜
        btn_holiday = (Button)findViewById(R.id.button_holiday);	//日祝
        int button_width = (int)((Parent1.disp_width - 20*Parent1.dip_scale)/3);	//ボタンのサイズを決める
        btn_weekday.setWidth(button_width);
        btn_saturday.setWidth(button_width);
        btn_holiday.setWidth(button_width);
        btn_weekday.setOnClickListener(new OnClickListener(){	//平日
			@Override
			public void onClick(View arg0) {
				Parent1.result_week = 0;	//曜日情報を書き換え
				setDayButtonColor();
				send_url();					//情報を再取得
				//send_url();で通信が終わったらhandlerが呼ばれてアダプタを再構築→セットされる
			}
        });
        btn_saturday.setOnClickListener(new OnClickListener(){	//土曜
			@Override
			public void onClick(View v) {
				Parent1.result_week = 1;
				setDayButtonColor();
				send_url();
				//send_url();で通信が終わったらhandlerが呼ばれてアダプタを再構築→セットされる
			}
        });
        btn_holiday.setOnClickListener(new OnClickListener(){	//日曜
			@Override
			public void onClick(View v) {
				Parent1.result_week = 2;
				setDayButtonColor();
				send_url();
				//send_url();で通信が終わったらhandlerが呼ばれてアダプタを再構築→セットされる
			}
        });

        //全表示・現在時刻
        btn_all = (Button)findViewById(R.id.button_all);
        btn_bytime = (Button)findViewById(R.id.button_bytime);
        button_width = (int)((Parent1.disp_width - 20*Parent1.dip_scale)/2);
        btn_all.setWidth(button_width);
        btn_bytime.setWidth(button_width);
        btn_all.setOnClickListener(new OnClickListener(){	//全表示
			@Override
			public void onClick(View v) {
				Parent1.result_all = true;	//全表示フラグを設定
				setShowButtonColor();
				make_adapter(Parent1.result_all);	//もう一度リストビューのアダプタを作り直す
			}
        });
        btn_bytime.setOnClickListener(new OnClickListener(){	//現在時刻
			@Override
			public void onClick(View v) {
				Parent1.result_all = false;
				setShowButtonColor();
				make_adapter(Parent1.result_all);
			}
        });

        //ボタンの色を選択してるっぽく変更
        setDayButtonColor();	//平日、土曜、日祝ボタン
        setShowButtonColor();	//全表示、現在時刻ボタン

        //リストビュー
        listview = (ListView)findViewById(R.id.listView1);
        listview.getLayoutParams().height = (int)(Parent1.disp_height*0.5);	//リストビューの高さを設定
        listview.requestLayout();											//設定を適用
        make_adapter(Parent1.result_all);	//アダプタを作ってセット

        //リストのアイテムが選択された時
        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // クリックされたアイテムを取得します
            	if(Parent1.result_all){	//全表示なら
                	Parent1.detail_selected_item = position;
                    Parent1 parentActivity = ((Parent1)getParent());
    		        parentActivity.showChild_detail();
            	}else{	//現在時刻からの表示なら
                	if(!bus_end){	//もう今日はバスが無いフラグが上がって無ければ表示
                    	Parent1.detail_selected_item = Parent1.result_time_position + position;
                        Parent1 parentActivity = ((Parent1)getParent());
        		        parentActivity.showChild_detail();
                	}
            	}
            }
        });

        //お気に入りに追加ボタン
        ImageButton btn_addbookmark = (ImageButton)findViewById(R.id.button_result_addbookmark);
        btn_addbookmark.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				new AlertDialog.Builder(getParent())	//ダイアログの設定
				.setMessage("お気に入りに登録しますか？")
				.setCancelable(false)
				.setPositiveButton("Cancel", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.cancel();	//キャンセル
					}
				})
				.setNegativeButton("OK", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						try {
							addBookmark();	//お気に入りに登録
						} catch (JSONException e) {
							Log.d("Child1_result", "Error_at_addBookmark()");
							e.printStackTrace();
						}
					}
				})
				.show();
			}
        });

        //NFC
        smartCardAccess = new SmartCardAccess();
		smartCardAccess.initialize(this);

	}

	//バックキーイベントをParent1じゃなくてこっちが受け取る場合がある
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if(keyCode==KeyEvent.KEYCODE_BACK){
			Parent1 parentActivity = ((Parent1)getParent());
	        parentActivity.showChild_search();
			return true;
		}
		return false;
	}

	@Override	//他アクティビティから戻ってきた時とかに呼ばれる、リストビューを更新する。
	protected void onResume() {
		super.onResume();
		Log.d("Child1_result", "onResume");

		//タイトルバーも変更
		title.setText(Parent1.route);
        //ボタンの色を選択してるっぽく変更
        setDayButtonColor();
        setShowButtonColor();
		//リストビュー
        make_adapter(Parent1.result_all);

		if(Nbus_AndroidActivity.select_bookmark == true){
	        Nbus_AndroidActivity.select_bookmark = false;
		}
		if(Nbus_AndroidActivity.select_stopname == true){	//停留所名のブックマーク選択でタブ切り替えが起きた時は結果表示に飛ばす
			Parent1 parentActivity = ((Parent1)getParent());
	        parentActivity.showChild_select();
	        Nbus_AndroidActivity.select_stopname = false;
		}

		smartCardAccess.enableDetection(this);
	}

	//JSONObjectからアダプターにデータを入れる
	public void make_adapter(Boolean all){
		//アダプタを初期化
        adapter = new ArrayAdapter<String>(this, R.layout.row, R.id.row_textview1);
        int now_time = 0;	//現在時刻
        Parent1.result_time_position = 0;	//選ばれた項目が何番目か

        Time time = new Time("Asia/Tokyo");	//時間を取得
        time.setToNow();
        now_time = time.hour *60 + time.minute;	//今日が始まってから何分、の単位で保存
    	int t;
    	String arr_hour;
    	String arr_minute;
    	String dep_hour;
    	String dep_minute;
        if(all){	//すべて表示する時
        	for(int i=0; i<Parent1.timetables.length; i++){
            	t = Parent1.timetables[i].arr_time/60;	//出発時刻
            	arr_hour = String.valueOf(t);
            	if(t<10){
            		arr_hour = "  "+arr_hour;
            	}
            	t = Parent1.timetables[i].arr_time%60;
            	arr_minute = String.valueOf(t);
            	if(t<10){
            		arr_minute = "0"+arr_minute;
            	}
            	t = Parent1.timetables[i].dep_time/60;	//到着時刻
            	dep_hour = String.valueOf(t);
            	if(t<10){
            		dep_hour = "  "+dep_hour;
            	}
            	t = Parent1.timetables[i].dep_time%60;
            	dep_minute = String.valueOf(t);
            	if(t<10){
            		dep_minute = "0"+dep_minute;
            	}
            	adapter.add(" "+arr_hour+":"+arr_minute+" → "+dep_hour+":"+dep_minute);
            }
        }else{		//現在時刻から表示する時
        	bus_end = false;	//もう今日のバスが終わっているか？終わってないことにしておく、要素をあとで数えて判断
            for(int i=0; i<Parent1.timetables.length; i++){
            	if(Parent1.timetables[i].arr_time>now_time){	//現在時刻以降のデータなら
            		t = Parent1.timetables[i].arr_time/60;	//出発時刻
            		arr_hour = String.valueOf(t);
            		if(t<10){
            			arr_hour = "  "+arr_hour;
            		}
            		t = Parent1.timetables[i].arr_time%60;
            		arr_minute = String.valueOf(t);
            		if(t<10){
            			arr_minute = "0"+arr_minute;
            		}
            		t = Parent1.timetables[i].dep_time/60;	//到着時刻
            		dep_hour = String.valueOf(t);
            		if(t<10){
            			dep_hour = "  "+dep_hour;
            		}
            		t = Parent1.timetables[i].dep_time%60;
            		dep_minute = String.valueOf(t);
            		if(t<10){
            			dep_minute = "0"+dep_minute;
            		}
            		adapter.add(" "+arr_hour+":"+arr_minute+" → "+dep_hour+":"+dep_minute);
            	}else{	//現在時刻より前で何回iが回ったか+1
            		Parent1.result_time_position = i+1;
            	}
            }
            if(adapter.getCount()==0){	//要素が無い時(最終が終わっている時)
            	bus_end = true;
            	adapter.add("  選択された経路での本日の運行は終了しました。");
            }
        }
        listview.setAdapter(adapter);
        listview.invalidateViews();
	}

	//平日、土曜、日祝のボタン色を変える
	public void setDayButtonColor(){
		if(Parent1.result_week == 0){
	       	btn_weekday.setBackgroundResource(R.drawable.button_weekday_on);
	       	btn_saturday.setBackgroundResource(R.drawable.button_saturday);
	       	btn_holiday.setBackgroundResource(R.drawable.button_holiday);
	    }else if(Parent1.result_week == 1){
	       	btn_weekday.setBackgroundResource(R.drawable.button_weekday);
	       	btn_saturday.setBackgroundResource(R.drawable.button_saturday_on);
	       	btn_holiday.setBackgroundResource(R.drawable.button_holiday);
	    }else if(Parent1.result_week == 2){
	       	btn_weekday.setBackgroundResource(R.drawable.button_weekday);
	       	btn_saturday.setBackgroundResource(R.drawable.button_saturday);
	       	btn_holiday.setBackgroundResource(R.drawable.button_holiday_on);
	    }
	}

	//全表示、現在時刻のボタン色を変える
	public void setShowButtonColor(){
		if(Parent1.result_all == true){
			btn_all.setBackgroundResource(R.drawable.button_weekday_on);
			btn_bytime.setBackgroundResource(R.drawable.button_holiday);
		}else{
			btn_all.setBackgroundResource(R.drawable.button_weekday);
			btn_bytime.setBackgroundResource(R.drawable.button_holiday_on);
		}
	}

	//情報再取得
	public void send_url(){
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
                Log.d("Child1_result", "network_error");
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
        /*public void run() {

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
                Log.d("Child1_result", "network_error");
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

	                		try{	//JSONObjectから各項目を取得
		                		Parent1.timetables[i] = new Timetable(time.getInt("arr_time"),
										time.getInt("dep_time"),
										time.getString("via"),
										time.getString("detail"));
	                		}catch(JSONException e){	//経由地が無くてエラーが出た場合はこっちでキャッチ
		                		Parent1.timetables[i] = new Timetable(time.getInt("arr_time"),
										time.getInt("dep_time"),
										"",	//viaの文字列を空に
										time.getString("detail"));
	                		}

	                	}

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
        }*/
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
                	//エラーが無く通信が終わればアダプタを作る
                	make_adapter(Parent1.result_all);
                }else{
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

    //お気に入りに追加ボタンが押された時
    public void addBookmark() throws JSONException{
		/*SharedPreferences sharedpreferences = getSharedPreferences("Nbus_Android", Activity.MODE_PRIVATE);
		int sum = sharedpreferences.getInt("DataSum", 0);	//お気に入りの総数を取得
		JSONObject root = new JSONObject();
		JSONArray route_array = new JSONArray();
		JSONObject route = new JSONObject();
		Boolean overlap = false; 	//重複してるかフラグ
		if(sum!=0){	//データが有る時、データをチェックして今回の登録が二重登録とならないかチェックしてから追加
			String str_json = sharedpreferences.getString("JSON", "");
			root = new JSONObject(str_json);
			route_array = root.getJSONArray("route");
			for(int i=0; i<route_array.length(); i++){
				route = route_array.getJSONObject(i);
				String fav_arr = route.getString("arr");
				String fav_dep = route.getString("dep");
				if(Parent1.geton_name.equals(fav_arr) && Parent1.getoff_name.equals(fav_dep)){	//重複してた
					overlap = true;
				}
			}
			if(overlap){	//重複してたら
				Toast.makeText(getApplicationContext(), "この経路はすでに登録されています。", Toast.LENGTH_SHORT).show();
			}else{			//重複してなかったらデータを用意
				route = new JSONObject();
				route.put("arr", Parent1.geton_name);
				route.put("dep", Parent1.getoff_name);
				route_array.put(route);
				root.put("route", route_array);
				sum++;
			}
		}else{	//お気に入りが未登録の時、データを用意
			route.put("arr", Parent1.geton_name);
			route.put("dep", Parent1.getoff_name);
			route_array.put(route);
			root.put("route", route_array);
			sum++;
		}

		if(!overlap){	//重複して無ければお気に入りに保存
			String str_json = root.toString();
			//Log.e("result", str_json);
			SharedPreferences.Editor editor = sharedpreferences.edit();
			editor.putInt("DataSum", sum);
			editor.putString("JSON", str_json);
			editor.commit();
			Toast.makeText(getApplicationContext(), "お気に入りに登録しました。", Toast.LENGTH_SHORT).show();
		}

		 */
		/*debug
		SharedPreferences aa = getSharedPreferences("Nbus_Android", Activity.MODE_PRIVATE);
		String j = aa.getString("JSON", "non");
		int s = aa.getInt("DataSum", 0);
		Log.d("asd", "json="+j+" sum="+String.valueOf(s));
		*/

    	FavoriteDBAccess dbAccess = new FavoriteDBAccess(this);

    	//重複チェック
    	if(dbAccess.IsDepulicate(Parent1.geton_id, Parent1.getoff_id, Parent1.company_id)){
    		Toast.makeText(getApplicationContext(), "この経路はすでに登録されています。", Toast.LENGTH_SHORT).show();
    		return;
    	}

    	dbAccess.addFavorite(Parent1.geton_id, Parent1.result_geton_name, Parent1.result_geton_ruby, Parent1.getoff_id, Parent1.getoff_name, Parent1.result_getoff_ruby, Parent1.company_id, Parent1.company_name);
    	Toast.makeText(getApplicationContext(), "お気に入りに登録しました。", Toast.LENGTH_SHORT).show();
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