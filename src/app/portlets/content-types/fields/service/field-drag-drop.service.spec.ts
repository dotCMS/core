import { DOTTestBed } from '../../../../test/dot-test-bed';
import { FieldDragDropService } from './field-drag-drop.service';
import { DragulaService } from 'ng2-dragula';
import { Subject, Observable } from 'rxjs';
import { filter, map } from 'rxjs/operators';
import { fakeAsync, tick } from '@angular/core/testing';

const by = (opt: string) => (source: Observable<any>) => {
    return source.pipe(
        filter((data: any) => data.val === opt),
        map((data: any) => data.payload)
    );
};

class MockDragulaService {
    name: string;
    options: any;
    mock: Subject<any> = new Subject();

    dropModel = () => this.mock.asObservable().pipe(by('dropModel'));
    removeModel = () => this.mock.asObservable().pipe(by('removeModel'));
    over = () => this.mock.asObservable().pipe(by('over'));
    out = () => this.mock.asObservable().pipe(by('out'));

    find(): any {
        return null;
    }

    createGroup(name: string, options: any): void {
        this.name = name;
        this.options = options;
    }

    emit(data: { val: string; payload: any }) {
        this.mock.next(data);
    }
}

describe('FieldDragDropService', () => {
    beforeEach(() => {
        this.injector = DOTTestBed.resolveAndCreate([FieldDragDropService, { provide: DragulaService, useClass: MockDragulaService }]);

        this.fieldDragDropService = this.injector.get(FieldDragDropService);
        this.dragulaService = this.injector.get(DragulaService);
    });

    describe('Setting FieldBagOptions', () => {
        it('should set name', () => {
            const findSpy = spyOn(this.dragulaService, 'find').and.returnValue(null);

            this.fieldDragDropService.setFieldBagOptions();

            expect(findSpy).toHaveBeenCalledWith('fields-bag');
            expect(this.dragulaService.name).toBe('fields-bag');
        });

        it('should set shouldCopy', () => {
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

    it('should set bag options for fields and rows', () => {
        spyOn(this.fieldDragDropService, 'setFieldRowBagOptions');
        spyOn(this.fieldDragDropService, 'setFieldBagOptions');

        this.fieldDragDropService.setBagOptions();

        expect(this.fieldDragDropService.setFieldBagOptions).toHaveBeenCalledTimes(1);
        expect(this.fieldDragDropService.setFieldRowBagOptions).toHaveBeenCalledTimes(1);
    });

    it(
        'should emit fieldDropFromSource',
        fakeAsync(() => {
            this.fieldDragDropService.fieldDropFromSource$.subscribe((data) => {
                this.fieldDropFromSource = true;
            });

            this.dragulaService.emit({
                val: 'dropModel',
                payload: {
                    name: 'fields-bag',
                    source: {
                        dataset: {
                            dragType: 'source'
                        }
                    }
                }
            });

            tick();

            expect(this.fieldDropFromSource).toBe(true);
        })
    );

    it(
        'should emit fieldDropFromTarget',
        fakeAsync(() => {
            this.fieldDragDropService.fieldDropFromTarget$.subscribe(() => {
                this.fieldDropFromTarget = true;
            });

            this.dragulaService.emit({
                val: 'dropModel',
                payload: {
                    name: 'fields-bag',
                    source: {
                        dataset: {
                            dragType: 'target'
                        }
                    }
                }
            });

            tick();

            expect(this.fieldDropFromTarget).toBe(true);
        })
    );

    it(
        'should emit fieldRowDropFromSource',
        fakeAsync(() => {
            this.fieldDragDropService.fieldRowDropFromSource$.subscribe(() => {
                this.fieldRowDropFromSource = true;
            });

            this.dragulaService.emit({
                val: 'dropModel',
                payload: {
                    name: 'fields-row-bag',
                    source: {
                        dataset: {
                            dragType: 'source'
                        }
                    }
                }
            });

            tick();

            expect(this.fieldRowDropFromSource).toBe(true);
        })
    );

    it(
        'should emit fieldRowDropFromTarget',
        fakeAsync(() => {
            this.fieldDragDropService.fieldRowDropFromTarget$.subscribe(() => {
                this.fieldRowDropFromTarget = true;
            });

            this.dragulaService.emit({
                val: 'dropModel',
                payload: {
                    name: 'fields-row-bag',
                    source: {
                        dataset: {
                            dragType: 'target'
                        }
                    }
                }
            });

            tick();

            expect(true).toBe(this.fieldRowDropFromTarget);
        })
    );

    it(
        'should toggle class to the target',
        fakeAsync(() => {
            const el = document.createElement('div');
            el.classList.add('row-columns__item');

            this.dragulaService.emit({
                val: 'over',
                payload: {
                    name: '',
                    container: el
                }
            });

            tick();
            expect(el.classList.contains('row-columns__item--over')).toBe(true);

            this.dragulaService.emit({
                val: 'out',
                payload: {
                    name: '',
                    container: el
                }
            });

            tick();
            expect(el.classList.contains('row-columns__item--over')).toBe(false);
        })
    );
});
