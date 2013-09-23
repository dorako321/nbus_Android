package jp.nbus;

import android.app.ActivityGroup;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Window;
import android.widget.LinearLayout;

public class Parent2 extends ActivityGroup{
	
	static LinearLayout container2;	//子ビューが入る、メインのコンテンツ表示部
	static int state = 0;
	/* state = 0 : お気に入り表示(Child2_bookmark)
	 *       = 1 : お気に入り編集(Child2_edit)*/
	
	@Override  
    public void onCreate(Bundle savedInstanceState) {  
        super.onCreate(savedInstanceState);  
        setContentView(R.layout.parent2);
        
        
        container2 = (LinearLayout)findViewById(R.id.parent2_container);
        
        showChild_bookmark();
        
	}
	
	//お気に入り表示
    public void showChild_bookmark() {
    	state = 0;
        container2.removeAllViews();
        Intent intent = new Intent(Parent2.this, Child2_bookmark.class);
        Window childActivity = getLocalActivityManager().startActivity("child1Activity_book",intent);
        container2.addView(childActivity.getDecorView());
    }
    
	//お気に入り表示
    public void showChild_edit() {
    	state = 1;
        container2.removeAllViews();
        Intent intent = new Intent(Parent2.this, Child2_edit.class);
        Window childActivity = getLocalActivityManager().startActivity("child1Activity_edit",intent);
        container2.addView(childActivity.getDecorView());
    }
    
    //バックキーが押された時の対応
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
    	if(keyCode==KeyEvent.KEYCODE_BACK){
    		
    		if(state == 0){	//現在の子アクティビティの１つ前の状態の子アクティビティに遷移
    			//finish();
    			Nbus_AndroidActivity.tabHost.setCurrentTab(0);	//タブを切り替え
    		}else if(state == 1){
    			showChild_bookmark();
    		}
    		return true;
    	}
    	return false;
    }

}
