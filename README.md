# Grid Rescue

This is a student assignment scaffold, not a completed game. The Java class
signatures and 53 grading tests are supplied. Implement the model for milestone 2,
then add the interface and project documentation for milestone 3.

Start by reading `docs/Project_Milestone_2.txt`, `docs/Game_Rules.txt`, and
`docs/UML_Class_Specification.txt`. The supplied constructors and methods currently
throw `UnsupportedOperationException` to mark work you must implement.

## Build and test

Install a JDK supporting Java 11 and Maven. Run these commands from this directory,
which contains `pom.xml`:

```sh
mvn test
```

The initial scaffold compiles, discovers 53 tests and fails them intentionally.
Do not delete, disable, rename or weaken the grading tests. Additional tests belong
in separately named suites and do not change the grading denominator.

To reproduce the automated grade locally, run the following commands in sequence.
Use `success` only if the Maven command completes successfully; use `failure` if
compilation or the build infrastructure fails. Maven's failure-ignore flag permits
partial test credit without treating a failed build as successful.

```sh
mvn --batch-mode clean test -Dmaven.test.failure.ignore=true
python3 tools/grade.py --build-outcome success
```

The grade is `1 + 2 * passed / 53`, from 1.00 to 3.00. Failed, errored, skipped,
missing or malformed grading reports never receive passing credit. PR requirements
and code quality supply the remaining manually assessed point. Python 3 is needed
only for this local grading command; `mvn test` uses Java and Maven.

## Project layout

- `src/main/java/edu/whitman/`: required model classes, enums and TODO bodies.
- `src/test/java/edu/whitman/`: eight grading suites and shared fixture helpers.
- `.github/workflows/ci.yml`: GitHub Actions grading workflow.
- `tools/grade.py`: grade calculation used by CI.
- `docs/`: rules, milestones, UML specification, test plan and instructor notes.

Upload the CONTENTS of this directory to the repository root so GitHub can find
`.github/workflows/ci.yml`. Keep the final assessed version on `main`.

## Milestone 3 README work to complete

Replace this scaffold introduction with your own project description. Add:

1. At least 24 numbered functional requirements describing the completed system.
2. At least 4 numbered nonfunctional requirements, with measurable checks.
3. Your chosen architecture, its justification, and a simple component diagram.
4. A working demo link or screenshots and instructions to launch the interface.
5. A link to your Google presentation.

The supplied rules establish the required behavior; this README is intentionally
not a completed milestone 3 submission. Keep development commits and pull requests
as evidence. The instructor must specify the solo application of the existing
CATME/teamwork categories without silently changing their weights.
