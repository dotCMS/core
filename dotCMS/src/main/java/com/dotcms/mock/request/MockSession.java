package com.dotcms.mock.request;

import com.dotmarketing.util.UUIDGenerator;

import java.util.*;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionContext;

@SuppressWarnings("deprecation")
public class MockSession implements  HttpSession {
	
	final String id;
	final long createionTime;
	final Map<String, Object> valmap = new HashMap<>();
	public MockSession(final String id) {
		super();
		this.id=id;
		createionTime = System.currentTimeMillis();
	}


	public MockSession(final HttpSession sessionIn) {
		super();
		this.id = UUIDGenerator.generateUuid();
		createionTime = System.currentTimeMillis();
		if (sessionIn != null) {
			Collections.list(sessionIn.getAttributeNames()).forEach(k -> valmap.put(k, sessionIn.getAttribute(k)));
		}
	}



	@Override
	public void setMaxInactiveInterval(int arg0) {


	}

	@Override
	public void setAttribute(String arg0, Object arg1) {
		valmap.put(arg0, arg1);

	}

	@Override
	public void removeValue(String arg0) {
		valmap.remove(arg0);

	}

	@Override
	public void removeAttribute(String arg0) {
		valmap.remove(arg0);

	}

	@Override
	public void putValue(String arg0, Object arg1) {
		valmap.put(arg0, arg1);

	}

	@Override
	public boolean isNew() {

		return (valmap.isEmpty());
	}

	@Override
	public void invalidate() {
		valmap.clear();

	}

	@Override
	public String[] getValueNames() {

		return valmap.keySet().toArray(new String[valmap.size()]);
	}

	@Override
	public Object getValue(String arg0) {
		return valmap.get(arg0);
	}

	@Override
	public ServletContext getServletContext() {

		return null;
	}

	@Override
	public int getMaxInactiveInterval() {

		return 0;
	}

	@Override
	public long getLastAccessedTime() {

		return 0;
	}

	@Override
	public String getId() {

		return id;
	}

	@Override
	public long getCreationTime() {

		return createionTime;
	}

	@Override
	public Enumeration<String> getAttributeNames() {
		return new Vector<String>(valmap.keySet()).elements();
	}

	@Override
	public Object getAttribute(String arg0) {
		return valmap.get(arg0);
	}

	@Override
	public HttpSessionContext getSessionContext() {
		// TODO Auto-generated method stub
		return null;
	}
}
