# Contributing to rcljava

Thanks for your interest in improving rcljava.

This project is published under the Apache License 2.0 and welcomes high-quality bug reports, fixes, documentation improvements, and examples.

## Ways to contribute

- Report bugs or unexpected behavior
- Suggest enhancements or new APIs
- Improve documentation and examples
- Submit code changes and tests

## Before you start

- Check whether an issue already exists for the topic you want to work on.
- Keep changes focused and small enough to review easily.
- Prefer clear commit messages and tests for behavioral changes.

## Development setup

Requirements:

- JDK 25
- Maven 3.9+
- ROS 2 Jazzy environment available in the devcontainer or host system

Common commands:

```bash
mvn -q -DskipTests package
mvn -q -pl rcljava-examples -am test
```

## Coding guidelines

- Prefer clear, idiomatic Java code.
- Keep public APIs minimal and documented.
- Put implementation details in internal packages rather than the public API surface.
- Add or update tests for logic changes and bug fixes.
- Avoid introducing unrelated refactors in the same change.

## Pull requests

1. Fork or branch from the repository's default branch.
2. Make your change with clear, focused commits.
3. Run the relevant Maven tests for the affected module.
4. Open a pull request with:
   - a concise title
   - a summary of the change
   - related issue references when applicable
   - validation steps performed

## Code of conduct

Please follow our [Code of Conduct](CODE_OF_CONDUCT.md). We aim to maintain a respectful, welcoming, and productive contributor community.

## License

By contributing, you agree that your contributions will be licensed under the project's [LICENSE](LICENSE).
