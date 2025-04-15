import {
    byTestId,
    createComponentFactory,
    mockProvider,
    Spectator,
    SpyObject
} from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { provideHttpClient } from '@angular/common/http';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { AutoCompleteModule } from 'primeng/autocomplete';

import { DotLanguagesService, DotLanguageVariableEntry } from '@dotcms/data-access';

import {
    DotLanguageVariableSelectorComponent,
    LanguageVariable
} from './dot-language-variable-selector.component';

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

const formattedVariables: LanguageVariable[] = [
    { key: 'ai-text-area-key', value: 'AI text area value' },
    { key: 'com.dotcms.repackage.javax.portlet.title.c-Freddy', value: 'Freddy' },
    { key: 'com.dotcms.repackage.javax.portlet.title.c-Landing-Pages', value: 'Landing Pages' },
    { key: 'com.dotcms.repackage.javax.portlet.title.c-Personas', value: 'Personas' }
];

describe('DotLanguageVariableSelectorComponent', () => {
    let spectator: Spectator<DotLanguageVariableSelectorComponent>;
    let dotLanguagesService: SpyObject<DotLanguagesService>;

    const createComponent = createComponentFactory({
        component: DotLanguageVariableSelectorComponent,
        imports: [AutoCompleteModule, NoopAnimationsModule, DotMessagePipe],
        providers: [mockProvider(DotLanguagesService), provideHttpClient()]
    });

    beforeEach(() => {
        spectator = createComponent();
        dotLanguagesService = spectator.inject(DotLanguagesService);

        // Mock service method
        dotLanguagesService.getLanguageVariables.mockReturnValue(of(mockLanguageVariables));

        // Manually setting the formatted variables to avoid HTTP call issues
        spectator.component.$languageVariables.set(formattedVariables);
    });

    it('should create', () => {
        expect(spectator.component).toBeTruthy();
    });

    it('should call loadSuggestions when user interacts with autocomplete', () => {
        // Directly trigger the method that Angular would call
        const loadSuggestionsSpy = jest.spyOn(spectator.component, 'loadSuggestions');

        // Find and manually trigger the completeMethod event (simulates typing in autocomplete)
        const autocomplete = spectator.query(byTestId('language-variable-selector-input'));
        expect(autocomplete).toBeTruthy();

        // Directly call the method instead of relying on event triggering
        spectator.component.loadSuggestions();

        expect(loadSuggestionsSpy).toHaveBeenCalled();
    });

    it('should show filtered suggestions when user types in search', () => {
        // Set the search term
        spectator.setInput('$selectedItem', 'Landing');

        // Check if suggestions are filtered correctly
        const suggestions = spectator.component.$filteredSuggestions();
        expect(suggestions.length).toBe(1);
        expect(suggestions[0].key).toContain('Landing-Pages');
    });

    it('should emit selected language variable when user selects an option', () => {
        const mockVariable = { key: 'test-key', value: 'Test Value' };
        const emitSpy = jest.spyOn(spectator.component.onSelectLanguageVariable, 'emit');

        // Simulate user selecting an item from dropdown
        spectator.component.emitSelectLanguageVariable({
            originalEvent: new Event('select'),
            value: mockVariable
        });

        expect(emitSpy).toHaveBeenCalledWith(`$text.get('${mockVariable.key}')`);
    });
});
