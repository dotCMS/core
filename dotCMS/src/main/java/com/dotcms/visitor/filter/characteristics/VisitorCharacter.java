package com.dotcms.visitor.filter.characteristics;

import com.dotcms.visitor.domain.Visitor.AccruedTag;
import com.dotmarketing.portlets.personas.model.IPersona;
import com.google.common.collect.ImmutableList;
import eu.bitwalker.useragentutils.UserAgent;
import io.vavr.control.Try;
import java.util.List;
import org.apache.commons.codec.digest.DigestUtils;

public class VisitorCharacter extends AbstractCharacter {


    public VisitorCharacter(AbstractCharacter incomingCharacter) {
        super(incomingCharacter);


        final IPersona persona = visitor.getPersona();
        final String dmid = (visitor.getDmid() == null) ? null : visitor.getDmid().toString();
        final String device = visitor.getDevice();
        final List<AccruedTag> tags = visitor.getTags();
        final UserAgent agent = visitor.getUserAgent();
        final int pagesViewed = visitor.getNumberPagesViewed();
        final String remoteIp = Try.of(()-> visitor.getIpAddress().getHostAddress()).getOrElse(request.getRemoteAddr());

        accrue("ipHash", DigestUtils.sha1Hex(remoteIp));
        accrue("dmid", dmid);
        accrue("device", device);
        accrue("weightedTags", ImmutableList.copyOf(tags));
        accrue("persona", (persona != null) ? persona.getKeyTag() : null);
        accrue("pagesViewed", pagesViewed);
        accrue("agent", agent);


    }

}
