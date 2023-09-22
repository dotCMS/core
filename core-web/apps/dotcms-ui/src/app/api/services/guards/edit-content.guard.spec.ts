import { of } from 'rxjs';

import { TestBed } from '@angular/core/testing';

import { DotPropertiesService } from '@dotcms/data-access';
import { FeaturedFlags } from '@dotcms/dotcms-models';

import { EditContentGuard } from './edit-content.guard';

describe('EditContentGuard', () => {
    let editContentGuardService: EditContentGuard;
    let dotPropertiesService: DotPropertiesService;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [DotPropertiesService]
        });
        editContentGuardService = TestBed.inject(EditContentGuard);

        dotPropertiesService = TestBed.inject(DotPropertiesService);
    });

    it('should be created', () => {
        expect(editContentGuardService).toBeTruthy();
    });

    it('should allow access to Edit Content new form', () => {
        let result: boolean;
        spyOn(dotPropertiesService, 'getKey').and.returnValue(of('true'));

        editContentGuardService.canActivate().subscribe((res) => (result = res));

        expect(dotPropertiesService.getKey).toHaveBeenCalledWith(
            FeaturedFlags.FEATURE_FLAG_CONTENT_EDITOR2_ENABLE
        );

        expect(result).toBe(true);
    });

    it('should deny access to Edit Content new form', () => {
        let result: boolean;

        spyOn(dotPropertiesService, 'getKey').and.returnValue(of('false'));

        editContentGuardService.canActivate().subscribe((res) => (result = res));

        expect(dotPropertiesService.getKey).toHaveBeenCalledWith(
            FeaturedFlags.DOTFAVORITEPAGE_FEATURE_ENABLE
        );

        expect(result).toBe(false);
    });
});
