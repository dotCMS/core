import { APIRequestContext, expect } from "@playwright/test";
import { admin1 } from "../tests/login/credentialsData";
import { generateBase64Credentials } from "@utils/generateBase64Credential";
import { getSchemas } from "./schemas";
import { getSites } from "./sites";

export interface ContentType {
  id: string;
  defaultType: boolean;
  icon: string | null;
  fixed: boolean;
  system: boolean;
  clazz: string;
  description: string;
  host: string;
  folder: string;
  name: string;
  systemActionMappings: Record<string, string>;
  metadata: Record<string, unknown>;
  workflow: string[];
}

type CreateContentType = Omit<ContentType, "id">;

export async function createFakeContentType(request: APIRequestContext, data: Partial<ContentType>) {

  const schemas = await getSchemas(request);
  const systemWorkflow = schemas.find((schema) => schema.variableName === "SystemWorkflow");

  const sites = await getSites(request);
  const defaultSite = sites.find((site) => site.host === "SYSTEM_HOST");

  const defaultContentType: CreateContentType = {
    defaultType: false,
    icon: null,
    fixed: false,
    system: false,
    clazz: "com.dotcms.contenttype.model.type.ImmutableSimpleContentType",
    description: "",
    host: defaultSite.identifier,
    folder: "SYSTEM_FOLDER",
    name: "New content type",
    systemActionMappings: {},
    metadata: {},
    workflow: [systemWorkflow.id],
  };

  return createContentType(request, {
    ...defaultContentType,
    ...data,
  });
}


export async function createContentType(request: APIRequestContext, data: CreateContentType) {
  const endpoint = `/api/v1/contenttype`;
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
  const results = responseData.entity as ContentType[];
  expect(results.length).toBe(1);
  return results[0];
}


export async function deleteContentType(request: APIRequestContext, id: string) {
  const endpoint = `/api/v1/contenttype/id/${id}`;
  const response = await request.delete(endpoint, {
    headers: {
      Authorization: generateBase64Credentials(admin1.username, admin1.password),
    },
  });
  expect(response.status()).toBe(200);
}
