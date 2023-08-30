import { Spectator, createComponentFactory } from '@ngneat/spectator';
import { of } from 'rxjs';

import { AutoCompleteModule } from 'primeng/autocomplete';
import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';

import { AddStyleClassesDialogComponent } from './add-style-classes-dialog.component';
import { JsonClassesService } from './services/json-classes.service';

describe('AddStyleClassesDialogComponent', () => {
    let spectator: Spectator<AddStyleClassesDialogComponent>;
    const createComponent = createComponentFactory({
        imports: [AutoCompleteModule],
        component: AddStyleClassesDialogComponent,
        componentMocks: [JsonClassesService],
        componentProviders: [
            {
                provide: JsonClassesService,
                useValue: {
                    getClasses() {
                        console.log('fake');

                        return jest.fn().mockReturnValue(of({ classes: ['class1', 'class2'] }));
                    }
                }
            }
        ],
        mocks: [DynamicDialogConfig, DynamicDialogRef],
        detectChanges: false
    });

    beforeEach(() => {
        spectator = createComponent();
    });

    it('should create', () => {
        expect(spectator.component).toBeTruthy();
    });

    it('should initialize selectedClasses from DynamicDialogConfig data', () => {
        spectator.inject(DynamicDialogConfig).data.selectedClasses = ['class1'];
        spectator.detectChanges();

        expect(spectator.component.selectedClasses).toEqual(['class1']);
    });

    it('should call getClasses from jsonClassesService on init', () => {
        const service = spectator.inject(JsonClassesService);
        service.getClasses.and.returnValue(of({ classes: ['class1', 'class2'] }));
        spectator.detectChanges();

        expect(service.getClasses).toHaveBeenCalled();
    });

    it('should filter classes based on query', () => {
        spectator.component.classes = ['class1', 'class2', 'class3'];
        spectator.component.filterClasses({ query: 'class1' });

        expect(spectator.component.filteredSuggestions).toEqual(['class1']);
    });

    it('should save selected classes and close the dialog', () => {
        const dialogRef = spectator.inject(DynamicDialogRef);
        spectator.component.selectedClasses = ['class1'];
        spectator.component.save();

        expect(dialogRef.close).toHaveBeenCalledWith(['class1']);
    });

    // More tests can be added as needed...
});
