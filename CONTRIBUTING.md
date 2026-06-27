# Contributing to Rory's Excavation

Thank you for considering a contribution. This document explains how to get involved.

---

## Ground Rules

- Keep pull requests focused. One feature or fix per PR.
- Discuss major changes in an issue before writing code.
- Follow the existing code style: meaningful names, no unnecessary comments, no giant classes.
- Every new feature must go in its own `feature/` class. Do not put logic in event handlers.

---

## Getting Started

1. Fork the repository and clone your fork.
2. Set up the Forge 1.2.5 development environment (see [docs/BUILD.md](docs/BUILD.md)).
3. Create a feature branch: `git checkout -b feature/my-feature`.
4. Write your code, keeping the layered architecture intact (see [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)).
5. Test in Prism Launcher with a clean Forge 3.4.9.171 profile.
6. Open a pull request against `main`.

---

## Code Style

- Package: `com.rorysmod.excavation.<layer>`
- Layers: `config`, `handler`, `feature`
- No static mutable state outside of Forge-required singletons.
- Config values are read in `ModConfig`, nowhere else.

---

## Reporting Issues

Open a GitHub issue with:

- Minecraft version and Forge version.
- Steps to reproduce.
- Expected vs. actual behaviour.
- Relevant log excerpt (no need to attach the full log unless asked).

---

## License

By contributing you agree that your changes will be released under the [MIT License](LICENSE).
