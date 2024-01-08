package com.dotcms.system.announcements;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.time.Instant;
import org.immutables.value.Value;

@Value.Style(typeImmutable="*", typeAbstract="Abstract*")
@Value.Immutable
@JsonDeserialize(as = Announcement.Builder.class)
public interface AbstractAnnouncement {

    String identifier();

    String inode();

    String languageCode();
    long languageId();

    String type();

    String title();

    String url();

    //I could have used an Instant here, but I ran into some issues with the jackson mapper since I need to register a new module to make a conversion from a string to an Instant
    //and at the end what we need is just a string representation of the date in the front end
    Instant date();

    String  dateAsISO8601();

}
