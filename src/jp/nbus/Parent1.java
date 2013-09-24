package jp.nbus;

import android.app.ActivityGroup;
import android.content.Intent;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;

public class Parent1 extends ActivityGroup {
	// staticな変数はほとんどここに書いとく
	static LinearLayout container1; // 子ビューが入る、メインのコンテンツ表示部
	static int disp_width = 0; // 画面サイズ
	static int disp_height = 0;
	static float dip_scale = 1.5f;

	// 旧API用 兼 ng.php問い合わせ用
	static String geton_name = null; // 入力された乗車停留所
	static String getoff_name = null; // 入力された降車停留所
	// Ash用
	static int geton_id = 0; // 入力された乗車停留所
	static int getoff_id = 0; // 入力された降車停留所
	static int company_id = 0;

	static Timetable[] timetables; // 読み込んだ時刻表をTimetable型で入れとく
	static BusStop[] busstops;
	static FavoriteRoutesAsh[] favoriteroutes; // SharedPreferencesのJSONデータからお気に入り経路を取り出して入れる

	// 結果表示画面でのオプション
	static Boolean result_all = false; // すべて表示するか
	static int result_week = 0; // 表示する曜日選択
	static int result_time_position; /*
									 * 時間ごとに表示する場合、すべての時刻表の何番目から表示することになるのか(時刻によって
									 * )
									 * リストがタッチされた時のアイテム取得と合わせて使うと元のリストの何番目がクリックされたか分かる
									 */

	static int detail_selected_item; // result_time_positionとlistview.getItemAtPositionから求めた元のリストでの位置

	static String route; // タイトルバー用に経路を用意、例：(長崎駅前→商業入口)

	static String result_geton_name = null;
	static String result_getoff_name = null;
	static String result_geton_ruby = null;
	static String result_getoff_ruby = null;
	static String company_name = null;

	// 画面遷移の状態
	static int state = 0;

	/*
	 * state = 0 : 検索(Child1_search) = 1 : 検索結果(Child1_result) = 2 :
	 * 経路詳細(Child1_detail) = 3 : 停留所検索結果(Child1_select)
	 */

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.parent1);

		// 画面サイズ取得
		WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
		Display disp = wm.getDefaultDisplay();
		disp_width = disp.getWidth();
		disp_height = disp.getHeight();

		// ピクセル密度からdipな倍率を取得
		DisplayMetrics metrics = new DisplayMetrics();
		wm.getDefaultDisplay().getMetrics(metrics);
		dip_scale = metrics.scaledDensity;

		// 子アクティビティを入れるコンテナ
		container1 = (LinearLayout) findViewById(R.id.parent1_container);

		// 検索画面を表示
		showChild_search();
	}

	/***** コンテナを初期化して子アクティビティを入れる関数、子アクティビティからも呼ばれる *****/
	// 検索画面
	public void showChild_search() {
		state = 0;
		container1.removeAllViews();
		Intent intent = new Intent(Parent1.this, Child1_search.class);
		Window childActivity = getLocalActivityManager().startActivity(
				"child1Activity_search", intent);
		container1.addView(childActivity.getDecorView());
	}

	// 検索結果
	public void showChild_result() {
		state = 1;
		container1.removeAllViews();
		Intent intent = new Intent(Parent1.this, Child1_result.class);
		Window childActivity = getLocalActivityManager().startActivity(
				"child1Activity_result", intent);
		container1.addView(childActivity.getDecorView());
	}

	// 時刻詳細
	public void showChild_detail() {
		state = 2;
		container1.removeAllViews();
		Intent intent = new Intent(Parent1.this, Child1_detail.class);
		Window childActivity = getLocalActivityManager().startActivity(
				"child1Activity_detail", intent);
		container1.addView(childActivity.getDecorView());
	}

	// バックキーが押された時の対応
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// 現在の子アクティビティの１つ前の状態の子アクティビティに遷移
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			if (state == 0) {
				finish();
			} else if (state == 1) {
				showChild_select();
			} else if (state == 2) {
				showChild_result();
			} else if (state == 3) {
				showChild_search();
			}
			return true;
		}
		return false;
	}

	public void showChild_select() {
		state = 3;
		container1.removeAllViews();
		Intent intent = new Intent(Parent1.this, Child1_select.class);
		Window childActivity = getLocalActivityManager().startActivity(
				"child1Activity_select", intent);
		container1.addView(childActivity.getDecorView());
	}

}