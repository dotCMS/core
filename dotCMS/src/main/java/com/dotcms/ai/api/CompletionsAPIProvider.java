package com.dotcms.ai.api;

/**
 * This class is in charge of providing the CompletionsAPI.
 * @author jsanca
 */
public interface CompletionsAPIProvider {

    CompletionsAPI getCompletionsAPI(Object... initArguments);
}
