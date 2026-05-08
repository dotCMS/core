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
 * Creates a workflow action in the given scheme/step with commentable and
 * assignable actionlets enabled (mirrors the unit-test mock configuration).
 *
 * POST /api/v1/workflow/actions
 */
export async function createCommentableWorkflowAction(
    request: APIRequestContext,
    { schemeId, stepId, name }: { schemeId: string; stepId: string; name: string }
): Promise<WorkflowActionCreated> {
    const response = await request.post('/api/v1/workflow/actions', {
        data: {
            schemeId,
            stepId,
            actionName: name,
            actionAssignable: true,
            actionCommentable: true,
            actionRoleHierarchyForAssign: false,
            actionNextStep: 'currentstep',
            actionNextAssign: '',
            showOn: ['NEW', 'EDITING', 'PUBLISHED', 'UNPUBLISHED', 'LOCKED', 'UNLOCKED'],
            actionCondition: '',
            actionIcon: 'workflowIcon'
        },
        headers: authHeaders()
    });

    if (response.status() !== 200) {
        const body = await response.json().catch(() => response.statusText());
        console.error('createCommentableWorkflowAction failed:', JSON.stringify(body, null, 2));
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
