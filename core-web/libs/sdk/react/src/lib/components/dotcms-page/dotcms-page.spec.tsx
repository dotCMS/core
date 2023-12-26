import { render } from '@testing-library/react';

import DotcmsPage from './dotcms-page';

describe('DotcmsPage', () => {
    it('should render successfully', () => {
        const { baseElement } = render(<DotcmsPage />);
        expect(baseElement).toBeTruthy();
    });
});
