package com.dotmarketing.portlets.contentlet.business;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.dotcms.DataProviderWeldRunner;
import com.dotcms.content.elasticsearch.business.ESContentFactoryImpl;
import com.dotcms.contenttype.model.field.DateField;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.TextField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.FieldDataGen;
import com.dotcms.datagen.TestDataUtils;
import com.dotcms.variant.VariantAPI;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.ContentletBaseTest;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.UtilMethods;
import com.google.common.collect.ImmutableList;
import com.rainerhahnekamp.sneakythrow.Sneaky;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.TimeZone;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.enterprise.context.ApplicationScoped;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Created by Jonathan Gamba.
 * Date: 3/19/12
 * Time: 11:32 AM
 */
@ApplicationScoped
@RunWith(DataProviderWeldRunner.class)
public class ContentletFactoryTest extends ContentletBaseTest {

    /**
     * Testing {@link com.dotmarketing.portlets.contentlet.business.ContentletFactory#findAllCurrent()}
     *
     * @throws com.dotmarketing.exception.DotDataException
     *
     * @throws com.dotmarketing.exception.DotSecurityException
     *
     * @see ContentletFactory
     */
    @Test ( expected = DotDataException.class )
    public void findAllCurrent () throws DotDataException, DotSecurityException {

        //Getting all contentlets live/working contentlets
        List<Contentlet> contentlets = contentletFactory.findAllCurrent();

        //Validations
        assertTrue( contentlets != null && !contentlets.isEmpty() );

        //Validate the integrity of the array
        Contentlet contentlet = contentletFactory.find( contentlets.iterator().next().getInode() );

        //Validations
        assertTrue( contentlet != null && ( contentlet.getInode() != null && !contentlet.getInode().isEmpty() ) );
    }

    /**
     * Testing {@link com.dotmarketing.portlets.contentlet.business.ContentletFactory#findAllCurrent(int, int)}
     *
     * @throws com.dotmarketing.exception.DotDataException
     *
     * @throws com.dotmarketing.exception.DotSecurityException
     *
     * @see ContentletFactory
     */
    @Test
    public void findAllCurrentOffsetLimit () throws DotDataException, DotSecurityException {

        final ContentType type = new ContentTypeDataGen().nextPersisted();
        final ContentletDataGen contentletDataGen = new ContentletDataGen(type.id());
        final List<Contentlet> newContentlets = new ArrayList<>();

        try {

            // Let's create 10 contentlets, 5 live 5 working
            IntStream.range(0, 10).forEach(
                    (i) -> {
                        final Contentlet newContent = contentletDataGen.nextPersisted();
                        if (i % 2 == 0) {
                            Sneaky.sneaked(() -> APILocator.getContentletAPI()
                                    .publish(newContent, user, false));
                        }
                        newContentlets.add(newContent);
                    }
            );

            //Getting all contentlets live/working contentlets
            List<Contentlet> contentlets = contentletFactory.findAllCurrent(0, 10);

            //Validations
            assertFalse(contentlets.isEmpty());
            assertTrue(contentlets.size() >= 10);

            // filter out null records because it might contain some
            contentlets = contentlets.stream().filter(Objects::nonNull).collect(Collectors.toList());

            //Search for one of the objects we found
            String inode = contentlets.get(0).getInode();
            Contentlet contentlet = contentletFactory.find(inode);

            //Validations
            assertTrue(
                    contentlet != null && (contentlet.getInode() != null && !contentlet.getInode()
                            .isEmpty()));
        } finally {
            if(UtilMethods.isSet(newContentlets)) {
                APILocator.getContentletAPI().destroy(newContentlets, user, false);
            }
        }
    }

    /**
     * Testing {@link com.dotmarketing.portlets.contentlet.business.ContentletFactory#find(String)}
     *
     * @throws com.dotmarketing.exception.DotDataException
     *
     * @throws com.dotmarketing.exception.DotSecurityException
     *
     * @see ContentletFactory
     */
    @Test
    public void find () throws DotSecurityException, DotDataException {

        //Getting a known contentlet
        Contentlet contentlet = contentlets.iterator().next();

        //Search the contentlet
        Contentlet foundContentlet = contentletFactory.find( contentlet.getInode() );

        //Validations
        assertNotNull( foundContentlet );
        assertEquals( foundContentlet.getInode(), contentlet.getInode() );
    }

    @Test
    public void Create_Contentlet_Then_find_Expect_Cache_Hit_Then_Remove_Expect_404()
            throws DotDataException, NoSuchFieldException, IllegalAccessException, DotSecurityException {
        
        final ContentletCache contentletCache = CacheLocator.getContentletCache();

        final ContentTypeDataGen contentTypeDataGen = new ContentTypeDataGen();
        final ContentType contentType = contentTypeDataGen.name("lol").nextPersisted();
        final ContentletDataGen contentletDataGen = new ContentletDataGen(contentType.id());
        //Create some content
        final List<Contentlet> persistentContentlets = ImmutableList.of(
                    contentletDataGen.nextPersisted(),
                    contentletDataGen.nextPersisted(),
                    contentletDataGen.nextPersisted()
        );
        //Find it and make sure that nothing like a 404 comes back from cache
        for(final Contentlet contentlet:persistentContentlets){
             final String inode = contentlet.getInode();
             assertNotNull(inode);
             assertNull(contentletCache.get(inode));
             assertNotNull(contentletFactory.find( inode ));
             assertNotNull(contentletCache.get(inode));
             assertNotEquals(ESContentFactoryImpl.CACHE_404_CONTENTLET, contentletCache.get(inode).getInode());
        }
        //Remove'em all
        contentletFactory.delete(persistentContentlets);
        //Now if we request them again, null must come back from the factory but a 404  must come back from cache
        for(final Contentlet contentlet:persistentContentlets){
            final String inode = contentlet.getInode();
            assertNotNull(inode);
            assertNull(contentletCache.get(inode));
            assertNull(contentletFactory.find( inode ));
            assertEquals(ESContentFactoryImpl.CACHE_404_CONTENTLET, contentletCache.get(inode).getInode());
        }
    }

    /**
     * Here we test that...
     * if we create 3 pieces of content of the same type our count method reflects that accurately
     * Also if we create a new version of the same content and we count again we should still get 3
     * That unless we use the param that will  include All Versions of the contentlets
     */
    @Test
    public void Create_Contentlets_Then_Count() {
        final ContentTypeDataGen contentTypeDataGen = new ContentTypeDataGen();
        final ContentType contentType = contentTypeDataGen.name("any").nextPersisted();
        final ContentletDataGen contentletDataGen = new ContentletDataGen(contentType.id());
        //Create some content
        final Contentlet con1 = contentletDataGen.nextPersisted();
        final Contentlet con2 =  contentletDataGen.nextPersisted();
        final Contentlet con3 =  contentletDataGen.nextPersisted();

        long count = contentletFactory.countByType(contentType, false);
        Assert.assertEquals(3, count);

        //This should generate another version of con1
        final Contentlet newVersion = ContentletDataGen.checkout(con1);
        newVersion.setBoolProperty(Contentlet.DONT_VALIDATE_ME, true);
        ContentletDataGen.checkin(newVersion);

        count = contentletFactory.countByType(contentType, false);
        Assert.assertEquals(3, count);

        count = contentletFactory.countByType(contentType, true);
        Assert.assertEquals(4, count);
    }

    /**
     * Method to test: {@link ContentletFactory.findContentletByIdentifier(String, long, String, Date)}
     * Given scenario: A contentlet is created with a future publish date. The method is called with a date in the future
     * Expected result: When the method is called with a date in the future, the contentlet is found. When the method is called with a date in the past, the contentlet is not found.
     * @throws DotDataException
     */
    @Test
    public void Test_Find_Contentlet_By_Identifier_Using_Time_Machine_Date() throws DotDataException {
        final TimeZone aDefault = TimeZone.getDefault();
        final TimeZone utc = TimeZone.getTimeZone("UTC");
        try {
            final String defaultVariant = VariantAPI.DEFAULT_VARIANT.name();
            TimeZone.setDefault(utc);
            final LocalDateTime now = LocalDateTime.now();
            final Instant instant1 = now.plusDays(4).atZone(utc.toZoneId()).toInstant();
            final Date publishDate = Date.from(instant1);
            final Instant instant2 = now.plusDays(8).atZone(utc.toZoneId()).toInstant();
            final Date expireDate = Date.from(instant2);

            final Instant instant4 = now.plusDays(16).atZone(utc.toZoneId()).toInstant();
            final Date pastExpireDate = Date.from(instant4);

            final ContentType contentType = new ContentTypeDataGen()
                    .fields(List.of(
                            new FieldDataGen()
                                    .type(TextField.class)
                                    .velocityVarName("title")
                                    .indexed(true)
                                    .next(),
                            new FieldDataGen()
                                    .name("postingDate")
                                    .velocityVarName("publishDate")
                                    .defaultValue(null)
                                    .type(DateField.class)
                                    .next(),
                            new FieldDataGen()
                                    .name("takeDownDate")
                                    .velocityVarName("expireDate")
                                    .defaultValue(null)
                                    .type(DateField.class)
                                    .next()
                    ))
                    .publishDateFieldVarName("publishDate")
                    .expireDateFieldVarName("expireDate")
                    .nextPersisted();
            assertNotNull(contentType.publishDateVar());
            final Contentlet contentlet = new ContentletDataGen(contentType.id())
                    .languageId(1L)
                    .setProperty("title", "Hello World")
                    .setProperty("publishDate", publishDate)
                    .setProperty("expireDate", expireDate)
                    .nextPersistedAndPublish();

            //Try to find it using the current date and time
            final Contentlet nowPublished = contentletFactory.findContentletByIdentifier(
                    contentlet.getIdentifier(),
                    contentlet.getLanguageId(), defaultVariant, new Date());
            // no match is expected
            assertNull(nowPublished);

            //Try to find it using the future date
            final Contentlet futurePublish = contentletFactory.findContentletByIdentifier(
                    contentlet.getIdentifier(),
                    contentlet.getLanguageId(), defaultVariant, publishDate);
            // match is expected
            assertNotNull(futurePublish);
            assertEquals(contentlet.getIdentifier(), futurePublish.getIdentifier());
            assertEquals(contentlet.getInode(), futurePublish.getInode());

            //Past the expire-date we shouldn't find a bit of content
            final Contentlet pastExpired = contentletFactory.findContentletByIdentifier(
                    contentlet.getIdentifier(),
                    contentlet.getLanguageId(), defaultVariant, pastExpireDate);
            assertNull(pastExpired);

        } finally {
            TimeZone.setDefault(aDefault);
        }
    }

}