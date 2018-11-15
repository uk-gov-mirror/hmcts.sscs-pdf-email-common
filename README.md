## sscs-pdf-robotics-email-common

This is the common code library for sscs.

###Build

To build run

```bash
./gradlew clean build
```
This will create a jar file in the projects build libs directory.
If you want to then depend on this without publishing it you can add the following to your build.gradle file
in the dependencies section.

```gradle
compile files('{PROJECT_DIR}/sscs-pdf-robotics-common/build/libs/sscs-pdf-robotics-common-0.0.84.jar')
```

###Release candidate

To release a candidate

1. Change the version to, e.g. 0.0.85-CANDIDATE
1. Commit and push upstream
1. Tag this commit and push the tag upstream
    ```bash
    git tag -a 0.0.85-CANDIDATE
    git push origin 0.0.85-CANDIDATE
    ```

This will start a pipeline that publishes a new artifact to bintray.
To use this new artifact, add the following to your build.gradle file in the dependencies section.

```gradle
compile group: 'uk.gov.hmcts.reform', name: 'sscs-pdf-robotics-email-common', version: '0.0.85-CANDIDATE'
```
