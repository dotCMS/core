import type { ExperimentsEngine } from './engine';
import type { EventContext } from '../pipeline/models';

interface PayloadWithContext {
    context?: EventContext;
    [key: string]: unknown;
}

/**
 * Analytics.js plugin that adds the session's `context.experiments` to every event. The
 * engine's `isUserIncluded` check starts in `init`, before Analytics.js loads.
 *
 * It must come right after the identity plugin, which creates `context`.
 *
 * @param engine - The experiments engine
 * @returns The plugin
 */
export const experimentsPlugin = (engine: ExperimentsEngine) => {
    const withExperiments = ({ payload }: { payload: PayloadWithContext }) => {
        const context = payload?.context;

        if (!context?.session_id) {
            return payload;
        }

        const experiments = engine.contextExperiments(context.session_id);

        if (experiments.length === 0) {
            return payload;
        }

        return { ...payload, context: { ...context, experiments } };
    };

    return {
        name: 'dot-events-experiments',

        pageStart: withExperiments,
        trackStart: withExperiments,

        loaded: () => true
    };
};
