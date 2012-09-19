package com.dotmarketing.portlets.osgi.AJAX;

import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.OSGIUtil;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.IOUtils;
import org.osgi.framework.BundleException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class OSGIAJAX extends OSGIBaseAJAX {

    @Override
    public void action ( HttpServletRequest request, HttpServletResponse response ) throws ServletException, IOException {
    }

    public void undeploy ( HttpServletRequest request, HttpServletResponse response ) throws ServletException, IOException {

        String jar = request.getParameter( "jar" );
        File from = new File( Config.CONTEXT.getRealPath( "/WEB-INF/felix/load/" + jar ) );
        File to = new File( Config.CONTEXT.getRealPath( "/WEB-INF/felix/undeployed/" + jar ) );
        from.renameTo( to );
        writeSuccess( response, "OSGI Bundle Undeployed" );
    }

    public void deploy ( HttpServletRequest request, HttpServletResponse response ) throws ServletException, IOException {

        String jar = request.getParameter( "jar" );
        File from = new File( Config.CONTEXT.getRealPath( "/WEB-INF/felix/undeployed/" + jar ) );
        File to = new File( Config.CONTEXT.getRealPath( "/WEB-INF/felix/load/" + jar ) );
        from.renameTo( to );
        writeSuccess( response, "OSGI Bundle Loaded" );
    }

    public void stop ( HttpServletRequest request, HttpServletResponse response ) throws ServletException, IOException {

        String bundleID = request.getParameter( "bundleId" );
        try {
            try {
                OSGIUtil.getInstance().getBundleContext().getBundle( new Long( bundleID ) ).stop();
            } catch ( NumberFormatException e ) {
                OSGIUtil.getInstance().getBundleContext().getBundle( bundleID ).stop();
            }
        } catch ( BundleException e ) {
            Logger.error( OSGIAJAX.class, e.getMessage(), e );
            throw new ServletException( e.getMessage() + " Unable to stop bundle", e );
        }
    }

    public void start ( HttpServletRequest request, HttpServletResponse response ) throws ServletException, IOException {

        String bundleID = request.getParameter( "bundleId" );
        try {
            try {
                OSGIUtil.getInstance().getBundleContext().getBundle( new Long( bundleID ) ).start();
            } catch ( NumberFormatException e ) {
                OSGIUtil.getInstance().getBundleContext().getBundle( bundleID ).start();
            }
        } catch ( BundleException e ) {
            Logger.error( OSGIAJAX.class, e.getMessage(), e );
            throw new ServletException( e.getMessage() + " Unable to start bundle", e );
        }
    }

    public void add ( HttpServletRequest request, HttpServletResponse response ) throws ServletException, IOException {

        FileItemFactory factory = new DiskFileItemFactory();
        ServletFileUpload upload = new ServletFileUpload( factory );
        FileItemIterator iterator = null;
        try {
            iterator = upload.getItemIterator( request );
            while ( iterator.hasNext() ) {
                FileItemStream item = iterator.next();
                InputStream in = item.openStream();
                if ( item.getFieldName().equals( "bundleUpload" ) ) {
                    String fname = item.getName();
                    if ( !fname.endsWith( ".jar" ) ) {
                        Logger.warn( this, "Cannot deplpy bundle as it is not a JAR" );
                        writeError( response, "Cannot deplpy bundle as it is not a JAR" );
                        break;
                    }

                    File to = new File( Config.CONTEXT.getRealPath( "/WEB-INF/felix/load/" + fname ) );
                    FileOutputStream out = new FileOutputStream( to );
                    IOUtils.copyLarge( in, out );
                    IOUtils.closeQuietly( out );
                    IOUtils.closeQuietly( in );
                }
            }
        } catch ( FileUploadException e ) {
            Logger.error( OSGIBaseAJAX.class, e.getMessage(), e );
            throw new IOException( e.getMessage(), e );
        }
    }

    public void restart ( HttpServletRequest request, HttpServletResponse response ) throws ServletException, IOException {

        //First we need to stop the framework
        OSGIUtil.getInstance().stopFramework();

        //Now we need to initialize it
        OSGIUtil.getInstance().initializeFramework();

        //Send a respose
        writeSuccess( response, "OSGI Framework Restarted" );
    }

}