package jp.nbus;

import jp.nbus.R;
import jp.nbus.R.id;
import jp.nbus.R.layout;
import jp.nbus.dto.DetailDto;
import jp.nbus.dto.TimetableDto;
import android.app.Activity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Window;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * 詳細画面表示クラス
 * @author gomess
 *
 */
public class SearchDetail extends Activity{
	/**
	 * 詳細情報表示テキスト
	 */
	public EditText edit;
	/**
	 * タイトル表示用
	 */
	public TextView title;
	/**
	 * スマートカード
	 */
	private SmartCardAccess smartCardAccess;

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.child1_detail);

        //タイトルバー
        LinearLayout titlebar = (LinearLayout)findViewById(R.id.child1_detail_titlebar);
        titlebar.setLayoutParams(new LinearLayout.LayoutParams(ParentSearch.disp_width, (int)(ParentSearch.disp_height/10)));
        title = (TextView)findViewById(R.id.child1_detail_title_text);
        title.setText(ParentSearch.route);

        //文字列作る
        String detail = makeDetail();	//詳細を生成

        //表示用のEditText、コピーとかさせたいのでEditText
        edit = (EditText)findViewById(R.id.child1_detail_editText);
        edit.setText(detail);

        //NFC
        smartCardAccess = new SmartCardAccess();
		smartCardAccess.initialize(this);
	}

	//バックキーイベントをParent1じゃなくてこっちが受け取る場合がある
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if(keyCode==KeyEvent.KEYCODE_BACK){
			ParentSearch parentActivity = ((ParentSearch)getParent());
	        parentActivity.showChild_result();
			return true;
		}
		return false;
	}

	@Override	//他アクティビティから戻ってきた時とかに呼ばれる
	protected void onResume() {
		Log.d("Child1_detail", "onResume");
		super.onResume();
		//2回目以降に詳細を表示する場合にもonResumeが呼ばれるので更新
		title.setText(ParentSearch.route);	//タイトル
		String detail = makeDetail();	//詳細を生成
		edit.setText(detail);			//セット
		if(Nbus_AndroidActivity.select_bookmark == true){	//ブックマーク選択でタブ切り替えが起きた時は結果表示に飛ばす
			ParentSearch parentActivity = ((ParentSearch)getParent());
	        parentActivity.showChild_result();
	        Nbus_AndroidActivity.select_bookmark = false;
		}
		if(Nbus_AndroidActivity.select_stopname == true){	//停留所名のブックマーク選択でタブ切り替えが起きた時は結果表示に飛ばす
			ParentSearch parentActivity = ((ParentSearch)getParent());
	        parentActivity.showChild_select();
	        Nbus_AndroidActivity.select_stopname = false;
		}

		smartCardAccess.enableDetection(this);
	}

	//詳細のEditTextに表示するテキストの生成
	public String makeDetail(){
		TimetableDto timetable = ParentSearch.timetableHolder.timetable.get(ParentSearch.detail_selected_item);
		String route = ParentSearch.route;	//経路
		DetailDto detail = timetable.detail;//詳細
		String fmTime = timetable.getFmTime();	//出発時刻
		String toTime = timetable.getToTime();	//出発時刻


		StringBuilder detailText = new StringBuilder();	//完成形の詳細
		// 経由地が空だったら項目に含めない
		detailText.append(route + "\n");
		detailText.append(fmTime + "→" + toTime + "\n");
		if (!TextUtils.isEmpty(timetable.via)) {
			detailText.append("経由地：" + timetable.via + "\n");
		}
		if (!TextUtils.isEmpty(timetable.destination)) {
			detailText.append("目的地：" + timetable.destination + "\n");
		}
		if (!TextUtils.isEmpty(timetable.direction)) {
			detailText.append("方向　：" + timetable.direction + "\n");
		}
		if (!TextUtils.isEmpty(detail.bill)) {
			detailText.append("料金　：" + detail.bill + "円\n");
		}
		if (!TextUtils.isEmpty(detail.transitTime)) {
			detailText.append("時間　：" + detail.transitTime + "分\n");
		}

		return detailText.toString();
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
