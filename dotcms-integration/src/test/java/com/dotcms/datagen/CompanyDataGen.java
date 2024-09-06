package com.dotcms.datagen;

import com.dotmarketing.cms.factories.PublicCompanyFactory;
import com.liferay.portal.SystemException;
import com.liferay.portal.model.Company;

public class CompanyDataGen extends AbstractDataGen<Company> {

    private String key;
    private String portalURL;
    private String homeURL;
    private String mx;
    private String name;
    private String shortName;
    private String type;
    private String size;
    private String street;
    private String city;
    private String state;
    private String zip;
    private String phone;
    private String fax;
    private String emailAddress;
    private String authType;
    private boolean autoLogin;
    private boolean strangers;

    public CompanyDataGen key(String key) {
        this.key = key;
        return this;
    }

    public CompanyDataGen portalURL(String portalURL) {
        this.portalURL = portalURL;
        return this;
    }

    public CompanyDataGen homeURL(String homeURL) {
        this.homeURL = homeURL;
        return this;
    }

    public CompanyDataGen mx(String mx) {
        this.mx = mx;
        return this;
    }

    public CompanyDataGen name(String name) {
        this.name = (name + System.currentTimeMillis());
        return this;
    }

    public CompanyDataGen shortName(String shortName) {
        this.shortName = shortName;
        return this;
    }

    public CompanyDataGen type(String type) {
        this.type = type;
        return this;
    }

    public CompanyDataGen size(String size) {
        this.size = size;
        return this;
    }

    public CompanyDataGen street(String street) {
        this.street = street;
        return this;
    }

    public CompanyDataGen city(String city) {
        this.city = city;
        return this;
    }

    public CompanyDataGen state(String state) {
        this.state = state;
        return this;
    }

    public CompanyDataGen zip(String zip) {
        this.zip = zip;
        return this;
    }

    public CompanyDataGen phone(String phone) {
        this.phone = phone;
        return this;
    }

    public CompanyDataGen fax(String fax) {
        this.fax = fax;
        return this;
    }

    public CompanyDataGen emailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
        return this;
    }

    public CompanyDataGen authType(String authType) {
        this.authType = authType;
        return this;
    }

    public CompanyDataGen autoLogin(boolean autoLogin) {
        this.autoLogin = autoLogin;
        return this;
    }

    public CompanyDataGen strangers(boolean strangers) {
        this.strangers = strangers;
        return this;
    }

    @Override
    public Company next() {
        final Company company = PublicCompanyFactory.create(name);
        company.setHomeURL(homeURL);
        company.setPortalURL(portalURL);
        company.setMx(mx);
        company.setName(name);
        company.setShortName(shortName);
        company.setKey(key);
        company.setAuthType(authType);
        company.setAutoLogin(autoLogin);
        company.setCity(city);
        company.setEmailAddress(emailAddress);
        company.setFax(fax);
        company.setPhone(phone);
        company.setSize(size);
        company.setState(state);
        company.setStrangers(strangers);
        company.setStreet(street);
        company.setType(type);
        company.setZip(zip);
        return company;
    }

    @Override
    public Company persist(Company object) {
        try {
            return PublicCompanyFactory.update(object);
        } catch (SystemException e) {
            throw new RuntimeException("Unable to persist company.", e);
        }
    }

    @Override
    public Company nextPersisted() {
        final Company next = next();
        return persist(next);
    }
}
