# Demo.sh Self-Contained Workspace Plan

> **For agentic workers:** Use `mobiai-mobile-executing-plans-with-subagents` (recommended) or `mobiai-mobile-executing-plans` to implement this plan task-by-task.

**Goal:** Eliminate fragile path detection in demo.sh by making it clone all repos into a relative `./signus_demo/` workspace.

**Architecture:** New `setup_workspace()` function creates a workspace directory, clones missing repos from GitHub with `--depth 1 --branch`, and assigns repo paths as workspace subdirectories. Fragile path detection loops are removed entirely.

**Tech Stack:** Bash, Git, Android SDK, Docker

**Platform:** Linux/macOS (Android development)

---

## Repos

| Repo | URL (SSH) | Branch |
|------|-----------|--------|
| `signus_infra` | `git@github.com:E-delSol/signus_infra.git` | `feat/demo-script` |
| `signus_app` | `git@github.com:E-delSol/signus_app.git` | `feat/demo-mode` |
| `signus_back` | `git@github.com:E-delSol/signus_back.git` | `feat/demo-optional-fcm` |

## Workspace Structure

```
<cwd>/signus_demo/
├── signus_infra/demo/demo.sh
├── signus_app/                  (feat/demo-mode)
└── signus_back/                 (feat/demo-optional-fcm)
```

## Implementation Tasks

### Task 1: Add setup_workspace() function

**Files:**
- Modify: `signus_infra/demo/demo.sh` (new function before check_prerequisites)

- [ ] **Step 1: Add setup_workspace function**

```bash
setup_workspace() {
    print_section "Setting up workspace"

    WORKSPACE="${DEMO_SIGNUS_DIR:-./signus_demo}"
    mkdir -p "$WORKSPACE"

    local github_org="E-delSol"
    local infra_branch="feat/demo-script"
    local app_branch="feat/demo-mode"
    local back_branch="feat/demo-optional-fcm"

    # Clone signus_infra if not present
    INFRA_DIR="${WORKSPACE}/signus_infra"
    if [ -d "$INFRA_DIR" ]; then
        print_ok "signus_infra already present"
    else
        print_step "Cloning signus_infra (${infra_branch})..."
        if git clone --branch "$infra_branch" --depth 1 \
            "git@github.com:${github_org}/signus_infra.git" "$INFRA_DIR" 2>&1; then
            print_ok "signus_infra cloned"
        else
            print_fail "Failed to clone signus_infra"
            echo "    Check SSH access to GitHub"
            exit 1
        fi
    fi

    # Clone signus_app if not present
    APP_DIR="${WORKSPACE}/signus_app"
    if [ -d "$APP_DIR" ]; then
        print_ok "signus_app already present"
    else
        print_step "Cloning signus_app (${app_branch})..."
        if git clone --branch "$app_branch" --depth 1 \
            "git@github.com:${github_org}/signus_app.git" "$APP_DIR" 2>&1; then
            print_ok "signus_app cloned"
        else
            print_fail "Failed to clone signus_app"
            exit 1
        fi
    fi

    # Clone signus_back if not present
    BACKEND_DIR="${WORKSPACE}/signus_back"
    if [ -d "$BACKEND_DIR" ]; then
        print_ok "signus_back already present"
    else
        print_step "Cloning signus_back (${back_branch})..."
        if git clone --branch "$back_branch" --depth 1 \
            "git@github.com:${github_org}/signus_back.git" "$BACKEND_DIR" 2>&1; then
            print_ok "signus_back cloned"
        else
            print_fail "Failed to clone signus_back"
            exit 1
        fi
    fi

    print_ok "Workspace ready: $WORKSPACE"
}
```

- [ ] **Step 2: Verify function syntax**

```bash
bash -n signus_infra/demo/demo.sh
```

Expected: no output (no syntax errors)

### Task 2: Remove fragile path detection

**Files:**
- Modify: `signus_infra/demo/demo.sh` (remove lines 34-70, replace with workspace assignment)

- [ ] **Step 1: Remove path detection and update SCRIPT_DIR**

Replace lines 34-70 (the REPOS_PARENT, candidate loops for BACKEND_DIR and APP_DIR) with:

```bash
# Path variables will be set by setup_workspace()
# Defaults can be overridden via environment for non-standard layouts
INFRA_DIR="${SIGNUS_INFRA_DIR:-}"
APP_DIR="${SIGNUS_APP_DIR:-}"
BACKEND_DIR="${SIGNUS_BACKEND_DIR:-}"
```

This keeps env var overrides working but removes all the fragile candidate loops. The actual assignment happens in `setup_workspace()`.

### Task 3: Update check_prerequisites

**Files:**
- Modify: `signus_infra/demo/demo.sh:97-172` (check_prerequisites function)

- [ ] **Step 1: Remove BACKEND_DIR and APP_DIR checks from prerequisites**

These checks are no longer needed because `setup_workspace()` guarantees the directories exist. Remove the two blocks:
- Lines 138-143: `if [ -d "$BACKEND_DIR" ]; then ...`
- Lines 145-150: `if [ -d "$APP_DIR" ]; then ...`

Keep all other checks (Java, ANDROID_HOME, adb, emulator, Docker, AVDs).

### Task 4: Update main() call order

**Files:**
- Modify: `signus_infra/demo/demo.sh:574-585` (main function)

- [ ] **Step 1: Add setup_workspace call**

```bash
main() {
    print_banner
    setup_workspace        # <-- NEW: clones repos, sets INFRA_DIR/APP_DIR/BACKEND_DIR
    check_prerequisites
    start_backend
    prepare_demo_users
    build_apk
    start_emulators
    wait_for_emulators
    install_and_launch
    print_summary
}
```

### Task 5: Verify the complete script

- [ ] **Step 1: Run syntax check**

```bash
bash -n signus_infra/demo/demo.sh
```

- [ ] **Step 2: Run shellcheck if available**

```bash
shellcheck signus_infra/demo/demo.sh
```

- [ ] **Step 3: Dry-run test (simulate workspace creation)**

```bash
cd /tmp && bash -x signus_infra/demo/demo.sh --dry-run 2>&1 | head -50
```

(If --dry-run isn't implemented, just verify syntax and structure are correct)

## What's NOT changing

- `wait_for_boot()` — already uses `grep -F` (bug fix from previous session)
- User creation, linking, APK build, emulator launch — all stay the same
- Token injection via DemoTokenReceiver — unchanged
- All output helpers (print_ok, print_fail, etc.) — unchanged
