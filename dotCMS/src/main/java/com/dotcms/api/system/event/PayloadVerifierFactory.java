package com.dotcms.api.system.event;

import com.dotcms.api.system.event.verifier.GlobalVerifier;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Factory that allow us to register {@link PayloadVerifier}'s and associated them to Payload visibilities ( {@link Visibility} )
 *
 * @author Jonathan Gamba
 *         11/15/16
 */
public class PayloadVerifierFactory implements Serializable {

    private final Map<Visibility, PayloadVerifier> verifiersInstancesMap = new ConcurrentHashMap<>();
    private final static GlobalVerifier GLOBAL_VERIFIER = new GlobalVerifier();

    private PayloadVerifierFactory() {
        // singleton
    }

    private static class SingletonHolder {
        private static final PayloadVerifierFactory INSTANCE = new PayloadVerifierFactory();
    }

    /**
     * Returns the PayloadVerifierFactory instance.
     *
     * @return PayloadVerifierFactory
     */
    public static PayloadVerifierFactory getInstance() {
        return PayloadVerifierFactory.SingletonHolder.INSTANCE;
    }

    /**
     * Register a new {@link PayloadVerifier} associated to a {@link Visibility}
     *
     * @param visibility      Visibility related to the verifier to register
     * @param payloadVerifier Verifier to register
     */
    public void register(final Visibility visibility, PayloadVerifier payloadVerifier) {
        this.verifiersInstancesMap.put(visibility, payloadVerifier);
    }

    /**
     * Returns the verifier associated to the given {@link Visibility}, if there is not verifier registered a
     * {@link GlobalVerifier} will be returned
     *
     * @param payload Payload of the verifier we want to obtain
     * @return The verifier related to the given visibility
     */
    public PayloadVerifier getVerifier(final Payload payload) {

        return getVerifier(payload.getVisibility());
    }

    /**
     * Returns the verifier associated to the given {@link Visibility}, if there is not verifier registered a
     * {@link GlobalVerifier} will be returned
     *
     * @param visibility visibility of the verifier we want to obtain
     * @return The verifier related to the given visibility
     */
    public PayloadVerifier getVerifier(final Visibility visibility) {

        return this.verifiersInstancesMap.containsKey(visibility) ?
                this.verifiersInstancesMap.get(visibility) : GLOBAL_VERIFIER;
    }
}