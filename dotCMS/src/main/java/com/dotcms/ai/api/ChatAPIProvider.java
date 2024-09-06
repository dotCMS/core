package com.dotcms.ai.api;

public interface ChatAPIProvider {

    ChatAPI getChatAPI(Object... initArguments);
}
