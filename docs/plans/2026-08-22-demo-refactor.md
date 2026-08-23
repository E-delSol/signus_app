# Demo.sh Refactor — Split into lib/ modules

> **For agentic workers:** Use `mobiai-mobile-executing-plans-with-subagents` (recommended) or `mobiai-mobile-executing-plans` to implement this plan task-by-task.

**Goal:** Split 646-line monolithic demo.sh into focused ~60-90 line modules under `lib/`.

**Architecture:** Thin entry point `demo.sh` sources all `lib/*.sh` modules. Each module contains functions for one concern. Global config stays in demo.sh.

**Tech Stack:** Bash

---

## Target Structure

```
demo/
├── demo.sh              ← entry point (~50 lines)
└── lib/
    ├── output.sh        ← print helpers
    ├── workspace.sh     ← setup_workspace()
    ├── prereqs.sh       ← check_prerequisites()
    ├── backend.sh       ← wait_for_port(), start_backend()
    ├── users.sh         ← user creation, linking
    ├── build.sh         ← build_apk()
    ├── emulators.sh     ← emulator lifecycle
    └── deploy.sh        ← install_and_launch()
```

## Implementation Tasks

### Task 1: Create lib/ directory and output.sh

- [ ] Create `demo/lib/` directory
- [ ] Extract output helpers (print_banner, print_step, print_ok, print_warn, print_fail, print_section) + color variables into `lib/output.sh`

### Task 2: Create workspace.sh

- [ ] Extract setup_workspace() into `lib/workspace.sh`

### Task 3: Create prereqs.sh

- [ ] Extract check_prerequisites() into `lib/prereqs.sh`

### Task 4: Create backend.sh

- [ ] Extract wait_for_port() and start_backend() into `lib/backend.sh`

### Task 5: Create users.sh

- [ ] Extract create_or_login_user(), verify_or_create_linking(), prepare_demo_users() into `lib/users.sh`

### Task 6: Create build.sh

- [ ] Extract build_apk() into `lib/build.sh`

### Task 7: Create emulators.sh

- [ ] Extract wait_for_boot(), start_emulator_for_avd(), start_emulators(), wait_for_emulators() into `lib/emulators.sh`

### Task 8: Create deploy.sh

- [ ] Extract install_and_launch(), print_summary() into `lib/deploy.sh`

### Task 9: Refactor demo.sh entry point

- [ ] Replace monolithic demo.sh with thin entry point: config + source lib/*.sh + main()

### Task 10: Verify

- [ ] `bash -n` syntax check on all files
- [ ] Full end-to-end test
