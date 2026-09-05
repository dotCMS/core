/**
 * Shared base path for every dotAI client service, so the literal is written once rather
 * than repeated across the five services that talk to this family.
 */
export const AI_API_ENDPOINT = '/api/v1/ai';

/** Workflow action used to publish a generated image as a dotAsset. */
export const API_ENDPOINT_FOR_PUBLISH = '/api/v1/workflow/actions/default/fire/PUBLISH';

export const AI_PLUGIN_KEY = {
    NOT_SET: 'NOT SET'
};

/**
 * Returned by the backend in place of `providerConfig` when redaction itself fails. It is a
 * sentinel, not JSON — parsing it throws. Note the em dash: it is copied verbatim from
 * `CompletionsResource`, and a hyphen here would silently stop matching.
 */
export const AI_REDACTION_FAILED_SENTINEL = '[CONFIG PRESENT — REDACTION FAILED]';
