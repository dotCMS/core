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
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.ContentletBaseTest;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.google.common.collect.ImmutableList;
import java.util.List;
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

        //Getting all contentlets live/working contentlets
        List<Contentlet> contentlets = contentletFactory.findAllCurrent( 0, 5 );

        //Validations
        assertTrue( contentlets != null && !contentlets.isEmpty() );
        assertEquals( contentlets.size(), 5 );

        //Validate the integrity of the array
        Contentlet foundContentlet = null;
        for ( Contentlet contentlet : contentlets ) {

            //TODO: We need to verify for null because the findAllCurrent CAN return null objects, this could happen because the index can return inodes that are not into the db....
            if ( contentlet != null ) {
                foundContentlet = contentlet;
                break;
            }
        }

        //Validations
        assertNotNull( foundContentlet );

        //Search for one of the objects we found
        String inode = foundContentlet.getInode();
        Contentlet contentlet = contentletFactory.find(inode);

        //Validations
        assertTrue( contentlet != null && ( contentlet.getInode() != null && !contentlet.getInode().isEmpty() ) );
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