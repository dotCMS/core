import { APIRequestContext, expect } from '@playwright/test';
import { admin1 } from '@utils/credentials';
import { generateBase64Credentials } from '@utils/generateBase64Credential';

export interface WorkflowActionCreated {
    id: string;
    name: string;
    schemeId: string;
    assignable: boolean;
    commentable: boolean;
    actionInputs: Array<{ id: string; body: Record<string, unknown> }>;
}

function authHeaders() {
    return { Authorization: generateBase64Credentials(admin1.username, admin1.password) };
}

/**
 * Returns the workflow step ID for a contentlet given its inode.
 * Uses GET /api/v1/workflow/status/{inode}.
 */
export async function getWorkflowStepId(
    request: APIRequestContext,
    inode: string
): Promise<string> {
    const response = await request.get(`/api/v1/workflow/status/${inode}`, {
        headers: authHeaders()
    });
    expect(response.status()).toBe(200);
    const data = await response.json();

    return data.entity.step.id as string;
}

/**
 * Creates a workflow action in the given scheme/step.
 * POST /api/v1/workflow/actions
 */
export async function createWorkflowAction(
    request: APIRequestContext,
    {
        schemeId,
        stepId,
        name,
        assignable = false,
        commentable = false
    }: {
        schemeId: string;
        stepId: string;
        name: string;
        assignable?: boolean;
        commentable?: boolean;
    }
): Promise<WorkflowActionCreated> {
    const response = await request.post('/api/v1/workflow/actions', {
        data: {
            schemeId,
            stepId,
            actionName: name,
            actionAssignable: assignable,
            actionCommentable: commentable,
            actionRoleHierarchyForAssign: false,
            actionNextStep: 'currentstep',
            // "Any User" role — required by the API; empty string causes a 500
            actionNextAssign: '654b0931-1027-41f7-ad4d-173115ed8ec1',
            whoCanUse: [],
            showOn: ['NEW', 'EDITING', 'PUBLISHED', 'UNPUBLISHED', 'LOCKED', 'UNLOCKED'],
            actionCondition: '',
            actionIcon: 'workflowIcon'
        },
        headers: authHeaders()
    });

    if (response.status() !== 200) {
        const body = await response.json().catch(() => response.statusText());
        console.error('createWorkflowAction failed:', JSON.stringify(body, null, 2));
    }
    expect(response.status()).toBe(200);

    const data = await response.json();

    return data.entity as WorkflowActionCreated;
}

/**
 * Deletes a workflow action by ID.
 * DELETE /api/v1/workflow/actions/{actionId}
 */
export async function deleteWorkflowAction(
    request: APIRequestContext,
    actionId: string
): Promise<void> {
    const response = await request.delete(`/api/v1/workflow/actions/${actionId}`, {
        headers: authHeaders()
    });
    expect([200, 404]).toContain(response.status());
}
