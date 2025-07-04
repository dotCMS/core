import { APIRequestContext, expect } from "@playwright/test";
import { admin1 } from "../tests/login/credentialsData";
import { generateBase64Credentials } from "@utils/generateBase64Credential";

export async function updateFeatureFlag(
  request: APIRequestContext,
  data: Record<string, unknown>,
) {
  const endpoint = `/api/v1/system-table/`;
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
}
