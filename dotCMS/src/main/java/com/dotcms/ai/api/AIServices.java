package com.dotcms.ai.api;

import com.dotcms.ai.model.AIProvider;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class AIServices {

    private final ConcurrentMap<AIProvider, List<AIClientAPI>> clients = new ConcurrentHashMap();

    public List<AIProvider> getProviders() {
        return List.of();
    }

    public List<AIClientAPI> getClients(final List<AIProvider> providers) {
        return List.of();
    }

}
