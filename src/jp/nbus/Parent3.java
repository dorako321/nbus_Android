package jp.nbus;

import android.app.ActivityGroup;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Window;
import android.widget.LinearLayout;

public class Parent3 extends ActivityGroup{
	
	static LinearLayout container3;	//子ビューが入る、メインのコンテンツ表示部
	static String[][] idms;
	static int state = 0;
	/* state = 0 : お気に入り表示(Child3_list)
	 *       = 1 : お気に入り編集(Child3_detail)*/
	
	@Override  
    public void onCreate(Bundle savedInstanceState) {  
        super.onCreate(savedInstanceState);  
        setContentView(R.layout.parent3);
        
        
        container3 = (LinearLayout)findViewById(R.id.parent3_container);
        
        showChild_list();
        
	}
	
	//お気に入り表示
    public void showChild_list() {
    	state = 0;
        container3.removeAllViews();
        Intent intent = new Intent(Parent3.this, Child3_list.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        Window childActivity = getLocalActivityManager().startActivity("child1Activity_cardlist",intent);
        container3.addView(childActivity.getDecorView());
    }
    
	//お気に入り表示
    public final void showChild_detail(String idm) {
    	state = 1;
        container3.removeAllViews();
        Intent intent = new Intent(getApplicationContext(), Child3_detail.class);
        intent.putExtra("idm", idm);
        Child3_detail.IDM_STATIC =idm;
       // setIntent(intent);
        Window childActivity = getLocalActivityManager().startActivity("child1Activity_carddetail",intent);
        container3.addView(childActivity.getDecorView());
    }
   
    //バックキーが押された時の対応
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
    	if(keyCode==KeyEvent.KEYCODE_BACK){
    		
    		if(state == 0){	//現在の子アクティビティの１つ前の状態の子アクティビティに遷移
    			//finish();
    			Nbus_AndroidActivity.tabHost.setCurrentTab(0);	//タブを切り替え
    		}else if(state == 1){
    			showChild_list();
    		}
    		return true;
    	}
    	return false;
    }

}
