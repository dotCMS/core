package com.ettrema.httpclient;

/**
 *
 * @author mcevoyb
 */
public interface ConnectionListener {

    void onStartRequest();

    void onFinishRequest();
}
