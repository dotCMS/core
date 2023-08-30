import { expect, it } from '@jest/globals';
import { Spectator, createComponentFactory } from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { NgIf, AsyncPipe } from '@angular/common';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { FormsModule } from '@angular/forms';

import { AutoCompleteModule } from 'primeng/autocomplete';
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

describe('AddStyleClassesDialogComponent', () => {
    let spectator: Spectator<AddStyleClassesDialogComponent>;
    let service: JsonClassesService;
    let dialogRef: DynamicDialogRef;
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

    beforeEach(() => {
        spectator = createComponent({
            providers: [
                {
                    provide: DynamicDialogConfig,
                    useValue: {
                        data: {
                            selectedClasses: []
                        }
                    }
                },
                {
                    provide: DotMessageService,
                    useValue: new MockDotMessageService(DOT_MESSAGES)
                },
                {
                    provide: JsonClassesService,
                    useValue: {
                        getClasses() {
                            return of({ classes: ['class1', 'class2'] });
                        }
                    }
                },
                {
                    provide: DynamicDialogRef,
                    useValue: {
                        close: jest.fn()
                    }
                }
            ]
        });

        service = spectator.inject(JsonClassesService);
        dialogRef = spectator.inject(DynamicDialogRef);
    });

    it('should create', () => {
        expect(spectator.component).toBeTruthy();
    });

    it('should call getClasses from jsonClassesService on init', () => {
        const getClassesMock = jest.spyOn(service, 'getClasses');
        spectator.detectChanges();

        expect(getClassesMock).toHaveBeenCalled();
    });

    it('should initialize selectedClasses from DynamicDialogConfig data', () => {
        spectator.inject(DynamicDialogConfig).data.selectedClasses = ['class1'];
        spectator.detectChanges();

        expect(spectator.component.selectedClasses).toEqual(['class1']);
    });

    it('should filter classes based on query', () => {
        spectator.component.classes = ['class1', 'class2', 'class3'];
        spectator.component.filterClasses({ query: 'class1' });

        expect(spectator.component.filteredSuggestions).toEqual(['class1']);
    });

    it('should save selected classes and close the dialog', () => {
        spectator.component.selectedClasses = ['class1'];
        spectator.component.save();
        spectator.detectChanges();

        expect(dialogRef.close).toHaveBeenCalledWith(['class1']);
    });

    // More tests can be added as needed...
});
