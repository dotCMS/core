
import { DOTTestBed } from '../../../../test/dot-test-bed';
import { FieldDragDropService } from './field-drag-drop.service';
import { DragulaService } from 'ng2-dragula';
import { EventEmitter } from '@angular/core';

class MockDragulaService {
    dropModel = new EventEmitter<any>();
    removeModel = new EventEmitter<any>();

    name: string;
    options: any;

    find(name: string): any {
        return null;
    }

    setOptions(name: string, options: any): void {
        this.name = name;
        this.options = options;
    }
}

describe('FieldDragDropService', () => {
    beforeEach(() => {
        this.injector = DOTTestBed.resolveAndCreate([
            FieldDragDropService,
            { provide: DragulaService, useClass: MockDragulaService },
        ]);

        this.fieldDragDropService = this.injector.get(FieldDragDropService);
        this.dragulaService = this.injector.get(DragulaService);
    });

    describe('Setting FieldBagOptions', () => {
        it('should set name', () => {
            const findSpy = spyOn(this.dragulaService, 'find').and.returnValue(null);

            this.fieldDragDropService.setFieldBagOptions();

            expect(findSpy).toHaveBeenCalledWith('fields-bag');
            expect('fields-bag').toBe(this.dragulaService.name);
        });

        it('should set shouldCopy', () => {
            const findSpy = spyOn(this.dragulaService, 'find').and.returnValue(null);

            this.fieldDragDropService.setFieldBagOptions();

            const copyFunc = this.dragulaService.options.copy;
            const source = {
                dataset: {
                    dragType: 'source'
                }
            };

            expect(true).toBe(copyFunc(null, source, null, null));

            source.dataset.dragType = 'target';
            expect(false).toBe(copyFunc(null, source, null, null));
        });

        it('should set shouldAccepts', () => {
            const findSpy = spyOn(this.dragulaService, 'find').and.returnValue(null);

            this.fieldDragDropService.setFieldBagOptions();
            const acceptsFunc = this.dragulaService.options.accepts;
            const source = {
                dataset: {
                    dragType: 'source'
                }
            };

            expect(false).toBe(acceptsFunc(null, source, null, null));

            source.dataset.dragType = 'target';
            expect(true).toBe(acceptsFunc(null, source, null, null));
        });
    });

    describe('Setting FieldRowBagOptions', () => {
        it('should set name', () => {
            const findSpy = spyOn(this.dragulaService, 'find').and.returnValue(null);

            this.fieldDragDropService.setFieldRowBagOptions();

            expect(findSpy).toHaveBeenCalledWith('fields-row-bag');
            expect('fields-row-bag').toBe(this.dragulaService.name);
        });

        it('should set shouldCopy', () => {
            const findSpy = spyOn(this.dragulaService, 'find').and.returnValue(null);

            this.fieldDragDropService.setFieldRowBagOptions();

            const copyFunc = this.dragulaService.options.copy;
            const source = {
                dataset: {
                    dragType: 'source'
                }
            };

            expect(true).toBe(copyFunc(null, source, null, null));

            source.dataset.dragType = 'target';
            expect(false).toBe(copyFunc(null, source, null, null));
        });
    });

    it('should emit fieldDropFromSource', () => {
        this.fieldDragDropService.fieldDropFromSource$.subscribe(() => this.fieldDropFromSource = true);

        this.dragulaService.dropModel.emit([
            'fields-bag',
            null,
            null,
            {
                dataset: {
                    dragType: 'source'
                }
            }
        ]);

        expect(true).toBe(this.fieldDropFromSource);
    });

    it('should emit fieldDropFromTarget', () => {
        this.fieldDragDropService.fieldDropFromTarget$.subscribe(() => this.fieldDropFromTarget = true);

        this.dragulaService.dropModel.emit([
            'fields-bag',
            null,
            null,
            {
                dataset: {
                    dragType: 'target'
                }
            }
        ]);

        expect(true).toBe(this.fieldDropFromTarget);
    });


    it('should emit fieldRowDropFromSource', () => {
        this.fieldDragDropService.fieldRowDropFromSource$.subscribe(() => this.fieldRowDropFromSource = true);

        this.dragulaService.dropModel.emit([
            'fields-row-bag',
            null,
            null,
            {
                dataset: {
                    dragType: 'source'
                }
            }
        ]);

        expect(true).toBe(this.fieldRowDropFromSource);
    });

    it('should emit fieldRowDropFromTarget', () => {
        this.fieldDragDropService.fieldRowDropFromTarget$.subscribe(() => this.fieldRowDropFromTarget = true);

        this.dragulaService.dropModel.emit([
            'fields-row-bag',
            null,
            null,
            {
                dataset: {
                    dragType: 'target'
                }
            }
        ]);

        expect(true).toBe(this.fieldRowDropFromTarget);
    });
});
