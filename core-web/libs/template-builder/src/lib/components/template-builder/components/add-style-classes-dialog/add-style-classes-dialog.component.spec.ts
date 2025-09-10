import { expect, it } from '@jest/globals';
import { createFakeEvent } from '@ngneat/spectator';
import { Spectator, byTestId, createComponentFactory, mockProvider } from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { provideHttpClient } from '@angular/common/http';
import { FormsModule } from '@angular/forms';

import { AutoComplete, AutoCompleteModule } from 'primeng/autocomplete';
import { ButtonModule } from 'primeng/button';
import { DynamicDialogConfig, DynamicDialogModule, DynamicDialogRef } from 'primeng/dynamicdialog';

import { DotMessageService } from '@dotcms/data-access';
import { DotMessagePipe } from '@dotcms/ui';
import { MockDotMessageService, mockMatchMedia } from '@dotcms/utils-testing';

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
    let jsonClassesService: JsonClassesService;
    let dialogRef: DynamicDialogRef;
    let autocomplete: AutoComplete;

    const createComponent = createComponentFactory({
        imports: [
            AutoCompleteModule,
            DynamicDialogModule,
            FormsModule,
            ButtonModule,
            DotMessagePipe
        ],
        component: AddStyleClassesDialogComponent,
        providers: [DynamicDialogRef, DynamicDialogConfig, DotMessageService, provideHttpClient()],
        detectChanges: false
    });

    describe('with classes', () => {
        beforeEach(() => {
            spectator = createComponent({
                providers: [
                    ...providers,
                    mockProvider(DynamicDialogConfig, {
                        data: {
                            selectedClasses: ['backend-class']
                        }
                    }),
                    mockProvider(JsonClassesService, {
                        getClasses: jest.fn().mockReturnValue(of(['class1', 'class2']))
                    })
                ]
            });

            jsonClassesService = spectator.inject(JsonClassesService, true);
            dialogRef = spectator.inject(DynamicDialogRef);
            autocomplete = spectator.query(AutoComplete);
            mockMatchMedia();
        });

        it('should set attributes to autocomplete', () => {
            spectator.detectChanges();
            expect(autocomplete.unique).toBe(true);
            expect(autocomplete.autofocus).toBe(true);
            expect(autocomplete.multiple).toBe(true);
            expect(autocomplete.inputId).toBe('auto-complete-input');
            expect(autocomplete.appendTo).toBe('body');
            expect(autocomplete.dropdown).toBe(true);
            expect(autocomplete.el.nativeElement.className).toContain('p-fluid');
            expect(autocomplete.suggestions).toEqual(['class1', 'class2']);
        });

        it('should call jsonClassesService.getClasses on init', async () => {
            spectator.detectChanges();
            expect(jsonClassesService.getClasses).toHaveBeenCalledTimes(1);
        });

        it('should set classes property on init', () => {
            spectator.detectChanges();

            expect(spectator.component.$classes()).toEqual(['class1', 'class2']);
        });

        it('should initialize selectedClasses from DynamicDialogConfig data', () => {
            spectator.detectChanges();

            expect(spectator.component.$selectedClasses()).toEqual(['backend-class']);
        });

        it('should filter suggestions and pass to autocomplete on completeMethod', () => {
            spectator.detectChanges();
            spectator.triggerEventHandler(AutoComplete, 'completeMethod', {
                query: 'class1',
                originalEvent: createFakeEvent('click')
            });

            expect(autocomplete.suggestions).toEqual(['class1']);
        });

        it('should add class on keyup.enter', () => {
            spectator.detectChanges();

            const input = spectator.query('input#auto-complete-input');

            spectator.typeInElement('new value', input);
            spectator.dispatchKeyboardEvent(input, 'keyup', 'Enter', input);

            expect(spectator.component.$selectedClasses()).toContain('new value');
        });

        it('should save selected classes and close the dialog', () => {
            spectator.component.$selectedClasses.set(['class1']);
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
                                return of([]);
                            }
                        }
                    }
                ]
            });

            jsonClassesService = spectator.inject(JsonClassesService, true);
            dialogRef = spectator.inject(DynamicDialogRef);
            autocomplete = spectator.query(AutoComplete);
        });

        it('should set dropdown to false in autocomplete', () => {
            spectator.detectChanges();
            expect(autocomplete.dropdown).toBe(false);
        });

        it('should set component.classes empty', () => {
            spectator.detectChanges();

            expect(spectator.component.$classes()).toEqual([]);
        });

        it('should have multiples help message', () => {
            spectator.detectChanges();
            const list = spectator.query(byTestId('list'));

            expect(list.textContent).toContain('no suggestions setup suggestions');
        });
    });
});
