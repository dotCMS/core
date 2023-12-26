import { render } from '@testing-library/react';

import PageProvider from './page-provider';

describe('PageProvider', () => {
    it('should render successfully', () => {
        const { baseElement } = render(<PageProvider />);
        expect(baseElement).toBeTruthy();
    });
});
