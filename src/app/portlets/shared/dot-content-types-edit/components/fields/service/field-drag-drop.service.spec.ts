import { DOTTestBed } from '@tests/dot-test-bed';
import { FieldDragDropService } from './field-drag-drop.service';
import { DragulaService } from 'ng2-dragula';
import { Subject, Observable } from 'rxjs';
import { filter, map } from 'rxjs/operators';
import { FieldUtil } from '../util/field-util';
import { DotAlertConfirmService } from '@services/dot-alert-confirm';
import { MockDotMessageService } from '@tests/dot-message-service.mock';
import { DotMessageService } from '@services/dot-message/dot-messages.service';

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

describe('FieldDragDropService', () => {
    let dotAlertConfirmService: DotAlertConfirmService;

    beforeEach(() => {
        this.injector = DOTTestBed.resolveAndCreate([
            FieldDragDropService,
            {
                provide: DragulaService,
                useClass: MockDragulaService
            },
            {
                provide: DotAlertConfirmService,
                useValue: {
                    alert: jasmine.createSpy('alert')
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
        ]);

        this.fieldDragDropService = this.injector.get(FieldDragDropService);
        this.dragulaService = this.injector.get(DragulaService);
        dotAlertConfirmService = this.injector.get(DotAlertConfirmService);
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

        describe('shouldAccepts', () => {
            let acceptsFunc;
            beforeEach(() => {
                this.fieldDragDropService.setFieldBagOptions();
                acceptsFunc = this.dragulaService.options.accepts;
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

                    this.dragulaService.emit({
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
                    this.dragulaService.emit({
                        val: 'drop',
                        payload: {
                            target: null
                        }
                    });

                    expect(dotAlertConfirmService.alert).toHaveBeenCalledTimes(1);
                    expect(dotAlertConfirmService.alert).toHaveBeenCalledWith(
                        jasmine.objectContaining({
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

    it('should emit fieldDropFromSource', () => {
        this.fieldDragDropService.fieldDropFromSource$.subscribe(() => {
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
                },
                target: {
                    dataset: {
                        columnid: '123'
                    }
                }
            }
        });

        expect(this.fieldDropFromSource).toBe(true);
    });

    it('should emit fieldDropFromTarget and set draggedEvent as active/true', () => {
        this.fieldDragDropService.fieldDropFromTarget$.subscribe(() => {
            this.fieldDropFromTarget = true;
        });

        this.dragulaService.emit({
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

        expect(this.fieldDropFromTarget).toBe(true);
        expect(this.fieldDragDropService.isDraggedEventStarted()).toBe(true);
    });

    it('should emit fieldRowDropFromTarget', () => {
        this.fieldDragDropService.fieldRowDropFromTarget$.subscribe(() => {
            this.fieldRowDropFromTarget = true;
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

        expect(this.fieldRowDropFromTarget).toBe(true);
    });

    it('should emit fieldRowDropFromTarget', () => {
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

        expect(true).toBe(this.fieldRowDropFromTarget);
    });

    it('should toggle class over class on over and drop events', () => {
        const source = document.createElement('div');

        const container1 = document.createElement('div');
        container1.classList.add('row-columns__item');

        const container2 = document.createElement('div');
        container2.classList.add('row-columns__item');

        this.dragulaService.emit({
            val: 'over',
            payload: { name: '', el: null, container: container1, source: source }
        });

        this.dragulaService.emit({
            val: 'over',
            payload: { name: '', el: null, container: container2, source: source }
        });

        expect(container2.classList.contains('row-columns__item--over')).toBe(true);

        this.dragulaService.emit({
            val: 'over',
            payload: { name: '', el: null, container: container2, source: source }
        });

        expect(container1.classList.contains('row-columns__item--over')).toBe(false);
        expect(container2.classList.contains('row-columns__item--over')).toBe(true);

        this.dragulaService.emit({
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
