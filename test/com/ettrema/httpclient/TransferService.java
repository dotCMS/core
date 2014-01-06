package com.ettrema.httpclient;

import com.dotcms.repackage.milton_1_8_1_4.com.bradmcevoy.http.Range;
import com.dotcms.repackage.milton_1_8_1_4.com.bradmcevoy.http.exceptions.BadRequestException;
import com.dotcms.repackage.milton_1_8_1_4.com.bradmcevoy.http.exceptions.ConflictException;
import com.dotcms.repackage.milton_1_8_1_4.com.bradmcevoy.http.exceptions.NotAuthorizedException;
import com.dotcms.repackage.milton_1_8_1_4.com.bradmcevoy.http.exceptions.NotFoundException;
import com.dotcms.repackage.milton_1_8_1_4.com.ettrema.common.LogUtils;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import com.dotcms.repackage.commons_httpclient_3_1.org.apache.commons.httpclient.HttpClient;
import com.dotcms.repackage.commons_httpclient_3_1.org.apache.commons.httpclient.HttpMethodBase;
import com.dotcms.repackage.commons_httpclient_3_1.org.apache.commons.httpclient.methods.GetMethod;
import com.dotcms.repackage.commons_httpclient_3_1.org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import com.dotcms.repackage.commons_httpclient_3_1.org.apache.commons.httpclient.methods.PutMethod;
import com.dotcms.repackage.commons_httpclient_3_1.org.apache.commons.httpclient.methods.RequestEntity;
import com.dotcms.repackage.commons_httpclient_3_1.org.apache.commons.httpclient.params.HttpMethodParams;
import com.dotcms.repackage.commons_io_2_0_1.org.apache.commons.io.IOUtils;
import com.dotcms.repackage.slf4j_api_1_6_0.org.slf4j.Logger;
import com.dotcms.repackage.slf4j_api_1_6_0.org.slf4j.LoggerFactory;

/**
 *
 * @author bradm
 */
public class TransferService {

    private static final Logger log = LoggerFactory.getLogger(TransferService.class);
    private final HttpClient client;
    private final List<ConnectionListener> connectionListeners;
    private int timeout;

    public TransferService(HttpClient client, List<ConnectionListener> connectionListeners) {
        this.client = client;
        this.connectionListeners = connectionListeners;
    }

    public synchronized void get(String url, StreamReceiver receiver, List<Range> rangeList, ProgressListener listener) throws com.ettrema.httpclient.HttpException, Utils.CancelledException, NotAuthorizedException, BadRequestException, ConflictException, NotFoundException {
        LogUtils.trace(log, "get: ", url);
        notifyStartRequest();
        HttpMethodBase m;
        if (rangeList != null) {
            m = new RangedGetMethod(url, rangeList);
        } else {
            m = new GetMethod(url);
        }
        InputStream in = null;
        NotifyingFileInputStream nin = null;
        try {
            int res = client.executeMethod(m);
            Utils.processResultCode(res, url);
            in = m.getResponseBodyAsStream();
            nin = new NotifyingFileInputStream(in, m.getResponseContentLength(), url, listener);
            receiver.receive(nin);
        } catch (com.dotcms.repackage.commons_httpclient_3_1.org.apache.commons.httpclient.HttpException ex) {
            m.abort();
            throw new GenericHttpException(ex.getReasonCode(), url);
        } catch (Utils.CancelledException ex) {
            m.abort();
            throw ex;
        } catch (IOException ex) {
            m.abort();
            throw new RuntimeException(ex);
        } finally {
            Utils.close(in);
            m.releaseConnection();
            notifyFinishRequest();
        }
    }

    public int put(String encodedUrl, InputStream content, Long contentLength, String contentType, ProgressListener listener) {
        LogUtils.trace(log, "put: ", encodedUrl);
        notifyStartRequest();
        String s = encodedUrl;
        PutMethod p = new PutMethod(s);

        HttpMethodParams params = new HttpMethodParams();
        params.setSoTimeout(timeout);
        p.setParams(params);
        NotifyingFileInputStream notifyingIn = null;
        try {
            notifyingIn = new NotifyingFileInputStream(content, contentLength, s, listener);
            RequestEntity requestEntity;
            if (contentLength == null) {
                log.trace("no content length");
                requestEntity = new InputStreamRequestEntity(notifyingIn, contentType);
            } else {
                requestEntity = new InputStreamRequestEntity(notifyingIn, contentLength, contentType);
            }
            p.setRequestEntity(requestEntity);
            int result = client.executeMethod(p);
            return result;
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        } finally {
            IOUtils.closeQuietly(notifyingIn);
            p.releaseConnection();
            notifyFinishRequest();
        }
    }

    private void notifyStartRequest() {
        for (ConnectionListener l : connectionListeners) {
            l.onStartRequest();
        }
    }

    private void notifyFinishRequest() {
        for (ConnectionListener l : connectionListeners) {
            l.onFinishRequest();
        }
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }
}
