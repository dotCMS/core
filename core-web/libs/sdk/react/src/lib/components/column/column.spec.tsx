import { render } from '@testing-library/react';

import Column from './column';

describe('Column', () => {
  it('should render successfully', () => {
    const { baseElement } = render(<Column />);
    expect(baseElement).toBeTruthy();
  });
});
