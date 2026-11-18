# BUILD dotCMS Java Base

This Dockerfile builds the base container woth java that will be used to build and run dotCMS. 

### CI preparation

The default JDK is Corretto `25.0.5.1.2-amzn`. The **Build SDKMan Base Java Image**
workflow also handles PR changes to `.sdkmanrc`, using trusted default-branch code.
Unchanged `.sdkmanrc` skips the workflow entirely: no runner or API calls.
For those PRs, the CI build only checks its local Git diff; no registry inspection.
It reuses an image containing Linux amd64 and arm64, or publishes both architectures
if either is missing. Registry errors fail instead of being treated as missing images.

The CI build waits for that publication before building its test image. Fork PRs
cannot publish; a maintainer must prepare a missing base through the manual workflow.
Concurrent requests for one tag are serialized and recheck availability before building.
SDKMAN identifiers keep `+` for installation; Docker tags replace it with `-`.

For the initial rollout, publish the Corretto base with the existing manual workflow
before relying on the new PR trigger, which becomes active after merge.
If preparation fails, inspect that workflow and rerun it manually; CI stops waiting
after 90 minutes without starting the dependent image build.
Manual dispatch retains its suffix, architecture and push controls.
Stacked PRs inheriting a parent's Java change still skip preparation when their own
`.sdkmanrc` is unchanged; rerun a racing child build after the parent publishes.

### Building Multiarch
To enable "multiarch" building, you need to use `docker buildx` and have defined a new builder (you might need to enable "experimental" features in order to enable `docker buildx`). If you have `buildx` installed, you can see what buildx builders you have available by doing a `ls`:

```
docker buildx ls
```

To create and use a new multiarch builder run:
```
docker buildx create --name multiarch
docker buildx use multiarch
```

At this point, you can use `buildx` to build your image and target the platform(s) you want to build for.  Mostly, the same arguements apply, though `buildx` also allows you to immediatly push your new image to docker hub after it is built.

Use the version specified
```
docker buildx build --platform linux/amd64,linux/arm64 --pull --push -t dotcms/java-base:25.0.5.1.2-amzn .
```

or specify a version
```
JAVA_VERSION="<sdkman-identifier>"
JAVA_TAG="${JAVA_VERSION//+/-}"
docker buildx build --platform linux/amd64,linux/arm64 --pull --push \
  --build-arg SDKMAN_JAVA_VERSION="$JAVA_VERSION" -t "dotcms/java-base:$JAVA_TAG" .

```
