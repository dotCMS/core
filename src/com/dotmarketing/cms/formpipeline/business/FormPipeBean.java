package com.dotmarketing.cms.formpipeline.business;

import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.StringTokenizer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.dotmarketing.util.Config;
import com.dotmarketing.util.UtilMethods;

public class FormPipeBean extends HashMap<String, Object> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	List<String> errorMessages;

	List<String> messages;

	String formPipe;

	String returnUrl;
	
	boolean redirect;

	HttpServletRequest _request;

	HttpServletResponse _response;

	public HttpServletRequest getUnderlyingRequest() {
		return _request;
	}

	public HttpServletResponse getUnderlyingResponse() {
		return _response;
	}

	protected void setUnderlyingRequest(HttpServletRequest req) throws FormPipeException {
		if (_request == null) {
			_request = req;

			super.put("referer", _request.getHeader("referer"));
			super.put("remoteAddr", _request.getRemoteAddr());
			Enumeration keys = _request.getHeaderNames();
			while (keys.hasMoreElements()) {
				String key = (String) keys.nextElement();
				super.put(key, _request.getHeader(key));
			}

			keys = _request.getParameterNames();
			while (keys.hasMoreElements()) {
				String key = (String) keys.nextElement();
				String[] param = _request.getParameterValues(key);
				if (param != null && param.length > 1) {
					super.put(key, _request.getParameterValues(key));
				} else if (param != null && param.length == 1) {
					super.put(key, _request.getParameterValues(key)[0]);
				} else {
					if (req.getParameter(key) != null) {
						super.put(key, req.getParameter(key));
					}
				}
			}
			String defaultReturn = Config.getStringProperty("org.dotcms.forms.default.return");

			if (UtilMethods.isSet(defaultReturn)) {
				setReturnUrl(defaultReturn);
			} else {
				setReturnUrl(_request.getHeader("referer"));
			}
			setFormPipe(_request.getParameter("formPipe"));
			if (getFormPipe() == null) {
				addErrorMessage("formPipe parameter is not set");
				throw new FormPipeException(true, true);
			}

		} else {
			throw new IllegalStateException("request has already been set in the FormPipeBean");
		}
	}

	protected void setUnderlyingResponse(HttpServletResponse res) {
		if (_response == null) {
			_response = res;
		} else {
			throw new IllegalStateException("response has already been set in the FormPipeBean");
		}
	}




	public List<FormPipe> getPipes() throws FormPipeException {

		List<FormPipe> l = new ArrayList<FormPipe>();
		String formPipeChain = Config.getStringProperty("org.dotcms.forms." + getFormPipe());
		if(formPipeChain == null){
			formPipeChain = Config.getStringProperty("org.dotcms.forms.default.FormPipe");
		}

		if (UtilMethods.isSet(formPipeChain)) {
			String clazz = null;
			try {
				StringTokenizer st = new StringTokenizer(formPipeChain, ", ;");
				while (st.hasMoreTokens()) {
					clazz = st.nextToken();

					FormPipe pipe = (FormPipe) Class.forName(clazz).newInstance();
					l.add(pipe);

				}
			}

			catch (Exception e) {

				throw new FormPipeException("Unable to create FormPipe. Looking for :" + clazz);

			}
		} 
		return l;
	}

	public FormPipeBean() {
		super();
		setFormPipe("default");
		errorMessages = new ArrayList<String>();
		messages = new ArrayList<String>();


	}

	public List<String> getErrorMessages() {
		return errorMessages;
	}

	public void addErrorMessage(String str) {
		errorMessages.add(str);
	}

	public void addMessage(String str) {
		messages.add(str);
	}

	public List<String> getMessages() {
		return messages;
	}



	public Object get(String key) {
		return super.get(key);
	}
	public String getString(String key) {
		return super.get(key).toString();
		
	}
	public int getInt(String key) {
		int x = 0;
		try {
			x = Integer.getInteger((String) get(key));
		} catch (Exception e) {

		}
		return x;
	}

	public long getLong(String key) {
		long x = 0;
		try {
			x = Long.getLong((String) get(key));
		} catch (Exception e) {

		}
		return x;
	}

	public boolean getBoolean(String key) {
		boolean x = false;
		try {
			x = Boolean.getBoolean((String) get(key));
		} catch (Exception e) {

		}
		return x;
	}

	public Date getDate(String key) {

		return null;
	}

	public String[] getStringArray(String key) {

		try {
			int length = ((String[]) get(key)).length;
			return (String[]) get(key);
		} catch (Exception e) {
			try {
				String x = (String) get(key);
				String[] y = { x };
				return y;
			} catch (Exception ex) {

			}
		}
		return null;
	}

	public String getReturnUrl() {
		return returnUrl;
	}

	public void setReturnUrl(String returnUrl) {
		this.returnUrl = returnUrl;
	}



	public boolean isRedirect() {
		return redirect;
	}

	public void setRedirect(boolean redirect) {
		this.redirect = redirect;
	}

    public String getFormPipe() {
        return formPipe;
    }

    public void setFormPipe(String formPipe) {
        this.formPipe = formPipe;
    }




}
