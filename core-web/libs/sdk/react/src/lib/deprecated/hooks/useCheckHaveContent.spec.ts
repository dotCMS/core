import { renderHook } from '@testing-library/react';
import React from 'react';

import { useCheckHaveContent } from './useCheckHaveContent';

describe('useCheckHaveContent', () => {
    it('should return true when there is content', () => {
        const div = document.createElement('div');
        const child = document.createElement('div');
        div.appendChild(child);

        jest.spyOn(child, 'getBoundingClientRect').mockImplementation(() => {
            return {
                bottom: 0,
                height: 100,
                left: 0,
                right: 0,
                top: 0,
                width: 0,
                x: 0,
                y: 0,
                toJSON: () => ({})
            };
        });

        jest.spyOn(React, 'useRef').mockReturnValue({
            current: div
        });

        const { result } = renderHook(() => useCheckHaveContent());

        expect(result.current.haveContent).toBe(true);
    });

    it('should return false when there is no content', () => {
        const div = document.createElement('div');
        const child = document.createElement('div');
        div.appendChild(child);

        jest.spyOn(React, 'useRef').mockReturnValue({
            current: div
        });

        const { result } = renderHook(() => useCheckHaveContent());

        expect(result.current.haveContent).toBe(false);
    });
});
