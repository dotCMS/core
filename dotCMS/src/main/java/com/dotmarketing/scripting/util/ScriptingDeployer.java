package com.dotmarketing.scripting.util;

/*
 * Copyright 2004,2004 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Role;
import com.dotmarketing.plugin.PluginDeployer;
import com.dotmarketing.util.Logger;

/**
 * This class deploys the  dotCMS Scripting Plugin.
 * 
 * @author Jason Tesser
 */

public class ScriptingDeployer implements PluginDeployer {

	public boolean deploy() {
		try {
			Role role = new Role();
			role.setName("Scripting Developer");
			role.setDescription("Role for allowing scripting");
			APILocator.getRoleAPI().save(role);
		} catch (Exception e) {
			Logger.error(this, e.getMessage(), e);
			return false;
		}
		return true;
	}

	public boolean redeploy(String version) {
//		try {
//			RoleLocalManagerUtil.addRole(PublicCompanyFactory.getDefaultCompanyId(), "Scripting Developer");
//		} catch (Exception e) {
//			Logger.error(this, e.getMessage(), e);
//			return false;
//		}
		return true;
	}

}
