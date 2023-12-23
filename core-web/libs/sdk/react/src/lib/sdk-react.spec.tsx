import { render } from '@testing-library/react';

import SdkReact from './sdk-react';

describe('SdkReact', () => {
  it('should render successfully', () => {
    const { baseElement } = render(<SdkReact />);
    expect(baseElement).toBeTruthy();
  });
});
