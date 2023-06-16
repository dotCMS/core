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

    it('should have selected style classes on init when passed classes', () => {
        service.init({ selectedClasses: ['class1', 'class2'] });
        service.state$.subscribe((state) => {
            expect(state.selectedClasses).toEqual([{ cssClass: 'class1' }, { cssClass: 'class2' }]);
        });
    });

    it('should not have selected style classes on init', () => {
        service.state$.subscribe((state) => {
            expect(state.selectedClasses.length).toBe(0);
        });
    });

    it('should add a class to selected ', () => {
        service.init({ selectedClasses: [] });

        service.addClass({ cssClass: 'class1' });

        service.state$.subscribe((state) => {
            expect(state.selectedClasses.length).toBe(1);
        });
    });

    it('should remove last class from selected ', () => {
        service.init({ selectedClasses: ['class1', 'class2'] });

        service.removeLastClass();

        service.state$.subscribe((state) => {
            expect(state.selectedClasses).toEqual([{ cssClass: 'class1' }]);
        });
    });

    it('should fetch style classes file', () => {
        service.fetchStyleClasses();
        const req = httpTestingController.expectOne('/application/templates/classes.json');
        expect(req.request.method).toEqual('GET');
        req.flush(MOCK_STYLE_CLASSES_FILE);
        service.state$.subscribe((state) => {
            expect(state.styleClasses).toEqual(
                MOCK_STYLE_CLASSES_FILE.classes.map((cssClass) => ({ cssClass }))
            );
        });
    });

    it('should set styleClasses to empty array if fetch style classes fails', () => {
        service.fetchStyleClasses();
        const req = httpTestingController.expectOne('/application/templates/classes.json');
        expect(req.request.method).toEqual('GET');
        req.flush("This file doesn't exist", { status: 404, statusText: 'Not Found' });
        service.state$.subscribe((state) => {
            expect(state.styleClasses).toEqual([]);
        });
    });

    it('should filter style classes by a query', () => {
        const query = 'align';

        service.fetchStyleClasses();
        const req = httpTestingController.expectOne('/application/templates/classes.json');
        req.flush(MOCK_STYLE_CLASSES_FILE);

        service.filterClasses(query);

        service.state$.subscribe((state) => {
            expect(
                state.filteredClasses.every(({ cssClass }) => cssClass.startsWith(query))
            ).toBeTruthy();
        });
    });

    it('should add class to selectedClasses when found a comma on query', () => {
        const query = 'align,';

        service.filterClasses(query);

        service.state$.subscribe((state) => {
            expect(state.selectedClasses).toEqual([{ cssClass: 'align' }]);
        });
    });

    it('should add class to selectedClasses when found a comma on query', () => {
        const query = 'align ';

        service.filterClasses(query);

        service.state$.subscribe((state) => {
            expect(state.selectedClasses).toEqual([{ cssClass: 'align' }]);
        });
    });

    it('should show query on filtered is nothing is found', () => {
        const query = 'align-custom-class';

        service.filterClasses(query);

        service.state$.subscribe((state) => {
            expect(state.filteredClasses).toEqual([{ cssClass: query }]);
        });
    });

    it('should filter selected classes from filteredClass', () => {
        const query = 'd-';
        const selectedClasses = ['d-flex'];
        service.init({ selectedClasses });

        service.fetchStyleClasses();
        const req = httpTestingController.expectOne('/application/templates/classes.json');
        req.flush(MOCK_STYLE_CLASSES_FILE);

        service.filterClasses(query);

        service.state$.subscribe((state) => {
            expect(
                state.filteredClasses.find(({ cssClass }) => cssClass == selectedClasses[0])
            ).toBeFalsy();
        });
    });
});
