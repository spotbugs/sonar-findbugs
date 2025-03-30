import * as core from '@actions/core'
import {searchLatestMinorVersion} from './sonarqube'

export async function update(
  token: string,
  prop: Map<string, string>,
  description: string,
  publicVersion: string,
  sqVersions: string,
  changelogUrl: string,
  downloadUrl: string
): Promise<Map<string, string>> {
  const prevPublicVersions = prop.get('publicVersions')
  if (!prevPublicVersions) {
    throw new Error('publicVersions should exist in the properties file')
  } else if (prevPublicVersions.includes(',')) {
    throw new Error('publicVersions should contain single version')
  }
  const copiedProp = new Map(prop)
  copiedProp.set('publicVersions', publicVersion)

  const prevArchivedVersions = copiedProp.get('archivedVersions')
  if (prevArchivedVersions) {
    copiedProp.set(
      'archivedVersions',
      `${prevArchivedVersions},${prevPublicVersions}`
    )
  } else {
    copiedProp.set('archivedVersions', prevPublicVersions)
  }

  copiedProp.set(`${publicVersion}.description`, description)
  copiedProp.set(`${publicVersion}.sqVersions`, sqVersions)
  copiedProp.set(
    `${publicVersion}.date`,
    new Date().toISOString().split('T')[0]
  )
  copiedProp.set(`${publicVersion}.changelogUrl`, changelogUrl)
  copiedProp.set(`${publicVersion}.downloadUrl`, downloadUrl)

  const prevSqVersions = copiedProp.get(`${prevPublicVersions}.sqVersions`)
  if (prevSqVersions?.endsWith(',LATEST]')) {
    const latestMinorVersion = await searchLatestMinorVersion(token)
    const updatedPrevSqVersions = prevSqVersions.replace(
      ',LATEST]',
      `,${latestMinorVersion}]`
    )
    core.debug(
      `Updating ${prevPublicVersions}.sqVersions from ${prevSqVersions} to ${updatedPrevSqVersions}...`
    )
    copiedProp.set(`${prevPublicVersions}.sqVersions`, updatedPrevSqVersions)
  }
  return copiedProp
}
