package com.dotmarketing.portlets.workflows.actionlet;

import com.dotcms.business.WrapInTransaction;
import com.dotcms.content.elasticsearch.business.ContentletIndexAPI;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.portlets.workflows.model.*;
import com.dotmarketing.util.Logger;
import com.google.common.annotations.VisibleForTesting;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Do the reindex for a content
 * @author jsanca
 */
public class ReindexContentActionlet extends WorkFlowActionlet {

	private static final String INDEX_POLICY = "indexPolicy";
	private final ContentletIndexAPI contentletIndexAPI;

	public ReindexContentActionlet() {

		this(APILocator.getContentletIndexAPI());
	}

	@VisibleForTesting
	protected ReindexContentActionlet(final ContentletIndexAPI contentletIndexAPI) {

		this.contentletIndexAPI = contentletIndexAPI;
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public String getName() {
		return "Reindex content";
	}

	public String getHowTo() {

		return "This actionlet will reindex the content.";
	}

	@WrapInTransaction
	public void executeAction(final WorkflowProcessor processor,
							  final Map<String, WorkflowActionClassParameter> params) throws WorkflowActionFailureException {

		try {

			final Contentlet contentlet       =
					processor.getContentlet();
			final String indexPolicyUserValue =
					params.get(INDEX_POLICY).getValue();

			final Optional<IndexPolicy> indexPolicy =
					this.getIndexPolicyFromString(indexPolicyUserValue);

			Logger.debug(this,
					"Indexing the content of the contentlet: " + contentlet.getIdentifier());

			indexPolicy.ifPresent(contentlet::setIndexPolicy);

			this.contentletIndexAPI.addContentToIndex(contentlet);
		} catch (Exception e) {

			Logger.error(this.getClass(),e.getMessage(),e);
			throw new  WorkflowActionFailureException(e.getMessage(),e);
		}
	}

	private Optional<IndexPolicy> getIndexPolicyFromString (final String value) {

		Optional<IndexPolicy> indexPolicy = Optional.empty();

		try {

			indexPolicy = Optional.of(IndexPolicy.valueOf(value));
		} catch (Exception e) {
			indexPolicy = Optional.empty();
		}

		return indexPolicy;
	}

	public WorkflowStep getNextStep() {

		return null;
	}

	@Override
	public List<WorkflowActionletParameter> getParameters() {
		List<WorkflowActionletParameter> params = new ArrayList<WorkflowActionletParameter>();

		params.add(new WorkflowActionletParameter(INDEX_POLICY, "Optional Index Policy for the contentlet (Valid values: DEFER, WAIT_FOR, FORCE)", "", false));

		return params;
	}
}
