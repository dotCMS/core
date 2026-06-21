import { anthropic } from '@ai-sdk/anthropic';
import { createOpenRouter } from '@openrouter/ai-sdk-provider';
import { type LanguageModel } from 'ai';

/**
 * LLM provider/model selection for the agent. The agent uses TOOL CALLING (not
 * forced structured output), so it doesn't require an Output.object-capable
 * model — any modern tool-calling model works. Provider is env-selectable per
 * plan §3 (provider not locked) — no loop rewrite to swap.
 *
 *   A11Y_AGENT_PROVIDER = "openrouter" (default) | "anthropic"
 *   A11Y_AGENT_MODEL    = model id for that provider (default depends on provider)
 *   OPENROUTER_KEY / OPENROUTER_API_KEY = key when provider=openrouter
 */

export type AgentProvider = 'anthropic' | 'openrouter';

export const DEFAULT_PROVIDER: AgentProvider =
    (process.env.A11Y_AGENT_PROVIDER as AgentProvider) || 'openrouter';

export const DEFAULT_MODEL =
    process.env.A11Y_AGENT_MODEL ??
    (DEFAULT_PROVIDER === 'openrouter' ? 'z-ai/glm-5.2' : 'claude-sonnet-4-6');

export function defaultModel(): LanguageModel {
    if (DEFAULT_PROVIDER === 'openrouter') {
        const apiKey = process.env.OPENROUTER_KEY ?? process.env.OPENROUTER_API_KEY;
        if (!apiKey) {
            throw new Error(
                'A11Y_AGENT_PROVIDER=openrouter but OPENROUTER_KEY (or OPENROUTER_API_KEY) is not set'
            );
        }
        return createOpenRouter({ apiKey }).chat(DEFAULT_MODEL);
    }
    // Anthropic reads ANTHROPIC_API_KEY from the environment itself.
    return anthropic(DEFAULT_MODEL);
}
