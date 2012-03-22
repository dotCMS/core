package com.dotmarketing.cms.myaccount.action;

import org.apache.struts.actions.DispatchAction;


public class AddressAction extends DispatchAction {
    /*public ActionForward editAddress(ActionMapping mapping, ActionForm lf, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        MyAccountForm form = (MyAccountForm) lf;

        User user = (User) request.getSession().getAttribute(WebKeys.CMS_USER);
        Address address = null;

        try {
            address = PublicAddressFactory.getAddressById(form.getAddressId());
        } catch (Exception e) {
            LogFactory.getLog(AddressAction.class).debug("Getting new Address");
        }

        if (!"0".equals(form.getAddressId()) && !user.getUserId().equals(address.getUserId())) {
            ActionErrors aes = new ActionErrors();
            LogFactory.getLog(AddressAction.class).error("Invalid addressId " + form.getAddressId() +
                " requested by userId:" + user.getUserId());
            LogFactory.getLog(AddressAction.class).error("Invalid Address requested by userId:" + user.getUserId());
            aes.add(Globals.ERROR_KEY, new ActionMessage("error.invalidAddress"));
            saveMessages(request, aes);
            address = null;
        }

        // set up the crumbtrail
        List list = new ArrayList();
        Map map = new HashMap();
        map.put("title", "Home");
        map.put("url", "/");
        list.add(map);

        map = new HashMap();
        map.put("title", "My Account");
        map.put("url", "/cms/myAccount");
        list.add(map);

        map = new HashMap();
        map.put("title", "Add/Edit Address");
        map.put("url", "/cms/myAccount?dispatch=editAddress&id=");
        map.put("theEnd", "true");
        list.add(map);

        request.setAttribute(com.dotmarketing.util.WebKeys.CRUMB_TRAIL, list);

        if (address != null) {
            // build User address
            form.setAddress1(address.getStreet1());
            form.setAddress2(address.getStreet2());
            form.setCity(address.getCity());
            form.setState(address.getState());
            form.setCountry(address.getCountry());
            form.setPhone(address.getPhone());
            form.setZip(address.getZip());
            form.setPhone(address.getPhone());
            form.setDescription(address.getDescription());
            form.setCell(address.getCell());
            form.setPriority(address.getPriority());
        }

        return mapping.findForward("myAccountAddressPage");
    }

    public ActionForward saveAddress(ActionMapping mapping, ActionForm lf, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        MyAccountForm form = (MyAccountForm) lf;

        ActionErrors ae = null;
        ae = form.validate(mapping, request);

        if ((ae != null) && (ae.size() > 0)) {
            saveMessages(request, ae);
            return mapping.findForward("myAccountAddressPage");
        }

        User user = (User) request.getSession().getAttribute(WebKeys.CMS_USER);
        Address address = PublicAddressFactory.getAddressById(form.getAddressId());
        LogFactory.getLog(AddressAction.class).error("address.getAddressId()" + address.getAddressId());
        LogFactory.getLog(AddressAction.class).error("address.getUserId()" + address.getUserId());
        
        
        if (! "0".equals(form.getAddressId()) && !user.getUserId().equals(address.getUserId()) && UtilMethods.isSet(address.getUserId())) {
            ActionErrors aes = new ActionErrors();
            LogFactory.getLog(AddressAction.class).error("Invalid Address " + form.getAddressId() + " requested by userId:" + user.getUserId());
            aes.add(Globals.ERROR_KEY, new ActionMessage("error.invalidAddress"));
            request.getSession().setAttribute(Globals.ERROR_KEY, aes);

            address = null;
            return mapping.findForward("myAccountAddressPage");
        }

        
        
        
        address.setUserId(user.getUserId());
        address.setStreet1(form.getAddress1());
        address.setStreet2(form.getAddress2());
        address.setCity(form.getCity());
        address.setState(form.getState());
        address.setCountry(form.getCountry());
        address.setZip(form.getZip());
        address.setPhone(form.getPhone());
        
        address.setDescription(form.getDescription());
        address.setCell(form.getCell());
        address.setCompanyId(user.getCompanyId());
        address.setPriority(form.getPriority());
        address.setUserName(user.getFullName());
        address.setModifiedDate(new java.util.Date());
        address.setClassName(User.class.getName());
        address.setClassPK(user.getUserId());

        if (address.getCreateDate() == null) {
            address.setCreateDate(new java.util.Date());
        }



        PublicAddressFactory.save(address);

        ActionMessages am = new ActionMessages();
        am.add(Globals.ERROR_KEY, new ActionMessage("message.addressSaved"));
        request.getSession().setAttribute(Globals.MESSAGE_KEY, am);

        ActionForward af = new ActionForward("/cms/myAccount");
        af.setRedirect(true);

        return af;
    }

    public ActionForward deleteAddress(ActionMapping mapping, ActionForm lf, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        MyAccountForm form = (MyAccountForm) lf;

        User user = (User) request.getSession().getAttribute(WebKeys.CMS_USER);

        Address address = null;

        try {
            address = PublicAddressFactory.getAddressById(form.getAddressId());
        } catch (Exception e) {
            LogFactory.getLog(AddressAction.class).debug("Getting new Address");
        }

        List adds = PublicAddressFactory.getAddressesByUserId(user.getUserId());

        if (!address.isNew() && !user.getUserId().equals(address.getUserId())) {
            ActionErrors aes = new ActionErrors();
            LogFactory.getLog(AddressAction.class).error("Invalid Address requested by userId:" + user.getUserId());
            aes.add(Globals.ERROR_KEY, new ActionMessage("error.invalidAddress"));
            request.getSession().setAttribute(Globals.ERROR_KEY, aes);

            address = null;
        } else if (adds.size() < 2) {
            ActionErrors aes = new ActionErrors();
            aes.add(Globals.ERROR_KEY, new ActionMessage("error.oneAddressRequired"));
            request.getSession().setAttribute(Globals.ERROR_KEY, aes);

            address = null;
        } else {
            PublicAddressFactory.delete(address);

            ActionMessages ams = new ActionMessages();
            ams.add(Globals.MESSAGE_KEY, new ActionMessage("message.addressDeleted"));
            request.getSession().setAttribute(Globals.MESSAGE_KEY, ams);
        }

        ActionForward af = new ActionForward("/cms/myAccount");
        af.setRedirect(true);

        return af;
    }*/
}
