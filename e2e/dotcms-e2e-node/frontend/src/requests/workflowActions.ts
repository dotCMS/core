import { APIRequestContext, expect } from "@playwright/test";
import { admin1 } from "../tests/login/credentialsData";
import { generateBase64Credentials } from "@utils/generateBase64Credential";

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
