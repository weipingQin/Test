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
	private List<KeyInfoStatus> serverkeyStatusList;
	private List<String> serverkeyIdList;
	private List<String> backupKeyIdList;//用来存放backupkeyidList

	private JSONArray keyInfoJsonArray;//用来存储keyInfo的JSONArray
	private JSONArray backupJsonArray;//用来存储服务端返回过来的backupids的JSONArray
	
	private KeyInfoStatus keyStatus;

	//定义异步
	private ExecutorService threadPool;


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
		keyStatus = new KeyInfoStatus();
		serverkeyStatusList = new ArrayList<>();
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

	//遍历本地所有的钥匙 获取房间的详细信息 
	private List<String> getAllRoomInfo(){
		List<String> infoList = new ArrayList<>();
		return infoList;
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
					if(doorKey.isUseLess()){
						doorKey.setUseLess(false);
						dbs.updateDoorKey(doorKey);
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
					backupJsonArray = json.getJSONArray("backupKeyIds");
					keyInfoJsonArray = json.getJSONArray("keyInfos");

					for(int i = 0 ; i < keyInfoJsonArray.length() ; i++){
						JSONObject jsonObject = keyInfoJsonArray.getJSONObject(i);
						String keyId = jsonObject.getString("keyId");
						String keystatus = jsonObject.getString("keyStatus");
						String userType = jsonObject.getString("userType");
						keyStatus.setKeyId(keyId);
						keyStatus.setKeyStatus(keystatus);
						keyStatus.setUserType(userType);
						serverkeyIdList.add(keyId);
						Log.d(TAG, "servercallbackKeyInfo-->"+jsonObject);
					}

					for(int i = 0 ; i <backupJsonArray.length();i++){
						String backupkeyId = backupJsonArray.get(i).toString();
						Log.d(TAG, "backupkeyId--->"+backupkeyId);
						backupKeyIdList.add(backupkeyId);
					}
					

					for(int i = 0 ; i < keyList.size() ; i++){
						Log.d(TAG, "本地钥匙的id-->"+keyList.get(i).getKid());
						getUnUselessDoorKeyList(keyList.get(i).getKid(),serverkeyIdList);
					}
					
					for(int i = 0 ; i < keyIdList.size();i++){
						Log.d(TAG, "没有用的钥匙id-->"+keyIdList.get(i));
					}
					//对没有用的钥匙进行状态更新
					updateUnUseLessDoorKeyList();
					
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
