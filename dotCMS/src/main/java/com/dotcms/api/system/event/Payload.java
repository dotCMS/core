package com.dotcms.api.system.event;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.cms.login.LoginService;
import com.dotcms.cms.login.LoginServiceFactory;
import com.dotcms.rest.api.v1.system.websocket.SessionWrapper;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.UserAPI;
import com.liferay.portal.model.User;

import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;
import java.util.Map;

/**
 * This class wraps the payload of a System Event, specifying its type (i.e.,
 * the Java class that the payload represents) so that the
 * marshalling/unmarshalling process can be performed without issues.
 * 
 * @author jsanca
 * @version 3.7
 * @since Jul 13, 2016
 */
@SuppressWarnings("serial")
public class Payload implements Serializable {

	private final String type;
	private final Object data;
	private final Visibility visibility;
	private final String  visibilityId; // user id, role uid or permission, if it is global, this is not need
	private final Map<String, Object> user;

	/**
	 * Creates a payload object.
	 *
	 * @param data {@link Object}
	 *            - Any Java object that represents the payload.
	 */
	public Payload(final Object data) {

		this(data, Visibility.GLOBAL, (String) null, null);
	}
	
	/**
	 * Creates a payload object without data. This method generates an empty 
	 * object payload using an empty Void generic class object. This allows 
	 * to send events that doesn't requires any data input on their payload.
	 *
	 */
	public Payload() {

		this(new Void(), Visibility.GLOBAL, (String) null, null);
	}
	
	/**
	 * Creates a payload object without data, but allowing to set the visility 
	 * and visibilityid. This method generates an empty object payload using an 
	 * empty Void generic class object. This allows to send events that doesn't 
	 * requires any data input on their payload.
	 * 
	 * @param visibility {@link Visibility}
	 * 			  - If the event should be apply just for a specific user, role or global
	 * @param visibilityId {@link String}
	 * 			  - Depending of the visibility type, this could be an userId or roleId, for global just keep it null.
	 */
	public Payload(final Visibility visibility,
				   final String visibilityId) {

		this(new Void(), visibility, visibilityId, null);
	}

	public Payload(final Object data,
				   final Visibility visibility,
				   final String visibilityId) {
		this(data, visibility, visibilityId, null);
	}

	/**
	 * Creates a payload object.
	 * 
	 * @param data {@link Object}
	 *            - Any Java object that represents the payload.
	 * @param visibility {@link Visibility}
	 * 			  - If the event should be apply just for a specific user, role or global
	 * @param visibilityId {@link String}
	 * 			  - Depending of the visibility type, this could be an userId or roleId, for global just keep it null.
	 */
	public Payload(final Object data,
				   final Visibility visibility,
				   final String visibilityId,
				   final Map<String, Object> user) {

		this.type = data.getClass().getName();
		this.data = data;
		this.visibility = visibility;
		this.visibilityId = visibilityId;

		if (user == null || user.isEmpty()) {
			this.user = LoginServiceFactory.getInstance().getLoginService().getLogInUser().toMap();
		}else{
			this.user = user;
		}
	}

	/**
	 * Returns the type (fully qualified name) of the Java class representing
	 * the payload of the System Event.
	 * 
	 * @return The fully qualified name of the payload class.
	 */
	public String getType() {
		return type;
	}

	/**
	 * Returns the System Event payload.
	 * 
	 * @return The payload.
	 */
	public Object getData() {

		Object data = this.data;

		if (data instanceof DataWrapper){
			data = DataWrapper.class.cast(data).getData();
		}

		return data;
	}

	public Object getRawData() {
		return this.data;
	}

	/**
	 * Returns the Visibility for this event
	 *
	 * @return Visibility
     */
	public Visibility getVisibility() {
		return visibility;
	}

	/**
	 * Returns the visibility id
	 * @return Object
     */
	public String getVisibilityId() {
		return visibilityId;
	}

	public boolean verified(SessionWrapper session) {
		return visibility.verified(session, this);
	}

	/**
	 * return the user who fire the event, if the user was fired by a Job and not by a User's action then the user
	 * is the System User
	 *
	 * @return
     */
	public Map<String, Object> getUser() {
		return user;
	}
} // E:O:F:Payload.
