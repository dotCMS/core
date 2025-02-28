/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.publishing.staticpublishing;

public class EndPointPublisherConnectionException extends RuntimeException{

    public EndPointPublisherConnectionException(final Throwable cause){
        super(cause.getMessage(), cause);
    }

}
