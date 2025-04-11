import { createComponentFactory, mockProvider, Spectator, SpyObject } from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { AutoCompleteModule } from 'primeng/autocomplete';

import { DotLanguagesService, DotLanguageVariableEntry } from '@dotcms/data-access';


import { DotLanguageVariableSelectorComponent } from './dot-language-variable-selector.component';

import { DotMessagePipe } from '../../dot-message/dot-message.pipe';

const mockLanguageVariables: Record<string, DotLanguageVariableEntry> = {
    'ai-text-area-key': {
        'en-us': {
            identifier: '034a07f0f308db12d55fa74bb3b265f0',
            value: 'AI text area value'
        },
        'es-es': null,
        'es-pa': null
    },
    'com.dotcms.repackage.javax.portlet.title.c-Freddy': {
        'en-us': null,
        'es-es': {
            identifier: '175d27eb-9e2c-4fdc-9c4a-0e7d88ce4e87',
            value: 'Freddy'
        },
        'es-pa': null
    },
    'com.dotcms.repackage.javax.portlet.title.c-Landing-Pages': {
        'en-us': {
            identifier: '06e1f11b-410a-428b-947c-ed60dcc8420d',
            value: 'Landing Pages'
        },
        'es-es': {
            identifier: '1547f21d-c357-4524-afb0-b728fe3217db',
            value: 'Landing Pages'
        },
        'es-pa': null
    },
    'com.dotcms.repackage.javax.portlet.title.c-Personas': {
        'en-us': {
            identifier: '1102be5608453fb28485c5f1060f5be3',
            value: 'Personas'
        },
        'es-es': null,
        es_pa: null
    }
};

describe('DotLanguageVariableSelectorComponent', () => {
    let spectator: Spectator<DotLanguageVariableSelectorComponent>;
    let dotLanguagesService: SpyObject<DotLanguagesService>;

    const createComponent = createComponentFactory({
        component: DotLanguageVariableSelectorComponent,
        imports: [AutoCompleteModule, NoopAnimationsModule, DotMessagePipe],
        providers: [mockProvider(DotLanguagesService)]
    });

    beforeEach(() => {
        spectator = createComponent();
        dotLanguagesService = spectator.inject(DotLanguagesService);
        dotLanguagesService.getLanguageVariables.mockReturnValue(of(mockLanguageVariables));
    });

    it('should create', () => {
        expect(spectator.component).toBeTruthy();
    });

    it('should load language variables when loadSuggestions is called', () => {
        spectator.component.loadSuggestions();
        expect(dotLanguagesService.getLanguageVariables).toHaveBeenCalled();
    });

    it('should filter suggestions based on search term', () => {
        spectator.component.loadSuggestions();
        spectator.component.$selectedItem.set('landing');

        const suggestions = spectator.component.$filteredSuggestions();
        expect(suggestions.length).toBe(1);
        expect(suggestions[0].key).toContain('Landing-Pages');
    });

    it('should emit selected language variable', () => {
        const mockVariable = { key: 'test-key', value: 'Test Value' };
        const emitSpy = jest.spyOn(spectator.component.onSelectLanguageVariable, 'emit');

        spectator.component.emitSelectLanguageVariable({
            originalEvent: new Event('select'),
            value: mockVariable
        });

        expect(emitSpy).toHaveBeenCalledWith(mockVariable);
    });

    it('should reset autocomplete on hide overlay', () => {
        spectator.component.onHideOverlay();
        expect(spectator.component.$selectedItem()).toBe('');
    });
});
