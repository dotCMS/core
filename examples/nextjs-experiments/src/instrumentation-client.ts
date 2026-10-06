import { dotEvents } from '@dotcms/events';

import { eventsConfig } from '@/config/dotcms.config';

// Runs after the HTML loads and before React hydrates: the earliest place to start events.
// This one call covers pageviews, impressions, clicks, conversions and experiments.
dotEvents.init(eventsConfig);
