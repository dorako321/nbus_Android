package jp.nbus;

import java.io.ByteArrayOutputStream;
import java.util.Calendar;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
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

public class Child3_list extends Activity{

	private int favorite_sum;	//お気に入りの登録件数
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
        setContentView(R.layout.child3_list);

        //タイトルバー
        LinearLayout titlebar = (LinearLayout)findViewById(R.id.child3_list_titlebar);
        titlebar.setLayoutParams(new LinearLayout.LayoutParams(Parent1.disp_width, (int)(Parent1.disp_height/10)));

     /*   //Editボタン
        Button btn_edit = (Button)findViewById(R.id.child2_bookmark_edit);
        btn_edit.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				if(favorite_sum==0){	//お気に入りが無い場合
					Toast.makeText(getApplicationContext(), "時刻表の検索結果画面からお気に入りの経路を登録してください。", Toast.LENGTH_LONG).show();
				}else{					//お気に入りがある場合、Edit画面に遷移
					Parent2 parentActivity = ((Parent2)getParent());
			        parentActivity.showChild_edit();
				}
			}
        }); */

        //リストビューが入るレイアウト
        LinearLayout content = (LinearLayout)findViewById(R.id.child3_list_content);
        content.setLayoutParams(new LinearLayout.LayoutParams(Parent1.disp_width, (int)(Parent1.disp_height/10*6)));

        //リストビュー
        listview = (ListView)findViewById(R.id.child3_list_listview);

        //SharedPreferencesからアダプタを作ってリストビューにセットする
        make_adapter();

        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            	if(Parent3.idms.length!=0){
            		Parent3 parentActivity = ((Parent3)getParent());
            		parentActivity.showChild_detail(Parent3.idms[position][1]);
            	}
            }
        });


        //NFC
        smartCardAccess = new SmartCardAccess();
		smartCardAccess.initialize(this);
	}


	@Override	//他アクティビティから戻ってきた時とかに呼ばれる、アダプタを作り直す
	protected void onResume() {
		Log.d("Child3_list", "onResume");
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
		/*
		//アダプタを初期化
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.row, R.id.row_textview1);
		//SQLiteから
		SmartCardHistoryManager smartCardHistoryManager = new SmartCardHistoryManager(this, 1);
		SQLiteDatabase db = smartCardHistoryManager.getReadableDatabase();
		String[][] idms = smartCardHistoryManager.idmList(db);

		int idms_sum = idms.length;

		//SharedPreferencesから情報を読み込む
        SharedPreferences sharedpreference = getSharedPreferences("Nbus_Android", Activity.MODE_PRIVATE);
        favorite_sum = sharedpreference.getInt("DataSum", 0);	//データ数を取得
        String str_json = "";

        //Log.e("Child2_bookmark", String.valueOf(favorite_sum));

        if(idms_sum == 0){	//データが無い時
        	adapter.add("カードの履歴が登録されていません。");
        }else{								//データがある時
        	try {
				for (String[] idm : idms){
					adapter.add("IDm:"+idm[1]);

				}
			} catch (Exception e) {
				e.printStackTrace();
			}
        }
        listview.setAdapter(adapter); */
		SmartCardAsyncTask async = new SmartCardAsyncTask();
		async.backgroundQueryIDm(this, listview);
	}
	/*
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
	}*/
/*
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
					Log.d("Child2_bookmark", "error_JSONObject");
				}
            } else {
                Log.d("Child2_bookmark", "Status" + status);
                //return;	returnすると繋がるまで何回でも繰り返してダイアログが終わらない
            }

            //-----[読み込み終了の通知]
            handler.sendEmptyMessage(0);
        }
    } */

	//通信終了後に呼ばれる
	/*
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
	*/
	@Override
    public void onPause(){
		super.onPause();
		//本当にActivityがPauseしているのか判定
		if (this.isFinishing()){
			smartCardAccess.disableDetection(this);
		}
	}

}
