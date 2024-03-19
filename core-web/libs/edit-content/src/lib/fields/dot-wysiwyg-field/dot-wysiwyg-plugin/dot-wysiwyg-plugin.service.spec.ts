import { TestBed } from '@angular/core/testing';

import { DotWysiwygPluginService } from './dot-wysiwyg-plugin.service';

describe('DotWysiwygPluginService', () => {
    let service: DotWysiwygPluginService;

    beforeEach(() => {
        TestBed.configureTestingModule({});
        service = TestBed.inject(DotWysiwygPluginService);
    });

    it('should be created', () => {
        expect(service).toBeTruthy();
    });
});
