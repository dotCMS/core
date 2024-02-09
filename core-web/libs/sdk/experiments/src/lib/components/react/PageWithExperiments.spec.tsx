import '@testing-library/jest-dom';
import { render, screen } from '@testing-library/react';

import { PageWithExperiments } from './PageWithExperiments';

describe('PageWithExperiments', () => {
  it('shows WIP and renders child elements', () => {
    render(<PageWithExperiments>Test Child</PageWithExperiments>);
    const containerElement = screen.getByTestId('page-with-experiments');
    expect(containerElement).toBeInTheDocument();

    const childElement = screen.getByText('Test Child');
    expect(childElement).toBeInTheDocument();
  });
});
