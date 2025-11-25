package com.dotcms.api.client.model;

import com.dotcms.api.Resetable;
import java.net.URL;
import java.util.Optional;

/**
 * The RemoteURLParam interface is used in the {@link com.dotcms.cli.common.AuthenticationMixin} to
 * set and get the dotCMS URL parameter. It provides methods to set and retrieve the URL.
 */
public interface RemoteURLParam extends Resetable {

    void setURL(URL remoteURL);

    Optional<URL> getURL();
    
}