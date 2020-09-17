package com.dotmarketing.portlets.workflows.actionlet;

import com.dotcms.publisher.ajax.RemotePublishAjaxAction;
import com.dotcms.publisher.bundle.bean.Bundle;
import com.dotcms.publisher.business.DotPublisherException;
import com.dotcms.publisher.business.PublisherAPI;
import com.dotcms.publisher.environment.bean.Environment;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClass;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClassParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowActionFailureException;
import com.dotmarketing.portlets.workflows.model.WorkflowActionletParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowProcessor;
import com.dotmarketing.util.Logger;
import com.google.common.collect.ImmutableList;
import com.liferay.portal.model.User;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Actionlet(pushPublish = true)
public class PushPublishActionlet extends WorkFlowActionlet implements BatchAction <String>  {

	private PublisherAPI publisherAPI = PublisherAPI.getInstance();

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public List<WorkflowActionletParameter> getParameters() {
		return null;
	}

	@Override
	public String getName() {
		return "Push Publish";
	}

	@Override
	public String getHowTo() {
		return "This actionlet will add the content to the remote publish queue";
	}

	/**
	 * add the contentlet to the publish queue
	 */
	@Override
	public void executeAction(final WorkflowProcessor processor,
			final Map<String, WorkflowActionClassParameter> params) throws WorkflowActionFailureException {

			//Gets available languages
			//List<Language> languages = languagesAPI.getLanguages();
			final Contentlet ref = processor.getContentlet();
		    final User user = processor.getUser();
		try {
			doPushPublish(getPushPublishDataAsMap(ref),Collections.singletonList(ref.getIdentifier()), user);
		} catch (Exception e) {
			Logger.debug(PushPublishActionlet.class, e.getMessage());
			throw new WorkflowActionFailureException(e.getMessage(), e);
		}
	}

	@Override
	public void preBatchAction(final WorkflowProcessor processor,
			final WorkflowActionClass actionClass,
			final Map<String, WorkflowActionClassParameter> params) {

		 final String actionletInstanceId = actionClass.getId();
		 final Contentlet contentlet = processor.getContentlet();
		 final ConcurrentMap<String,Object> context = processor.getActionsContext();
		 context.computeIfAbsent(actionletInstanceId, key ->
		    new PushPublishBatchData(getPushPublishDataAsMap(contentlet))
		 );

		 context.computeIfPresent(actionletInstanceId,(key, o) -> {
			         final PushPublishBatchData data = PushPublishBatchData.class.cast(o);
					 data.addIdentifier(contentlet.getIdentifier());
					 data.addInode(contentlet.getInode());
					 return data;
				 }
		 );
	}

	@Override
	public void executeBatchAction(final User user,
			final ConcurrentMap<String, Object> context,
			final WorkflowActionClass actionClass,
			final Map<String, WorkflowActionClassParameter> params) throws WorkflowActionFailureException {

		 final Object object = context.get(actionClass.getId());
		 final PushPublishBatchData pushPublishBatchData = PushPublishBatchData.class.cast(object);
		 try {
			 doPushPublish(pushPublishBatchData.getData(), pushPublishBatchData.getIdentifiers(), user);
		 }catch (Exception e){
			 Logger.debug(PushPublishActionlet.class, e.getMessage());
		 	 throw new WorkflowActionFailureException(e.getMessage(), e);
		 }
	}

	@Override
	public List <String> getObjectsForBatch(final ConcurrentMap<String, Object> context,
			final WorkflowActionClass actionClass){
		final String actionletInstanceId = actionClass.getId();
		final Object object = context.get(actionletInstanceId);
		if(null == object) {
		   return Collections.emptyList();
		}
		final PushPublishBatchData pushPublishBatchData = PushPublishBatchData.class.cast(object);
		return pushPublishBatchData.getIdentifiers();
	}



	private void doPushPublish(final Map<String,String> pushPublishData, final List<String> identifiers, final User user)
			throws DotDataException, ParseException, DotPublisherException {

			final String contentPushPublishDate = pushPublishData.get(Contentlet.WORKFLOW_PUBLISH_DATE);
			final String contentPushPublishTime = pushPublishData.get(Contentlet.WORKFLOW_PUBLISH_TIME);
			final String contentPushExpireDate = pushPublishData.get(Contentlet.WORKFLOW_EXPIRE_DATE);
			final String contentPushExpireTime = pushPublishData.get(Contentlet.WORKFLOW_EXPIRE_TIME);
			final String contentNeverExpire = pushPublishData.get(Contentlet.WORKFLOW_NEVER_EXPIRE);
			final boolean contentPushNeverExpire = ("on".equals(contentNeverExpire) ||
					  "true".equals(contentNeverExpire)
			);
			final String whoToSendTmp = pushPublishData.get(Contentlet.WHERE_TO_SEND);
			final String filterKey = pushPublishData.get(Contentlet.FILTER_KEY);
			final String iWantTo = pushPublishData.get(Contentlet.I_WANT_TO);
			final boolean forcePush = (boolean) APILocator.getPublisherAPI().getFilterDescriptorByKey(filterKey).getFilters().getOrDefault("forcePush",false);
			final List<Environment> envsToSendTo = getEnvironmentsToSendTo(whoToSendTmp);
			
			if(envsToSendTo.isEmpty()) {
			    Logger.warn(this.getClass(), "There are no valid environments to sent to - looking for:" + whoToSendTmp);
			}
			
			
			
			final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-H-m");
			final Date publishDate = dateFormat
					.parse(contentPushPublishDate + "-" + contentPushPublishTime);

		if ( RemotePublishAjaxAction.DIALOG_ACTION_PUBLISH.equals( iWantTo ) || RemotePublishAjaxAction.DIALOG_ACTION_PUBLISH_AND_EXPIRE.equals( iWantTo ) ) {
			final Bundle bundle = new Bundle(null, publishDate, null, user.getUserId(), forcePush,
					filterKey);
			APILocator.getBundleAPI().saveBundle(bundle, envsToSendTo);
			publisherAPI.addContentsToPublish(identifiers, bundle.getId(), publishDate, user);
		}
		if ( RemotePublishAjaxAction.DIALOG_ACTION_EXPIRE.equals( iWantTo ) || RemotePublishAjaxAction.DIALOG_ACTION_PUBLISH_AND_EXPIRE.equals( iWantTo ) ) {
			if (!contentPushNeverExpire && (!"".equals(contentPushExpireDate.trim()) && !""
					.equals(contentPushExpireTime.trim()))) {
				final Date expireDate = dateFormat
						.parse(contentPushExpireDate + "-" + contentPushExpireTime);
				final Bundle bundle = new Bundle(null, publishDate, expireDate, user.getUserId(), forcePush,
						filterKey);
				APILocator.getBundleAPI().saveBundle(bundle, envsToSendTo);
				publisherAPI.addContentsToUnpublish(identifiers, bundle.getId(), expireDate, user);
			}
		}
	}



	private Map<String,String> getPushPublishDataAsMap(final Contentlet contentlet){
		final Map<String,String> map = new HashMap<>();
        map.put(Contentlet.WORKFLOW_PUBLISH_DATE,contentlet.getStringProperty(Contentlet.WORKFLOW_PUBLISH_DATE));
		map.put(Contentlet.WORKFLOW_PUBLISH_TIME,contentlet.getStringProperty(Contentlet.WORKFLOW_PUBLISH_TIME));
		map.put(Contentlet.WORKFLOW_EXPIRE_DATE,contentlet.getStringProperty(Contentlet.WORKFLOW_EXPIRE_DATE));
		map.put(Contentlet.WORKFLOW_EXPIRE_TIME,contentlet.getStringProperty(Contentlet.WORKFLOW_EXPIRE_TIME));
		map.put(Contentlet.WORKFLOW_NEVER_EXPIRE,contentlet.getStringProperty(Contentlet.WORKFLOW_NEVER_EXPIRE));
		map.put(Contentlet.WHERE_TO_SEND,contentlet.getStringProperty(Contentlet.WHERE_TO_SEND));
		map.put(Contentlet.FILTER_KEY,contentlet.getStringProperty(Contentlet.FILTER_KEY));
		map.put(Contentlet.I_WANT_TO,contentlet.getStringProperty(Contentlet.I_WANT_TO));
		return map;
	}

	public static List<Environment> getEnvironmentsToSendTo(final String whoToSendTo){
	    if(whoToSendTo==null) {
	        return ImmutableList.of();
	    }
		final String[] whereToSend = whoToSendTo.split(",");
		return Stream.of(whereToSend).map(id -> {
			try {
				return APILocator.getEnvironmentAPI().findEnvironmentById(id);
			} catch (DotDataException e) {
				Logger.error(PushPublishActionlet.class,
						"Error retrieving environment from id: " + id, e);
			}
			return null;
		}).filter(Objects::nonNull).collect(Collectors.toList());
	}

	static class PushPublishBatchData {

		private final List<String> identifiers;

		private final List<String> inodes;

        private final Map<String,String> data;

		PushPublishBatchData( final Map<String,String> data) {
			this.data = data;
			this.identifiers = new ArrayList<>();
			this.inodes = new ArrayList<>();
		}

		void addIdentifier(final String identifier){
			identifiers.add(identifier);
		}

		public List<String> getIdentifiers() {
			return identifiers;
		}

		void addInode(final String inode){
			inodes.add(inode);
		}

		public List<String> getInodes() {
			return inodes;
		}

		public Map<String, String> getData() {
			return data;
		}

	}
}
