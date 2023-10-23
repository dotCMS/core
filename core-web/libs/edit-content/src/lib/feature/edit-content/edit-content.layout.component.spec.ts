import { Spectator, createComponentFactory, SpyObject } from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { HttpClientTestingModule } from '@angular/common/http/testing';
import { ActivatedRoute } from '@angular/router';

import { EditContentLayoutComponent } from './edit-content.layout.component';

import { DotEditContentService } from '../../shared/services/dot-edit-content.service';
import { CONTENT_TYPE_MOCK, LAYOUT_MOCK } from '../../shared/utils/mocks';

const createEditContentLayoutComponent = (params: { contentType?: string; id?: string }) => {
    return createComponentFactory({
        component: EditContentLayoutComponent,
        imports: [HttpClientTestingModule],
        componentProviders: [
            {
                provide: ActivatedRoute,
                useValue: { snapshot: { params } }
            }
        ]
    });
};

describe('EditContentLayoutComponent with identifier', () => {
    let spectator: Spectator<EditContentLayoutComponent>;
    let dotEditContentService: SpyObject<DotEditContentService>;

    const createComponent = createEditContentLayoutComponent({ contentType: undefined, id: '1' });

    beforeEach(async () => {
        spectator = createComponent({
            detectChanges: false,
            providers: [
                {
                    provide: DotEditContentService,
                    useValue: {
                        getContentTypeFormData: jest.fn().mockReturnValue(of(LAYOUT_MOCK)),
                        getContentById: jest.fn().mockReturnValue(of(CONTENT_TYPE_MOCK)),
                        saveContentlet: jest.fn().mockReturnValue(of({}))
                    }
                }
            ]
        });

        dotEditContentService = spectator.inject(DotEditContentService, true);
    });

    it('should set identifier from activatedRoute and contentType undefined', () => {
        expect(spectator.component.contentType).toEqual(undefined);
        expect(spectator.component.identifier).toEqual('1');
    });

    it('should call getContentById and getContentTypeFormData with contentType if identifier is present', () => {
        spectator.detectChanges();

        expect(dotEditContentService.getContentById).toHaveBeenCalledWith('1');
    });

    it('should call dotEditContentService.saveContentlet with the correct parameters - Using contentType from getContentById', () => {
        spectator.detectChanges();
        spectator.component.saveContent({ key: 'value' });
        expect(dotEditContentService.saveContentlet).toHaveBeenCalledWith({
            key: 'value',
            inode: '1',
            contentType: 'Test'
        });
    });

    it('should have a [formData] reference on the <dot-edit-content-form>', async () => {
        spectator.detectChanges();
        const formElement = spectator.query('dot-edit-content-form');
        expect(formElement.hasAttribute('ng-reflect-form-data')).toBe(true);
        expect(formElement).toBeDefined();
    });
});

describe('EditContentLayoutComponent without identifier', () => {
    let spectator: Spectator<EditContentLayoutComponent>;
    let dotEditContentService: SpyObject<DotEditContentService>;

    const createComponent = createEditContentLayoutComponent({
        contentType: 'test',
        id: undefined
    });

    beforeEach(() => {
        spectator = createComponent({
            detectChanges: false,
            providers: [
                {
                    provide: DotEditContentService,
                    useValue: {
                        getContentTypeFormData: jest.fn().mockReturnValue(of(LAYOUT_MOCK)),
                        getContentById: jest.fn().mockReturnValue(of(CONTENT_TYPE_MOCK))
                    }
                }
            ]
        });

        dotEditContentService = spectator.inject(DotEditContentService, true);
    });

    it('should set contentType from activatedRoute - Identifier undefined.', () => {
        expect(spectator.component.contentType).toEqual('test');
        expect(spectator.component.identifier).toEqual(undefined);
    });

    it('should call getContentById and getContentTypeFormData with contentType if identifier is NOT present', () => {
        spectator.detectChanges();
        expect(dotEditContentService.getContentById).not.toHaveBeenCalled();
        expect(dotEditContentService.getContentTypeFormData).toHaveBeenCalledWith('test');
    });
});
