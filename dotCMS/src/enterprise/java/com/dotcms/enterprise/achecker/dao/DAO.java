/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.achecker.dao;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public interface DAO {

	public List<Map<String, Object>> execute(String sql) throws SQLException;

}
