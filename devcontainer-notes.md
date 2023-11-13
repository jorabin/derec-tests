1. See [devcontainer-setting.xml](./devcontainer-settings.xml) for a discussion
of using a PAT to access the `derec-api` dependency.
2. Comments in JSON (specifically devcontainer.json) see https://github.com/microsoft/vscode/issues/139599
3. [Setting up a Java Project in Codespaces](https://docs.github.com/en/codespaces/setting-up-your-project-for-codespaces/adding-a-dev-container-configuration/setting-up-your-java-project-for-codespaces)
4. [GitHub CLI](https://cli.github.com/manual/gh_auth_setup-git)
5. Adding a local JAR file to a maven project (deprecated)
    ```xml
    <dependency>
      <groupId>com.sample</groupId>
      <artifactId>sample</artifactId>
      <version>1.0</version>
      <scope>system</scope>
      <systemPath>${project.basedir}/src/main/resources/Name_Your_JAR.jar</systemPath>
    </dependency>
    ```
6. GitHub packages [working with the Apache Maven registry](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-apache-maven-registry#authenticating-with-a-personal-access-token)
7. 



