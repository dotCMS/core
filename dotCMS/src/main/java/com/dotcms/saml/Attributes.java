package com.dotcms.saml;

import com.dotmarketing.util.UUIDUtil;
import com.dotmarketing.util.UtilMethods;

import java.io.Serializable;
import java.util.Map;

/**
 * Encapsulates the attributes retrieve from the Saml Assertion
 *
 * @author jsanca
 */
public class Attributes implements Serializable {

	private static final long serialVersionUID = 1836313856887837731L;

	// user email from opensaml
	private final String email;

	// user last name from opensaml
	private final String lastName;

	// user first name from opensaml
	private final String firstName;

	// true if opensaml returned roles
	private final boolean addRoles;

	// Saml object with the roles info.
	private final Object roles;

	// Saml object with the NameID.
	private final Object nameID;

	// SAML Session Index
	private final String sessionIndex;

	private final Map<String, Object> additionalAttributes;

	private Attributes(final Builder builder) {

		final String uuid = UUIDUtil.uuid();
		this.email        = UtilMethods.isSet(builder.email)?     builder.email:     uuid+"@dotcms.com";
		this.lastName     = UtilMethods.isSet(builder.lastName)?  builder.lastName:  uuid+"lastName";
		this.firstName    = UtilMethods.isSet(builder.firstName)? builder.firstName: uuid+"firstName";
		this.addRoles     = builder.addRoles;
		this.roles        = builder.roles;
		this.nameID       = builder.nameID;
		this.sessionIndex = builder.sessionIndex;
		this.additionalAttributes = builder.additionalAttributes;
	}

	public String getEmail()
	{
		return email;
	}

	public String getLastName()
	{
		return lastName;
	}

	public String getFirstName()
	{
		return firstName;
	}

	public boolean isAddRoles()
	{
		return addRoles;
	}

	public Object getRoles()
	{
		return roles;
	}

	public Object getNameID()
	{
		return nameID;
	}

	public String getSessionIndex() {
		return sessionIndex;
	}

	public Map<String, Object> getAdditionalAttributes() {
		return additionalAttributes;
	}

	@Override
	public String toString() {
		return "Attributes{" +
				"email='" + email + '\'' +
				", lastName='" + lastName + '\'' +
				", firstName='" + firstName + '\'' +
				", addRoles=" + addRoles +
				", roles=" + roles +
				", nameID=" + nameID +
				", sessionIndex='" + sessionIndex + '\'' +
				", additionalAttributes=" + additionalAttributes +
				'}';
	}

	public static final class Builder {
		String email     = "";
		String lastName  = "";
		String firstName = "";
		boolean addRoles = false;
		Object roles     = null;
		Object nameID    = null;
		String sessionIndex;
		Map<String, Object> additionalAttributes;

		public Builder additionalAttributes(final Map<String, Object> additionalAttributes)
		{
			this.additionalAttributes = additionalAttributes;
			return this;
		}

		public Builder email(final String email )
		{
			this.email = email;
			return this;
		}

		public Builder lastName(final String lastName )
		{
			this.lastName = lastName;
			return this;
		}

		public Builder firstName(final String firstName)
		{
			this.firstName = firstName;
			return this;
		}

		public Builder addRoles(final boolean addRoles)
		{
			this.addRoles = addRoles;
			return this;
		}

		public Builder roles(final Object roles)
		{
			this.roles = roles;
			return this;
		}

		public Builder nameID(final Object nameID)
		{
			this.nameID = nameID;
			return this;
		}

		public Builder sessionIndex(final String sessionIndex)
		{
			this.sessionIndex = sessionIndex;
			return this;
		}


		public String getEmail()
		{
			return email;
		}

		public String getLastName()
		{
			return lastName;
		}

		public String getFirstName()
		{
			return firstName;
		}

		public boolean isAddRoles()
		{
			return addRoles;
		}

		public Object getRoles()
		{
			return roles;
		}

		public Object getNameID()
		{
			return nameID;
		}

		public Attributes build()
		{
			return new Attributes( this );
		}
	}
}
