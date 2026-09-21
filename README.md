# jenkins-pipeline

![Jenkins](https://img.shields.io/badge/Jenkins-Declarative%20Pipeline-D24939?logo=jenkins&logoColor=white)
![Java](https://img.shields.io/badge/Java-17-orange?logo=openjdk&logoColor=white)
![Maven](https://img.shields.io/badge/Build-Maven-C71A36?logo=apachemaven&logoColor=white)
![JUnit5](https://img.shields.io/badge/Tests-JUnit%205-25A162?logo=junit5&logoColor=white)
![jenkinsfile-runner](https://img.shields.io/badge/Verified%20with-jenkinsfile--runner-D24939?logo=jenkins&logoColor=white)
![CI](https://github.com/Mehedi-K/jenkins-pipeline/actions/workflows/ci.yml/badge.svg)

A **Jenkins declarative pipeline-as-code** portfolio project: a real
`Jenkinsfile` driving a Java/Maven build-test-package-report flow for a small,
self-contained utility library, with the pipeline itself executed headlessly
on every push using
[`jenkinsfile-runner`](https://github.com/jenkinsci/jenkinsfile-runner) so the
`Jenkinsfile` is continuously proven to actually work rather than just
looking plausible.

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
  plugins.txt                 # extra Jenkins plugins jenkinsfile-runner needs
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
syntactically plausible) takes an extra step: it is executed headlessly with
[`jenkinsfile-runner`](https://github.com/jenkinsci/jenkinsfile-runner), the
official tool for running a single `Jenkinsfile` against a workspace without
standing up a full Jenkins controller/UI.

**This is wired into `.github/workflows/ci.yml` and runs on every push/PR**,
not just as a one-off local check - see the `verify-jenkinsfile` job. It:

1. Pulls `ghcr.io/jenkinsci/jenkinsfile-runner:jre-21-alpine`.
2. Mounts this repo into the container at both `/workspace` (where the
   `Jenkinsfile` is auto-discovered) and `/build` (the actual pipeline
   execution workspace, so `sample-app/` is present for the `sh` steps).
3. Passes `plugins.txt` via `-p` so Jenkins resolves and installs the handful
   of plugins the pipeline needs beyond what the jenkinsfile-runner "vanilla"
   image already bundles (core pipeline support, plus `git`).
4. Runs the real `Jenkinsfile` end to end: checkout → build → test → static
   analysis → package → publish report, using the exact same stages a real
   Jenkins job would run.

### Running it yourself

```bash
docker pull ghcr.io/jenkinsci/jenkinsfile-runner:jre-21-alpine

docker run --rm \
  -v "$(pwd)":/workspace \
  -v "$(pwd)":/build \
  ghcr.io/jenkinsci/jenkinsfile-runner:jre-21-alpine \
  -p /workspace/plugins.txt
```

- `/workspace` is where jenkinsfile-runner looks for `Jenkinsfile` by default.
- `/build` is the actual pipeline run workspace (where `sh`/`dir` steps
  execute) - it needs to contain `sample-app/` too, hence mounting the repo
  to both paths.
- `-p /workspace/plugins.txt` tells jenkinsfile-runner which additional
  plugins to fetch (each plugin's own declared dependencies are then resolved
  by Jenkins itself at boot against the configured update center).

A successful run prints each stage's Maven output, ends with
`Finished: SUCCESS`, and leaves `sample-app/target/` populated (surefire
reports, the HTML report, the built jar) exactly as a real Jenkins job would.

**Honesty note on what's actually verified:** the CI job above genuinely
executes the pipeline's Groovy/declarative syntax and every `sh` step through
real Jenkins pipeline execution code (via jenkinsfile-runner) - it is not a
syntax-only lint. What it does *not* exercise is anything specific to a
persistent Jenkins controller (e.g. cross-build history, the classic web UI,
credentials binding, or a multibranch job's SCM webhook trigger), since
jenkinsfile-runner is deliberately a single-shot, throwaway-controller tool.
For that layer, see "Pointing a real Jenkins instance at this repo" below.
Development on this machine has no local Docker daemon available, so the
`jenkinsfile-runner` command above was designed and reasoned through against
the tool's actual source (plugin resolution behavior, default workspace
mounts, bundled plugin set) rather than run locally - the authoritative,
continuously-repeated verification is the `verify-jenkinsfile` job in
`.github/workflows/ci.yml`, which anyone can inspect via the CI badge above
or `gh run list`. The Maven build/test/package/checkstyle steps themselves
(the `build-and-test` CI job) were additionally run and verified directly on
the development machine, independent of Jenkins or Docker.

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
