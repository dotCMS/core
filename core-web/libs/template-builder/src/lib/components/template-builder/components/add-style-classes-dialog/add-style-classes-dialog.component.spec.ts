import { expect, it } from '@jest/globals';
import { Spectator, byTestId, createComponentFactory } from '@ngneat/spectator/jest';
import { of, throwError } from 'rxjs';

import { NgIf, AsyncPipe } from '@angular/common';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { FormsModule } from '@angular/forms';

import { AutoComplete, AutoCompleteModule } from 'primeng/autocomplete';
import { ButtonModule } from 'primeng/button';
import { DynamicDialogConfig, DynamicDialogRef, DynamicDialogModule } from 'primeng/dynamicdialog';

import { DotMessageService } from '@dotcms/data-access';
import { DotMessagePipe } from '@dotcms/ui';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { AddStyleClassesDialogComponent } from './add-style-classes-dialog.component';
import { JsonClassesService } from './services/json-classes.service';

const DOT_MESSAGES = {
    'dot.template.builder.autocomplete.has.suggestions': 'has suggestions',
    'dot.template.builder.autocomplete.no.suggestions': 'no suggestions',
    'dot.template.builder.autocomplete.setup.suggestions': 'setup suggestions',
    'dot.template.builder.classes.dialog.update.button': 'update button'
};

const providers = [
    {
        provide: DotMessageService,
        useValue: new MockDotMessageService(DOT_MESSAGES)
    },
    {
        provide: DynamicDialogRef,
        useValue: {
            close: jest.fn()
        }
    }
];

describe('AddStyleClassesDialogComponent', () => {
    let spectator: Spectator<AddStyleClassesDialogComponent>;
    let service: JsonClassesService;
    let dialogRef: DynamicDialogRef;
    let autocomplete: AutoComplete;

    const createComponent = createComponentFactory({
        imports: [
            AutoCompleteModule,
            HttpClientTestingModule,
            DynamicDialogModule,
            FormsModule,
            ButtonModule,
            DotMessagePipe,
            NgIf,
            AsyncPipe
        ],
        component: AddStyleClassesDialogComponent,
        providers: [JsonClassesService, DynamicDialogRef, DynamicDialogConfig, DotMessageService],
        detectChanges: false
    });

    describe('with classes', () => {
        beforeEach(() => {
            spectator = createComponent({
                providers: [
                    ...providers,
                    {
                        provide: DynamicDialogConfig,
                        useValue: {
                            data: {
                                selectedClasses: ['backend-class']
                            }
                        }
                    },
                    {
                        provide: JsonClassesService,
                        useValue: {
                            getClasses() {
                                return of({ classes: ['class1', 'class2'] });
                            }
                        }
                    }
                ]
            });

            service = spectator.inject(JsonClassesService);
            dialogRef = spectator.inject(DynamicDialogRef);
            autocomplete = spectator.query(AutoComplete);
        });

        it('should set attributes to autocomplete', () => {
            spectator.detectChanges();
            expect(autocomplete.unique).toBe(true);
            expect(autocomplete.autofocus).toBe(true);
            expect(autocomplete.multiple).toBe(true);
            expect(autocomplete.size).toBe(446);
            expect(autocomplete.inputId).toBe('auto-complete-input');
            expect(autocomplete.appendTo).toBe('body');
            expect(autocomplete.dropdown).toBe(true);
            expect(autocomplete.el.nativeElement.className).toContain('p-fluid');
            expect(autocomplete.suggestions).toBe(null);
        });

        it('should call jsonClassesService.getClasses on init', () => {
            const getClassesMock = jest.spyOn(service, 'getClasses');
            spectator.detectChanges();

            expect(getClassesMock).toHaveBeenCalledTimes(1);
        });

        it('should set classes property on init', () => {
            spectator.detectChanges();

            expect(spectator.component.classes).toEqual(['class1', 'class2']);
        });

        it('should initialize selectedClasses from DynamicDialogConfig data', () => {
            spectator.detectChanges();

            expect(spectator.component.selectedClasses).toEqual(['backend-class']);
        });

        it('should filter suggestions and pass to autocomplete on completeMethod', () => {
            spectator.detectChanges();
            spectator.triggerEventHandler(AutoComplete, 'completeMethod', { query: 'class1' });

            expect(autocomplete.suggestions).toEqual(['class1']);
        });

        it('should add class on keyup.enter', () => {
            const selectItemSpy = jest.spyOn(autocomplete, 'selectItem');
            spectator.detectChanges();

            const input = document.createElement('input');
            input.value = 'class1';

            spectator.triggerEventHandler(AutoComplete, 'onKeyUp', { key: 'Enter', target: input });

            expect(selectItemSpy).toBeCalledWith('class1');
        });

        it('should save selected classes and close the dialog', () => {
            spectator.component.selectedClasses = ['class1'];
            spectator.component.save();
            spectator.detectChanges();

            expect(dialogRef.close).toHaveBeenCalledWith(['class1']);
        });

        it('should have help message', () => {
            spectator.detectChanges();
            const list = spectator.query(byTestId('list'));

            expect(list.textContent).toContain('has suggestions');
        });
    });

    describe('no classes', () => {
        beforeEach(() => {
            spectator = createComponent({
                providers: [
                    ...providers,
                    {
                        provide: DynamicDialogConfig,
                        useValue: {
                            data: {
                                selectedClasses: []
                            }
                        }
                    },
                    {
                        provide: JsonClassesService,
                        useValue: {
                            getClasses() {
                                return of({ classes: [] });
                            }
                        }
                    }
                ]
            });

            service = spectator.inject(JsonClassesService);
            dialogRef = spectator.inject(DynamicDialogRef);
            autocomplete = spectator.query(AutoComplete);
        });

        it('should set dropdown to false in autocomplete', () => {
            spectator.detectChanges();
            expect(autocomplete.dropdown).toBe(false);
        });

        it('should set component.classes empty', () => {
            spectator.detectChanges();

            expect(spectator.component.classes).toEqual([]);
        });

        it('should have multiples help message', () => {
            spectator.detectChanges();
            const list = spectator.query(byTestId('list'));

            expect(list.textContent).toContain('no suggestions setup suggestions');
        });
    });

    describe('bad format json', () => {
        beforeEach(() => {
            spectator = createComponent({
                providers: [
                    ...providers,
                    {
                        provide: DynamicDialogConfig,
                        useValue: {
                            data: {
                                selectedClasses: []
                            }
                        }
                    },
                    {
                        provide: JsonClassesService,
                        useValue: {
                            getClasses() {
                                return of({ badFormat: ['class1'] });
                            }
                        }
                    }
                ]
            });

            service = spectator.inject(JsonClassesService);
            dialogRef = spectator.inject(DynamicDialogRef);
            autocomplete = spectator.query(AutoComplete);
        });

        it('should set dropdown to false in autocomplete', () => {
            spectator.detectChanges();
            expect(autocomplete.dropdown).toBe(false);
        });

        it('should set component.classes empty', () => {
            spectator.detectChanges();

            expect(spectator.component.classes).toEqual([]);
        });

        it('should have multiples help message', () => {
            spectator.detectChanges();
            const list = spectator.query(byTestId('list'));

            expect(list.textContent).toContain('no suggestions setup suggestions');
        });
    });

    describe('error', () => {
        beforeEach(() => {
            spectator = createComponent({
                providers: [
                    ...providers,
                    {
                        provide: DynamicDialogConfig,
                        useValue: {
                            data: {
                                selectedClasses: []
                            }
                        }
                    },
                    {
                        provide: JsonClassesService,
                        useValue: {
                            getClasses() {
                                return throwError(
                                    new Error('An error occurred while fetching classes')
                                );
                            }
                        }
                    }
                ]
            });

            service = spectator.inject(JsonClassesService);
            dialogRef = spectator.inject(DynamicDialogRef);
            autocomplete = spectator.query(AutoComplete);
        });

        it('should set dropdown to false in autocomplete', () => {
            spectator.detectChanges();
            expect(autocomplete.dropdown).toBe(false);
        });

        it('should set component.classes empty', () => {
            spectator.detectChanges();

            expect(spectator.component.classes).toEqual([]);
        });
    });

    // More tests can be added as needed...
});
