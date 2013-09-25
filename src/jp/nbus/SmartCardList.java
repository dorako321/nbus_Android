package jp.nbus;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;

public class SmartCardList extends Activity{

	private ListView listview;


	private SmartCardAccess smartCardAccess;

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.child3_list);

        //タイトルバー
        LinearLayout titlebar = (LinearLayout)findViewById(R.id.child3_list_titlebar);
        titlebar.setLayoutParams(new LinearLayout.LayoutParams(ParentSearch.disp_width, (int)(ParentSearch.disp_height/10)));

        //リストビューが入るレイアウト
        LinearLayout content = (LinearLayout)findViewById(R.id.child3_list_content);
        content.setLayoutParams(new LinearLayout.LayoutParams(ParentSearch.disp_width, (int)(ParentSearch.disp_height/10*6)));

        //リストビュー
        listview = (ListView)findViewById(R.id.child3_list_listview);

        //SharedPreferencesからアダプタを作ってリストビューにセットする
        make_adapter();

        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            	if(ParentSmartCard.idms.length!=0){
            		ParentSmartCard parentActivity = ((ParentSmartCard)getParent());
            		parentActivity.showChild_detail(ParentSmartCard.idms[position][1]);
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
		SmartCardAsyncTask async = new SmartCardAsyncTask();
		async.backgroundQueryIDm(this, listview);
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
