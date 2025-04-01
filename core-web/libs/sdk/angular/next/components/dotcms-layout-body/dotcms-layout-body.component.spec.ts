import { expect } from '@jest/globals';
import { Spectator, createRoutingFactory } from '@ngneat/spectator/jest';

import { DotcmsLayoutBodyComponent } from './dotcms-layout-body.component';

jest.mock('@dotcms/client', () => ({
    ...jest.requireActual('@dotcms/client'),
    isInsideEditor: jest.fn().mockReturnValue(true),
    initEditor: jest.fn(),
    updateNavigation: jest.fn(),
    postMessageToEditor: jest.fn()
}));

jest.mock('@dotcms/uve', () => ({
    ...jest.requireActual('@dotcms/uve'),
    createUVESubscription: jest.fn(),
    getUVEState: jest.fn().mockReturnValue({
        mode: 'preview',
        languageId: 'en',
        persona: 'admin',
        variantName: 'default',
        experimentId: '123'
    })
}));

describe('DotcmsLayoutBodyComponent', () => {
    let spectator: Spectator<DotcmsLayoutBodyComponent>;

    const createComponent = createRoutingFactory({
        component: DotcmsLayoutBodyComponent
    });

    beforeEach(() => {
        spectator = createComponent();
    });

    afterEach(() => {
        jest.clearAllMocks();
    });

    it('should render rows', () => {
        spectator.detectChanges();
        expect(spectator.queryAll('.row').length).toBe(3);
    });
});
