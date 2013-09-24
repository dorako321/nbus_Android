package jp.nbus;


import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import jp.nbus.SmartCardAccess.SmartCardAccessException;
import jp.nbus.util.*;

import com.google.ads.AdRequest;
import com.google.ads.AdSize;
import com.google.ads.AdView;

import android.app.TabActivity;
import android.content.Context;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;


@SuppressWarnings("deprecation")
public class Nbus_AndroidActivity extends TabActivity {
    /** Called when the activity is first created. */

	static TabHost tabHost;

	static Boolean select_bookmark = false;	//ブックマーク一覧から項目が選択されてChild1_resultに飛ばされた時に使うフラグ
	static Boolean select_stopname = false; //ブックマーク一覧からバス停名のみの項目が選択されてChild1_selectに飛ばす時に使うフラグ
	
	private AdView adView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);	//タイトルバー非表示
        setContentView(R.layout.tab_layout);

		tabHost = getTabHost();
		TabHost.TabSpec spec;
		Intent intent;

		//タブにセットするビュー(アイコンとか)
		View view1 = View.inflate(getApplication(), R.layout.tabview1, null);
		View view2 = View.inflate(getApplication(), R.layout.tabview2, null);

		//タブの中身をIntentで指定
		intent = new Intent().setClass(this, Parent1.class);	//Parent1、Child1_系のアクティビティが入る
		spec = tabHost.newTabSpec("tab1")	//タブで選択された画面の中身
				.setIndicator(view1)
				.setContent(intent);
		tabHost.addTab(spec);

		intent = new Intent().setClass(this, Parent2.class);	//Parent2、Child2_系のアクティビティが入る
		spec = tabHost.newTabSpec("tab2")
				.setIndicator(view2)
				.setContent(intent);
		tabHost.addTab(spec);

		//NFC対応の端末の場合の処理
		if (Nfc.isNfc(this)) {
			View view3 = View.inflate(getApplication(), R.layout.tabview3, null);

			intent = new Intent().setClass(this, Parent3.class); // Parent3、Child3_系のアクティビティが入る
			spec = tabHost.newTabSpec("tab3").setIndicator(view3)
					.setContent(intent);
			tabHost.addTab(spec);
			tabHost.setCurrentTab(2);
		}


		tabHost.setCurrentTab(0);	//開始時は検索タブ



		//AdMobのViewを作成
        adView = new AdView(this, AdSize.BANNER, "a1509526f038057");
        //レイアウトに追加
        LinearLayout layout = (LinearLayout)findViewById(R.id.admob_layout);
        layout.addView(adView);


        //Admob表示処理
        if(Utility.isDebug(this)){
        	if(Utility.isEmulator(this)){
        		//エミュレータでのデバッグ時
        		//どーゆーわけだか実機でもこれでテスト動くっぽいけど
        		AdRequest req = new AdRequest();
        		req.addTestDevice(AdRequest.TEST_EMULATOR);
        		adView.loadAd(req);
        	}else{
                //実機でのデバッグ時
                //広告をテストモードで表示
                AdRequest req = new AdRequest();
                req.addTestDevice(AdRequest.TEST_EMULATOR);
                //req.addTestDevice("テストする端末の端末ID");	//実機でテストする場合、端末IDの入力が必要
                adView.loadAd(req);
        	}
        }else{
        	//リリース時の処理
        	//広告の表示
        	adView.loadAd(new AdRequest());

        }


    }

	@Override
	public void onNewIntent(Intent intent){
		String action = intent.getAction();
		if(action.equals((NfcAdapter.ACTION_TECH_DISCOVERED))){
			//TechDiscoveredから間をおかずにreadSmartCardしたい
			final SmartCardAccess smartCardAccess = new SmartCardAccess();
			try{
				smartCardAccess.readSmartCard(intent);
				//残高の数値を生成しStringに改める
				String nfcstatus = getString(R.string.balance_label)+getString(R.string.currency_denomination_front)+String.valueOf(smartCardAccess.getBalanceOfCard())+getString(R.string.currency_denomination_back);
				//Activity名を使って三段階で結果表示用TextViewを取得
			    Parent3 parent3 = (Parent3)getLocalActivityManager().getActivity("tab3");
			    parent3.showChild_list();
			    Child3_list child3_list = (Child3_list)parent3.getLocalActivityManager().getActivity("child1Activity_cardlist");
			    TextView child3_nfcstatus = (TextView) child3_list.findViewById(R.id.child3_nfc_status);

				child3_nfcstatus.setText(nfcstatus);

				final SmartCardAsyncTask async = new SmartCardAsyncTask();
				async.backgroundParsing(smartCardAccess,this,smartCardAccess.getIDm());

				Nbus_AndroidActivity.tabHost.setCurrentTab(2);	//タブを切り替え

			} catch (NullPointerException e){
				Toast.makeText(this, getString(R.string.card_reading_error), Toast.LENGTH_LONG).show();
			} catch (SmartCardAccessException e) {
				// TODO Auto-generated catch block
				Toast.makeText(this, getString(R.string.card_reading_error), Toast.LENGTH_LONG).show();
			} finally{
			}
		}
	}


	@Override
	public void onDestroy() {
		// AdMobのViewの破棄処理
		adView.destroy();
		super.onDestroy();
	}



}