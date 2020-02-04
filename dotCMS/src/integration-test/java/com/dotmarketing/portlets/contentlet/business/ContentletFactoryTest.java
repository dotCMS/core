package com.dotmarketing.portlets.contentlet.business;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.dotcms.content.elasticsearch.business.ESContentFactoryImpl;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.ContentletBaseTest;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.UtilMethods;
import com.google.common.collect.ImmutableList;
import com.rainerhahnekamp.sneakythrow.Sneaky;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.Test;

/**
 * Created by Jonathan Gamba.
 * Date: 3/19/12
 * Time: 11:32 AM
 */
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

            // filter out null records
            contentlets = contentlets.stream().filter(Objects::nonNull).collect(Collectors.toList());
            //Validations
            assertTrue(contentlets != null && !contentlets.isEmpty());
            assertTrue(contentlets.size() >= 10);

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


}