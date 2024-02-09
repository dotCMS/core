import { ReactElement, ReactNode } from 'react';

export interface PageWithExperimentsProps {
  readonly children: ReactNode;
}

/**
 * `PageWithExperiment` is a component to activate the automatic switch between variants
 *
 * @param {ReactElement} children - The children elements of the page.
 * @returns {ReactElement} - The created page with the children elements.
 */
export function PageWithExperiments({ children }: PageWithExperimentsProps): ReactElement {
  return <div data-testid="page-with-experiments">{children}</div>;
}
