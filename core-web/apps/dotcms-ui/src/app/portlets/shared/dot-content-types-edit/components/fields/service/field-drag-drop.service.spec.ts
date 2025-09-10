/* eslint-disable @typescript-eslint/no-explicit-any */

import { DragulaService } from 'ng2-dragula';
import { Observable, Subject } from 'rxjs';

import { TestBed } from '@angular/core/testing';

import { filter, map } from 'rxjs/operators';

import { DotAlertConfirmService, DotMessageService } from '@dotcms/data-access';
import { FieldUtil } from '@dotcms/utils';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { FieldDragDropService } from './field-drag-drop.service';

const by = (opt: string) => (source: Observable<any>) => {
    return source.pipe(
        filter((data: any) => data.val === opt),
        map((data: any) => data.payload)
    );
};

const COLUMN_BREAK_FIELD = FieldUtil.createColumnBreak();

class MockDragulaService {
    name: string;
    options: any;
    mock: Subject<any> = new Subject();

    dropModel = () => this.mock.asObservable().pipe(by('dropModel'));
    drop = () => this.mock.asObservable().pipe(by('drop'));
    dragend = () => this.mock.asObservable().pipe(by('dragend'));
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

let fieldDragDropService;
let dragulaService;

describe('FieldDragDropService', () => {
    let dotAlertConfirmService: DotAlertConfirmService;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [
                FieldDragDropService,
                {
                    provide: DragulaService,
                    useClass: MockDragulaService
                },
                {
                    provide: DotAlertConfirmService,
                    useValue: {
                        alert: jest.fn()
                    }
                },
                {
                    provide: DotMessageService,
                    useValue: new MockDotMessageService({
                        'contenttypes.fullrow.dialog.header': 'This row is full',
                        'contenttypes.fullrow.dialog.message':
                            'The maximum number of columns per row is limited to four.',
                        'contenttypes.fullrow.dialog.accept': 'Dismiss'
                    })
                }
            ]
        });

        fieldDragDropService = TestBed.inject(FieldDragDropService);
        dragulaService = TestBed.inject(DragulaService);
        dotAlertConfirmService = TestBed.inject(DotAlertConfirmService);
    });

    describe('Setting FieldBagOptions', () => {
        it('should set name', () => {
            const findSpy = jest.spyOn(dragulaService, 'find').mockReturnValue(null);

            fieldDragDropService.setFieldBagOptions();

            expect(findSpy).toHaveBeenCalledWith('fields-bag');
            expect(dragulaService.name).toBe('fields-bag');
        });

        it('should set shouldCopy', () => {
            fieldDragDropService.setFieldBagOptions();

            const copyFunc = dragulaService.options.copy;
            const source = {
                dataset: {
                    dragType: 'source'
                }
            };

            expect(true).toBe(copyFunc(null, source, null, null));

            source.dataset.dragType = 'target';
            expect(false).toBe(copyFunc(null, source, null, null));
        });

        describe('shouldAccepts', () => {
            let acceptsFunc;
            beforeEach(() => {
                fieldDragDropService.setFieldBagOptions();
                acceptsFunc = dragulaService.options.accepts;
            });

            it('should return true for any field', () => {
                const target = {
                    parentElement: { querySelectorAll: () => [] }
                };

                const el = {
                    dataset: {
                        clazz: 'whats'
                    }
                };

                expect(acceptsFunc(el, target, null, null)).toBe(true);
            });

            it('should return true for break column field', () => {
                const target = {
                    parentElement: { querySelectorAll: () => [] }
                };

                const el = {
                    dataset: {
                        clazz: COLUMN_BREAK_FIELD.clazz
                    }
                };

                expect(acceptsFunc(el, target, null, null)).toBe(true);
            });

            it('should return false when for break column when row have 4 colums', () => {
                const target = {
                    parentElement: {
                        querySelectorAll: () => [1, 2, 3, 4],
                        parentElement: {
                            style: {}
                        }
                    }
                };

                const el = {
                    dataset: {
                        clazz: COLUMN_BREAK_FIELD.clazz
                    }
                };

                expect(acceptsFunc(el, target, null, null)).toBe(false);
            });

            describe('style row', () => {
                let target;
                let el;

                beforeEach(() => {
                    target = {
                        parentElement: {
                            querySelectorAll: () => [1, 2, 3, 4],
                            parentElement: {
                                style: {}
                            }
                        }
                    };

                    el = {
                        dataset: {
                            clazz: COLUMN_BREAK_FIELD.clazz
                        }
                    };
                });

                it('should add custom style to row when cant add column', () => {
                    acceptsFunc(el, target, null, null);

                    expect(target.parentElement.parentElement.style).toEqual({
                        opacity: '0.4',
                        cursor: 'not-allowed'
                    });
                });

                it('should remove custom style to row on drop', () => {
                    acceptsFunc(el, target, null, null);

                    dragulaService.emit({
                        val: 'drop',
                        payload: {
                            target: null
                        }
                    });

                    expect(target.parentElement.parentElement.style).toEqual({
                        opacity: null,
                        cursor: null
                    });
                });

                it('should show alert when cant add column', () => {
                    acceptsFunc(el, target, null, null);
                    dragulaService.emit({
                        val: 'drop',
                        payload: {
                            target: null
                        }
                    });

                    expect(dotAlertConfirmService.alert).toHaveBeenCalledTimes(1);
                    expect(dotAlertConfirmService.alert).toHaveBeenCalledWith(
                        expect.objectContaining({
                            header: 'This row is full',
                            message: 'The maximum number of columns per row is limited to four.',
                            footerLabel: {
                                accept: 'Dismiss'
                            }
                        })
                    );
                });
            });
        });
    });

    describe('Setting FieldRowBagOptions', () => {
        it('should set name', () => {
            const findSpy = jest.spyOn(dragulaService, 'find').mockReturnValue(null);

            fieldDragDropService.setFieldRowBagOptions();

            expect(findSpy).toHaveBeenCalledWith('fields-row-bag');
            expect('fields-row-bag').toBe(dragulaService.name);
        });

        it('should set shouldCopy', () => {
            fieldDragDropService.setFieldRowBagOptions();

            const copyFunc = dragulaService.options.copy;
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
        jest.spyOn(fieldDragDropService, 'setFieldRowBagOptions');
        jest.spyOn(fieldDragDropService, 'setFieldBagOptions');

        fieldDragDropService.setBagOptions();

        expect(fieldDragDropService.setFieldBagOptions).toHaveBeenCalledTimes(1);
        expect(fieldDragDropService.setFieldRowBagOptions).toHaveBeenCalledTimes(1);
    });

    it('should emit fieldDropFromSource', () => {
        let result;
        fieldDragDropService.fieldDropFromSource$.subscribe(() => {
            result = true;
        });

        dragulaService.emit({
            val: 'dropModel',
            payload: {
                name: 'fields-bag',
                source: {
                    dataset: {
                        dragType: 'source'
                    }
                },
                target: {
                    dataset: {
                        columnid: '123'
                    }
                }
            }
        });

        expect(result).toBe(true);
    });

    it('should emit fieldDropFromTarget and set draggedEvent as active/true', () => {
        let result;
        fieldDragDropService.fieldDropFromTarget$.subscribe(() => {
            result = true;
        });

        dragulaService.emit({
            val: 'dropModel',
            payload: {
                name: 'fields-bag',
                container: document.createElement('div'),
                source: {
                    dataset: {
                        dragType: 'target'
                    }
                },
                target: {
                    dataset: {
                        columnid: '123'
                    }
                }
            }
        });

        expect(result).toBe(true);
        expect(fieldDragDropService.isDraggedEventStarted()).toBe(true);
    });

    it('should emit fieldRowDropFromTarget', () => {
        let result;
        fieldDragDropService.fieldRowDropFromTarget$.subscribe(() => {
            result = true;
        });

        dragulaService.emit({
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

        expect(result).toBe(true);
    });

    it('should emit fieldRowDropFromTarget', () => {
        let result;
        fieldDragDropService.fieldRowDropFromTarget$.subscribe(() => {
            result = true;
        });

        dragulaService.emit({
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

        expect(true).toBe(result);
    });

    it('should toggle class over class on over and drop events', () => {
        const source = document.createElement('div');

        const container1 = document.createElement('div');
        container1.classList.add('row-columns__item');

        const container2 = document.createElement('div');
        container2.classList.add('row-columns__item');

        dragulaService.emit({
            val: 'over',
            payload: { name: '', el: null, container: container1, source: source }
        });

        dragulaService.emit({
            val: 'over',
            payload: { name: '', el: null, container: container2, source: source }
        });

        expect(container2.classList.contains('row-columns__item--over')).toBe(true);

        dragulaService.emit({
            val: 'over',
            payload: { name: '', el: null, container: container2, source: source }
        });

        expect(container1.classList.contains('row-columns__item--over')).toBe(false);
        expect(container2.classList.contains('row-columns__item--over')).toBe(true);

        dragulaService.emit({
            val: 'dragend',
            payload: {
                name: 'fields-bag',
                source: {
                    dataset: {
                        dragType: 'target'
                    }
                }
            }
        });

        expect(container2.classList.contains('row-columns__item--over')).toBe(false);
    });
});
