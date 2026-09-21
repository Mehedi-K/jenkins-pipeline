# jenkins-pipeline

![Jenkins](https://img.shields.io/badge/Jenkins-Declarative%20Pipeline-D24939?logo=jenkins&logoColor=white)
![Java](https://img.shields.io/badge/Java-17-orange?logo=openjdk&logoColor=white)
![Maven](https://img.shields.io/badge/Build-Maven-C71A36?logo=apachemaven&logoColor=white)
![JUnit5](https://img.shields.io/badge/Tests-JUnit%205-25A162?logo=junit5&logoColor=white)
![Verified](https://img.shields.io/badge/Verified%20with-real%20Jenkins%20controller-D24939?logo=jenkins&logoColor=white)
![CI](https://github.com/Mehedi-K/jenkins-pipeline/actions/workflows/ci.yml/badge.svg)

A **Jenkins declarative pipeline-as-code** portfolio project: a real
`Jenkinsfile` driving a Java/Maven build-test-package-report flow for a small,
self-contained utility library, with the pipeline itself executed on every
push against a real, disposable `jenkins/jenkins:lts` controller spun up in
CI, so the `Jenkinsfile` is continuously proven to actually work rather than
just looking plausible.

This is a portfolio project focused on **CI/CD pipeline authoring**: writing
a Jenkins pipeline the way it would be written for a real team (multiple
purposeful stages, parameterization, post-build cleanup, published test/report
artifacts) and, since Jenkins is not a hosted CI you can just push to like
GitHub Actions, proving the `Jenkinsfile` actually runs rather than shipping
untested YAML/Groovy.

## What's here

```
jenkins-pipeline/
  Jenkinsfile                 # the pipeline under test
  plugins.txt                 # Jenkins plugins this Jenkinsfile needs
  config/checkstyle.xml       # ruleset for the Static Analysis stage
  sample-app/                 # small Java/Maven library the pipeline builds
    pom.xml
    mvnw / mvnw.cmd / .mvn/   # Maven Wrapper - pins the exact Maven version
    src/main/java/io/pipelinedemo/utils/
      Calculator.java
      StringUtils.java
      Validator.java
    src/test/java/io/pipelinedemo/utils/
      CalculatorTest.java
      StringUtilsTest.java
      ValidatorTest.java
  .github/workflows/ci.yml    # runs the Maven build AND the Jenkinsfile itself
```

`sample-app` is intentionally small: three utility classes (a calculator, a
string-helpers class, and basic input validators) with 48 real JUnit 5 tests,
including a handful of deliberate edge cases (division by zero, null
handling, inclusive age-range boundaries, multi-label email domains). It
exists purely to give the pipeline something real to compile, test, lint, and
package - not as a demo "application."

## The pipeline (`Jenkinsfile`)

Declarative syntax, six stages:

| Stage | What it does |
|---|---|
| **Checkout** | `checkout scm` |
| **Build** | `./mvnw clean compile` |
| **Unit Tests** | `./mvnw test`, then `junit` archives the Surefire XML results (runs in a stage `post { always {...} }` so results publish even on failure) |
| **Static Analysis** | `./mvnw checkstyle:check` against `config/checkstyle.xml`. Skippable via the `SKIP_STATIC_ANALYSIS` build parameter (`when { expression { ... } }`) |
| **Package** | `./mvnw package -DskipTests`, then `archiveArtifacts` fingerprints the built jar |
| **Publish Report** | `./mvnw surefire-report:report`, then `publishHTML` publishes the generated Surefire HTML report as a build tab |

Other things demonstrated:

- `options { buildDiscarder(...); timeout(...); timestamps() }`
- `parameters { booleanParam(...) }` - a real parameterized-build example, not
  just a stub
- `environment { APP_DIR = 'sample-app'; ... }`, referenced via `dir(env.APP_DIR)`
  in every stage instead of hardcoding the path repeatedly
- Top-level `post { always / success / failure / cleanup }` for
  notification-style logging and `cleanWs()`
- **No `tools { maven ...; jdk ... }` block.** That directive depends on named
  tool installations being pre-configured on the Jenkins controller (Manage
  Jenkins → Global Tool Configuration, or JCasC), which a portfolio repo has
  no way to guarantee on whatever agent runs it. Instead `sample-app` ships
  its own **Maven Wrapper** (`sample-app/mvnw`), so every stage builds with a
  pinned Maven version regardless of what is - or isn't - installed on the
  agent; only a JDK needs to be on `PATH`, which every Jenkins agent has by
  definition.

## How this was verified

Jenkins doesn't offer a hosted "push and see the pipeline run" workflow the
way GitHub Actions does, so proving the `Jenkinsfile` is real (not just
syntactically plausible) takes an extra step. The first approach tried here
was [`jenkinsfile-runner`](https://github.com/jenkinsci/jenkinsfile-runner)
(the official single-shot Jenkinsfile execution tool) - it got as far as
resolving plugins and booting, but its slim core payload deliberately strips
`WEB-INF/detached-plugins` to shrink the image, which broke classloading for
a plugin (`caffeine-api`) needed transitively by `script-security`, with no
clean fix short of patching the image. Rather than ship that as an unverified
guess, this repo verifies against a **full, real `jenkins/jenkins:lts`
controller** instead - slower to boot, but a far more faithful stand-in for
an actual Jenkins server, and it doesn't have that stripped-payload problem.

**This is wired into `.github/workflows/ci.yml` and runs on every push/PR**,
not just as a one-off local check - see the `verify-jenkinsfile` job. It:

1. Starts `jenkins/jenkins:lts` with the setup wizard disabled (an ephemeral,
   unauthenticated, CI-only instance - see the security note below).
2. Installs the plugins listed in `plugins.txt` with `jenkins-plugin-cli`
   (bundled in the official image) and restarts to load them.
3. Creates a real Pipeline job via the Jenkins REST API (`POST
   /createItem`), configured with **Git SCM pointed at a `file://` checkout
   of this same repository** and `scriptPath: Jenkinsfile` - i.e. the exact
   same "Pipeline script from SCM" setup a real team would use, just aimed at
   a local checkout instead of a remote GitHub URL.
4. Triggers a build (`POST /job/.../build`), polls
   `/job/.../lastBuild/api/json` until it finishes, and fails the CI job
   unless the result is `SUCCESS`.
5. Prints the full build console log either way, and uploads the pipeline's
   own build output (surefire reports, the HTML report, the jar) as a CI
   artifact.

A successful run's console shows every stage executing for real: `mvn`
compiling, 48 JUnit tests passing, 0 Checkstyle violations, the jar getting
archived, and the Surefire HTML report getting published - ending with
`Finished: SUCCESS`. You can see this directly in any green run of the
`verify-jenkinsfile` job (CI badge above, or `gh run list` /
`gh run view --log`).

### Verifying it yourself

You need Docker. This mirrors exactly what CI does:

```bash
docker run -d --name jenkins -p 8080:8080 \
  -e JAVA_OPTS="-Djenkins.install.runSetupWizard=false -Dhudson.plugins.git.GitSCM.ALLOW_LOCAL_CHECKOUT=true" \
  -v "$(pwd)":/repo \
  jenkins/jenkins:lts

# wait for it to come up, then:
docker exec jenkins jenkins-plugin-cli --plugin-file /repo/plugins.txt
docker restart jenkins
# wait for it to come back up, then:
docker exec jenkins git config --global --add safe.directory '*'
docker exec jenkins git config --global protocol.file.allow always

# create + trigger the job - see .github/workflows/ci.yml for the exact
# REST API calls (job-config.xml, crumb handling, polling for the result).
```

The full, exact, currently-passing sequence (including crumb/cookie
handling and result polling) lives in `.github/workflows/ci.yml` - that file
*is* the canonical "how to verify this locally" reference, since it's
proven to work on every push.

**Security note:** disabling the setup wizard and allowing local git
checkouts is intentional and safe **only** for this throwaway, single-purpose
container - it is never exposed beyond `localhost`, never connected to real
infrastructure, and torn down at the end of the job. None of this reflects
how you'd configure a real, persistent Jenkins controller.

**Honesty note on what's actually verified:** this genuinely executes the
pipeline's declarative/Groovy syntax and every `sh` step through real Jenkins
pipeline execution code against a real controller - it is not a syntax-only
lint, and not a single-shot/throwaway-runner shortcut either. What it does
*not* exercise is a webhook-triggered multibranch job or credentials binding,
since those need a reachable, persistent Jenkins instance. For that layer,
see "Pointing a real Jenkins instance at this repo" below. Development on
this machine has no local Docker daemon available, so this whole approach
was iterated on and debugged directly through the GitHub Actions runner
(which does have Docker) rather than locally - every fix described above
(crumb/cookie handling, the local-checkout security guard, the shallow-clone
issue, the report-path normalization) was diagnosed from a real failing run,
not guessed. The Maven build/test/package/checkstyle steps themselves (the
`build-and-test` CI job) were additionally run and verified directly on the
development machine, independent of Jenkins or Docker.

## Pointing a real Jenkins instance at this repo

1. Install the plugins in `plugins.txt` (Manage Jenkins → Plugins), if not
   already present.
2. New Item → **Multibranch Pipeline**.
3. Branch Sources → Git → this repository's URL.
4. Build Configuration → by Jenkinsfile, script path `Jenkinsfile` (default).
5. Save. Jenkins will discover the `main` branch, run the pipeline, and show
   the same six stages, the JUnit trend graph, the archived jar, and the
   Surefire HTML report tab described above.

No credentials, tokens, or private infrastructure are required - this repo is
fully self-contained and does not depend on or reference any real Jenkins
server.

## Running the sample app directly

```bash
cd sample-app
./mvnw clean verify checkstyle:check   # compile, test, package, lint
```

48 tests, 0 Checkstyle violations, builds `target/sample-app.jar`.
