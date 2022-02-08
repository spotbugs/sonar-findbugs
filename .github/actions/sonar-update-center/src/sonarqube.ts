import {getOctokit} from '@actions/github'
import gt from 'semver/functions/gt'

export async function searchLatestMinorVersion(token: string): Promise<string> {
  const octokit = getOctokit(token)
  let latest = ''
  for await (const response of octokit.paginate.iterator(
    octokit.rest.repos.listReleases,
    {
      owner: 'SonarSource',
      repo: 'sonarqube'
    }
  )) {
    for (const release of response.data) {
      const tag = dropAdditionalVer(release.tag_name)
      if (!latest || gt(tag, latest, false)) {
        latest = tag
      }
    }
  }

  return replacePatch(latest)
}

export function dropAdditionalVer(version: string): string {
  const split = version.split('.')
  split.length = 3
  return split.join('.')
}

export function replacePatch(version: string): string {
  const split = version.split('.')
  split[split.length - 1] = '*'
  return split.join('.')
}
