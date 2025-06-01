package com.dotcms.rendering.velocity.services;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.UtilMethods;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.Method;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit test for PageRenderUtil date validation logic.
 */
public class PageRenderUtilDateValidationTest {

    @Mock
    private Contentlet mockContentlet;

    @Mock
    private ContentType mockContentType;

    private PageRenderUtil pageRenderUtil;
    private Method hasInvalidDateConfigurationMethod;

    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        
        hasInvalidDateConfigurationMethod = PageRenderUtil.class.getDeclaredMethod("hasInvalidDateConfiguration", Contentlet.class);
        hasInvalidDateConfigurationMethod.setAccessible(true);
    }

    @Test
    public void testValidDateConfiguration() throws Exception {
        Date publishDate = new Date(System.currentTimeMillis());
        Date expireDate = new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(10));
        
        setupMockContentlet("publishDateField", "expireDateField", publishDate, expireDate);
        
        boolean result = testHasInvalidDateConfiguration(mockContentlet);
        
        assertFalse(result, "Should return false for valid date configuration (publishDate < expireDate)");
    }

    @Test
    public void testInvalidDateConfiguration() throws Exception {
        Date publishDate = new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(10));
        Date expireDate = new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(5));
        
        setupMockContentlet("publishDateField", "expireDateField", publishDate, expireDate);
        
        boolean result = testHasInvalidDateConfiguration(mockContentlet);
        
        assertTrue(result, "Should return true for invalid date configuration (expireDate < publishDate)");
    }

    @Test
    public void testMissingPublishDateField() throws Exception {
        setupMockContentlet(null, "expireDateField", null, new Date());
        
        boolean result = testHasInvalidDateConfiguration(mockContentlet);
        
        assertFalse(result, "Should return false when publishDateVar is not set");
    }

    @Test
    public void testMissingExpireDateField() throws Exception {
        setupMockContentlet("publishDateField", null, new Date(), null);
        
        boolean result = testHasInvalidDateConfiguration(mockContentlet);
        
        assertFalse(result, "Should return false when expireDateVar is not set");
    }

    @Test
    public void testNullPublishDate() throws Exception {
        setupMockContentlet("publishDateField", "expireDateField", null, new Date());
        
        boolean result = testHasInvalidDateConfiguration(mockContentlet);
        
        assertFalse(result, "Should return false when publishDate is null");
    }

    @Test
    public void testNullExpireDate() throws Exception {
        setupMockContentlet("publishDateField", "expireDateField", new Date(), null);
        
        boolean result = testHasInvalidDateConfiguration(mockContentlet);
        
        assertFalse(result, "Should return false when expireDate is null");
    }

    @Test
    public void testEqualDates() throws Exception {
        Date sameDate = new Date(System.currentTimeMillis());
        
        setupMockContentlet("publishDateField", "expireDateField", sameDate, sameDate);
        
        boolean result = testHasInvalidDateConfiguration(mockContentlet);
        
        assertFalse(result, "Should return false when publishDate equals expireDate");
    }

    private void setupMockContentlet(String publishDateVar, String expireDateVar, Date publishDate, Date expireDate) {
        when(mockContentType.publishDateVar()).thenReturn(publishDateVar);
        when(mockContentType.expireDateVar()).thenReturn(expireDateVar);
        when(mockContentlet.getContentType()).thenReturn(mockContentType);
        
        Map<String, Object> contentletMap = new HashMap<>();
        if (publishDateVar != null) {
            contentletMap.put(publishDateVar, publishDate);
        }
        if (expireDateVar != null) {
            contentletMap.put(expireDateVar, expireDate);
        }
        when(mockContentlet.getMap()).thenReturn(contentletMap);
    }

    private boolean testHasInvalidDateConfiguration(Contentlet contentlet) {
        final ContentType contentType = contentlet.getContentType();
        
        if (!UtilMethods.isSet(contentType.publishDateVar()) || 
            !UtilMethods.isSet(contentType.expireDateVar())) {
            return false;
        }
        
        final Date publishDate = (Date) contentlet.getMap().get(contentType.publishDateVar());
        final Date expireDate = (Date) contentlet.getMap().get(contentType.expireDateVar());
        
        if (publishDate == null || expireDate == null) {
            return false;
        }
        
        return expireDate.before(publishDate);
    }
}