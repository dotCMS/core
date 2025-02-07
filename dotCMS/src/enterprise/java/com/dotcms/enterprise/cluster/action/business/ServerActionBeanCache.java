/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included 
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.cluster.action.business;

import java.util.List;

import com.dotcms.enterprise.cluster.action.model.ServerActionBean;
import com.dotmarketing.business.Cachable;

public interface ServerActionBeanCache extends Cachable {

  List<ServerActionBean> getServerActions();
  
  void clearServerActions();

  void putServerActions(List<ServerActionBean> serverActionBeans);
  
  
}
