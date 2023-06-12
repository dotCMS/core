import { expect, describe } from '@jest/globals';

import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { DotAddStyleClassesDialogStore } from './add-style-classes-dialog.store';

import { MOCK_STYLE_CLASSES_FILE } from '../../../utils/mocks';

describe('DotAddStyleClassesDialogStore', () => {
    let service: DotAddStyleClassesDialogStore;
    let httpTestingController: HttpTestingController;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [HttpClientTestingModule],
            providers: [DotAddStyleClassesDialogStore]
        });
        service = TestBed.inject(DotAddStyleClassesDialogStore);
        httpTestingController = TestBed.inject(HttpTestingController);
    });

    afterEach(() => httpTestingController.verify());

    it('should fetch style classes file', () => {
        service.getStyleClassesFromFile();
        const req = httpTestingController.expectOne('/application/templates/classes.json');
        expect(req.request.method).toEqual('GET');
        req.flush(MOCK_STYLE_CLASSES_FILE);
        service.state$.subscribe((state) => {
            expect(state.styleClasses).toEqual(
                MOCK_STYLE_CLASSES_FILE.classes.map((cssClass) => ({ cssClass: cssClass }))
            );
        });
    });

    it('should set styleClasses to empty array if fetch style classes fails', () => {
        service.getStyleClassesFromFile();
        const req = httpTestingController.expectOne('/application/templates/classes.json');
        expect(req.request.method).toEqual('GET');
        req.flush("This file doesn't exist", { status: 404, statusText: 'Not Found' });
        service.state$.subscribe((state) => {
            expect(state.styleClasses).toEqual([]);
        });
    });
});
