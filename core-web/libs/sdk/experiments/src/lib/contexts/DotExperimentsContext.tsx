import { createContext } from 'react';

import { DotExperiments } from '../dot-experiments';

/**
 * `DotExperimentsContext` is a React context that is designed to provide an instance of
 * `DotExperiments` to all of the components within its tree that are Consumers of this context.
 *
 * The context is created with a default value of `null`. It is meant to be provided a real value
 * using the `DotExperimentsProvider` component.
 *
 * @see {@link https://reactjs.org/docs/context.html|React Context}
 */
const DotExperimentsContext = createContext<DotExperiments | null>(null);

export default DotExperimentsContext;
