import { APIRequestContext, expect } from "@playwright/test";
import { generateBase64Credentials } from "@utils/generateBase64Credential";

import { admin1 } from "../tests/auth/credentialsData";

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
