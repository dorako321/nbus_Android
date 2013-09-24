package jp.nbus;

import java.util.ArrayList;
import java.util.Calendar;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class Child3_detail extends Activity{

	private int favorite_sum;	//お気に入りの登録件数
	private ListView listview;

	public static String IDM_STATIC;

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
        setContentView(R.layout.child3_detail);

        //タイトルバー
        LinearLayout titlebar = (LinearLayout)findViewById(R.id.child3_detail_titlebar);
        titlebar.setLayoutParams(new LinearLayout.LayoutParams(Parent1.disp_width, (int)(Parent1.disp_height/10)));

        //リストビューが入るレイアウト
        LinearLayout content = (LinearLayout)findViewById(R.id.child3_detail_content);
        content.setLayoutParams(new LinearLayout.LayoutParams(Parent1.disp_width, (int)(Parent1.disp_height/10*6)));

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
			Parent3 parentActivity = ((Parent3)getParent());
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
	/*
	 * ArrayAdapterの独自拡張を利用して履歴を表示させようとしたときの残骸たち
	 *
	static class HistoryAdapter extends ArrayAdapter<TinyHistory>{

		private ArrayList<TinyHistory> histories = new ArrayList<TinyHistory>();
        private LayoutInflater inflater;
        private int layout;

		public HistoryAdapter(Context context,
				int textViewResourceId) {

			super(context,textViewResourceId);
			this.inflater = (LayoutInflater)context.getSystemService(LAYOUT_INFLATER_SERVICE);

			this.layout = textViewResourceId;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent){
			View view = convertView;
			if(view!=null){
				view = this.inflater.inflate(this.layout, null);
			}
			TinyHistory history = this.histories.get(position);

			TextView datetime = (TextView)view.findViewById(R.id.row_textview_datetime);
			TextView utiltype = (TextView)view.findViewById(R.id.row_textview_utiltype);
			TextView fareview = (TextView)view.findViewById(R.id.row_textview_fare);
			TextView balance = (TextView)view.findViewById(R.id.row_textview_balance);

        	String c_f = view.getResources().getString(R.string.currency_denomination_front);
        	String c_b = view.getResources().getString(R.string.currency_denomination_back);
        	String labelBalance = view.getResources().getString(R.string.label_balance);


			datetime.setText(String.format("%s %s",history.date, history.time));
			utiltype.setText(history.getUtilTypeInString(view.getResources()));
			String str_fare;
			if(history.fare<0){
				//積み増しのとき
				//TODO:積み増しの履歴表示で色を変える?
				str_fare = String.format("%s+%,d%s", c_f,history.fare*(-1),c_b);
			}else{
				//それ以外
				str_fare = String.format("%s%,d%s", c_f,history.fare,c_b);
			}
			fareview.setText(str_fare);
			balance.setText(String.format("%s%s%,d%s",labelBalance,c_f,history.balance,c_b));

			return view;
		}
		@Override
		public void add(TinyHistory history){
			super.add(history);
			this.histories.add(history);
		}

	} */

	@Override
    public void onPause(){
		super.onPause();
		//本当にActivityがPauseしているのか判定
		if (this.isFinishing()){
			smartCardAccess.disableDetection(this);
		}
	}
}
