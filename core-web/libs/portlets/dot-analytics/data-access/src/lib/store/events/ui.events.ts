import { type } from '@ngrx/signals';
import { eventGroup } from '@ngrx/signals/events';

/**
 * Local UI events for dialog open/close and banner interactions.
 *
 * These are intent events: components dispatch them; handlers may persist
 * state (e.g. localStorage for the banner) or update store-managed UI
 * signals.
 */
export const uiEvents = eventGroup({
    source: 'Analytics UI',
    events: {
        /** "How it's calculated" dialog opened (from the engagement report). */
        calculationDialogOpened: type<void>(),
        /** "How it's calculated" dialog closed. */
        calculationDialogClosed: type<void>(),
        /** Top informational message banner dismissed (persists to localStorage). */
        messageBannerDismissed: type<void>()
    }
});
