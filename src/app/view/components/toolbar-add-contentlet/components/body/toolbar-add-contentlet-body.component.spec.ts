import { DebugElement, Component } from '@angular/core';
import { async, ComponentFixture } from '@angular/core/testing';

import { DOTTestBed } from '../../../../../test/dot-test-bed';
import { ToolbarAddContenletBodyComponent } from './toolbar-add-contentlet-body.component';
import { ActivatedRouteSnapshot } from '@angular/router';
import { StructureType } from '../../../../../shared/models/contentlet';
import { RouterTestingModule } from '@angular/router/testing';
import { By } from '@angular/platform-browser';
import { DotRouterService } from '../../../../../api/services/dot-router/dot-router.service';

@Component({
    selector: 'dot-mock',
    template: ''
})
class MockComponent {
}

describe('ToolbarAddContenletBodyComponent', () => {
    let component: ToolbarAddContenletBodyComponent;
    let fixture: ComponentFixture<ToolbarAddContenletBodyComponent>;
    let de: DebugElement;
    let dotRouterService: DotRouterService;


    beforeEach(
        async(() => {
            DOTTestBed.configureTestingModule({
                declarations: [ToolbarAddContenletBodyComponent, MockComponent],
                providers: [DotRouterService],
                imports: [RouterTestingModule.withRoutes([
                    {
                        path: 'add',
                        children: [
                            {
                                component: MockComponent,
                                path: ':id'
                            }
                        ]
                    }
                ])]
            });

            fixture = DOTTestBed.createComponent(ToolbarAddContenletBodyComponent);
            component = fixture.componentInstance;
            de = fixture.debugElement;
            dotRouterService = fixture.debugElement.injector.get(DotRouterService);
        })
    );

    it('should reload current add contentlet page', () => {
        component.structureTypeViews = [{
            name: 'Hello',
            label: 'World',
            types: [
                {
                    type: StructureType.CONTENT,
                    name: 'Hola',
                    inode: '123',
                    action: '',
                    variable: 'hola'
                },
                {
                    type: StructureType.CONTENT,
                    name: 'Mundo',
                    inode: '456',
                    action: '',
                    variable: 'mundo'
                },
                {
                    type: StructureType.CONTENT,
                    name: 'Test',
                    inode: '789',
                    action: '',
                    variable: 'test'
                }
            ]
        }];
        spyOn(component.select, 'emit').and.callThrough();
        spyOn(dotRouterService, 'reloadCurrentPortlet').and.callThrough();
        spyOnProperty(dotRouterService, 'currentPortlet', 'get').and.returnValue({
            url: '/add/hola',
            id: 'hola'
        });
        fixture.detectChanges();

        const link: DebugElement = de.query(By.css('.toolbar-add-contentlet-body__list-item a'));

        link.nativeElement.click();

        expect(dotRouterService.reloadCurrentPortlet).toHaveBeenCalledTimes(1);
        expect(component.select.emit).toHaveBeenCalledTimes(1);
    });
});
