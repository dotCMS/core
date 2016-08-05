package com.dotcms.contenttype.model.type;

import java.io.Serializable;

import org.immutables.value.Value;

@Value.Immutable
public abstract class SimpleContentType extends ContentType implements UrlMapable, Serializable, Expireable {

	private static final long serialVersionUID = 1L;

	@Override
	public BaseContentType baseType() {
		return BaseContentType.CONTENT;
	}

	public abstract static class Builder implements ContentTypeBuilder {
	}

}
