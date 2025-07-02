import { createContext } from 'react';

import { DotCMSAnalytics } from '../../dotAnalytics/shared/dot-content-analytics.model';

/**
 * `DotContentAnalyticsContext` is a React context that is designed to provide an instance of
 * `DotCMSAnalytics` to all of the components within its tree that are Consumers of this context.
 *
 * The context is created with a default value of `null`. It is meant to be provided a real value
 * using the `DotContentAnalyticsProvider` component.
 *
 * @see {@link https://reactjs.org/docs/context.html|React Context}
 */
const DotContentAnalyticsContext = createContext<DotCMSAnalytics | null>(null);

export default DotContentAnalyticsContext;
