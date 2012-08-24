package com.dotmarketing.logConsole.business;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.logConsole.model.LogMapperRow;
import com.dotmarketing.util.Logger;
import org.apache.commons.beanutils.BeanUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ConsoleLogFactoryImpl implements ConsoleLogFactory {

    public List convertListToObjects ( List<Map<String, Object>> rs, Class clazz ) throws DotDataException {

        final List ret = new ArrayList();
        try {
            for ( final Map<String, Object> map : rs ) {
                ret.add( this.convertMaptoObject( map, clazz ) );
            }
        } catch ( final Exception e ) {
            throw new DotDataException( "cannot convert object to " + clazz + " " + e.getMessage() );

        }
        return ret;

    }

    public Object convertMaptoObject ( Map<String, Object> map, Class clazz ) throws InstantiationException, IllegalAccessException, InvocationTargetException {

        final Object obj = clazz.newInstance();

        if ( obj instanceof LogMapperRow ) {
            return this.convertLogMapper( map );
        }

        return this.convert( obj, map );
    }

    private Object convertLogMapper ( Map<String, Object> row ) throws IllegalAccessException, InvocationTargetException {

        final LogMapperRow scheme = new LogMapperRow();
        row.put( "actionId", row.get( "workflow_action_id" ) );

        BeanUtils.copyProperties( scheme, row );
        return scheme;
    }

    private Object convert ( Object obj, Map<String, Object> map ) throws IllegalAccessException, InvocationTargetException {
        BeanUtils.copyProperties( obj, map );
        return obj;
    }

    public List<LogMapperRow> findLogMapper () throws DotDataException {

        final DotConnect db = new DotConnect();
        try {

            db.setSQL( ConsoleLoggerSQL.SELECT_LOGGING_CRITERIA );
            db.loadResult();

        } catch ( final Exception e ) {
            Logger.debug( this.getClass(), e.getMessage(), e );
        }

        return (List<LogMapperRow>) this.convertListToObjects( db.loadObjectResults(), LogMapperRow.class );
    }

    public void updateLogMapper ( LogMapperRow r ) throws DotDataException {

        final DotConnect db = new DotConnect();
        try {

            db.setSQL( ConsoleLoggerSQL.UPDATE_LOGGING_CRITERIA );

            Boolean enabled = r.getEnabled();
            if ( DbConnectionFactory.isPostgres() ) {
                db.addParam( enabled ? 1 : 0 );
            } else if ( DbConnectionFactory.isMsSql() ) {
                db.addParam( enabled ? 1 : 0 );
            } else if ( DbConnectionFactory.isMySql() ) {
                db.addParam( enabled );
            } else if ( DbConnectionFactory.isOracle() ) {
                db.addParam( enabled ? 1 : 0 );
            }

            db.addParam( r.getLog_name() );
            db.loadResult();

        } catch ( final Exception e ) {
            Logger.debug( this.getClass(), e.getMessage(), e );
        } finally {
            HibernateUtil.commitTransaction();
        }

    }

}