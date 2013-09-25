package jp.nbus;

import java.util.ArrayList;

import android.content.Context;
import android.content.res.Resources;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class SmartCardAsyncTask{

	void backgroundParsing(final SmartCardAccess smartCardAccess,final Context context, final String idm){
		new AsyncTask<Void,Void,Void>() {
			@Override
			protected Void doInBackground(Void... params) {
				// TODO Auto-generated method stub
				SmartCardHistory[] smartCardHistory = smartCardAccess.getHistoryOfCard();
			//	Nbus_AndroidActivity activity = new Nbus_AndroidActivity();

			//	SmartCardHistoryManager manager = new SmartCardHistoryManager(activity.getApplicationContext(), 0);
				SmartCardHistoryManager manager = new SmartCardHistoryManager(context, 1);
				SQLiteDatabase db = manager.getWritableDatabase();
				SmartCardHistory[] history = manager.keepSmartCardHistory(smartCardHistory);
				manager.putSmartCardHistory(db, history, idm);

				return null;
			}
		}.execute();
	}

	void backgroundQueryIDm(final Context context, final ListView listView){
		new AsyncTask<Void,Void,String[][]>(){

			@Override
			protected String[][] doInBackground(Void... params) {
				// TODO Auto-generated method stub
				SmartCardHistoryManager manager = new SmartCardHistoryManager(context, 1);
				SQLiteDatabase db = manager.getWritableDatabase();

				String[][] idms = manager.idmList(db);
				return idms;
			}

			@Override
			protected void onPostExecute(String[][] idms){
				//アダプタを初期化
				ArrayAdapter<String> adapter = new ArrayAdapter<String>(context, R.layout.row, R.id.row_textview1);

				if(idms.length == 0){	//データが無い時
		        	adapter.add("カードの履歴が登録されていません。");
		        }else{								//データがある時
		        	try {
						for (int i=0;i<idms.length;i++){
							adapter.add("IDm:"+idms[i][1]);
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
		        }
		        listView.setAdapter(adapter);
		        ParentSmartCard.idms = idms;
			}

		}.execute();
	}

	void backgroundQueryHistory(final Context context, final String idm, final ListView listview, final Resources resource) {
		// TODO Auto-generated method stub
		new AsyncTask<Void,Void,ArrayList<TinyHistory>>(){

			@Override
			protected ArrayList<TinyHistory> doInBackground(Void... params) {
				// TODO Auto-generated method stub
				SmartCardHistoryManager manager = new SmartCardHistoryManager(context, 1);
				SQLiteDatabase db = manager.getWritableDatabase();

				ArrayList<TinyHistory> histories = manager.getHistory(db,idm);
				return histories;
			}

			@Override
			protected void onPostExecute(ArrayList<TinyHistory> histories){
				//アダプタを初期化
				ArrayAdapter<String> adapter = new ArrayAdapter<String>(context, R.layout.row3, R.id.row_textview2);

				if(histories.size() == 0){	//データが無い時
		        	adapter.add(resource.getString(R.string.cardhistories_notfound));
		        }else{								//データがある時
		        	String c_f = resource.getString(R.string.currency_denomination_front);
		        	String c_b = resource.getString(R.string.currency_denomination_back);
		        	String labelBalance = resource.getString(R.string.label_balance);
		        	try {
						for (TinyHistory history : histories){
							adapter.add(String.format("%s %s |%s %s%d%s  %s%s%d%s", history.date, history.time, history.getUtilTypeInString(resource), c_f, history.fare, c_b, labelBalance ,c_f, history.balance, c_b));
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
		        }
		        listview.setAdapter(adapter);
			}

		}.execute();
	}

}
