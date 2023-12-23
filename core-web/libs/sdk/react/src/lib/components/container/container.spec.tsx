import { render } from '@testing-library/react';

import Container from './container';

describe('Container', () => {
  it('should render successfully', () => {
    const { baseElement } = render(<Container />);
    expect(baseElement).toBeTruthy();
  });
});
