package jp.nbus;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import jp.nbus.R;
import jp.nbus.dto.TimetableDto;
import jp.nbus.dto.TimetableHolderDto;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
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

public class SearchResult extends Activity {

	private ArrayAdapter<String> adapter;
	/**
	 * 時刻表情報DTO
	 */
	public TimetableHolderDto timetableHolderDto;
	/**
	 * 通信中ダイアログ
	 */
	private ProgressDialog dialog;
	private ListView listview;
	private TextView title;

	private Button btn_weekday, btn_saturday, btn_holiday, btn_all, btn_bytime;

	private Boolean isEndBus = false; // 今日のバス、もう終わってない？

	// 通信中に発生するエラーについて
	private Boolean net_error = false; // エラーが起きているか
	private String error_message = ""; // エラーメッセージ

	// nbus.jpから帰って来るエラーについて
	private int json_error; // エラー起きてないか
	private String json_error_reason; // 起きてた場合、理由は何か

	private SmartCardAccess smartCardAccess;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE); // アプリのタイトルバーを表示しない
		setContentView(R.layout.child1_result);
		Log.d("Child1_result", "onCreate");

		// タイトルバー
		LinearLayout titlebar = (LinearLayout) findViewById(R.id.child1_result_titlebar);
		titlebar.setLayoutParams(new LinearLayout.LayoutParams(
				ParentSearch.disp_width,
				(int) (ParentSearch.disp_height / 10 * 1.2)));
		title = (TextView) findViewById(R.id.child1_result_title_text);
		title.setText(ParentSearch.route);

		// 曜日設定ボタン
		btn_weekday = (Button) findViewById(R.id.button_weekday); // 平日
		btn_saturday = (Button) findViewById(R.id.button_saturday); // 土曜
		btn_holiday = (Button) findViewById(R.id.button_holiday); // 日祝
		int button_width = (int) ((ParentSearch.disp_width - 20 * ParentSearch.dip_scale) / 3); // ボタンのサイズを決める
		btn_weekday.setWidth(button_width);
		btn_saturday.setWidth(button_width);
		btn_holiday.setWidth(button_width);
		btn_weekday.setOnClickListener(new OnClickListener() { // 平日
					@Override
					public void onClick(View arg0) {
						ParentSearch.result_week = 0; // 曜日情報を書き換え
						setDayButtonColor();
						sendUrl(); // 情報を再取得
						// send_url();で通信が終わったらhandlerが呼ばれてアダプタを再構築→セットされる
					}
				});
		btn_saturday.setOnClickListener(new OnClickListener() { // 土曜
					@Override
					public void onClick(View v) {
						ParentSearch.result_week = 1;
						setDayButtonColor();
						sendUrl();
						// send_url();で通信が終わったらhandlerが呼ばれてアダプタを再構築→セットされる
					}
				});
		btn_holiday.setOnClickListener(new OnClickListener() { // 日曜
					@Override
					public void onClick(View v) {
						ParentSearch.result_week = 2;
						setDayButtonColor();
						sendUrl();
						// send_url();で通信が終わったらhandlerが呼ばれてアダプタを再構築→セットされる
					}
				});

		// 全表示・現在時刻
		btn_all = (Button) findViewById(R.id.button_all);
		btn_bytime = (Button) findViewById(R.id.button_bytime);
		button_width = (int) ((ParentSearch.disp_width - 20 * ParentSearch.dip_scale) / 2);
		btn_all.setWidth(button_width);
		btn_bytime.setWidth(button_width);
		btn_all.setOnClickListener(new OnClickListener() { // 全表示
			@Override
			public void onClick(View v) {
				ParentSearch.result_all = true; // 全表示フラグを設定
				setShowButtonColor();
				make_adapter(ParentSearch.result_all); // もう一度リストビューのアダプタを作り直す
			}
		});
		btn_bytime.setOnClickListener(new OnClickListener() { // 現在時刻
					@Override
					public void onClick(View v) {
						ParentSearch.result_all = false;
						setShowButtonColor();
						make_adapter(ParentSearch.result_all);
					}
				});

		// ボタンの色を選択してるっぽく変更
		setDayButtonColor(); // 平日、土曜、日祝ボタン
		setShowButtonColor(); // 全表示、現在時刻ボタン

		// リストビュー
		listview = (ListView) findViewById(R.id.listView1);
		listview.getLayoutParams().height = (int) (ParentSearch.disp_height * 0.5); // リストビューの高さを設定
		listview.requestLayout(); // 設定を適用
		make_adapter(ParentSearch.result_all); // アダプタを作ってセット

		// リストのアイテムが選択された時
		listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				// クリックされたアイテムを取得します
				if (ParentSearch.result_all) { // 全表示なら
					ParentSearch.detail_selected_item = position;
					ParentSearch parentActivity = ((ParentSearch) getParent());
					parentActivity.showChild_detail();
				} else { // 現在時刻からの表示なら
					if (!isEndBus) { // もう今日はバスが無いフラグが上がって無ければ表示
						ParentSearch.detail_selected_item = ParentSearch.result_time_position
								+ position;
						ParentSearch parentActivity = ((ParentSearch) getParent());
						parentActivity.showChild_detail();
					}
				}
			}
		});

		// お気に入りに追加ボタン
		ImageButton btn_addbookmark = (ImageButton) findViewById(R.id.button_result_addbookmark);
		btn_addbookmark.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				new AlertDialog.Builder(getParent())
						// ダイアログの設定
						.setMessage("お気に入りに登録しますか？")
						.setCancelable(false)
						.setPositiveButton("Cancel",
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog,
											int id) {
										dialog.cancel(); // キャンセル
									}
								})
						.setNegativeButton("OK",
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog,
											int id) {
										try {
											addBookmark(); // お気に入りに登録
										} catch (JSONException e) {
											Log.d("Child1_result",
													"Error_at_addBookmark()");
											e.printStackTrace();
										}
									}
								}).show();
			}
		});

		// NFC
		smartCardAccess = new SmartCardAccess();
		smartCardAccess.initialize(this);

	}

	// バックキーイベントをParent1じゃなくてこっちが受け取る場合がある
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			ParentSearch parentActivity = ((ParentSearch) getParent());
			parentActivity.showChild_search();
			return true;
		}
		return false;
	}

	@Override
	// 他アクティビティから戻ってきた時とかに呼ばれる、リストビューを更新する。
	protected void onResume() {
		super.onResume();
		Log.d("Child1_result", "onResume");

		// タイトルバーも変更
		title.setText(ParentSearch.route);
		// ボタンの色を選択してるっぽく変更
		setDayButtonColor();
		setShowButtonColor();
		// リストビュー
		make_adapter(ParentSearch.result_all);

		if (Nbus_AndroidActivity.select_bookmark == true) {
			Nbus_AndroidActivity.select_bookmark = false;
		}
		if (Nbus_AndroidActivity.select_stopname == true) { // 停留所名のブックマーク選択でタブ切り替えが起きた時は結果表示に飛ばす
			ParentSearch parentActivity = ((ParentSearch) getParent());
			parentActivity.showChild_select();
			Nbus_AndroidActivity.select_stopname = false;
		}

		smartCardAccess.enableDetection(this);
	}

	// JSONObjectからアダプターにデータを入れる
	public void make_adapter(Boolean all) {
		// アダプタを初期化
		adapter = new ArrayAdapter<String>(this, R.layout.row,
				R.id.row_textview1);
		// 選ばれた項目が何番目か
		ParentSearch.result_time_position = 0;
		// 現在時刻
		int intTime = getIntTIme();
		int i = 0;
		isEndBus = false;
		for(TimetableDto timetable : ParentSearch.timetableHolder.timetable){
			if(all){
				//全て表示
				adapter.add(" " + timetable.getFmTime()
						+ " → " + timetable.getToTime());

			}else if(timetable.fmTime > intTime){
				//現在時刻以降を表示
				adapter.add(" " + timetable.getFmTime()
						+ " → " + timetable.getToTime());
			}else{
				// 現在時刻より前で何回iが回ったか+1
				ParentSearch.result_time_position = i + 1;
			}
			i++;
		}

        if(adapter.getCount()==0){	//要素が無い時(最終が終わっている時)
        	isEndBus = true;
        	adapter.add("  選択された経路での本日の運行は終了しました。");
        }

		listview.setAdapter(adapter);
		listview.invalidateViews();
	}

	/**
	 * int形式による現在時刻の取得
	 *
	 * @return
	 */
	private int getIntTIme() {
		int intTime;
		Time time = new Time("Asia/Tokyo"); // 時間を取得
		time.setToNow();
		intTime = time.hour * 60 + time.minute; // 今日が始まってから何分、の単位で保存
		return intTime;
	}

	// 平日、土曜、日祝のボタン色を変える
	public void setDayButtonColor() {
		if (ParentSearch.result_week == 0) {
			btn_weekday.setBackgroundResource(R.drawable.button_weekday_on);
			btn_saturday.setBackgroundResource(R.drawable.button_saturday);
			btn_holiday.setBackgroundResource(R.drawable.button_holiday);
		} else if (ParentSearch.result_week == 1) {
			btn_weekday.setBackgroundResource(R.drawable.button_weekday);
			btn_saturday.setBackgroundResource(R.drawable.button_saturday_on);
			btn_holiday.setBackgroundResource(R.drawable.button_holiday);
		} else if (ParentSearch.result_week == 2) {
			btn_weekday.setBackgroundResource(R.drawable.button_weekday);
			btn_saturday.setBackgroundResource(R.drawable.button_saturday);
			btn_holiday.setBackgroundResource(R.drawable.button_holiday_on);
		}
	}

	// 全表示、現在時刻のボタン色を変える
	public void setShowButtonColor() {
		if (ParentSearch.result_all == true) {
			btn_all.setBackgroundResource(R.drawable.button_weekday_on);
			btn_bytime.setBackgroundResource(R.drawable.button_holiday);
		} else {
			btn_all.setBackgroundResource(R.drawable.button_weekday);
			btn_bytime.setBackgroundResource(R.drawable.button_holiday_on);
		}
	}

	// 情報再取得
	public void sendUrl() {
		// -----[ダイアログの設定]
		dialog = new ProgressDialog(getParent()); // 通常はnew
													// ProgressDialog(this)だがタブ内なのでgetParent()
		dialog.setTitle("通信中");
		dialog.setMessage("時刻表取得中");
		dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		dialog.show();

		// -----[ローディングの描画は別スレッドで行う]
		Thread thread = new Thread(new Progress());
		thread.start();
	}

	// ダイアログが出てる間に通信
	private class Progress implements Runnable {
		public void run() {

			String strJson = null; // 通信して取得したJSONな文字列、後でJSONObjectに変換される
			HttpClient httpClient = new DefaultHttpClient();

			// URLを生成
			StringBuilder uri = new StringBuilder("http://nbus.jp/ttm.php?fm="
					+ ParentSearch.geton_id + "&to=" + ParentSearch.getoff_id
					+ "&wk=" + String.valueOf(ParentSearch.result_week)
					+ "&co=" + String.valueOf(ParentSearch.company_id)
					+ "&v=cacao");

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
				// return; returnすると繋がるまで何回でも繰り返してダイアログが終わらない
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
				// return; returnすると繋がるまで何回でも繰り返してダイアログが終わらない
			}

			// -----[読み込み終了の通知]
			handler.sendEmptyMessage(0);
		}
	}

	// 通信終了後に呼ばれる
	private Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			// -----[ダイアログを閉じる]
			dialog.dismiss();
			if (net_error) { // 通信に失敗していた場合
				net_error = false; // フラグ下ろす
				Toast.makeText(getApplicationContext(), error_message,
						Toast.LENGTH_LONG).show();
			} else { // 通信には成功していた場合
				if (json_error == 0) { // 正常に時刻表が取得できてたら
					// エラーが無く通信が終わればアダプタを作る
					make_adapter(ParentSearch.result_all);
				} else {
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

	// お気に入りに追加ボタンが押された時
	public void addBookmark() throws JSONException {
		FavoriteDBAccess dbAccess = new FavoriteDBAccess(this);

		// 重複チェック
		if (dbAccess.IsDepulicate(ParentSearch.geton_id,
				ParentSearch.getoff_id, ParentSearch.company_id)) {
			Toast.makeText(getApplicationContext(), "この経路はすでに登録されています。",
					Toast.LENGTH_SHORT).show();
			return;
		}

		dbAccess.addFavorite(ParentSearch.geton_id,
				ParentSearch.fmName, ParentSearch.fmRuby,
				ParentSearch.getoff_id, ParentSearch.toName,
				ParentSearch.toRuby, ParentSearch.company_id,
				ParentSearch.companyName);
		Toast.makeText(getApplicationContext(), "お気に入りに登録しました。",
				Toast.LENGTH_SHORT).show();
	}

	@Override
	public void onPause() {
		super.onPause();
		// 本当にActivityがPauseしているのか判定
		if (this.isFinishing()) {
			smartCardAccess.disableDetection(this);
		}
	}
}
