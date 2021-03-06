package jp.nbus;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;

import jp.nbus.dto.BusStopDto;
import jp.nbus.dto.FavoriteRoutesAshDto;
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

public class FavoriteForm extends Activity{

	private ListView listview;
	/**
	 * 時刻表情報DTO
	 */
	public TimetableHolderDto timetableHolderDto;
	/**
	 * 経路情報DTO
	 */
	ArrayList<PathDto> pathDto;
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
        titlebar.setLayoutParams(new LinearLayout.LayoutParams(ParentSearch.disp_width, (int)(ParentSearch.disp_height/10)));

        //Editボタン
        Button btn_edit = (Button)findViewById(R.id.child2_bookmark_edit);
        btn_edit.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				if(ParentSearch.favoriteroutes.length==0){	//お気に入りが無い場合
					Toast.makeText(getApplicationContext(), "時刻表の検索結果画面からお気に入りの経路を登録してください。", Toast.LENGTH_LONG).show();
				}else{					//お気に入りがある場合、Edit画面に遷移
					ParentFavorite parentActivity = ((ParentFavorite)getParent());
			        parentActivity.showChild_edit();
				}
			}
        });

        //リストビューが入るレイアウト
        LinearLayout content = (LinearLayout)findViewById(R.id.child2_bookmark_content);
        content.setLayoutParams(new LinearLayout.LayoutParams(ParentSearch.disp_width, (int)(ParentSearch.disp_height/10*6)));

        //リストビュー
        listview = (ListView)findViewById(R.id.child2_bookmark_listview);

        //SharedPreferencesからアダプタを作ってリストビューにセットする
        make_adapter();

        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // クリックされたアイテムを取得します
            	if(ParentSearch.favoriteroutes.length == 0){	//お気に入りが0件の時は何もしない
            	}else{				//お気に入りがあれば時刻表取得
            		if (ParentSearch.favoriteroutes[position].isSearchFavorite) {
						//停留所名のみ保持されているとき
            			//Child1_selectに遷移
            			ParentSearch.fmFormName = ParentSearch.favoriteroutes[position].fm_name;
            			ParentSearch.toFormName = ParentSearch.favoriteroutes[position].to_name;

            			send_neighbor();
					} else {
						//停留所idなどが保持されているとき
						//Child1_resultに遷移
	            		ParentSearch.geton_id = ParentSearch.favoriteroutes[position].fm_id;
	            		ParentSearch.getoff_id = ParentSearch.favoriteroutes[position].to_id;
	            		ParentSearch.company_id = ParentSearch.favoriteroutes[position].co;
            			ParentSearch.fmFormName = ParentSearch.favoriteroutes[position].fm_name;
            			ParentSearch.toFormName = ParentSearch.favoriteroutes[position].to_name;
	            		ParentSearch.route = ParentSearch.fmFormName+"→"+ParentSearch.toFormName;	//タイトルバー用経路文字列
	            		ParentSearch.result_all = false;
	            		Calendar calendar = Calendar.getInstance();
	            		int week = calendar.get(Calendar.DAY_OF_WEEK)-1;	//曜日取得
	            		if(week==0){	//日曜
	            			ParentSearch.result_week = 2;
	            		}else if(week==6){	//土曜
	            			ParentSearch.result_week = 1;
	            		}else{	//平日
	            			ParentSearch.result_week = 0;
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


		FavoriteDBAccess dbAccess = new FavoriteDBAccess(this);
		ParentSearch.favoriteroutes = dbAccess.readFavorites();


        //Log.e("Child2_bookmark", String.valueOf(favorite_sum));

        if(ParentSearch.favoriteroutes.length == 0){	//データが無い時
        	adapter.add("お気に入りの経路が登録されていません。時刻表の検索結果画面からお気に入りを登録してください。");
        }else{
        	int length=ParentSearch.favoriteroutes.length;
        	for (int i = 0; i < length; i++) {
				FavoriteRoutesAshDto favRoute= ParentSearch.favoriteroutes[i];
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
					+ ParentSearch.fmFormName + "&to=" + ParentSearch.toFormName);

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
				} catch (Exception e) {
					Log.d("Child1_search", "error_httpResponse(ng)");
				}

                //マッピング
				ObjectMapper mapper = new ObjectMapper();
				try {
					pathDto = mapper.readValue(str_json, new TypeReference<ArrayList<PathDto>>() {});
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
				ParentSearch.path = pathDto;

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

        	String strJson = "";	//通信して取得したJSONな文字列、後でJSONObjectに変換される
        	HttpClient httpClient = new DefaultHttpClient();

    		//URLを生成
            StringBuilder uri = new StringBuilder("http://nbus.jp/ttm.php?fm="
            										+ParentSearch.geton_id+"&to="
            										+ParentSearch.getoff_id+"&wk="
            										+String.valueOf(ParentSearch.result_week)+"&co="
            										+String.valueOf(ParentSearch.company_id)
            										+"&v=cacao");

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


	@Override
    public void onPause(){
		super.onPause();
		//本当にActivityがPauseしているのか判定
		if (this.isFinishing()){
			smartCardAccess.disableDetection(this);
		}
	}

}
