# Conventions

Each entry lives in `<language>/<YYYY-MM-DD>-<kebab-slug>/` and contains a `NOTES.md` (problem, approach, complexity table, interview talking points, run command) plus:

| Language   | Files | Test command (run inside the entry folder) |
|------------|-------|--------------------------------------------|
| Python     | `solution.py`, `test_solution.py` (pytest, standard library only) | `python -m pytest -q` |
| TypeScript | `solution.ts`, `solution.test.ts` (`node:test` + `node:assert`, no deps) | `npx --yes tsx --test solution.test.ts` |
| Java       | `Solution.java`, `SolutionTest.java` (no package; `main` uses `assert` / throws on failure) | `javac *.java && java -ea SolutionTest` |
| C++        | `solution.hpp`, `test.cpp` (C++17, `<cassert>`) | `g++ -std=c++17 -Wall -o test test.cpp && ./test` |

The language rotation is Python → TypeScript → Java → C++, continuing from the language of the latest entry in the README index.

Add a row to the README table between the `INDEX` markers, with the newest entry at the bottom.
