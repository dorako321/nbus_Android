package jp.nbus;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.ListView;

public class SmartCardDtail extends Activity{

	private ListView listview;

	public static String IDM_STATIC;

	private SmartCardAccess smartCardAccess;

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.child3_detail);

        //タイトルバー
        LinearLayout titlebar = (LinearLayout)findViewById(R.id.child3_detail_titlebar);
        titlebar.setLayoutParams(new LinearLayout.LayoutParams(ParentSearch.disp_width, (int)(ParentSearch.disp_height/10)));

        //リストビューが入るレイアウト
        LinearLayout content = (LinearLayout)findViewById(R.id.child3_detail_content);
        content.setLayoutParams(new LinearLayout.LayoutParams(ParentSearch.disp_width, (int)(ParentSearch.disp_height/10*6)));

        //リストビュー
        listview = (ListView)findViewById(R.id.child3_detail_listview);

        //NFC
        smartCardAccess = new SmartCardAccess();
		smartCardAccess.initialize(this);
	}


	@Override	//他アクティビティから戻ってきた時とかに呼ばれる、アダプタを作り直す
	protected void onResume() {
		Log.d("Child2_bookmark", "onResume");
		super.onResume();

        Intent intent = null;
        intent = getIntent();
        if (intent != null){
        	make_adapter(IDM_STATIC);
        }

        //NFC
        smartCardAccess.enableDetection(this);
	}

	//バックキーイベントをParent2じゃなくてこっちが受け取る場合がある
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if(keyCode==KeyEvent.KEYCODE_BACK){
			//Nbus_AndroidActivity.tabHost.setCurrentTab(0);	//タブを切り替え[
			ParentSmartCard parentActivity = ((ParentSmartCard)getParent());
			parentActivity.showChild_list();
			return true;
		}
		return false;
	}


	//SharedPreferencesからアダプタを作ってリストビューにセットする
	public void make_adapter(String idm){
		//DBアクセスがあるから非同期でやらないといけない。
		SmartCardAsyncTask async = new SmartCardAsyncTask();
		async.backgroundQueryHistory(this, idm, listview, getResources());
	}
	@Override
	public void onNewIntent(Intent intent){
        if (intent != null){
        	String idm = intent.getStringExtra("idm");
        	//SharedPreferencesからアダプタを作ってリストビューにセットする
        	make_adapter(idm);
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
