package jp.nbus;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;

import jp.nbus.R;
import jp.nbus.dto.BusStopDto;
import jp.nbus.dto.PathDto;
import jp.nbus.dto.TimetableDto;
import jp.nbus.dto.TimetableHolderDto;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

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
	/**
	 * 時刻表情報DTO
	 */
	public TimetableHolderDto timetableHolderDto;
	/**
	 * 通信中のダイアログ
	 */
	private ProgressDialog dialog;
	private ListView listview;
	private TextView title;

	//通信中に発生するエラーについて
	private Boolean net_error = false;	//エラーが起きているか
	private String error_message = "";	//エラーメッセージ

	//nbus.jpから帰って来るエラーについて
	private int json_error;	//エラー起きてないか
	private String json_error_reason;	//起きてた場合、理由は何か
	private Boolean isEmptyAdapter = false; //該当する経路がなかった場合

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
        makeAdapter();	//アダプタを作ってセット

        //リストのアイテムが選択された時
		listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				if (!isEmptyAdapter) {
					// ng.phpからの結果が空になっているときはタップしても実行しないようにする
					// クリックされたアイテムを取得します
					PathDto path = ParentSearch.path.get(position);
					ParentSearch.company_id = path.co;
					ParentSearch.companyName = path.co_name;
					ParentSearch.geton_id = path.fm.id;
					ParentSearch.getoff_id = path.to.id;
					// result_weekは別途設定済み
					// 画面遷移先Activityのtitlebarで表示する用のString
					ParentSearch.route = path.fm.name + "→" + path.to.name;

					sendUrl();// リクエストを送る
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
        makeAdapter();

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
	public void makeAdapter(){
		//アダプタを初期化
        adapter = new ArrayAdapter<String>(this, R.layout.row, R.id.row_textview1);
        ParentSearch.result_time_position = 0;	//選ばれた項目が何番目か
        isEmptyAdapter = false;//空の結果を受けとってないと仮定。実際にはあとでadapterの数から把握する。

		String fmStopName;
		String toStopName;
		String companyName;
		for (PathDto path : ParentSearch.path) {
			fmStopName = path.fm.name;
			toStopName = path.to.name;
			companyName = path.co_name;
			adapter.add(" " + fmStopName + "→" + toStopName + " ("
					+ companyName + ")");
		}
		if (adapter.getCount() == 0) { // 要素が無い時(経路がないとき)
			isEmptyAdapter = true;// 結果が空である
			adapter.add("  指定された経路が見つかりませんでした。");
		} else if (adapter.getCount() == 1) {
			// 一つしかアイテムが設定されていない場合、自動で時刻表検索画面に飛ばす
			PathDto path = ParentSearch.path.get(0);
			ParentSearch.company_id = path.co;
			ParentSearch.geton_id = path.fm.id;
			ParentSearch.getoff_id = path.to.id;
			// result_weekはonCreateで別途設定済み…
			// 画面遷移先Activityのtitlebarで表示する用のString
			ParentSearch.route = path.fm.name + "→" + path.to.name;

			sendUrl();// リクエストを送る

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

        	String strJson = null;	//通信して取得したJSONな文字列、後でJSONObjectに変換される
        	HttpClient httpClient = new DefaultHttpClient();

    		//URLを生成
            StringBuilder uri = new StringBuilder("http://nbus.jp/ttm.php?fm=");
            uri.append(ParentSearch.geton_id);
            uri.append("&to=");
            uri.append(ParentSearch.getoff_id);
            uri.append("&wk=");
            uri.append(String.valueOf(ParentSearch.result_week));
            uri.append("&co=");
            uri.append(String.valueOf(ParentSearch.company_id));
            uri.append("&v=cacao");


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
                    strJson = outputStream.toString(); // JSONデータ

                } catch (Exception e) {
                      Log.d("Child1_result", "error_httpResponse");
                }


                //マッピング
				ObjectMapper mapper = new ObjectMapper();
				try {
					timetableHolderDto = mapper.readValue(strJson, TimetableHolderDto.class);
				} catch (JsonParseException e) {
					Log.d("Child1_search", "JsonParseException");
					e.printStackTrace();
				} catch (JsonMappingException e) {
					Log.d("Child1_search", "JsonMappingException");
					e.printStackTrace();
				} catch (IOException e) {
					Log.d("Child1_search", "IOException");
					e.printStackTrace();
				}
				ParentSearch.timetableHolder = timetableHolderDto;
            	ParentSearch.fmName = timetableHolderDto.fm.name;
            	ParentSearch.fmRuby = timetableHolderDto.fm.ruby;
            	ParentSearch.toName = timetableHolderDto.to.name;
            	ParentSearch.toRuby = timetableHolderDto.to.ruby;

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
