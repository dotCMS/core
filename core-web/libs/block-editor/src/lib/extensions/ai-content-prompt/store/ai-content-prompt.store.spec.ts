import { TestBed } from '@angular/core/testing';

import { AiContentPromptStore } from './ai-content-prompt.store';

describe('AiContentPromptStore', () => {
    let store: AiContentPromptStore;

    beforeEach(() => {
        TestBed.configureTestingModule({});
        store = TestBed.inject(AiContentPromptStore);
    });

    it('should be created', () => {
        expect(store).toBeTruthy();
    });
});
