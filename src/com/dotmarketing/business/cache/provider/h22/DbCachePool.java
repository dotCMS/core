package com.dotmarketing.business.cache.provider.h22;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;

public interface DbCachePool {
	Optional<Connection> connection() throws SQLException;
	void dispose();
}
