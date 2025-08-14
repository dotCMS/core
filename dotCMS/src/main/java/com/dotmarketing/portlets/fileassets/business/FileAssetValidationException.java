package com.dotmarketing.portlets.fileassets.business;

import com.dotmarketing.portlets.contentlet.business.DotContentletValidationException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.util.importer.exception.ImportLineError;

import java.util.List;
import java.util.Map;

public class FileAssetValidationException extends DotContentletValidationException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;


	public FileAssetValidationException(String x) {
		super(x);
		// TODO Auto-generated constructor stub
	}
	
	public FileAssetValidationException(String x, Exception cause) {
        super(x,cause);
        // TODO Auto-generated constructor stub
    }

	/**
	 * Package-private constructor for setting ImportLineError from Builder
	 * @param x
	 * @param importLineError
	 */
	public FileAssetValidationException(String x, ImportLineError importLineError) {
		super(x, importLineError);
	}

	/**
	 * Package-private constructor for setting ImportLineError and validation details from Builder
	 * @param x
	 * @param importLineError
	 * @param notValidFields
	 * @param notValidRelationships
	 */
	public FileAssetValidationException(String x, ImportLineError importLineError,
			Map<String, List<Field>> notValidFields,
			Map<String, Map<Relationship, List<Contentlet>>> notValidRelationships) {
		super(x, importLineError, notValidFields, notValidRelationships);
	}

}
