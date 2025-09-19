import { APIRequestContext, expect } from "@playwright/test";
import { generateBase64Credentials } from "@utils/generateBase64Credential";



import { Template } from "@models/template.model";

import { admin1 } from "../tests/auth/credentialsData";

type CreateTemplate = Omit<Template, "identifier">;

export async function createTemplate(
  request: APIRequestContext,
  data: CreateTemplate,
) {
  const endpoint = `/api/v1/templates`;
  const response = await request.post(endpoint, {
    data,
    headers: {
      Authorization: generateBase64Credentials(
        admin1.username,
        admin1.password,
      ),
    },
  });

  expect(response.status()).toBe(200);

  const responseData = await response.json();
  return responseData.entity as Template;
}

export async function getTemplate(
  request: APIRequestContext,
  identifier: string,
) {
  const endpoint = `/api/v1/templates/${identifier}`;
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
  return responseData.entity as Template;
}

export async function deleteTemplate(
  request: APIRequestContext,
  identifiers: [string],
) {
  const endpoint = `/api/v1/templates`;
  const response = await request.delete(endpoint, {
    data: identifiers,
    headers: {
      Authorization: generateBase64Credentials(
        admin1.username,
        admin1.password,
      ),
    },
  });
  expect(response.status()).toBe(200);
}

export async function archiveTemplate(
  request: APIRequestContext,
  identifiers: [string],
) {
  const endpoint = `/api/v1/templates/_archive`;
  const response = await request.put(endpoint, {
    data: identifiers,
    headers: {
      Authorization: generateBase64Credentials(
        admin1.username,
        admin1.password,
      ),
    },
  });
  expect(response.status()).toBe(200);
}
