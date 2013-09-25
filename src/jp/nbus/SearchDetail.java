package jp.nbus;

import jp.nbus.R;
import jp.nbus.R.id;
import jp.nbus.R.layout;
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
		String route = ParentSearch.route;	//経路
		String detail = ParentSearch.timetables[ParentSearch.detail_selected_item].detail;//詳細
		String str_via = ParentSearch.timetables[ParentSearch.detail_selected_item].via;	//経由地
		int time = ParentSearch.timetables[ParentSearch.detail_selected_item].fmtime;	//出発時刻
		int hour = time/60;
		int minute = time%60;
		String str_arr_time;	//出発時刻
		if(minute <10){
			str_arr_time = String.valueOf(hour)+":0"+String.valueOf(minute);
		}else{
			str_arr_time = String.valueOf(hour)+":"+String.valueOf(minute);
		}
		time = ParentSearch.timetables[ParentSearch.detail_selected_item].totime;
		hour = time/60;
		minute = time%60;
		String str_dep_time;	//到着時刻
		if(minute < 10){
			str_dep_time = "→"+String.valueOf(hour)+":0"+String.valueOf(minute)+"\n";
		}else{
			str_dep_time = "→"+String.valueOf(hour)+":"+String.valueOf(minute)+"\n";
		}
		//detailから運賃の部分を抽出
		char[] detail_array = detail.toCharArray();
		String str_fare = "";	//料金
		Boolean n = false;
		for(int i=0; i<detail_array.length; i++){
			if(n){
				str_fare = str_fare + detail_array[i];
			}
			if(detail_array[i] == '賃'){
				n = true;
			}
		}
		str_fare = "料金:" + str_fare.substring(1, str_fare.length());

		String detail_text = "";	//完成形の詳細
        if(TextUtils.isEmpty(str_via)){	//経由地が空だったら項目に含めない
    		detail_text = route+"\n"+str_arr_time+str_dep_time+str_fare;
        }else{
    		detail_text = route+"\n"+str_arr_time+str_dep_time+"経由地:"+str_via+"\n"+str_fare;
        }

		return detail_text;
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
