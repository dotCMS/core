package com.dotcms.ai.api;

import com.dotcms.ai.model.AIProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

public class AIServices {

    private final ConcurrentMap<AIProvider, List<AIClientAPI>> providerClients = new ConcurrentHashMap<>();

    public List<AIProvider> getProviders() {
        return new ArrayList<>(providerClients.keySet());
    }

    public List<AIClientAPI> getClients(final List<AIProvider> providers) {
        return providers.stream()
                .flatMap(provider -> providerClients.get(provider).stream())
                .collect(Collectors.toList());
    }



}
