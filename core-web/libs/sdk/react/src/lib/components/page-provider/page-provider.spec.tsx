import { render } from '@testing-library/react';

import PageProvider from './page-provider';

xdescribe('PageProvider', () => {
    it('should render successfully', () => {
        const { baseElement } = render(<PageProvider />);
        expect(baseElement).toBeTruthy();
    });
});
