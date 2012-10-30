package com.dotmarketing.portlets.checkurl.util;

public class ProxyManager {
	
	public static ProxyManager INSTANCE = new ProxyManager();
	
	private ProxyManager(){}
	
	private Connection connection;
	private Mail mail;

	public Connection getConnection() {
		return connection;
	}

	public void setConnection(Connection connection) {
		this.connection = connection;
	}	
	
	public Mail getMail() {
		return mail;
	}

	public void setMail(Mail mail) {
		this.mail = mail;
	}

	public boolean isLoaded(){
		if(null==connection)
			return false;
		else
			return true;
	}

}
