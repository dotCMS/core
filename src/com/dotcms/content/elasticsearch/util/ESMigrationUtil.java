package com.dotcms.content.elasticsearch.util;

import java.util.List;

import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.client.Client;

import com.dotcms.content.business.DotMappingException;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.structure.factories.StructureFactory;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.Logger;

public class ESMigrationUtil {

	
	/**
	 * This method will take a structure and move the contents 
	 * @param struct
	 * @throws DotDataException
	 * @throws DotSecurityException
	 * @throws DotMappingException
	 */
	public void migrateStructure(Structure struct) throws DotDataException, DotSecurityException, DotMappingException {
		
		ESUtils.checkAndInitialiazeIndex();
		
		
		ContentletAPI capi = APILocator.getContentletAPI();

		String type = struct.getVelocityVarName();
		for (int i = 0; i < 10000; i++) {

			int limit = 100;
			int offset = i * 100;

			List<Contentlet> cons = capi.findByStructure(struct.getInode(), APILocator.getUserAPI().getSystemUser(), false, limit, offset);
			if (cons.size() == 0) {
				break;
			}
			Client client = new ESClient().getClient();
			BulkRequestBuilder bulkRequest = client.prepareBulk();
			for (Contentlet c : cons) {

				//bulkRequest.add(client.prepareIndex(ESIndexAPI.ES_INDEX_NAME, type, c.getInode()).setSource(
				//		new ESMappingAPIImpl().toJson(c)));

			}
			BulkResponse bulkResponse = bulkRequest.execute().actionGet();
			if (bulkResponse.hasFailures()) {
				Logger.error(this.getClass(), bulkResponse.buildFailureMessage());
			}

		

		}
	}

	
	public void migrateAllStructures() throws DotDataException, DotSecurityException, DotMappingException {
		List<Structure> structs = StructureFactory.getStructures();
		for(Structure struct : structs){
			migrateStructure(struct);
		}
	}
}
