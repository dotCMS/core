import { TestBed } from '@angular/core/testing';

import { DotTemplateBuilderStore } from './template-builder.store';

describe('DotTemplateBuilderStore', () => {
    let store: DotTemplateBuilderStore;
    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [DotTemplateBuilderStore]
        });

        store = TestBed.inject(DotTemplateBuilderStore);
    });

    it('should exist', () => {
        expect(store).toBeTruthy();
    });
});
