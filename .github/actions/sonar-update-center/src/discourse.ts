import {request} from 'https'

export async function createTopic(
  apiKey: string,
  mavenArtifactId: string,
  publicVersion: string,
  body: string
): Promise<string> {
  const title = `[NEW RELEASE] ${mavenArtifactId} ${publicVersion}`
  const data = JSON.stringify({
    title,
    category: 15, // for plugin development
    raw: `${body}\n<!-- this topic was created by sonar-update-center-action -->`
  })
  // creating a new topic by https://docs.discourse.org/#tag/Topics/paths/~1posts.json/post
  return new Promise((resolve, reject) => {
    const req = request(
      {
        hostname: 'community.sonarsource.com',
        path: '/posts.json',
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Content-Length': data.length,
          'User-Api-Key': apiKey
        }
      },
      res => {
        if (res.statusCode !== 200) {
          reject(
            new Error(
              `Failed to create a topic in the community forum. The status code is ${JSON.stringify(
                res.statusCode
              )} and message is ${res.statusMessage}.`
            )
          )
        }

        res.on('data', payload => {
          const {topic_id, topic_slug} = JSON.parse(payload)
          resolve(
            `https://community.sonarsource.com/t/${topic_slug}/${topic_id}`
          )
        })
      }
    )
    req.write(data)
    req.end()
  })
}
