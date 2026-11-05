package com.dotcms.saml.service.impl;

import org.apache.velocity.app.VelocityEngine;
import org.opensaml.saml.saml2.binding.encoding.impl.HTTPPostEncoder;

/**
 * Http Post Deflate Encoder
 * Pre: velocity engine
 * This class uses velocity to creates a html form to do the client post auth request XML
 * @author jsanca
 */
public class DotHTTPPOSTDeflateEncoder extends HTTPPostEncoder {

    public DotHTTPPOSTDeflateEncoder(final VelocityEngine velocityEngine) {
        this.setVelocityTemplateId("/templates/saml2-post-binding.vm");
        this.setVelocityEngine(velocityEngine);
    }
}
