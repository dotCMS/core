import { APIRequestContext, expect } from "@playwright/test";

export async function updateFeatureFlag(
  request: APIRequestContext,
  data: Record<string, unknown>,
) {
  const endpoint = `/api/v1/system-table/`;
  const contentTypeResponse = await request.post(endpoint, { data });

  expect(contentTypeResponse.status()).toBe(200);
}
