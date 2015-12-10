package com.scaf.android.client.ui;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.scaf.android.client.model.DoorKey;
import com.scaf.android.client.model.KeyInfo;
import com.scaf.android.client.model.KeyInfoStatus;
import com.scaf.android.client.net.NewResponseService;
import com.scaf.android.client.openapi.ThreadPool;
import com.scaf.android.client.service.DBOperatorService;
import com.scaf.android.client.service.SettingHelper;

import android.app.Activity;
import android.media.DeniedByServerException;
import android.os.Bundle;
import android.util.Log;

public class MyTestActivity extends Activity{

	private static final String TAG = MyTestActivity.class.getSimpleName();

	private DoorKey doorKey;
	private List<DoorKey> keyList;

	private DBOperatorService dbs;
	private String packageName;


	private List<String> keyIdList;
	private List<KeyInfoStatus> keyInfoStatus;
	private List<String> serverkeyIdList;
	private List<String> backupKeyIdList;//用来存放backupkeyidList

	private JSONArray keyInfoJsonArray;//用来存储keyInfo的JSONArray
	private JSONArray backupJsonArray;//用来存储服务端返回过来的backupids的JSONArray

	

	//定义异步
	private ExecutorService threadPool;
	private String status;


	public static final String USERTYPE_ADMIN		 	= "110301";		//管理员 
	public static final String USERTYPE_EKEY      		= "110302";		//普通用户 电子钥匙
	public static final String USERTYPE_OLD_USER 		= "110303";		//锁用户 老版本锁

	//定义钥匙的相关状态 
	public static final String KEY_STATUS_NORMAL		= "110401";		//正常使用中
	public static final String KEY_STATUS_FREEZING		= "110404";		//冻结中
	public static final String KEY_STATUS_FREEZED		= "110405";		//已冻结
	public static final String KEY_STATUS_UNFREEZING	= "110406";		//解除冻结中
	public static final String KEY_STATUS_DELETING 		= "110407";		//删除中


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		init();
	}

	@Override
	protected void onResume() {
		super.onResume();
	}

	private void init(){
		keyList = new ArrayList<>();
		dbs = new DBOperatorService(MyTestActivity.this);
		keyInfoStatus = new ArrayList<>();
		serverkeyIdList = new ArrayList<>();
		backupKeyIdList = new ArrayList<>();
		threadPool = ThreadPool.getCachedThreadPool();
		initKeyList();
	}

	//将数据上传到服务端 
	private JSONArray uploadKeyListJsonArray(List<DoorKey> keyList){
		keyIdList = new ArrayList<>();
		JSONArray keyListJSONArray = new JSONArray();
		for(int i = 0 ; i < keyList.size() ; i++){
			keyIdList.add(keyList.get(i).getKid());
			JSONObject jsonObj = new JSONObject();
			try {
				jsonObj.put("keyId",String.valueOf(keyList.get(i).getKid()));
				jsonObj.put("roomId",String.valueOf(keyList.get(i).getRoomid()));
				keyListJSONArray.put(i,jsonObj);
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		return keyListJSONArray;
	}

	/**
	 * 本地去取一个keyId依次和服务端一一比较 key.get(i).getKId();
	 * 筛选出本地没有用的钥匙id 直接将其备份删除掉 
	 */
	private List<String> getUnUselessDoorKeyList(String localKeyId,List<String> serverkeyIdList){
		doorKey = new DoorKey();
		for(int i = 0 ; i < serverkeyIdList.size() ; i++){
			if(localKeyId.equals(serverkeyIdList.get(i))){
				doorKey = dbs.getDoorKeyByIDAndUid(localKeyId, SettingHelper.GET_UID(MyTestActivity.this));
				//如果是正常的钥匙的话 原先若是设置了错误的 那就给他纠正过来 
				if(doorKey != null){
					if(keyInfoStatus.get(i).getUserType().equals(USERTYPE_ADMIN)){
						doorKey.setUseLess(false);
						dbs.updateDoorKey(doorKey);
					}
					if(keyInfoStatus.get(i).getUserType().equals(USERTYPE_EKEY)){
						String status = keyInfoStatus.get(i).getKeyStatus();
						if(status !=null && status.equals("")){
							asyncDoorKeyStatus(status,doorKey);
						}
					}
					UpdateBackUpKeyStatusKeyList(serverkeyIdList.get(i),backupKeyIdList);
				}
				keyIdList.remove(serverkeyIdList.get(i));
			}
		}
		return keyIdList;
	}

	/**
	 * 根据获取到的backupkeyId来设置备份钥匙的状态 
	 */
	private void updateBackUpStatus(String backupkeyid){
		doorKey = dbs.getDoorKeyByIDAndUid(backupkeyid, SettingHelper.GET_UID(MyTestActivity.this));
		if(doorKey != null){
			dbs.updateDoorKey(doorKey);
		}
	}


	/**
	 * 服务端返回过来的取一把serverKeyId和本地的backUpkeyId进行一一比较  并将钥匙的状态更新掉
	 * 
	 */
	private void UpdateBackUpKeyStatusKeyList(String serverKeyId,List<String> backUpIdList){
		for(int i = 0 ; i <backUpIdList.size() ;i++){
			if(serverKeyId.equals(backUpIdList.get(i))){
				updateBackUpStatus(backUpIdList.get(i));
			}
		}
	}


	private void updateUnUseLessDoorKeyList(){
		doorKey = new DoorKey();
		for(int i = 0 ; i < keyIdList.size();i++){
			doorKey = dbs.getDoorKeyByIDAndUid(keyIdList.get(i), SettingHelper.GET_UID(MyTestActivity.this));
			dbs.updateDoorKey(doorKey);
		}
	}

	//同步钥匙的状态 和 服务端一致
	//首先要确保该钥匙是有用的 
	private void asyncDoorKeyStatus(String serverKeyStatus,DoorKey useDoorKey){
		switch (serverKeyStatus) {

		case KEY_STATUS_NORMAL: //正常状态下 
			useDoorKey.setBlocked(false);
			break;
		case KEY_STATUS_FREEZING: //冻结中 
			if(!useDoorKey.isBlocked()){
				useDoorKey.setBlocked(true);
			}
			break;
		case KEY_STATUS_FREEZED: //已冻结
			String json = NewResponseService.confirmFreezeDoorkey(useDoorKey); //本地钥匙已冻结 发确认冻结指令
			try {
				JSONObject jsonObject = new JSONObject(json);
				int errorCode = jsonObject.getInt("errorCode");
				if(errorCode == 0){
					if(!useDoorKey.isBlocked()){
						useDoorKey.setBlocked(true);
					}
				}
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			break;
		case KEY_STATUS_UNFREEZING:	//确认冻结反馈
			String json2 = NewResponseService.confirmUnFreezeDoorkey(useDoorKey);
			try {
				JSONObject jsonObject = new JSONObject(json2);
				int errorCode = jsonObject.getInt("errorCode");
				if(errorCode == 0){
					if(useDoorKey.isBlocked()){
						useDoorKey.setBlocked(false);
					}
				}
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			break;
		case KEY_STATUS_DELETING: //删除中状态
			int operatorUid = Integer.parseInt(SettingHelper.GET_UID(MyTestActivity.this));
			int lockId = useDoorKey.getRoomid();
			String json3 = NewResponseService.deleteEKeyFeedback(operatorUid, lockId, String.valueOf(useDoorKey.getKid()));
			try{
				JSONObject jsonObject = new JSONObject(json3);
				int errorCode  = jsonObject.getInt("errorCode");
				if(errorCode == 0){
					dbs.deleteDoorKey(useDoorKey.getId());
				}
			}catch(JSONException e){
				e.printStackTrace();
			}
			break;
		default:
			break;
		}
		dbs.updateDoorKey(useDoorKey); //将数据库更新掉
	}


	private void initKeyList(){
		keyList = dbs.getDoorkeysByUid(SettingHelper.GET_UID(MyTestActivity.this));
		final JSONArray array = uploadKeyListJsonArray(keyList);
		final String uniqueid = SettingHelper.GET_UNIQUEID(MyTestActivity.this);
		final int operatorUid = Integer.parseInt(SettingHelper.GET_UID(MyTestActivity.this));
		packageName = "com.scaf.android.client";

		threadPool.execute(new Runnable() {

			@Override
			public void run() {
				String result = NewResponseService.chekOpenApp(operatorUid, array, uniqueid, packageName);
				try {
					JSONObject json = new JSONObject(result);
					if(json.has("keyInfos")){
						keyInfoJsonArray = json.getJSONArray("keyInfos");
						for(int i = 0 ; i < keyInfoJsonArray.length() ; i++){
							JSONObject jsonObject = keyInfoJsonArray.getJSONObject(i);
							String keyId = jsonObject.getString("keyId");
							String keystatus = jsonObject.getString("keyStatus");
							String userType = jsonObject.getString("userType");
						    KeyInfoStatus keyInfoItem = new KeyInfoStatus();
							keyInfoItem.setKeyId(keyId);
							keyInfoItem.setKeyStatus(keystatus);
							keyInfoItem.setUserType(userType);
							keyInfoStatus.add(keyInfoItem);
							serverkeyIdList.add(keyId);
							Log.d(TAG, "servercallbackKeyInfo-->"+jsonObject);
						}
					}

					if(json.has("backupKeyIds")){
						backupJsonArray = json.getJSONArray("backupKeyIds");
						for(int i = 0 ; i <backupJsonArray.length();i++){
							String backupkeyId = backupJsonArray.get(i).toString();
							Log.d(TAG, "backupkeyId--->"+backupkeyId);
							backupKeyIdList.add(backupkeyId);
						}
					}

					for(int i = 0 ; i < keyList.size() ; i++){
						Log.d(TAG, "本地钥匙的id-->"+keyList.get(i).getKid());
						Log.d(TAG, "是否管理员-->"+keyList.get(i).isAdmin());
						getUnUselessDoorKeyList(keyList.get(i).getKid(),serverkeyIdList);
					}

					for(int i = 0 ; i < keyIdList.size();i++){
						Log.d(TAG, "没有用的钥匙id-->"+keyIdList.get(i));
					}
					//对没有用的钥匙进行状态更新
					updateUnUseLessDoorKeyList();

					//再去刷新本地的UI状态

					//存放服务端返回过来的keyId的集合
					if(json.has("errorCode")){
						int errorCode = json.getInt("errorCode");
						if(errorCode == 0){
							Log.d(TAG, "成功！");
						}else{
							Log.d(TAG, "失败！");
						}
					}
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});

	}
}
