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

    Instant date();

    String  dateAsISO8601();

    Instant modDate();

    String  modDateAsISO8601();

}
