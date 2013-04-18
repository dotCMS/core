package com.dotcms.publisher.mapper;

import java.util.Date;
import java.util.Map;

import com.dotcms.publisher.business.PublishAuditHistory;
import com.dotcms.publisher.business.PublishAuditStatus;

public class PublishAuditStatusMapper extends CommonRowMapper<PublishAuditStatus> {

	@Override
	public PublishAuditStatus mapObject(Map<String, Object> row) {
		PublishAuditStatus objToReturn = new PublishAuditStatus();
		
		objToReturn.setBundleId((String) row.get("bundle_id"));
		objToReturn.setStatusPojo(PublishAuditHistory
				.getObjectFromString(
						(String) row.get("status_pojo")));
		
		if(row.get("status") != null) {
			objToReturn.setStatus(PublishAuditStatus.getStatusObjectByCode(
					getIntegerFromObj(row.get("status"))));
		}
		
		objToReturn.setCreateDate((Date) row.get("create_date"));
		objToReturn.setStatusUpdated((Date) row.get("status_updated"));
		
		return objToReturn;
	}

}
