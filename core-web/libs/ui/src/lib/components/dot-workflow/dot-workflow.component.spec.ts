import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
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
            imports: [DotWorkflowComponent, HttpClientTestingModule],
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
        expect(component.contentTypes).toEqual(mockContentTypes);
        expect(component.loading).toBe(false);
    });

    it('should emit valueChange and onChange when content type changes', () => {
        spyOn(component.valueChange, 'emit');
        spyOn(component.onChange, 'emit');

        const selectedContentType = mockContentTypes[0];
        component.onContentTypeChange(selectedContentType);

        expect(component.valueChange.emit).toHaveBeenCalledWith(selectedContentType);
        expect(component.onChange.emit).toHaveBeenCalledWith(selectedContentType);
    });
});
