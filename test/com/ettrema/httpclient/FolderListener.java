package com.ettrema.httpclient;

/**
 *
 * @author mcevoy
 */
public interface FolderListener {

    void onChildAdded( Folder parent, Resource child );

    void onChildRemoved( Folder parent, Resource child );
}
