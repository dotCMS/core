package com.dotcms.cdn.pushpublish.receiver.event;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dotcms.publisher.business.PublishQueueElement;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.liferay.portal.model.User;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for how {@link PushPublishOnReceiverEndSubscriber} finds the contentlet behind each
 * received queue element before purging it from the CDN.
 */
class PushPublishOnReceiverEndSubscriberTest {

    private final ContentletAPI contentletAPI = mock(ContentletAPI.class);
    private final User user = mock(User.class);

    /**
     * Elements built from a bundle manifest hold the contentlet's inode and no language id. They
     * must be looked up by inode rather than unboxing the missing language.
     */
    @Test
    void resolveContentlet_manifestElementWithoutLanguage_findsByInode() throws Exception {
        final Contentlet contentlet = new Contentlet();
        when(contentletAPI.find("inode-1", user, false)).thenReturn(contentlet);

        final Contentlet found = PushPublishOnReceiverEndSubscriber
                .resolveContentlet(contentletAPI, user, element("inode-1", null));

        assertSame(contentlet, found);
        verify(contentletAPI, never()).findContentletByIdentifier(anyString(), anyBoolean(),
                anyLong(), any(User.class), anyBoolean());
    }

    @Test
    void resolveContentlet_elementWithLanguage_findsByIdentifierAndLanguage() throws Exception {
        final Contentlet contentlet = new Contentlet();
        when(contentletAPI.findContentletByIdentifier("id-1", false, 2L, user, false))
                .thenReturn(contentlet);

        final Contentlet found = PushPublishOnReceiverEndSubscriber
                .resolveContentlet(contentletAPI, user, element("id-1", 2));

        assertSame(contentlet, found);
        verify(contentletAPI, never()).find(anyString(), any(User.class), anyBoolean());
    }

    @Test
    void resolveContentlet_lookupFails_returnsNull() throws Exception {
        when(contentletAPI.find("missing", user, false))
                .thenThrow(new DotDataException("not found"));

        assertNull(PushPublishOnReceiverEndSubscriber
                .resolveContentlet(contentletAPI, user, element("missing", null)));
    }

    private static PublishQueueElement element(final String asset, final Integer languageId) {
        final PublishQueueElement element = new PublishQueueElement();
        element.setAsset(asset);
        element.setType("contentlet");
        element.setLanguageId(languageId);
        return element;
    }
}
