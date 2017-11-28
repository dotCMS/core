import { DotContainer } from '../../../shared/models/container/dot-container.model';
import { By } from '@angular/platform-browser';
import { PaginatorService } from './../../../api/services/paginator/paginator.service';
import { IframeOverlayService } from './../_common/iframe/service/iframe-overlay.service';
import { MockMessageService } from './../../../test/message-service.mock';
import { MessageService } from './../../../api/services/messages-service';
import { SearchableDropDownModule } from './../_common/searchable-dropdown/searchable-dropdown.module';
import { DOTTestBed } from './../../../test/dot-test-bed';
import { Observable } from 'rxjs/Observable';
import { async, ComponentFixture, fakeAsync, tick } from '@angular/core/testing';
import { DebugElement } from '@angular/core';
import { DotContainerSelectorComponent } from './dot-container-selector.component';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

describe('ContainerSelectorComponent', () => {
    let comp: DotContainerSelectorComponent;
    let fixture: ComponentFixture<DotContainerSelectorComponent>;
    let de: DebugElement;
    let el: HTMLElement;
    let searchableDropdownComponent;
    let containers: DotContainer[];

    beforeEach(async(() => {
        const messageServiceMock = new MockMessageService({
            addcontainer: 'Add a Container'
        });

        DOTTestBed.configureTestingModule({
            declarations: [DotContainerSelectorComponent],
            imports: [ SearchableDropDownModule, BrowserAnimationsModule],
            providers: [
                { provide: MessageService, useValue: messageServiceMock },
                IframeOverlayService,
                PaginatorService
            ]
        });

        fixture = DOTTestBed.createComponent(DotContainerSelectorComponent);
        comp = fixture.componentInstance;
        de = fixture.debugElement;
        el = de.nativeElement;

        searchableDropdownComponent = de.query(By.css('searchable-dropdown')).componentInstance;

        containers = [
            {
                categoryId: '427c47a4-c380-439f-a6d0-97d81deed57e',
                deleted: false,
                friendlyName: 'Friendly Container name',
                identifier: '427c47a4-c380-439f',
                name: 'Container 1',
                type: 'Container'
            },
            {
                categoryId: '40204d-c380-439f-a6d0-97d8sdeed57e',
                deleted: false,
                friendlyName: 'Friendly Container2 name',
                identifier: '427c47a4-c380-439f',
                name: 'Container 2',
                type: 'Container'
            }
        ];
    }));

    it('should change Page', fakeAsync(() => {
        const filter = 'filter';
        const page = 1;

        fixture.detectChanges();

        const paginatorService: PaginatorService = de.injector.get(PaginatorService);
        paginatorService.totalRecords = 2;
        spyOn(paginatorService, 'getWithOffset').and.returnValue(Observable.of([]));

        fixture.detectChanges();

        searchableDropdownComponent.pageChange.emit({
            filter: filter,
            first: 10,
            page: page,
            pageCount: 10,
            rows: 0
        });

        tick();
        expect(paginatorService.getWithOffset).toHaveBeenCalledWith(10);
    }));

    it('should paginate when the filter change', fakeAsync(() => {
        const filter = 'filter';

        fixture.detectChanges();

        const paginatorService: PaginatorService = de.injector.get(PaginatorService);
        paginatorService.totalRecords = 2;
        spyOn(paginatorService, 'getWithOffset').and.returnValue(Observable.of([]));

        fixture.detectChanges();

        searchableDropdownComponent.filterChange.emit(filter);

        tick();
        expect(paginatorService.getWithOffset).toHaveBeenCalledWith(0);
        expect(paginatorService.filter).toEqual(filter);
    }));

    it('should emit an event of close when click on the X icon top-right', () => {
        const removeBlockElem: DebugElement = de.query(By.css('.container-selector__header-remove'));
        let removeAction;

        comp.remove.subscribe(res => {
            removeAction = res;
        });

        removeBlockElem.nativeElement.click();

        expect(removeAction).toBeDefined();
    });

    it('should add containers to containers list and emit a change event', () => {
        comp.currentContainers = containers;

        searchableDropdownComponent.change.emit(containers[0]);

        expect(comp.selectedContainersList).toContain(containers[0]);
        expect(comp.selectedContainersList.length).toEqual(1);
    });

    it('should remove containers after click on trash icon', () => {
        const bodySelectorList = de.query(By.css('.container-selector__list'));
        const bodySelectorListItems = bodySelectorList.nativeElement.children;

        comp.currentContainers = containers;

        searchableDropdownComponent.change.emit(containers[0]);

        fixture.detectChanges();

        bodySelectorListItems[0].children[0].click();

        expect(comp.selectedContainersList).not.toContain(containers[0]);
        expect(comp.selectedContainersList.length).toEqual(0);
    });

   it('should not add duplicated containers to the list', () => {
        comp.currentContainers = containers;

        searchableDropdownComponent.change.emit(containers[0]);
        fixture.detectChanges();

        expect(comp.selectedContainersList.length).toEqual(1);

        searchableDropdownComponent.change.emit(containers[0]);
        fixture.detectChanges();

        expect(comp.selectedContainersList.length).toEqual(1);
    });
});
