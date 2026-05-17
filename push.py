#!/usr/bin/env python3
"""
push.py — Git helper for PrivacyShield.

Modes:
    python3 push.py                         # stage + commit + push
    python3 push.py "my message"            # stage + commit with message + push
    python3 push.py --push-only             # push already-committed changes only
    python3 push.py --push-only "ignored"   # same as above

Reads GITHUB_PERSONAL_ACCESS_TOKEN from the environment to authenticate
HTTPS pushes. The token is injected into the remote URL at push time and
is never printed or logged.
"""

import os
import subprocess
import sys
from datetime import datetime
from urllib.parse import urlparse, urlunparse


def run(cmd: list[str], check: bool = True, capture: bool = False) -> subprocess.CompletedProcess:
    """Run a shell command, streaming output unless capture=True."""
    return subprocess.run(
        cmd,
        check=check,
        capture_output=capture,
        text=True,
    )


def get_current_branch() -> str:
    result = run(["git", "rev-parse", "--abbrev-ref", "HEAD"], capture=True)
    return result.stdout.strip()


def get_remote_url(remote: str = "origin") -> str:
    result = run(["git", "remote", "get-url", remote], capture=True)
    return result.stdout.strip()


def inject_token_into_url(url: str, token: str) -> str:
    """Return an HTTPS URL with the token embedded as the username."""
    parsed = urlparse(url)
    if parsed.scheme not in ("http", "https"):
        raise ValueError(
            f"Remote URL uses scheme '{parsed.scheme}'. "
            "Convert the remote to HTTPS or use an SSH key instead of a token."
        )
    host = parsed.hostname or ""
    port_part = f":{parsed.port}" if parsed.port else ""
    authed_netloc = f"{token}@{host}{port_part}"
    authed = parsed._replace(netloc=authed_netloc)
    return urlunparse(authed)


def has_uncommitted_changes() -> bool:
    result = run(["git", "status", "--porcelain"], capture=True)
    return bool(result.stdout.strip())


def count_unpushed_commits(branch: str) -> int:
    """Return the number of local commits not yet on origin/branch."""
    result = run(
        ["git", "rev-list", f"origin/{branch}..HEAD", "--count"],
        capture=True,
        check=False,
    )
    try:
        return int(result.stdout.strip())
    except ValueError:
        return 0


def do_push(branch: str) -> None:
    token = os.environ.get("GITHUB_PERSONAL_ACCESS_TOKEN", "").strip()
    if token:
        try:
            remote_url = get_remote_url("origin")
            authed_url = inject_token_into_url(remote_url, token)
            run(["git", "push", authed_url, f"HEAD:{branch}"])
        except ValueError as err:
            print(f"\nError: {err}")
            sys.exit(1)
    else:
        print(
            "Warning: GITHUB_PERSONAL_ACCESS_TOKEN is not set. "
            "Attempting push with existing credentials…"
        )
        result = run(["git", "push", "origin", branch], check=False)
        if result.returncode != 0:
            print(
                "\nPush failed. Set GITHUB_PERSONAL_ACCESS_TOKEN and retry:\n"
                "  export GITHUB_PERSONAL_ACCESS_TOKEN=your_token\n"
                "  python3 push.py --push-only"
            )
            sys.exit(result.returncode)


def main() -> None:
    args = sys.argv[1:]
    push_only = "--push-only" in args
    msg_parts = [a for a in args if a != "--push-only"]
    commit_message: str = " ".join(msg_parts).strip()

    print("── PrivacyShield push helper ──")
    run(["git", "status"])

    branch = get_current_branch()

    if push_only:
        unpushed = count_unpushed_commits(branch)
        if unpushed == 0 and not has_uncommitted_changes():
            print(f"\nAlready up to date with origin/{branch}. Nothing to push.")
            sys.exit(0)
        print(f"\nPush-only mode: pushing {unpushed} commit(s) to origin/{branch}…")
        do_push(branch)
    else:
        if not has_uncommitted_changes():
            # Nothing new to commit — check if there are unpushed commits to push
            unpushed = count_unpushed_commits(branch)
            if unpushed > 0:
                print(f"\nWorking tree clean but {unpushed} unpushed commit(s) found.")
                print(f"Pushing to origin/{branch}…")
                do_push(branch)
            else:
                print("\nNothing to commit and nothing to push. Already up to date.")
            sys.exit(0)

        # Stage all changes
        print("\nStaging all changes…")
        run(["git", "add", "."])

        if not commit_message:
            timestamp = datetime.now().strftime("%Y-%m-%d %H:%M")
            commit_message = f"chore: update [{timestamp}]"

        print(f"\nCommitting: \"{commit_message}\"")
        run(["git", "commit", "-m", commit_message])

        print(f"\nPushing to origin/{branch}…")
        do_push(branch)

    print(f"\nDone. Changes are on origin/{branch}.")


if __name__ == "__main__":
    main()
