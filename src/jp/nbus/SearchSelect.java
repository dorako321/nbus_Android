package jp.nbus;

import java.io.ByteArrayOutputStream;
import java.util.Calendar;

import jp.nbus.R;
import jp.nbus.dto.BusStopDto;
import jp.nbus.dto.TimetableDto;

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
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class SearchSelect extends Activity {
	private ArrayAdapter<String> adapter;
	private ProgressDialog dialog;	//通信中ダイアログ
	private ListView listview;
	private TextView title;

	//通信中に発生するエラーについて
	private Boolean net_error = false;	//エラーが起きているか
	private String error_message = "";	//エラーメッセージ

	//nbus.jpから帰って来るエラーについて
	private int json_error;	//エラー起きてないか
	private String json_error_reason;	//起きてた場合、理由は何か
	private Boolean emptyResult = false; //該当する経路がなかった場合

	private SmartCardAccess smartCardAccess;

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);	//アプリのタイトルバーを表示しない
        setContentView(R.layout.child1_select);
        Log.d("Child1_result", "onCreate");

        //タイトルバー
        LinearLayout titlebar = (LinearLayout)findViewById(R.id.child1_select_titlebar);
        titlebar.setLayoutParams(new LinearLayout.LayoutParams(ParentSearch.disp_width, (int)(ParentSearch.disp_height/10*1.2)));
        title = (TextView)findViewById(R.id.child1_select_title_text);
        title.setText(ParentSearch.route);


        //リストビュー
        listview = (ListView)findViewById(R.id.listView1);
        listview.getLayoutParams().height = (int)(ParentSearch.disp_height*0.5);	//リストビューの高さを設定
        listview.requestLayout();											//設定を適用
        make_adapter();	//アダプタを作ってセット

        //リストのアイテムが選択された時
        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if(!emptyResult){//ng.phpからの結果が空になっているときはタップしても実行しないようにする
            	// クリックされたアイテムを取得します
	                BusStopDto stop = ParentSearch.busstops[position];
	                ParentSearch.company_id = stop.companyId;
	                ParentSearch.company_name = stop.companyName;
	                ParentSearch.geton_id = stop.fmId;
	                ParentSearch.getoff_id = stop.toId;
	                //result_weekは別途設定済み
	                ParentSearch.route = stop.fmName + "→" + stop.toName;//画面遷移先Activityのtitlebarで表示する用のString

	                sendUrl();//リクエストを送る
                }
            }
        });

        //ttm.phpへのリクエストの際に必要な曜日番号をresult_weekに設定する
		Calendar calendar = Calendar.getInstance();
		int week = calendar.get(Calendar.DAY_OF_WEEK)-1;	//曜日取得
		if(week==0){	//日曜
			ParentSearch.result_week = 2;
		}else if(week==6){	//土曜
			ParentSearch.result_week = 1;
		}else{	//平日
			ParentSearch.result_week = 0;
		}

        //NFC
        smartCardAccess = new SmartCardAccess();
		smartCardAccess.initialize(this);

	}

	//バックキーイベントをParent1じゃなくてこっちが受け取る場合がある
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if(keyCode==KeyEvent.KEYCODE_BACK){
			ParentSearch parentActivity = ((ParentSearch)getParent());
	        parentActivity.showChild_search();
			return true;
		}
		return false;
	}

	@Override	//他アクティビティから戻ってきた時とかに呼ばれる、リストビューを更新する。
	protected void onResume() {
		super.onResume();
		Log.d("Child1_select", "onResume");

		//タイトルバーも変更
		title.setText(ParentSearch.route);

		//リストビュー
        make_adapter();

		if(Nbus_AndroidActivity.select_bookmark == true){	//ブックマーク選択でタブ切り替えが起きた時は結果表示に飛ばす
			ParentSearch parentActivity = (ParentSearch)getParent();
	        parentActivity.showChild_result();
	        Nbus_AndroidActivity.select_bookmark = false;
		}
		if(Nbus_AndroidActivity.select_stopname == true){	//停留所名のブックマーク選択でタブ切り替えが起きた時は結果表示に飛ばす
	        Nbus_AndroidActivity.select_stopname = false;
		}

		smartCardAccess.enableDetection(this);
	}

	//JSONObjectからアダプターにデータを入れる
	public void make_adapter(){
		//アダプタを初期化
        adapter = new ArrayAdapter<String>(this, R.layout.row, R.id.row_textview1);
        ParentSearch.result_time_position = 0;	//選ばれた項目が何番目か
        emptyResult = false;//空の結果を受けとってないと仮定。実際にはあとでadapterの数から把握する。

    	String fromStopName;
    	String toStopName;
    	String companyName;
        	for(int i=0; i<ParentSearch.busstops.length; i++){
            	fromStopName = ParentSearch.busstops[i].fmName;
            	toStopName = ParentSearch.busstops[i].toName;
        		companyName = ParentSearch.busstops[i].companyName;
            	adapter.add(" "+fromStopName+"→"+toStopName+" ("+companyName+")");
            }
            if(adapter.getCount()==0){	//要素が無い時(経路がないとき)
            	emptyResult = true;//結果が空である
            	adapter.add("  指定された経路が見つかりませんでした。");
            }else if(adapter.getCount()==1){
            	//一つしかアイテムが設定されていない場合、自動で時刻表検索画面に飛ばす
                BusStopDto stop = ParentSearch.busstops[0];
                ParentSearch.company_id = stop.companyId;
                ParentSearch.geton_id = stop.fmId;
                ParentSearch.getoff_id = stop.toId;
                //result_weekはonCreateで別途設定済み…

                ParentSearch.route = stop.fmName + "→" + stop.toName;//画面遷移先Activityのtitlebarで表示する用のString

                sendUrl();//リクエストを送る

            }
        listview.setAdapter(adapter);
        listview.invalidateViews();
	}



	//情報再取得
	public void sendUrl(){
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
	public class Progress implements Runnable {
        public void run() {

        	String str_json = "";	//通信して取得したJSONな文字列、後でJSONObjectに変換される
        	JSONObject rootObject;
        	JSONArray json_timetables;

        	HttpClient httpClient = new DefaultHttpClient();

    		//URLを生成
            StringBuilder uri = new StringBuilder("http://nbus.jp/ttm.php?fm="
            										+ParentSearch.geton_id+"&to="
            										+ParentSearch.getoff_id+"&wk="
            										+String.valueOf(ParentSearch.result_week)+"&co="
            										+String.valueOf(ParentSearch.company_id));

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
	                	ParentSearch.timetables = new TimetableDto[lenght];	//JSONArrayのサイズで配列を作り直す
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
		                		ParentSearch.timetables[i] = new TimetableDto(time.getInt("arr_time"),
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
	                	ParentSearch.result_geton_name = from.getString("name");
	                	ParentSearch.result_geton_ruby = from.getString("ruby");
	                	JSONObject to = rootObject.getJSONObject("to");
	                	ParentSearch.result_getoff_name = to.getString("name");
	                	ParentSearch.result_getoff_ruby = to.getString("ruby");

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

	//通信終了後に呼ばれる
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            //-----[ダイアログを閉じる]
            dialog.dismiss();
            if(net_error){	//通信に失敗していた場合
              	net_error = false;	//フラグ下ろす
            	Toast.makeText(getApplicationContext(), error_message, Toast.LENGTH_LONG).show();
            	return;
            }

            //通信に失敗していた場合
			if (json_error != 0) {
				// エラーダイアログ
				new AlertDialog.Builder(getParent())
						.setTitle("エラー")
						.setMessage(json_error_reason)
						.setCancelable(false)
						.setPositiveButton("OK",
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog,
											int id) {
										dialog.cancel();
									}
								}).show();
			}
			//正常に取得
			ParentSearch parentActivity = ((ParentSearch) getParent()); // 画面遷移
			parentActivity.showChild_result();



        }
    };

	@Override
    public void onPause(){
		super.onPause();
		//本当にActivityがPauseしているのか判定
		if (this.isFinishing()){
			smartCardAccess.disableDetection(this);
		}
	}
}
