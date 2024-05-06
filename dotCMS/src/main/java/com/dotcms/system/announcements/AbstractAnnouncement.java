package com.dotcms.system.announcements;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.time.Instant;
import javax.annotation.Nullable;
import org.immutables.value.Value;

@Value.Style(typeImmutable="*", typeAbstract="Abstract*")
@Value.Immutable
@JsonDeserialize(as = Announcement.Builder.class)
public interface AbstractAnnouncement {

    String identifier();

    String inode();

    String languageId();

    String type();

    String title();

    @Nullable
    String description();

    String url();

    Instant announcementDate();

    String  announcementDateAsISO8601();

    Instant modDate();

    String  modDateAsISO8601();

}
