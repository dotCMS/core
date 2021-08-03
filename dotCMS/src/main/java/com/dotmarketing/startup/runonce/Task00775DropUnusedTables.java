package com.dotmarketing.startup.runonce;

import java.util.List;

import com.dotmarketing.startup.AbstractJDBCStartupTask;

public class Task00775DropUnusedTables extends AbstractJDBCStartupTask {

	@Override
	public String getMSSQLScript() {
		return "drop TABLE ECOM_PRODUCT_FORMAT;" +
		"drop table WEB_EVENT_ATTENDEE;" +
		"drop TABLE ECOM_ORDER;" +
		"drop TABLE FACILITY;" +
		"drop TABLE RECURANCE;" +
		"drop TABLE ENTITY;" +
		"drop TABLE EVENT_REGISTRATION;" +
		"drop TABLE WEB_EVENT;" +
		"drop TABLE ECOM_PRODUCT_PRICE;" +
		"drop TABLE WEB_EVENT_REGISTRATION;" +
		"drop TABLE BANNER;" +
		"drop TABLE ECOM_ORDER_ITEM;" +
		"drop TABLE ORGANIZATION;" +
		"drop TABLE ECOM_PRODUCT;" +
		"drop TABLE WEB_EVENT_LOCATION;" +
		"drop TABLE ECOM_DISCOUNT_CODE;" +
		"drop TABLE EVENT;"+
		"delete from tree where child in(select inode from inode where type in('ecom_product','entity','ecom_product_format','ecom_product_price','entity')) " +
		"or parent in(select inode from inode where type in('ecom_product','entity','ecom_product_format','ecom_product_price','entity'));" +
		"delete from inode where type in('ecom_product','entity','ecom_product_format','ecom_product_price','entity');";
	}

	@Override
	public String getMySQLScript() {
		return "drop TABLE ECOM_PRODUCT_FORMAT;" +
		"drop table WEB_EVENT_ATTENDEE;" +
		"drop TABLE ECOM_ORDER;" +
		"drop TABLE FACILITY;" +
		"drop TABLE RECURANCE;" +
		"drop TABLE ENTITY;" +
		"drop TABLE EVENT_REGISTRATION;" +
		"drop TABLE WEB_EVENT;" +
		"drop TABLE ECOM_PRODUCT_PRICE;" +
		"drop TABLE WEB_EVENT_REGISTRATION;" +
		"drop TABLE BANNER;" +
		"drop TABLE ECOM_ORDER_ITEM;" +
		"drop TABLE ORGANIZATION;" +
		"drop TABLE ECOM_PRODUCT;" +
		"drop TABLE WEB_EVENT_LOCATION;" +
		"drop TABLE ECOM_DISCOUNT_CODE;" +
		"drop TABLE EVENT;" +
		"delete from tree where child in(select inode from inode where type in('ecom_product','entity','ecom_product_format','ecom_product_price','entity')) " +
		"or parent in(select inode from inode where type in('ecom_product','entity','ecom_product_format','ecom_product_price','entity'));" +
		"delete from inode where type in('ecom_product','entity','ecom_product_format','ecom_product_price','entity');";
	}

	@Override
	public String getOracleScript() {
		return "drop TABLE ECOM_PRODUCT_FORMAT;" +
			"drop table WEB_EVENT_ATTENDEE;" +
			"drop TABLE ECOM_ORDER;" +
			"drop TABLE FACILITY;" +
			"drop TABLE RECURANCE;" +
			"drop TABLE ENTITY;" +
			"drop TABLE EVENT_REGISTRATION;" +
			"drop TABLE WEB_EVENT;" +
			"drop TABLE ECOM_PRODUCT_PRICE;" +
			"drop TABLE WEB_EVENT_REGISTRATION;" +
			"drop TABLE BANNER;" +
			"drop TABLE ECOM_ORDER_ITEM;" +
			"drop TABLE ORGANIZATION;" +
			"drop TABLE ECOM_PRODUCT;" +
			"drop TABLE WEB_EVENT_LOCATION;" +
			"drop TABLE ECOM_DISCOUNT_CODE;" +
			"drop TABLE EVENT;"+
			"delete from tree where child in(select inode from inode where type in('ecom_product','entity','ecom_product_format','ecom_product_price','entity')) " +
			"or parent in(select inode from inode where type in('ecom_product','entity','ecom_product_format','ecom_product_price','entity'));" +
			"delete from inode where type in('ecom_product','entity','ecom_product_format','ecom_product_price','entity');";
	}

	@Override
	public String getPostgresScript() {
		return "drop TABLE ECOM_PRODUCT_FORMAT;" +
		"drop table WEB_EVENT_ATTENDEE;" +
		"drop TABLE ECOM_ORDER;" +
		"drop TABLE FACILITY;" +
		"drop TABLE RECURANCE;" +
		"drop TABLE ENTITY;" +
		"drop TABLE EVENT_REGISTRATION;" +
		"drop TABLE WEB_EVENT;" +
		"drop TABLE ECOM_PRODUCT_PRICE;" +
		"drop TABLE WEB_EVENT_REGISTRATION;" +
		"drop TABLE BANNER;" +
		"drop TABLE ECOM_ORDER_ITEM;" +
		"drop TABLE ORGANIZATION;" +
		"drop TABLE ECOM_PRODUCT;" +
		"drop TABLE WEB_EVENT_LOCATION;" +
		"drop TABLE ECOM_DISCOUNT_CODE;" +
		"drop TABLE EVENT;" +
		"delete from tree where child in(select inode from inode where type in('ecom_product','entity','ecom_product_format','ecom_product_price','entity')) " +
		"or parent in(select inode from inode where type in('ecom_product','entity','ecom_product_format','ecom_product_price','entity'));" +
		"delete from inode where type in('ecom_product','entity','ecom_product_format','ecom_product_price','entity');";
	}

	@Override
	protected List<String> getTablesToDropConstraints() {
		return null;
	}

	public boolean forceRun() {
		return true;
	}

}
