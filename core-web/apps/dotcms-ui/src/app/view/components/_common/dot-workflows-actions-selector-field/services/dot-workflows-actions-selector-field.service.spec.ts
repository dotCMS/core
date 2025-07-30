/* eslint-disable @typescript-eslint/no-explicit-any */

import { of, throwError } from 'rxjs';

import { HttpResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';

import { SelectItemGroup } from 'primeng/api';

import { DotHttpErrorManagerService, DotWorkflowsActionsService } from '@dotcms/data-access';
import { HttpCode, ResponseView } from '@dotcms/dotcms-js';
import { mockWorkflows, mockWorkflowsActions } from '@dotcms/utils-testing';

import { DotWorkflowsActionsSelectorFieldService } from './dot-workflows-actions-selector-field.service';

describe('DotWorkflowsActionsSelectorFieldService', () => {
    let dotWorkflowsActionsService: DotWorkflowsActionsService;
    let dotHttpErrorManagerService: DotHttpErrorManagerService;
    let service: DotWorkflowsActionsSelectorFieldService;
    let spy: jasmine.Spy;
    let result: SelectItemGroup[];

    beforeEach(() =>
        TestBed.configureTestingModule({
            providers: [
                DotWorkflowsActionsSelectorFieldService,
                {
                    provide: DotWorkflowsActionsService,
                    useValue: {
                        getByWorkflows() {
                            return of(mockWorkflowsActions);
                        }
                    }
                },
                {
                    provide: DotHttpErrorManagerService,
                    useValue: {
                        handle: jasmine.createSpy().and.returnValue(of({}))
                    }
                }
            ]
        }).compileComponents()
    );

    beforeEach(() => {
        dotHttpErrorManagerService = TestBed.inject(DotHttpErrorManagerService);
        dotWorkflowsActionsService = TestBed.inject(DotWorkflowsActionsService);
        service = TestBed.inject(DotWorkflowsActionsSelectorFieldService);
        spy = spyOn(dotWorkflowsActionsService, 'getByWorkflows').and.callThrough();

        service.get().subscribe((actions: SelectItemGroup[]) => {
            result = actions;
        });
    });

    it('should be get actions grouped by workflows as SelectItemGroup', () => {
        service.load(mockWorkflows);

        expect(dotWorkflowsActionsService.getByWorkflows).toHaveBeenCalledWith(
            jasmine.arrayContaining(mockWorkflows)
        );

        expect(result).toEqual([
            {
                label: 'Default Scheme',
                value: '85c1515c-c4f3-463c-bac2-860b8fcacc34',
                items: [
                    {
                        label: 'Assign Workflow',
                        value: '44d4d4cd-c812-49db-adb1-1030be73e69a'
                    }
                ]
            },
            {
                label: 'Document Management',
                value: '77a9bf3f-a402-4c56-9b1f-1050b9d345dc',
                items: []
            },
            {
                label: 'System Workflow',
                value: 'd61a59e1-a49c-46f2-a929-db2b4bfa88b2',
                items: [
                    { label: 'Save', value: 'ceca71a0-deee-4999-bd47-b01baa1bcfc8' },
                    {
                        label: 'Save / Publish',
                        value: 'b9d89c80-3d88-4311-8365-187323c96436'
                    }
                ]
            }
        ]);
    });

    it('should handle error', () => {
        const mock = new ResponseView(
            new HttpResponse({
                body: null,
                status: HttpCode.BAD_REQUEST,
                headers: null,
                url: ''
            })
        );
        spy.and.returnValue(throwError(mock));
        service.load(mockWorkflows);

        expect<any>(dotHttpErrorManagerService.handle).toHaveBeenCalledWith(mock);
        expect(result).toEqual([]);
    });
});
