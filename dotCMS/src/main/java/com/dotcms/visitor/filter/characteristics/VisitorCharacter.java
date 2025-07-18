package com.dotcms.visitor.filter.characteristics;

import com.dotcms.visitor.domain.Visitor.AccruedTag;

import com.dotmarketing.portlets.personas.model.IPersona;

import com.dotmarketing.util.WebKeys;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.digest.DigestUtils;

import com.google.common.collect.ImmutableList;

import eu.bitwalker.useragentutils.UserAgent;

public class VisitorCharacter extends AbstractCharacter {


    public VisitorCharacter(AbstractCharacter incomingCharacter) {
        super(incomingCharacter);

        final IPersona persona = visitor.getPersona();
        final String dmid = (visitor.getDmid() == null) ? null : visitor.getDmid().toString();
        final String device = visitor.getDevice();
        final List<AccruedTag> tags = visitor.getTags();
        final UserAgent agent = visitor.getUserAgent();
        final int pagesViewed = visitor.getNumberPagesViewed();
        final String remoteIp = visitor.getIpAddress().getHostAddress();

        getMap().put("ipHash", DigestUtils.sha1Hex(remoteIp));
        getMap().put(WebKeys.LONG_LIVED_DOTCMS_ID_COOKIE, dmid);
        getMap().put("device", device);
        getMap().put("weightedTags", ImmutableList.copyOf(tags));
        getMap().put("persona", (persona != null) ? persona.getKeyTag() : null);
        getMap().put("pagesViewed", pagesViewed);
        getMap().put("agent", agent);


    }

}
