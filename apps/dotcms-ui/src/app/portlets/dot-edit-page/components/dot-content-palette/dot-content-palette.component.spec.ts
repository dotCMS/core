import { ComponentFixture, fakeAsync, TestBed, tick } from '@angular/core/testing';

import { DotContentPaletteComponent } from './dot-content-palette.component';
import { DotPipesModule } from '@pipes/dot-pipes.module';
import { DotIconModule } from '@dotcms/ui';
import { DotCMSContentType } from '@dotcms/dotcms-models';
import { By } from '@angular/platform-browser';
import { MockDotMessageService } from '@tests/dot-message-service.mock';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { Injectable } from '@angular/core';
import { DotContentletEditorService } from '@components/dot-contentlet-editor/services/dot-contentlet-editor.service';
import { DotFilterPipeModule } from '@pipes/dot-filter/dot-filter-pipe.module';
import { FormsModule } from '@angular/forms';

const data = [
    {
        icon: 'cloud',
        id: 'a1661fbc-9e84-4c00-bd62-76d633170da3',
        name: 'Product'
    },
    {
        icon: 'alt_route',
        id: '799f176a-d32e-4844-a07c-1b5fcd107578',
        name: 'Blog'
    },
    {
        icon: 'cloud',
        id: '897cf4a9-171a-4204-accb-c1b498c813fe',
        name: 'Contact'
    },
    {
        icon: 'person',
        id: '6044a806-f462-4977-a353-57539eac2a2c',
        name: 'Long name Blog Comment'
    }
];

@Injectable()
class MockDotContentletEditorService {
    setDraggedContentType = jasmine.createSpy('setDraggedContentType');
}

describe('DotContentPaletteComponent', () => {
    let component: DotContentPaletteComponent;
    let fixture: ComponentFixture<DotContentPaletteComponent>;
    let dotContentletEditorService: DotContentletEditorService;

    const messageServiceMock = new MockDotMessageService({
        structure: 'Content Type'
    });

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [DotContentPaletteComponent],
            imports: [DotPipesModule, DotIconModule, DotFilterPipeModule, FormsModule],
            providers: [
                { provide: DotMessageService, useValue: messageServiceMock },
                { provide: DotContentletEditorService, useClass: MockDotContentletEditorService }
            ]
        }).compileComponents();
        dotContentletEditorService = TestBed.inject(DotContentletEditorService);
    });

    beforeEach(() => {
        fixture = TestBed.createComponent(DotContentPaletteComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    it('should list items correctly', () => {
        component.items = (data as unknown) as DotCMSContentType[];
        fixture.detectChanges();
        const contents = fixture.debugElement.queryAll(By.css('[data-testId="paletteItem"]'));
        expect(contents.length).toEqual(4);
        expect(contents[0].nativeElement.draggable).toEqual(true);
    });

    it('should show empty state', () => {
        component.items = [];
        fixture.detectChanges();
        const emptyState = fixture.debugElement.query(By.css('.dot-content-palette__empty'));

        expect(emptyState).not.toBeNull();
    });

    it('should show correct search Box', () => {
        const icon = fixture.debugElement.query(By.css('[data-testId="searchIcon"]'));
        const input = fixture.debugElement.query(By.css('[data-testId="searchInput"]'));
        expect(icon.componentInstance.name).toEqual('search');
        expect(icon.componentInstance.size).toEqual('18');
        expect(input.nativeElement.placeholder).toEqual('CONTENT TYPE');
    });

    it('should filter items on search', () => {
        component.items = (data as unknown) as DotCMSContentType[];
        const input = fixture.debugElement.query(By.css('[data-testId="searchInput"]'))
            .nativeElement;
        input.value = 'Product';
        input.dispatchEvent(new Event('input'));
        fixture.detectChanges();
        const contents = fixture.debugElement.queryAll(By.css('[data-testId="paletteItem"]'));
        expect(contents.length).toEqual(1);
    });

    it('should set Dragged ContentType on dragStart', () => {
        component.items = (data as unknown) as DotCMSContentType[];
        fixture.detectChanges();
        const content = fixture.debugElement.query(By.css('[data-testId="paletteItem"]'));
        content.triggerEventHandler('dragstart', data[0]);
        expect(dotContentletEditorService.setDraggedContentType).toHaveBeenCalledOnceWith(
            data[0] as DotCMSContentType
        );
    });
});
