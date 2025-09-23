import { APIRequestContext, expect } from "@playwright/test";
import { admin1 } from "../tests/login/credentialsData";
import { generateBase64Credentials } from "@utils/generateBase64Credential";

export interface Schema {
  archived: boolean;
  creationDate: number;
  defaultScheme: boolean;
  description: string;
  entryActionId: string | null;
  id: string;
  mandatory: boolean;
  modDate: number;
  name: string;
  system: boolean;
  variableName: string;
}

export async function getSchemas(
  request: APIRequestContext,
) {
  const endpoint = `/api/v1/schemas`;
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
  return responseData.entity as Schema[];
}
