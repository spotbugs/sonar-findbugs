import nock from 'nock'
import {update} from '../src/update'
import releases from './fixtures/sonarqube-releases.json'

const token = process.env.GITHUB_TOKEN || ''
const integrationTest = token ? test : test.skip

afterEach(() => {
  nock.cleanAll()
})

integrationTest(
  'update() replaces the LATEST in the previous version',
  async () => {
    const prev = new Map<string, string>()
    prev.set('publicVersions', '1.0.0')
    prev.set('1.0.0.sqVersions', '[7.9,LATEST]')
    const scope = nock('https://api.github.com')
      .get('/repos/SonarSource/sonarqube/releases')
      .reply(200, releases)

    const updated = await update(
      token,
      prev,
      'description',
      '1.0.1',
      '[7.9,LATEST]',
      'http://example.com/changelog',
      'http://example.com/download/1.0.1.jar'
    )
    scope.done()
    expect(updated.get('1.0.0.sqVersions')).toBe('[7.9,8.5.*]')
  }
)

integrationTest('update() replaces the publicVersions', async () => {
  const prev = new Map<string, string>()
  prev.set('publicVersions', '1.0.0')
  prev.set('1.0.0.sqVersions', '[7.9,LATEST]')

  const updated = await update(
    token,
    prev,
    'description',
    '1.0.1',
    '[7.9,LATEST]',
    'http://example.com/changelog',
    'http://example.com/download/1.0.1.jar'
  )
  expect(updated.get('publicVersions')).toBe('1.0.1')
  expect(updated.get('archivedVersions')).toBe('1.0.0')
})

integrationTest('update() appends the archivedVersions', async () => {
  const prev = new Map<string, string>()
  prev.set('publicVersions', '1.0.1')
  prev.set('archivedVersions', '1.0.0')
  prev.set('1.0.0.sqVersions', '[7.9,LATEST]')

  const updated = await update(
    token,
    prev,
    'description',
    '1.1.0',
    '[7.9,LATEST]',
    'http://example.com/changelog',
    'http://example.com/download/1.0.1.jar'
  )
  expect(updated.get('publicVersions')).toBe('1.1.0')
  expect(updated.get('archivedVersions')).toBe('1.0.0,1.0.1')
})
