import * as core from '@actions/core'
import fetch, {Response} from 'node-fetch'

interface Package {
  id: number
  name: string
  url: string
  package_html_url: string
  created_at: string
  updated_at: string
  html_url: string
  metadata: Metadata
}

interface Metadata {
  package_type: string
  container: Container
}

interface Container {
  tags: string[]
}

const packageType = core.getInput('package_type')
const packageName = core.getInput('package_name')
const deleteTags = core
  .getInput('delete_tags')
  .split('\n')
  .map(t => t.trim())
  .filter(t => !!t)
const accessToken = core.getInput('access_token')
const headers: HeadersInit = {
  Accept: 'application/vnd.github+json',
  Authorization: `Bearer ${accessToken}`,
  'X-GitHub-Api-Version': '2022-11-28'
}

export const deletePackages = async () => {
  const packagesResponse = await fetchPackages()
  const foundPackages = (await packagesResponse.json()) as Package[]

  if (!foundPackages || foundPackages.length === 0) {
    core.error(`Fetched packages were not found or they are empty, aborting`)
    return
  }

  for (const tag of deleteTags) {
    const finalVersion = tag.includes(':') ? tag.split(':')[1] : tag
    const deletePackages = finalVersion
      ? [foundPackages.find(p => p.metadata.container.tags.includes(finalVersion))]
      : foundPackages
    core.info(`Found packages to delete:\n${JSON.stringify(deletePackages, null, 2)}`)

    const versionsToDelete: number[] = deletePackages.filter(p => !!p).flatMap(p => p!.id)
    if (!versionsToDelete || versionsToDelete.length === 0) {
      core.error(`Rsolved package versions are empty, aborting`)
      continue
    }

    await _deletePackages(versionsToDelete)
  }
}

const fetchPackages = async (): Promise<Response> => {
  const url = `https://api.github.com/orgs/dotcms/packages/${packageType}/${packageName}/versions`
  core.info(`Sending GET to ${url}`)
  const response: Response = await fetch(url, {
    method: 'GET',
    headers
  })

  return response
}

const _deletePackages = async (versions: number[]) => {
  core.info(`Deleting ${packageType} package named ${packageName} versions: [${versions.join(', ')}]`)
  versions.forEach(async v => await deletePackage(v))
}

const deletePackage = async (version: number) => {
  core.info(`Deleting ${packageType} package named ${packageName} version: ${version}`)
  const url = `https://api.github.com/orgs/dotcms/packages/${packageType}/${packageName}/versions/${version}`
  const response: Response = await fetch(url, {
    method: 'DELETE',
    headers
  })

  core.info(
    `Got response status:\n${JSON.stringify(response.status, null, 2)}\nbody: (${JSON.stringify(
      response.body,
      null,
      2
    )})`
  )
}
