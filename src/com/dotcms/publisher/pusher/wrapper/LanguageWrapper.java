package com.dotcms.publisher.pusher.wrapper;

import com.dotcms.publisher.pusher.PushPublisherConfig.Operation;
import com.dotmarketing.portlets.languagesmanager.model.Language;

public class LanguageWrapper {
	private Language language;
	private Operation operation;

	public LanguageWrapper(Language language) {
		this.language = language;
	}

	public Language getLanguage() {
		return language;
	}
	public void setLanguage(Language language) {
		this.language = language;
	}
	public Operation getOperation() {
		return operation;
	}
	public void setOperation(Operation operation) {
		this.operation = operation;
	}
}
