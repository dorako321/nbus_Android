package jp.nbus;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.NfcF;
import android.util.Log;

public class SmartCardAccess {
	//NFCタグ関連
	//private NfcAdapter nfcAdapter;
	private Object nfcAdapterObj;
	private PendingIntent pendingIntent;
	private IntentFilter[] intentFilter;
	private String[][] techLists;
	
	//読み取り結果格納
	private int balanceOfCard;
	private byte[][] byteHistory;
	private String idm;
	
	public void initialize(Context context){
        //NFC検出intentのあとの処理のためのForegroundDispatch引数の準備
		//TODO:NfcAdapter.getDefaultAdapterの呼び出しをリフレクション経由でやらないと2.3.2以前で死ぬ
		try {
			Class<?> clazz = Class.forName("android.nfc.NfcAdapter");
			if(clazz!=null){
				//NfcAdapter adapter_instance = (NfcAdapter)clazz.newInstance();
				//nfcAdapter = adapter_instance.getDefaultAdapter(context);
				Method method = clazz.getMethod("getDefaultAdapter", new Class[]{Context.class});
				nfcAdapterObj = method.invoke(null,context);
				pendingIntent = PendingIntent.getActivity(context, 0, new Intent(context,Nbus_AndroidActivity.class).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
				Field field = clazz.getField("ACTION_TECH_DISCOVERED");
				intentFilter = new IntentFilter[] {new IntentFilter((String) field.get(null)),};
				/*Class<?> nfcF = Class.forName("android.nfc.tech.NfcF");
				Object obj = nfcF.newInstance();
				Method getName = obj.getClass().getMethod("getName", (Class<?>[])null);
				String nfcFName = (String) getName.invoke((Object[])null,(Object[])null);
				techLists = new String[][]{new String[] {nfcFName}};*/
				techLists = new String[][]{new String[] {NfcF.class.getName()}};
			}
		} catch (ClassNotFoundException e) {
			// TODO 自動生成された catch ブロック
			Log.i("SmartCardAccess",e.toString());
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO 自動生成された catch ブロック
			Log.i("SmartCardAccess",e.toString());
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			// TODO 自動生成された catch ブロック
			Log.i("SmartCardAccess",e.toString());
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO 自動生成された catch ブロック
			Log.i("SmartCardAccess",e.toString());
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO 自動生成された catch ブロック
			Log.i("SmartCardAccess",e.toString());
			e.printStackTrace();
		} catch (NoSuchFieldException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}

	}
	
	public void enableDetection(Context context){
		//FeliCa接触時のintentを優先的にもらうように
		//nfcAdapter.enableForegroundDispatch((Activity) context, pendingIntent, intentFilter, techLists);
		if(nfcAdapterObj!=null){
			try {
				Class<?>[] arg = new Class[]{Activity.class,PendingIntent.class,IntentFilter[].class,String[][].class};
				Method method = nfcAdapterObj.getClass().getMethod("enableForegroundDispatch",arg );
				method.invoke(nfcAdapterObj, (Activity) context, pendingIntent, intentFilter, techLists);
			} catch (NoSuchMethodException e) {
				// TODO 自動生成された catch ブロック
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				// TODO 自動生成された catch ブロック
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO 自動生成された catch ブロック
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				// TODO 自動生成された catch ブロック
				e.printStackTrace();
			}
		}
	}
	
	public void disableDetection(Context context){
		//FeliCa接触時のintentをもらわないようにする
		//nfcAdapter.disableForegroundDispatch((Activity) context);
		if(nfcAdapterObj!=null){
			try {
				Method method = nfcAdapterObj.getClass().getMethod("disableForegroundDispatch", new Class[]{Activity.class});
				method.invoke(nfcAdapterObj, (Activity) context);
			} catch (NoSuchMethodException e) {
				// TODO 自動生成された catch ブロック
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				// TODO 自動生成された catch ブロック
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO 自動生成された catch ブロック
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				// TODO 自動生成された catch ブロック
				e.printStackTrace();
			}
		}
	}
	
	
	public void readSmartCard(Intent intent) throws SmartCardAccessException{
		//NfcFなタグが見つかった場合の処理
		//長崎スマートカードの読み取り用FeliCaコマンド
	    byte[] cmdPollingCommonArea = new byte[]{(byte)0x06, (byte)0x00, (byte) 0x80, (byte) 0x16, (byte)0x00, (byte)0x0F};

	    byte[] cmdRequestServiceNumber = new byte[]{(byte)0x0d, (byte)0x02, (byte) 0x00, (byte) 0x00, (byte)0x00, (byte)0x00, (byte) 0x00, (byte) 0x00, (byte)0x00, (byte)0x00,
	    															(byte)0x01, (byte)0x4b, (byte)0x00};
	    byte[] cmdReadWoEncryption = new byte[]{(byte)0x2c, (byte)0x06, (byte) 0x00, (byte) 0x00, (byte)0x00, (byte)0x00, (byte) 0x00, (byte) 0x00, (byte)0x00, (byte)0x00,
	    															(byte)0x04, (byte)0x4b, (byte)0x00, (byte)0x8b, (byte)0x00, (byte)0xcd, (byte)0x00, (byte)0xcb, (byte)0x18,
	    															(byte)0x0c, (byte)0x80, (byte)0x00, (byte)0x81, (byte)0x00, (byte)0x81, (byte)0x01, (byte)0x81, (byte)0x02, (byte)0x82, (byte)0x00, (byte)0x82, (byte)0x01, (byte)0x82, (byte)0x02, (byte)0x82, (byte)0x03, (byte)0x82, (byte)0x04,
	    															(byte)0x83, (byte)0x00, (byte)0x83, (byte)0x01, (byte)0x83, (byte)0x02};

		
	    //NfcAdapter上のTagを取得(どのようなTagが接触しているかなどか)
		Tag tag = (Tag) intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
		NfcF techF = NfcF.get(tag);
		
		try{
		techF.connect();
		if (techF.isConnected()){
			//FeliCaの共通領域を読み出すコマンドを送り、応答をresponseに格納
			byte[] resCommonArea = techF.transceive(cmdPollingCommonArea);
			//byte[] idmArr = null;//IDm
			//IDmをresponseから読み取る。8byteなので8回
			byte[] idmbyte = new byte[8];
			for(int i = 0; i < 8; i++){
				idmbyte[i] = resCommonArea[i+2];
				cmdRequestServiceNumber[i+2] = idmbyte[i];
				cmdReadWoEncryption[i+2] = idmbyte[i];
			}
			idm = String.format("%02x%02x%02x%02x%02x%02x%02x%02x",idmbyte[0],idmbyte[1],idmbyte[2],idmbyte[3],idmbyte[4],idmbyte[5],idmbyte[6],idmbyte[7]);
			
			byte[] resServiceNumber = techF.transceive(cmdRequestServiceNumber);
			//resServiceNumberの11,12両方がFFの場合は該当するサービスがないということ
			if(!((resServiceNumber[11] == (byte)0xFF) && (resServiceNumber[12]==(byte)0xFF))){

				byte[] resReadWoEncryption = techF.transceive(cmdReadWoEncryption);
				
				if((resReadWoEncryption[10]==(byte)0x00)&&(resReadWoEncryption[11]==(byte)0x00)){
					//とりあえず残高を読み取ろう…
					//bytebufferのallocateの引数がびみょい
					ByteBuffer bf = ByteBuffer.allocate(4);
					bf.put((byte)0x00);
					bf.put((byte)0x00);
					bf.put(resReadWoEncryption[48]);
					bf.put(resReadWoEncryption[49]);
					
					balanceOfCard = bf.getInt(0);
					
		    		byteHistory = new byte[5][16];
		    		
			    	for(int i = 0; i < 5; i++) {
			    		for(int j = 0; j < 16; j++) {
			    			byteHistory[i][j] = resReadWoEncryption[16 * (i + 4) + 13 + j]; 
			    		}
			    	}
				}else{
					throw new SmartCardAccessException(String.format("An error has occurred while loading the card:(%02x,%02x)",resReadWoEncryption[10],resReadWoEncryption[11]));
				}
			} else {
				throw new SmartCardAccessException("SmartCardService is not found on the card");
			}
		}
		} catch (IOException e){
			throw new SmartCardAccessException(String.format("IOException:%s",e));
		}
	}
	
	//残高を返すメソッド　失敗しているときはどうしようか→ここでパースしてエラー判定するとか
	public int getBalanceOfCard(){
		return balanceOfCard;
	}
	public String getIDm(){
		return idm;
	}
	
	public SmartCardHistory[] getHistoryOfCard(){
		SmartCardHistory[] smartCardHistoryArr = new SmartCardHistory[5];
		for(int i=0; i<5; i++){
			byte[] historyElem = new byte[16];
			for(int j=0; j<16; j++){
				historyElem[j] = byteHistory[i][j];
			}
			smartCardHistoryArr[i] = new SmartCardHistory(historyElem,idm);
		}
		return smartCardHistoryArr;

	}
	
	class SmartCardAccessException extends Exception{
		/**
		 * 
		 */
		private static final long serialVersionUID = 1830905568437117662L;

		public SmartCardAccessException(String str){
			super(str);
		}
	}
}
