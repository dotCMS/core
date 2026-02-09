package com.dotcms.contenttype.business;

import com.dotmarketing.portlets.contentlet.business.DotContentletValidationException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.util.importer.exception.ImportLineError;

import java.util.List;
import java.util.Map;

/**
 * This is the specialized validation exception meant to be used for DotAssets when a validation required is not meant
 * This exception is not expected from a content-import process
 */
public class DotAssetValidationException extends DotContentletValidationException {

	private static final long serialVersionUID = 1L;

    /**
     * Construct the exception with a custom message 
     * @param x
     */
	public DotAssetValidationException(String x) {
		super(x);
		// TODO Auto-generated constructor stub
	}

    /**
     * Construct the exception with a custom message and a wrapped cause exception
     * @param x
     * @param cause
     */
	public DotAssetValidationException(String x, Exception cause) {
        super(x,cause);
        // TODO Auto-generated constructor stub
    }

	/**
	 * Package-private constructor for setting ImportLineError from Builder
	 * @param x
	 * @param importLineError
	 */
	public DotAssetValidationException(String x, ImportLineError importLineError) {
		super(x, importLineError);
	}

	/**
	 * Package-private constructor for setting ImportLineError and validation details from Builder
	 * @param x
	 * @param importLineError
	 * @param notValidFields
	 * @param notValidRelationships
	 */
	public DotAssetValidationException(String x, ImportLineError importLineError,
			Map<String, List<Field>> notValidFields,
			Map<String, Map<Relationship, List<Contentlet>>> notValidRelationships) {
		super(x, importLineError, notValidFields, notValidRelationships);
	}

}