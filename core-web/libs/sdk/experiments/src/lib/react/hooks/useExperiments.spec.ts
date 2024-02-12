import { renderHook } from '@testing-library/react';

import { useExperiments } from './useExperiments';

describe('useExperiments function', () => {
  beforeEach(() => {
    console.warn = jest.fn();
  });

  it('should warn about the provided URL', () => {
    const testUrl = new URL('http://test.com');
    renderHook(() => useExperiments(testUrl));
    expect(console.warn).toHaveBeenCalledWith(testUrl);
  });
});
