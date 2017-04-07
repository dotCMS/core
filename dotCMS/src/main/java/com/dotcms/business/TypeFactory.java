package com.dotcms.business;

import com.dotcms.api.system.event.SystemEventsFactory;
import com.dotmarketing.beans.Inode;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Map;

/**
 * Defines the Factory to return the instance of the {@link TypeFactory}
 * @author jsanca
 */
public class TypeFactory implements Serializable {

    private final TypeDAO typeDAO = new TypeDAOImpl();

    protected TypeFactory () {}

    /**
     * Singleton holder using initialization on demand
     */
    private static class SingletonHolder {
        private static final TypeFactory INSTANCE = new TypeFactory();
    }

    /**
     * Returns a single instance of this factory.
     *
     * @return A unique {@link SystemEventsFactory} instance.
     */
    public static TypeFactory getInstance() {
        return TypeFactory.SingletonHolder.INSTANCE;
    }

    public TypeDAO getTypeDAO() {
        return typeDAO;
    }

    private final class TypeDAOImpl implements TypeDAO {

        private static final String SELECT_ASSET_TYPE_FROM_IDENTIFIER_WHERE_ID = "select asset_type from identifier where id =?";
        private static final String SELECT_TYPE_FROM_INODE_WHERE_INODE         = "select type from inode where inode = ?";
        private static final String TYPE         = "type";
        private static final String ASSET_TYPE   = "asset_type";
        private static final String INODE        = "inode";


        @Override
        public Class getInodeType(final String inodeId) {

            Class clazz = null;
            final DotConnect dotConnect = new DotConnect();

            try {

                dotConnect.setSQL(SELECT_TYPE_FROM_INODE_WHERE_INODE);
                dotConnect.addParam(inodeId);
                clazz = (dotConnect.loadResults().size() > 0)?
                    InodeUtils.getClassByDBType(dotConnect.getString(TYPE)):null;
            } catch (DotDataException e) {
                // this is not an INODE
                Logger.debug(this,  inodeId + " is not an Inode", e );
                clazz = null;
            }

            return clazz;
        } // getInodeType.

        @Override
        public Class getIdentifierType(final String identifier) {

            Class clazz = null;
            String assetType = null;

            try {

                assetType = this.getIdentifierAssetType(identifier);
                clazz     = (null != assetType)?
                        InodeUtils.getClassByDBType(assetType):null;
            } catch (Exception e) {
                // this is not an INODE
                Logger.debug(this,  identifier + " is not an Identifier", e );
                clazz = null;
            }

            return clazz;
        } // getIdentifierType.

        @Override
        public String getIdentifierAssetType(final String identifier) {

            String assetType   = null;
            final DotConnect dotConnect  = new DotConnect();
            ArrayList assetResult = null;

            try {

                dotConnect.setSQL(SELECT_ASSET_TYPE_FROM_IDENTIFIER_WHERE_ID);
                dotConnect.addParam(identifier);

                assetResult = dotConnect.loadResults();

                assetType = (dotConnect.loadResults().size() > 0)?
                        (String)Map.class.cast(assetResult.get(0)).get(ASSET_TYPE):null;
            } catch (DotDataException e) {
                // this is not an INODE
                Logger.debug(this,  identifier + " is not an Identifier", e );
                assetType = null;
            }

            return assetType;
        } // getIdentifierAssetType.

        @Override
        public Inode findFirstInodeByIdentifier(final String identifier) {

            final String assetType   = this.getIdentifierAssetType(identifier);
            DotConnect dotConnect  = null;
            ArrayList results     = null;
            Inode inode       = null;
            Map resultMap   = null;

            if(UtilMethods.isSet(assetType)) {

                dotConnect = new DotConnect();
                dotConnect.setSQL("select i.inode, type from inode i," +
                        Inode.Type.valueOf(assetType.toUpperCase()).getTableName() +
                        " a where i.inode = a.inode and a.identifier = ?");
                dotConnect.addParam(identifier);
                results = dotConnect.loadResults();

                if(results.size() > 0) {

                    resultMap = (Map)results.get(0);
                    inode     = this.findByInode((String)resultMap.get(INODE),
                                        InodeUtils.getClassByDBType((String)resultMap.get(TYPE)));
                }
            }

            return inode;
        } // findFirstInodeByIdentifier.

        @Override
        public <T extends Inode> T findByInode(final String inodeId, final Class<T> clazz) {

            T inode = null;

            try {

                inode = (T)new HibernateUtil(clazz).load(inodeId);
            } catch (Exception e) {

                Logger.debug(this, e.getMessage(), e);
            }

            return inode;
        } // findByInode.

        @Override
        public Inode findByInode(final String inodeId) {

            final Class realClass = this.getInodeType(inodeId);;

            Logger.debug(this, "You should not send Inode.class to getInode.  Send the extending class instead (inode:" + inodeId + ")" );

            if(null == realClass) {

                Logger.debug(this, "Not any inode associate to the id :" + inodeId + "), returning an empty Inode" );
                return new Inode();
            }

            return this.findByInode(inodeId, realClass);
        } // findByInode.

    } // TypeDAOImpl.
} // E:O:F:TypeFactory.
