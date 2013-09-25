package jp.nbus;

import java.io.ByteArrayOutputStream;

import jp.nbus.R;
import jp.nbus.R.array;
import jp.nbus.R.id;
import jp.nbus.R.layout;
import jp.nbus.dto.BusStopDto;

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
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

public class SearchForm extends Activity {
	/**
	 * 乗車停留所入力欄
	 */
	private AutoCompleteTextView fmForm;
	/**
	 * 降車停留所入力欄
	 */
	private AutoCompleteTextView toForm;

	private ProgressDialog dialog; // 通信中ダイアログ

	// 通信中に発生するエラーについて
	private Boolean net_error = false; // エラーが起きているか
	private String error_message = ""; // エラーメッセージ

	// nbus.jpから帰って来るエラーについて
	private int json_error; // エラー起きてないか
	private String json_error_reason; // 起きてた場合、理由は何か

	private String[] BusStopName = null; // 停留所名の配列

	private SmartCardAccess smartCardAccess;

	/**
	 * テキストフォームの準備
	 * @param rId
	 * @param width
	 * @param hind
	 * @param th
	 * @return
	 */
	protected AutoCompleteTextView prepareTextForm(int rId, int width, String hind, int th){
		AutoCompleteTextView form = (AutoCompleteTextView) findViewById(rId);
		form.setWidth(width);
		form.setHint(hind);
		form.setThreshold(th);
		return form;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.child1_search);

		// タイトルバー(nbus.jpって表示するところ)
		LinearLayout titlebar = (LinearLayout) findViewById(R.id.child1_search_titlebar);
		titlebar.setLayoutParams(new LinearLayout.LayoutParams(
				ParentSearch.disp_width, (int) (ParentSearch.disp_height / 10)));

		// 乗車停留所入力欄
		int textFormWidth = (int) ((int)ParentSearch.disp_width * 0.7);
		fmForm = prepareTextForm(R.id.edit_geton, textFormWidth, "例：長崎駅前", 1);
		// もし一度入力したあとにもう一度この画面に戻ってきてたら前回入力データを入力
		if (!TextUtils.isEmpty(ParentSearch.fmName)) {
			fmForm.setText(ParentSearch.fmName);
		}

		// 降車停留所入力欄
		toForm = prepareTextForm(R.id.edit_getoff, textFormWidth, "例：昭和町", 1);
		// もし一度入力したあとにもう一度この画面に戻ってきてたら前回入力データを入力
		if (!TextUtils.isEmpty(ParentSearch.toName)) {
			toForm.setText(ParentSearch.toName);
		}


		// Android2.3以降なら自動補完を行う
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
			// 入力補完用に停留所名一覧を準備
			BusStopName = getResources().getStringArray(R.array.bus_stop_name);
			ArrayAdapter<String> adapter_bus = new ArrayAdapter<String>(this,
					R.layout.autocomplete_text, BusStopName);
			fmForm.setAdapter(adapter_bus); // 乗車
			toForm.setAdapter(adapter_bus); // 降車
		}

		// 検索ボタン
		Button btn_search = (Button) findViewById(R.id.button_search);
		btn_search.setWidth((int) (ParentSearch.disp_width * 0.8));
		btn_search.setHeight(ParentSearch.disp_height / 11);
		btn_search.setTextSize(20);


		btn_search.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				ParentSearch.result_all = false; // 結果表示画面では全表示ではなく現在時刻からの時刻表表示モードにしとく
				// 乗車停留所
				ParentSearch.fmName = trimSpace(fmForm.getText().toString());
				// 降車停留所
				ParentSearch.toName = trimSpace(toForm.getText().toString());

				if (TextUtils.isEmpty(ParentSearch.fmName)) {
					// 乗車停留所が未入力
					Toast.makeText(getApplicationContext(), "乗車停留所を入力してください。",
							Toast.LENGTH_SHORT).show();
					return;
				}

				if (TextUtils.isEmpty(ParentSearch.toName)) {
					// 降車停留所が未入力
					Toast.makeText(getApplicationContext(), "降車停留所を入力してください。",
							Toast.LENGTH_SHORT).show();
					return;
				}

				// どっちも入力してる
				ParentSearch.route = ParentSearch.fmName + "→"
						+ ParentSearch.toName;
				// ソフトウェアキーボードを閉じる
				closeSoftwareKeyboard(view);
				// APIを使ってデータ取得
				send_neighbor();

			}
			/**
			 * `ソフトウェアキーボードを閉じる
			 * @param view
			 */
			private void closeSoftwareKeyboard(View view) {
				InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
				inputMethodManager.hideSoftInputFromWindow(
						view.getWindowToken(), 0);
			}
			/**
			 * 空白を削除する
			 * @param str
			 * @return
			 */
			private String trimSpace(String str) {
				return str.replaceAll("　", "")
						.replaceAll(" ", "");
			}
		});
		smartCardAccess = new SmartCardAccess();
		smartCardAccess.initialize(this);

	}

	@Override
	// 他アクティビティから戻ってきた時とかに呼ばれる
	protected void onResume() {
		super.onResume();
		// Log.d("Child1_search", "onResume");
		if (Nbus_AndroidActivity.select_bookmark == true) { // ブックマーク選択でタブ切り替えが起きた時は結果表示に飛ばす
			ParentSearch parentActivity = (ParentSearch) getParent();
			parentActivity.showChild_result();
			Nbus_AndroidActivity.select_bookmark = false;
		}
		if (Nbus_AndroidActivity.select_stopname == true) { // 停留所名のブックマーク選択でタブ切り替えが起きた時は結果表示に飛ばす
			ParentSearch parentActivity = ((ParentSearch) getParent());
			parentActivity.showChild_select();
			Nbus_AndroidActivity.select_stopname = false;
		}

		// 長崎スマートカードの検出を開始する。(検出時はSmartCardAccessクラス内で指定しているアクティビティにintentをなげる)
		smartCardAccess.enableDetection(this);

	}

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
					+ ParentSearch.fmName + "&to="
					+ ParentSearch.toName);

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
					ParentSearch.busstops = new BusStopDto[length]; // JSONArrayのサイズで配列を作り直す
					// 構造体っぽいクラスにJSONObject Parent1.json_timetablesからデータを格納していく
					for (int i = 0; i < length; i++) {

						try {
							JSONObject stop = rootArrayObject.getJSONObject(i);
							JSONObject from = stop.getJSONObject("fm");
							JSONObject to = stop.getJSONObject("to");
							ParentSearch.busstops[i] = new BusStopDto(
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

					}
				} catch (JSONException e) {
					e.printStackTrace();
					Log.d("Child1_search", "error_JSONObject");
				}

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
				return;
			}

			// 通信には失敗していた場合
			if (json_error != 0) {
				// エラーダイアログの出力
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

			ParentSearch parentActivity = ((ParentSearch) getParent()); // 画面遷移
			parentActivity.showChild_select();


		}
	};

	@Override
	public void onPause() {
		super.onPause();
		// 本当にActivityがPauseしているのか判定
		if (this.isFinishing()) {
			smartCardAccess.disableDetection(this);
		}
	}
}
