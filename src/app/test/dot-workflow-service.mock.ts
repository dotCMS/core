import { Observable } from 'rxjs/Observable';
import { DotWorkflowAction } from '../shared/models/dot-workflow-action/dot-workflow-action.model';
import { DotWorkflow } from '../shared/models/dot-workflow/dot-workflow.model';
import * as _ from 'lodash';

export const mockWorkflowsActions = [
    {
        assignable: true,
        commentable: true,
        condition: '',
        icon: 'workflowIcon',
        id: '44d4d4cd-c812-49db-adb1-1030be73e69a',
        name: 'Assign Workflow',
        nextAssign: 'db0d2bca-5da5-4c18-b5d7-87f02ba58eb6',
        nextStep: '43e16aac-5799-46d0-945c-83753af39426',
        nextStepCurrentStep: false,
        order: 0,
        owner: null,
        requiresCheckout: true,
        roleHierarchyForAssign: true,
        schemeId: '85c1515c-c4f3-463c-bac2-860b8fcacc34',
        showOn: ['UNLOCKED', 'LOCKED'],
        stepId: null
    },
    {
        assignable: false,
        commentable: false,
        condition: '',
        icon: 'workflowIcon',
        id: 'ceca71a0-deee-4999-bd47-b01baa1bcfc8',
        name: 'Save',
        nextAssign: '654b0931-1027-41f7-ad4d-173115ed8ec1',
        nextStep: 'ee24a4cb-2d15-4c98-b1bd-6327126451f3',
        nextStepCurrentStep: false,
        order: 0,
        owner: null,
        requiresCheckout: true,
        roleHierarchyForAssign: false,
        schemeId: 'd61a59e1-a49c-46f2-a929-db2b4bfa88b2',
        showOn: ['LOCKED'],
        stepId: null
    },
    {
        assignable: false,
        commentable: false,
        condition: '',
        icon: 'workflowIcon',
        id: 'b9d89c80-3d88-4311-8365-187323c96436',
        name: 'Save / Publish',
        nextAssign: '654b0931-1027-41f7-ad4d-173115ed8ec1',
        nextStep: 'dc3c9cd0-8467-404b-bf95-cb7df3fbc293',
        nextStepCurrentStep: false,
        order: 0,
        owner: null,
        requiresCheckout: false,
        roleHierarchyForAssign: false,
        schemeId: 'd61a59e1-a49c-46f2-a929-db2b4bfa88b2',
        showOn: ['LOCKED'],
        stepId: null
    }
];

export const mockWorkflows: DotWorkflow[] = [
    {
        id: '85c1515c-c4f3-463c-bac2-860b8fcacc34',
        creationDate: new Date(1522938093320),
        name: 'Default Scheme',
        description: 'This is the default workflow scheme that will be applied to all content',
        archived: false,
        mandatory: false,
        defaultScheme: true,
        modDate: new Date(1522881184568),
        entryActionId: null,
        system: false
    },
    {
        id: '77a9bf3f-a402-4c56-9b1f-1050b9d345dc',
        creationDate: new Date(1522938093321),
        name: 'Document Management',
        description: 'Default workflow for documents',
        archived: true,
        mandatory: false,
        defaultScheme: false,
        modDate: new Date(1522794958967),
        entryActionId: null,
        system: false
    },
    {
        id: 'd61a59e1-a49c-46f2-a929-db2b4bfa88b2',
        creationDate: new Date(1522938093321),
        name: 'System Workflow',
        description: '',
        archived: false,
        mandatory: false,
        defaultScheme: false,
        modDate: new Date(1522794958958),
        entryActionId: null,
        system: true
    }
];

export class DotWorkflowServiceMock {
    get(): Observable<DotWorkflow[]> {
        return Observable.of(_.cloneDeep(mockWorkflows));
    }

    getDefault(): Observable<DotWorkflow> {
        return Observable.of(_.cloneDeep(mockWorkflows[0]));
    }

    getContentWorkflowActions(_inode: string): Observable<DotWorkflowAction[]> {
        return Observable.of(_.cloneDeep(mockWorkflowsActions));
    }

    fireWorkflowAction(): Observable<any[]> {
        return Observable.of([]);
    }
}
