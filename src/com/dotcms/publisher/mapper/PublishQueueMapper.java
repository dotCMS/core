package com.dotcms.publisher.mapper;

import java.util.Date;
import java.util.Map;

import com.dotcms.publisher.business.PublishQueueElement;

public class PublishQueueMapper extends CommonRowMapper<PublishQueueElement> {

	@Override
	public PublishQueueElement mapObject(Map<String, Object> row) {
		PublishQueueElement objToReturn = new PublishQueueElement();
		
		objToReturn.setId(getIntegerFromObj(row.get("id")));
		objToReturn.setAsset((String) row.get("asset"));
		objToReturn.setBundleId((String) row.get("bundle_id"));
		objToReturn.setEnteredDate((Date) row.get("entered_date"));
		objToReturn.setOperation(getIntegerFromObj(row.get("operation")));
		objToReturn.setPublishDate((Date) row.get("publish_date"));
		objToReturn.setLanguageId(getIntegerFromObj(row.get("language_id")));
		
		
		
		return objToReturn;
	}

}
