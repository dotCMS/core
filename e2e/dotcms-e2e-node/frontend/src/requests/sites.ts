import { APIRequestContext, expect } from "@playwright/test";
import { admin1 } from "../tests/login/credentialsData";
import { generateBase64Credentials } from "@utils/generateBase64Credential";

export interface Site {
  aliases: string | null;
  archived: boolean;
  categoryId: string;
  contentTypeId: string;
  default: boolean;
  dotAsset: boolean;
  fileAsset: boolean;
  folder: string;
  form: boolean;
  host: string;
  hostThumbnail: string | null;
  hostname: string;
  htmlpage: boolean;
  identifier: string;
  indexPolicyDependencies: string;
  inode: string;
  keyValue: boolean;
  languageId: number;
  languageVariable: boolean;
  live: boolean;
  locked: boolean;
  lowIndexPriority: boolean;
  modDate: number;
  modUser: string;
  name: string;
  new: boolean;
  owner: string;
  parent: boolean;
  permissionId: string;
  permissionType: string;
  persona: boolean;
  sortOrder: number;
  structureInode: string;
  systemHost: boolean;
  tagStorage: string;
  title: string;
  titleImage: string | null;
  type: string;
  vanityUrl: boolean;
  variantId: string;
  versionId: string;
  working: boolean;
}

export async function getSites(
  request: APIRequestContext,
) {
  const endpoint = `/api/v1/site?filter=*&per_page=15&system=true`;
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
  return responseData.entity as Site[];
}
