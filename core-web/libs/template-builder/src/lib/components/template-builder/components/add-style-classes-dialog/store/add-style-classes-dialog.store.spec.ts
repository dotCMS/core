import { expect, describe } from '@jest/globals';

import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { DotAddStyleClassesDialogStore } from './add-style-classes-dialog.store';

import { MOCK_STYLE_CLASSES_FILE } from '../../../utils/mocks';

jest.mock('uuid', () => ({
    v4: () => 'test-id'
}));

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

    it('should have selected style classes on init when passed classes', (done) => {
        service.init({ selectedClasses: ['class1', 'class2'] });
        service.state$.subscribe((state) => {
            expect(state.selectedClasses).toEqual([
                { cssClass: 'class1', id: 'test-id' },
                { cssClass: 'class2', id: 'test-id' }
            ]);
            done();
        });
    });

    it('should not have selected style classes on init', (done) => {
        service.state$.subscribe((state) => {
            expect(state.selectedClasses.length).toBe(0);
            done();
        });
    });

    it('should add a class to selected ', (done) => {
        service.init({ selectedClasses: [] });

        service.addClass({ cssClass: 'class1', id: 'test-id' });

        service.state$.subscribe((state) => {
            expect(state.selectedClasses.length).toBe(1);
            done();
        });
    });

    it('should remove a class from selected that have the same id', (done) => {
        service.init({ selectedClasses: ['class1'] });

        service.removeClass({
            cssClass: 'class1',
            id: 'test-id'
        });

        service.state$.subscribe((state) => {
            expect(state.selectedClasses).toEqual([]);
            done();
        });
    });

    it('should fetch style classes file', (done) => {
        service.fetchStyleClasses();
        const req = httpTestingController.expectOne('/application/templates/classes.json');
        expect(req.request.method).toEqual('GET');
        req.flush(MOCK_STYLE_CLASSES_FILE);
        service.state$.subscribe((state) => {
            expect(state.styleClasses).toEqual(
                MOCK_STYLE_CLASSES_FILE.classes.map((cssClass) => ({ cssClass, id: 'test-id' }))
            );
            done();
        });
    });

    it('should set styleClasses to empty array if fetch style classes fails', (done) => {
        service.fetchStyleClasses();
        const req = httpTestingController.expectOne('/application/templates/classes.json');
        expect(req.request.method).toEqual('GET');
        req.flush("This file doesn't exist", { status: 404, statusText: 'Not Found' });
        service.state$.subscribe((state) => {
            expect(state.styleClasses).toEqual([]);
            done();
        });
    });

    it('should filter style classes by a query', (done) => {
        const query = 'align';

        service.fetchStyleClasses();
        const req = httpTestingController.expectOne('/application/templates/classes.json');
        req.flush(MOCK_STYLE_CLASSES_FILE);

        service.filterClasses(query);

        service.state$.subscribe((state) => {
            expect(
                state.filteredClasses.every(({ cssClass }) => cssClass.startsWith(query))
            ).toBeTruthy();
            done();
        });
    });

    it('should add class to selectedClasses when found a comma on query', (done) => {
        const query = 'align,';

        service.filterClasses(query);

        service.state$.subscribe((state) => {
            expect(state.selectedClasses).toEqual([{ cssClass: 'align', id: 'test-id' }]);
            done();
        });
    });

    it('should add class to selectedClasses when found a comma on query', (done) => {
        const query = 'align ';

        service.filterClasses(query);

        service.state$.subscribe((state) => {
            expect(state.selectedClasses).toEqual([{ cssClass: 'align', id: 'test-id' }]);
            done();
        });
    });

    it('should show query on filtered is nothing is found', (done) => {
        const query = 'align-custom-class';

        service.filterClasses(query);

        service.state$.subscribe((state) => {
            expect(state.filteredClasses).toEqual([{ cssClass: query, id: 'test-id' }]);
            done();
        });
    });

    it('should filter selected classes from filteredClass', (done) => {
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
            done();
        });
    });
});
