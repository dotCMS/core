package com.dotcms.saml;

import java.io.Serializable;

/**
 * Encapsulates the attributes retrieve from the Saml Assertion
 *
 * @author jsanca
 */
public class Attributes implements Serializable
{
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

	private Attributes(final Builder builder) {

		this.email     = builder.email;
		this.lastName  = builder.lastName;
		this.firstName = builder.firstName;
		this.addRoles  = builder.addRoles;
		this.roles     = builder.roles;
		this.nameID    = builder.nameID;
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

	@Override
	public String toString() {
		return "AttributesBean{" + "nameID='" + nameID + '\'' + ", email='" + email + '\'' + ", lastName='" + lastName + '\''
				+ ", firstName='" + firstName + '\'' + ", addRoles=" + addRoles + ", roles=" + roles + '}';
	}

	public static final class Builder {
		String email     = "";
		String lastName  = "";
		String firstName = "";
		boolean addRoles = false;
		Object roles     = null;
		Object nameID    = null;

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
