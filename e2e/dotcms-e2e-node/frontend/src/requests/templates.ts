import { APIRequestContext, expect } from "@playwright/test";
import { admin1 } from "../tests/login/credentialsData";
import { generateBase64Credentials } from "@utils/generateBase64Credential";

interface Template {
  friendlyName: string;
  identifier: string;
  image: string;
  theme: string;
  title: string;
  layout?: Record<string, unknown>;
}

type CreateTemplate = Omit<Template, "identifier">;

export async function createTemplate(request: APIRequestContext, data: CreateTemplate) {
  const endpoint = `/api/v1/templates`;
  const contentTypeResponse = await request.post(endpoint, {
    data,
    headers: {
      Authorization: generateBase64Credentials(
        admin1.username,
        admin1.password,
      ),
    },
  });

  expect(contentTypeResponse.status()).toBe(200);

  const response = await contentTypeResponse.json() as Template;
  return response;
}
