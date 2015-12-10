package com.scaf.android.client.model;

public class KeyInfoStatus {
	///用来存储服务端返回的Key的状态 包括钥匙id 冻结 解除冻结状态 还有普通用户 
	private String keyId;
	private String keyStatus;
	private String userType;
	
	public KeyInfoStatus(){
		
	}
	
	public String getKeyId() {
		return keyId;
	}
	public void setKeyId(String keyId) {
		this.keyId = keyId;
	}
	public String getKeyStatus() {
		return keyStatus;
	}
	public void setKeyStatus(String keyStatus) {
		this.keyStatus = keyStatus;
	}
	public String getUserType() {
		return userType;
	}
	public void setUserType(String userType) {
		this.userType = userType;
	}
}
