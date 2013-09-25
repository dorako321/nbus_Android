package jp.nbus;


import org.json.JSONException;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;

/**
 * お気に入り編集画面
 * @version 1.0.0
 *
 */
public class FavoriteEdit extends Activity{

	private ListView listview;		//リストビュー

	private SmartCardAccess smartCardAccess;

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.child2_edit);

      //タイトルバー
        LinearLayout titlebar = (LinearLayout)findViewById(R.id.child2_edit_titlebar);
        titlebar.setLayoutParams(new LinearLayout.LayoutParams(ParentSearch.disp_width, (int)(ParentSearch.disp_height/10)));

        LinearLayout content = (LinearLayout)findViewById(R.id.child2_edit_content);
        content.setLayoutParams(new LinearLayout.LayoutParams(ParentSearch.disp_width, (int)(ParentSearch.disp_height/10*6)));

        //Editボタン
        Button btn_back = (Button)findViewById(R.id.child2_edit_button);
        btn_back.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				ParentFavorite parentActivity = ((ParentFavorite)getParent());
		        parentActivity.showChild_bookmark();
			}
        });


        //リストビュー
        listview = (ListView)findViewById(R.id.child2_edit_listview);
        //アダプタ作ってセット
        make_adapter();

        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
                // クリックされたアイテムを取得します
            	new AlertDialog.Builder(getParent())	//ダイアログの設定
				.setMessage("お気に入りから削除しますか？")
				.setCancelable(false)
				.setPositiveButton("Cancel", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.cancel();
					}
				})
				.setNegativeButton("OK", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						try {
							delete_route(position);		//削除処理
						} catch (JSONException e) {
							e.printStackTrace();
						}
						make_adapter();		//再度アダプタを作ってリストにセット
					}
				})
				.show();
            }
        });

        //NFC
        smartCardAccess = new SmartCardAccess();
		smartCardAccess.initialize(this);
	}

	@Override	//他アクティビティから戻ってきた時とかに呼ばれる、アダプタを作り直す
	protected void onResume() {
		//Log.d("Child2_edit", "onResume");
		super.onResume();
		make_adapter();

		smartCardAccess.enableDetection(this);
	}

	//SharedPreferencesからアダプタを作ってリストビューにセットする
	public void make_adapter(){
		//アダプタを初期化
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.row2, R.id.row2_textview1);

		FavoriteDBAccess dbAccess = new FavoriteDBAccess(this);
		ParentSearch.favoriteroutes = dbAccess.readFavorites();


        //Log.e("Child2_bookmark", String.valueOf(favorite_sum));

        if(ParentSearch.favoriteroutes.length == 0){	//データが無い時
        	adapter.add("お気に入りの経路が登録されていません。時刻表の検索結果画面からお気に入りを登録してください。");
        }else{
        	//データがある時
        	int length=ParentSearch.favoriteroutes.length;
        	for (int i = 0; i < length; i++) {
				FavoriteRoutesAshDto favRoute= ParentSearch.favoriteroutes[i];
				//ホントは独自のArrayAdapterを実装したい。そのほうがきれいに仕上げられる。
				adapter.add(favRoute.fm_name+"→"+favRoute.to_name+" ["+favRoute.co_name+"]");
			}
        }
        listview.setAdapter(adapter);
	}

	//経路が選択された時、削除してSharedPreferencesに再保存
	public void delete_route(int position) throws JSONException{
		FavoriteDBAccess dbAccess=new FavoriteDBAccess(this);
		dbAccess.deleteFavorite(ParentSearch.favoriteroutes[position].id);
	}


	//バックキーイベントをParent2じゃなくてこっちが受け取る場合がある
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if(keyCode==KeyEvent.KEYCODE_BACK){
			ParentFavorite parentActivity = ((ParentFavorite)getParent());
	        parentActivity.showChild_bookmark();
			return true;
		}
		return false;
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
