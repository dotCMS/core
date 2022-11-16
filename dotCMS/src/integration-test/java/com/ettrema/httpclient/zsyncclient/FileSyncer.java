package com.ettrema.httpclient.zsyncclient;

import com.bradmcevoy.common.Path;
import com.bradmcevoy.http.exceptions.BadRequestException;
import com.bradmcevoy.http.exceptions.ConflictException;
import com.bradmcevoy.http.exceptions.NotAuthorizedException;
import com.bradmcevoy.http.exceptions.NotFoundException;
import com.ettrema.httpclient.Host;
import com.ettrema.httpclient.HttpException;
import com.ettrema.httpclient.ProgressListener;
import com.ettrema.httpclient.Utils;
import java.io.File;
import java.io.IOException;

/**
 * Interface for various methods for syncronising local and remote files. Implementations will
 * efficiently update either the remote file (upload) or local file (downloade) transferring only
 * those bytes required to make the other file identical.
 *
 * @author brad
 */
public interface FileSyncer {
    File download(Host host, Path remotePath, File localFile, final ProgressListener listener) throws IOException, NotFoundException, HttpException, Utils.CancelledException, NotAuthorizedException, BadRequestException, ConflictException;
    
    void upload(Host host, File localcopy, Path remotePath, final ProgressListener listener) throws IOException, NotFoundException, Utils.CancelledException, NotAuthorizedException, ConflictException;
}
