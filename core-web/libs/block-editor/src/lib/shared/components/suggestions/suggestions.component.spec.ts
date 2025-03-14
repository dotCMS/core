import { Spectator, createComponentFactory, mockProvider } from '@ngneat/spectator/jest';

import { DotLanguagesService } from '@dotcms/data-access';
import { DotLanguagesServiceMock } from '@dotcms/utils-testing';

import { SuggestionsComponent } from './suggestions.component';

import { SuggestionsService } from '../../services/suggestions/suggestions.service';
import { EmptyMessageComponent } from '../empty-message/empty-message.component';

describe('SuggestionsComponent', () => {
    let spectator: Spectator<SuggestionsComponent>;
    const createComponent = createComponentFactory({
        component: SuggestionsComponent,
        declarations: [EmptyMessageComponent],
        providers: [
            mockProvider(SuggestionsService),
            {
                provide: DotLanguagesService,
                useClass: DotLanguagesServiceMock
            }
        ]
    });

    beforeEach(() => (spectator = createComponent()));

    it('should create', () => {
        expect(spectator.component).toBeTruthy();
    });
});
