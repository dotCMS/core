import { APIRequestContext, expect } from "@playwright/test";
import { generateBase64Credentials } from "@utils/generateBase64Credential";

import { admin1 } from "../tests/login/credentialsData";

export interface Page {
  identifier: string;
  contentType: "htmlpageasset";
  title: string;
  url: string;
  hostFolder: "default";
  template: "SYSTEM_TEMPLATE";
  friendlyName: string;
  cachettl: number;
  inode: string;
}

type CreatePage = Omit<Page, "identifier" | "inode">;

export async function createPage(request: APIRequestContext, data: CreatePage) {
  const endpoint = `/api/v1/workflow/actions/default/fire/PUBLISH?indexPolicy=WAIT_FOR`;
  const response = await request.post(endpoint, {
    data: {
      contentlet: data,
    },
    headers: {
      Authorization: generateBase64Credentials(
        admin1.username,
        admin1.password,
      ),
    },
  });

  expect(response.status()).toBe(200);

  const responseData = await response.json();
  const results = responseData.entity.results as Page[];
  expect(results.length).toBe(1);
  const key = Object.keys(results[0])[0];
  return results[0][key];
}

export async function executeAction(
  request: APIRequestContext,
  actionId: string,
  inode: string,
) {
  const endpoint = `/api/v1/workflow/actions/${actionId}/fire?indexPolicy=WAIT_FOR&inode=${inode}`;
  const response = await request.put(endpoint, {
    headers: {
      Authorization: generateBase64Credentials(
        admin1.username,
        admin1.password,
      ),
    },
    data: {},
  });
  expect(response.status()).toBe(200);
}

export interface Action {
  id: string;
  name: string;
}

export async function getActionsByContentlet(
  request: APIRequestContext,
  inode: string,
) {
  const endpoint = `/api/v1/workflow/contentlet/${inode}/actions?renderMode=LISTING`;
  const response = await request.get(endpoint, {
    headers: {
      Authorization: generateBase64Credentials(
        admin1.username,
        admin1.password,
      ),
    },
  });
  expect(response.status()).toBe(200);
  const responseData = await response.json();
  return responseData.entity as Action[];
}

export async function actionsPageWorkflow(
  request: APIRequestContext,
  inode: string,
  steps: string[],
) {
  for (const step of steps) {
    const actions = await getActionsByContentlet(request, inode);
    const action = actions.find((a) => a.name === step);
    if (action) {
      await executeAction(request, action.id, inode);
    } else {
      console.warn(
        `Action not found for workflow step: ${step}. Skipping remaining steps.`,
      );
      break;
    }
  }
}
