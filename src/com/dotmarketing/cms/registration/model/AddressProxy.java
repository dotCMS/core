package com.dotmarketing.cms.registration.model;

import com.dotmarketing.beans.Inode;

public class AddressProxy extends Inode
{
	String addressId;
    public AddressProxy() {
    	this.setType("address_proxy");
    }
	public String getAddressId() {
		return addressId;
	}

	public void setAddressId(String addressId) {
		this.addressId = addressId;
	}	
}
