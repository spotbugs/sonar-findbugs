# Automate making the sonar-update-center-properties file with GitHub Actions

The GitHub Action for SonarQube plugin authors to automate the last mile in SonarQube plugin release procedure.

This [release procedure](https://community.sonarsource.com/t/deploying-to-the-marketplace/35236) contains three steps:

1. Publishing the `.jar` file with metadata
2. Announcing new release at the [Community Forum](https://community.sonarsource.com/c/plugins)
3. Creating a PR on [sonar-update-center-properties repo](https://github.com/SonarSource/sonar-update-center-properties)

The 1st part is already automated with build tool plugins such as [sonar-packaging-maven-plugin](https://github.com/SonarSource/sonar-packaging-maven-plugin) and [gradle-sonar-packaging-plugin](https://github.com/iwarapter/gradle-sonar-packaging-plugin). This GitHub Action will automate the 2nd and 3rd steps.

## Supported Feature

This action is still in beta, so provides limited features:

- [x] Fork the [sonar-update-center-properties repo](https://github.com/SonarSource/sonar-update-center-properties) into your GitHub account
- [x] Sync the default branch from the repo in SonarSource organization to the repo in your GitHub account
- [x] Update the properties file, and push the topic branch to the repo in your GitHub account
- [x] Create a draft PR to the [sonar-update-center-properties repo](https://github.com/SonarSource/sonar-update-center-properties)
- [x] Post to the [Community Forum](https://community.sonarsource.com/c/plugins)

It means that, you need to create a PR based on the topic branch pushed by this GitHub Action.
Note that you needs to review the created PR manually, and mark it as ready-to-review.

## How to configure

In your workflow file under `.github/workdlows`, add a step using this plugin:

```yml
      # Assume that ${{ github.event.release.tag_name }} follows semver2 and has no 'v' prefix
      # e.g. 1.0.0, 2.3.4
      - uses: KengoTODA/sonar-update-center-action@main
        with:
          prop-file: findbugs.properties # the name of your target file
          description: Use SpotBugs 4.2.0, sb-contrib 7.4.7, and findsecbugs 1.11.0 # The description of your release
          minimal-supported-sq-version: 7.9 # The minimal supported SonarQube version
          latest-supported-sq-version: LATEST # The latest supported SonarQube version, default is 'LATEST'
          changelog-url: https://github.com/spotbugs/sonar-findbugs/releases/tag/${{ github.event.release.tag_name }} # The URL of changelog for your release
          download-url: https://repo.maven.apache.org/maven2/com/github/spotbugs/sonar-findbugs-plugin/${{ github.event.release.tag_name }}/sonar-findbugs-plugin-${{ github.event.release.tag_name }}.jar # The URL to download your plugin
          public-version: ${{ github.event.release.tag_name }} # The version to publish
          sonar-cloud-url: https://sonarcloud.io/dashboard?branch=${{ github.event.release.tag_name }}&id=com.github.spotbugs%3Asonar-findbugs-plugin # The URL of SQ analysis result
          github-token: ${{ secrets.PAT_TO_FORK }} # The GitHub Personal Access Token
          discourse-api-key: ${{ secrets.DISCOURSE_API_KEY }} # The User API key for https://community.sonarsource.com/
          skip-creating-pull-request: false # Skip creating a PR
          skip-announcing: false # Skip announcing at the Community Forum
```

### How to generate discourse-api-key

The author of this project also published the [discourse-api-key-generator](https://github.com/KengoTODA/discourse-api-key-generator). You can generate API key by invoking it via `npx` just like below:

```sh
$ npx discourse-api-key-generator --app=sonar-update-center-action --url=https://community.sonarsource.com
```

Set the generated API key to the `DISCOURSE_API_KEY` GitHub Secrets, and refer it in the workflow as `${{ secrets.DISCOURSE_API_KEY }}`.

## Which kind of PAT (Personal Access Token) you need to use

You need to [create a personal access token](https://docs.github.com/en/free-pro-team@latest/github/authenticating-to-github/creating-a-personal-access-token) with the `public_repo` permission, to operate the following operations:

1. Fork the [sonar-update-center-properties repo](https://github.com/SonarSource/sonar-update-center-properties) into your GitHub account
2. Pull the default branch from [sonar-update-center-properties repo](https://github.com/SonarSource/sonar-update-center-properties) and push it to the repo in your GitHub account
3. Commit necessary changes
4. Push the topic branch to the repo in your GitHub account
5. Create a PR
