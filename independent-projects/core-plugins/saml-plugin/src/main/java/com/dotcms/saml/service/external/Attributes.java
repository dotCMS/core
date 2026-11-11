package com.dotcms.saml.service.external;

import org.opensaml.saml.saml2.core.Attribute;
import org.opensaml.saml.saml2.core.NameID;

import java.io.Serializable;

/**
 * Encapsulates the attributes retrieve from the
 * {@link org.opensaml.saml.saml2.core.Assertion}
 *
 * This class only encapsulates a few parameters, future forward we need tor encapsulate such as
 * a map to group extra attributes that will be place on the Information Json field of the User
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
	private final Attribute roles;

	// Saml object with the NameID.
	private final NameID nameID;

	private Attributes(final Builder builder) {

		this.email = builder.email;
		this.lastName = builder.lastName;
		this.firstName = builder.firstName;
		this.addRoles = builder.addRoles;
		this.roles = builder.roles;
		this.nameID = builder.nameID;
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

	public Attribute getRoles()
	{
		return roles;
	}

	public NameID getNameID()
	{
		return nameID;
	}

	@Override
	public String toString() {

		return "AttributesBean{" + "nameID='" + nameID.getValue() + '\'' + ", email='" + email + '\'' + ", lastName='" + lastName + '\'' + ", firstName='" + firstName + '\'' + ", addRoles=" + addRoles + ", roles=" + roles + '}';
	}

	public static final class Builder {

		String email = "";
		String lastName = "";
		String firstName = "";
		boolean addRoles = false;
		Attribute roles = null;
		NameID nameID = null;

		public Builder email(final String email) {
			this.email = email;
			return this;
		}

		public Builder lastName(final String lastName ) {
			this.lastName = lastName;
			return this;
		}

		public Builder firstName(final String firstName ) {
			this.firstName = firstName;
			return this;
		}

		public Builder addRoles(final boolean addRoles ) {
			this.addRoles = addRoles;
			return this;
		}

		public Builder roles(final Attribute roles ) {
			this.roles = roles;
			return this;
		}

		public Builder nameID( NameID nameID ) {
			this.nameID = nameID;
			return this;
		}

		public String getEmail() {
			return email;
		}

		public String getLastName() {
			return lastName;
		}

		public String getFirstName() {
			return firstName;
		}

		public boolean isAddRoles() {
			return addRoles;
		}

		public Attribute getRoles() {
			return roles;
		}

		public NameID getNameID() {
			return nameID;
		}

		public Attributes build() {
			return new Attributes( this );
		}
	}
}
