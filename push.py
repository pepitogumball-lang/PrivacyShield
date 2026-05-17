#!/usr/bin/env python3
"""
push.py — Git helper for PrivacyShield.

Usage:
    python3 push.py [optional commit message]

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
        # SSH URL — cannot inject a token; caller must handle separately
        raise ValueError(
            f"Remote URL uses scheme '{parsed.scheme}'. "
            "Convert the remote to HTTPS or use an SSH key instead of a token."
        )
    authed = parsed._replace(netloc=f"{token}@{parsed.hostname}{':' + str(parsed.port) if parsed.port else ''}{parsed.path}")
    # netloc needs to be rebuilt cleanly
    host = parsed.hostname or ""
    port_part = f":{parsed.port}" if parsed.port else ""
    authed_netloc = f"{token}@{host}{port_part}"
    authed = parsed._replace(netloc=authed_netloc)
    return urlunparse(authed)


def has_changes() -> bool:
    result = run(["git", "status", "--porcelain"], capture=True)
    return bool(result.stdout.strip())


def main() -> None:
    commit_message: str = " ".join(sys.argv[1:]).strip()

    print("── PrivacyShield push helper ──")

    # 1. Status
    run(["git", "status"])

    if not has_changes():
        print("\nNothing to commit — working tree clean.")
        sys.exit(0)

    # 2. Stage all changes
    print("\nStaging all changes…")
    run(["git", "add", "."])

    # 3. Commit message
    if not commit_message:
        timestamp = datetime.now().strftime("%Y-%m-%d %H:%M")
        commit_message = f"chore: update [{timestamp}]"

    print(f"\nCommitting with message: \"{commit_message}\"")
    run(["git", "commit", "-m", commit_message])

    # 4. Determine branch and remote
    branch = get_current_branch()
    print(f"\nPushing to origin/{branch}…")

    token = os.environ.get("GITHUB_PERSONAL_ACCESS_TOKEN", "").strip()

    if token:
        try:
            remote_url = get_remote_url("origin")
            authed_url = inject_token_into_url(remote_url, token)
            # Push using the authed URL directly, without rewriting the stored remote
            run(["git", "push", authed_url, f"HEAD:{branch}"])
        except ValueError as err:
            print(f"\nError: {err}")
            sys.exit(1)
    else:
        # No token — attempt push with whatever credential helper is configured
        print(
            "Warning: GITHUB_PERSONAL_ACCESS_TOKEN is not set. "
            "Attempting push with existing credentials…"
        )
        result = run(["git", "push", "origin", branch], check=False)
        if result.returncode != 0:
            print(
                "\nPush failed. Set the GITHUB_PERSONAL_ACCESS_TOKEN environment variable "
                "and retry:\n  export GITHUB_PERSONAL_ACCESS_TOKEN=your_token\n  python3 push.py"
            )
            sys.exit(result.returncode)

    print(f"\nDone. Changes pushed to origin/{branch}.")


if __name__ == "__main__":
    main()
