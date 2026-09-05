import { byTestId, createComponentFactory, mockProvider, Spectator } from '@openng/spectator/jest';

import { DotMessageService } from '@dotcms/data-access';

import { DotAiSettingsPanelComponent } from './dot-ai-settings-panel.component';

import { DotAiStore } from '../../store/dot-ai.store';

describe('DotAiSettingsPanelComponent', () => {
    let spectator: Spectator<DotAiSettingsPanelComponent>;

    const storeMock = {
        indexesForbidden: jest.fn().mockReturnValue(false),
        indexOptions: jest
            .fn()
            .mockReturnValue([{ label: 'blogs - (contents:4)', value: 'blogs' }]),
        chatModels: jest.fn().mockReturnValue(['gpt-4o-mini']),
        settingsIndexName: jest.fn().mockReturnValue('blogs'),
        settingsThreshold: jest.fn().mockReturnValue(0.25),
        settingsOperator: jest.fn().mockReturnValue('cosine'),
        settingsModel: jest.fn().mockReturnValue('gpt-4o-mini'),
        settingsTemperature: jest.fn().mockReturnValue(0),
        settingsResponseLength: jest.fn().mockReturnValue(1024),
        settingsContentTypes: jest.fn().mockReturnValue(''),
        setSettings: jest.fn()
    };

    const createComponent = createComponentFactory({
        component: DotAiSettingsPanelComponent,
        componentProviders: [{ provide: DotAiStore, useValue: storeMock }],
        providers: [mockProvider(DotMessageService)],
        shallow: true
    });

    beforeEach(() => {
        jest.clearAllMocks();
        storeMock.indexesForbidden.mockReturnValue(false);
    });

    it('should render the panel', () => {
        spectator = createComponent();

        expect(spectator.query(byTestId('dotai-settings-panel'))).toBeTruthy();
    });

    it('should offer all three operators, using innerProduct not the legacy value', () => {
        spectator = createComponent();

        expect(spectator.query(byTestId('dotai-settings-operator-cosine'))).toBeTruthy();
        expect(spectator.query(byTestId('dotai-settings-operator-distance'))).toBeTruthy();
        // FR-024: the legacy screen's `product` is not a value the backend accepts.
        expect(spectator.query(byTestId('dotai-settings-operator-innerProduct'))).toBeTruthy();
        expect(spectator.query(byTestId('dotai-settings-operator-product'))).toBeFalsy();
    });

    it('should write content types straight to the store', () => {
        spectator = createComponent();

        spectator.typeInElement(
            'Blog, News',
            spectator.query(byTestId('dotai-settings-content-types')) as HTMLInputElement
        );

        expect(storeMock.setSettings).toHaveBeenCalledWith({ settingsContentTypes: 'Blog, News' });
    });

    it('should explain the administrator requirement instead of an empty picker (FR-049)', () => {
        storeMock.indexesForbidden.mockReturnValue(true);
        spectator = createComponent();

        expect(spectator.query(byTestId('dotai-settings-index-forbidden'))).toBeTruthy();
        expect(spectator.query(byTestId('dotai-settings-index'))).toBeFalsy();
    });
});
