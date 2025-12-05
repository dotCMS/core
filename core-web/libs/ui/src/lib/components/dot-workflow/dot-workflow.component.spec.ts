import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { of } from 'rxjs';
import { DotWorkflowComponent } from './dot-workflow.component';
import { DotContentTypeService } from '@dotcms/data-access';
import { DotCMSContentType } from '@dotcms/dotcms-models';

const mockContentTypes: DotCMSContentType[] = [
    {
        id: '1',
        name: 'Blog',
        variable: 'Blog'
    } as DotCMSContentType,
    {
        id: '2',
        name: 'News',
        variable: 'News'
    } as DotCMSContentType
];

describe('DotWorkflowComponent', () => {
    let component: DotWorkflowComponent;
    let fixture: ComponentFixture<DotWorkflowComponent>;
    let contentTypeService: jasmine.SpyObj<DotContentTypeService>;

    beforeEach(async () => {
        const contentTypeServiceSpy = jasmine.createSpyObj('DotContentTypeService', ['getContentTypes']);

        await TestBed.configureTestingModule({
            imports: [DotWorkflowComponent, HttpClientTestingModule, ReactiveFormsModule],
            providers: [
                {
                    provide: DotContentTypeService,
                    useValue: contentTypeServiceSpy
                }
            ]
        }).compileComponents();

        contentTypeService = TestBed.inject(DotContentTypeService) as jasmine.SpyObj<DotContentTypeService>;
        contentTypeService.getContentTypes.and.returnValue(of(mockContentTypes));

        fixture = TestBed.createComponent(DotWorkflowComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    it('should load content types on init', () => {
        expect(contentTypeService.getContentTypes).toHaveBeenCalledWith({ page: 100 });
        expect(component.contentTypes()).toEqual(mockContentTypes);
        expect(component.loading()).toBe(false);
    });

    it('should emit onChange when content type changes', () => {
        spyOn(component.onChange, 'emit');

        const selectedContentType = mockContentTypes[0];
        component.onContentTypeChange(selectedContentType);

        expect(component.value()).toEqual(selectedContentType);
        expect(component.onChange.emit).toHaveBeenCalledWith(selectedContentType);
    });

    describe('ControlValueAccessor Implementation', () => {
        it('should write value to component', () => {
            const testValue = mockContentTypes[0];
            component.writeValue(testValue);

            expect(component.value()).toEqual(testValue);
        });

        it('should handle null value in writeValue', () => {
            component.writeValue(null);

            expect(component.value()).toBeNull();
        });

        it('should register onChange callback', () => {
            const onChangeSpy = jasmine.createSpy('onChange');
            component.registerOnChange(onChangeSpy);

            const testValue = mockContentTypes[0];
            component.value.set(testValue);
            fixture.detectChanges();

            expect(onChangeSpy).toHaveBeenCalledWith(testValue);
        });

        it('should register onTouched callback', () => {
            const onTouchedSpy = jasmine.createSpy('onTouched');
            component.registerOnTouched(onTouchedSpy);

            const testValue = mockContentTypes[0];
            component.onContentTypeChange(testValue);

            expect(onTouchedSpy).toHaveBeenCalled();
        });

        it('should set disabled state', () => {
            component.setDisabledState(true);

            expect(component.$isDisabled()).toBe(true);
        });

        it('should work with FormControl', () => {
            const formControl = new FormControl<DotCMSContentType | null>(null);
            const onChangeSpy = jasmine.createSpy('onChange');
            const onTouchedSpy = jasmine.createSpy('onTouched');

            component.registerOnChange(onChangeSpy);
            component.registerOnTouched(onTouchedSpy);

            // Simulate form control setting a value
            const testValue = mockContentTypes[0];
            component.writeValue(testValue);
            fixture.detectChanges();

            expect(component.value()).toEqual(testValue);

            // Simulate user changing value
            component.onContentTypeChange(mockContentTypes[1]);
            fixture.detectChanges();

            expect(onChangeSpy).toHaveBeenCalledWith(mockContentTypes[1]);
            expect(onTouchedSpy).toHaveBeenCalled();
        });
    });
});
