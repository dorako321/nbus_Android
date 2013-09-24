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
public class Child2_edit extends Activity{

	private ListView listview;		//リストビュー

	private SmartCardAccess smartCardAccess;

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.child2_edit);

      //タイトルバー
        LinearLayout titlebar = (LinearLayout)findViewById(R.id.child2_edit_titlebar);
        titlebar.setLayoutParams(new LinearLayout.LayoutParams(Parent1.disp_width, (int)(Parent1.disp_height/10)));

        LinearLayout content = (LinearLayout)findViewById(R.id.child2_edit_content);
        content.setLayoutParams(new LinearLayout.LayoutParams(Parent1.disp_width, (int)(Parent1.disp_height/10*6)));

        //Editボタン
        Button btn_back = (Button)findViewById(R.id.child2_edit_button);
        btn_back.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				Parent2 parentActivity = ((Parent2)getParent());
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
		/*
		//SharedPreferencesから情報を読み込む
        SharedPreferences sharedpreference = getSharedPreferences("Nbus_Android", Activity.MODE_PRIVATE);
        favorite_sum = sharedpreference.getInt("DataSum", 0);	//データ数を取得
        String str_json = "";
        */

		FavoriteDBAccess dbAccess = new FavoriteDBAccess(this);
		Parent1.favoriteroutes = dbAccess.readFavorites();


        //Log.e("Child2_bookmark", String.valueOf(favorite_sum));

        if(Parent1.favoriteroutes.length == 0){	//データが無い時
        	adapter.add("お気に入りの経路が登録されていません。時刻表の検索結果画面からお気に入りを登録してください。");
        }else{
        	//データがある時
        	/*str_json = sharedpreference.getString("JSON", "");
        	try {
				JSONObject root = new JSONObject(str_json);//文字列を元にJSONObjectを作る
				JSONArray array_route = root.getJSONArray("route");
				Parent1.favoriteroutes = new FavoriteRoutes[array_route.length()];
				for(int i=0; i<array_route.length(); i++){
					JSONObject route = array_route.getJSONObject(i);
					Parent1.favoriteroutes[i] = new FavoriteRoutes(route.getString("arr"), route.getString("dep"));
					adapter.add(route.getString("arr")+"→"+route.getString("dep"));
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}*/
        	int length=Parent1.favoriteroutes.length;
        	for (int i = 0; i < length; i++) {
				FavoriteRoutesAsh favRoute= Parent1.favoriteroutes[i];
				//ホントは独自のArrayAdapterを実装したい。そのほうがきれいに仕上げられる。
				adapter.add(favRoute.fm_name+"→"+favRoute.to_name+" ["+favRoute.co_name+"]");
			}
        }
        listview.setAdapter(adapter);
	}

	//経路が選択された時、削除してSharedPreferencesに再保存
	public void delete_route(int position) throws JSONException{
/*		JSONObject root = new JSONObject();
		JSONArray route_array = new JSONArray();
		for(int i=0; i<Parent1.favoriteroutes.length; i++){
			if(i==position){
				//削除したいデータが来たら何もしない
				//Log.e(String.valueOf(i), "delete");
			}else{
				//削除しないデータは保存
				JSONObject route = new JSONObject();
				route.put("arr", Parent1.favoriteroutes[i].arr);
				route.put("dep", Parent1.favoriteroutes[i].dep);
				route_array.put(route);
				//Log.e(String.valueOf(i), Parent1.favoriteroutes[i].arr);
			}
		}
		favorite_sum--;	//データ数合計を減らす
		root.put("route", route_array);
		//SharedPreferencesに保存
		SharedPreferences sharedpreferences = getSharedPreferences("Nbus_Android", Activity.MODE_PRIVATE);
		SharedPreferences.Editor editor = sharedpreferences.edit();
		editor.putInt("DataSum", favorite_sum);
		editor.putString("JSON", root.toString());
		editor.commit();*/
		FavoriteDBAccess dbAccess=new FavoriteDBAccess(this);
		dbAccess.deleteFavorite(Parent1.favoriteroutes[position].id);
	}


	//バックキーイベントをParent2じゃなくてこっちが受け取る場合がある
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if(keyCode==KeyEvent.KEYCODE_BACK){
			Parent2 parentActivity = ((Parent2)getParent());
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
