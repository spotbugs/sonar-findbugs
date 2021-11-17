import {checkoutSourceRepo, fork} from '../src/github'

const token = process.env.GITHUB_TOKEN || ''
const integrationTest = token ? test : test.skip

integrationTest(
  'fork() does nothing if the forked repo already exists',
  async () => {
    const resp = await fork(token)
  }
)

integrationTest(
  'checkoutSourceRepo() can perform Git commands without error',
  async () => {
    const {owner} = await fork(token)
    await checkoutSourceRepo(token, owner)
  },
  5 * 60 * 1000
)
