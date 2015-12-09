package com.scaf.android.client.ui;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.scaf.android.client.model.DoorKey;
import com.scaf.android.client.net.NewResponseService;
import com.scaf.android.client.service.DBOperatorService;
import com.scaf.android.client.service.SettingHelper;

import android.app.Activity;
import android.media.DeniedByServerException;
import android.os.Bundle;
import android.util.Log;

public class MyTestActivity extends Activity{
	
	private static final String TAG = MyTestActivity.class.getSimpleName();
	
	private List<DoorKey> keyList;
	private DBOperatorService dbs;
	private String packageName;
	
	
	private List<String> keyIdList;
	private List<String> serverkeyIdList;
	
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
		DoorKey doorKey = new DoorKey();
		for(int i = 0 ; i < serverkeyIdList.size() ; i++){
			if(localKeyId.equals(serverkeyIdList.get(i))){
				doorKey = dbs.getDoorKeyByIDAndUid(localKeyId, SettingHelper.GET_UID(MyTestActivity.this));
				//如果是正常的钥匙的话 原先若是设置了错误的 那就给他纠正过来 
				if(doorKey != null){
					if(doorKey.isUseLess()){
						doorKey.setUseLess(false);
						
						updateBackUpStatus();
						
						dbs.updateDoorKey(doorKey);
					}
				}
				keyIdList.remove(serverkeyIdList.get(i));
			}
		}
		return keyIdList;
	}
	
	/**
	 * 根据服务端返回过来的backupids 来更新备份的状态
	 */
	private void updateBackUpStatus(){
		
	}
	
	/**
	 * 在这个函数getUnUselessDoorKeyList()之后执行
	 * 
	 */
	private void updateUselessDoorKeyList(){
		DoorKey doorKey = new DoorKey();
		for(int i = 0 ; i < keyIdList.size() ; i++){
			doorKey = dbs.getDoorKeyByIDAndUid(keyIdList.get(i), SettingHelper.GET_UID(MyTestActivity.this));
			if(doorKey != null){
				doorKey.setUseLess(true);
				dbs.updateDoorKey(doorKey);
			}
		}
	}
	
	private void updateDB(){
		
	}
	
	private void initKeyList(){
		keyList = dbs.getDoorkeysByUid(SettingHelper.GET_UID(MyTestActivity.this));
		uploadKeyListJsonArray(keyList);
		String uniqueid = SettingHelper.GET_UNIQUEID(MyTestActivity.this);
		int operatorUid = Integer.parseInt(SettingHelper.GET_UID(MyTestActivity.this));
		packageName = "com.scaf.android.client";
		String result = NewResponseService.chekOpenApp(operatorUid, uploadKeyListJsonArray(keyList), uniqueid, packageName);
		try {
			JSONObject json = new JSONObject(result);
			
			//存放服务端返回过来的keyId的集合
			
			if(json.has("errorCode")){
				int errorCode = json.getInt("errorCode");
				if(errorCode == 0){
					Log.d(TAG, "成功！");
				}
			}else{
				Log.d(TAG, "失败！");
			}
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
