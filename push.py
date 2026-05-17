#!/usr/bin/env python3
"""
push.py — PrivacyShield GitHub push helper.

Usage
-----
  python3 push.py                     # auto-commit any pending changes, then push
  python3 push.py "commit message"    # same, with a custom commit message
  python3 push.py --push-only         # push already-committed changes, no commit step

Authentication
--------------
  Reads GITHUB_PERSONAL_ACCESS_TOKEN from the environment.
  The token is injected into the remote HTTPS URL at push time only
  and is never written to disk or printed to stdout.
"""

import os
import subprocess
import sys
from datetime import datetime
from urllib.parse import urlparse, urlunparse


# ── helpers ────────────────────────────────────────────────────────────────

def git(*args, capture: bool = False, check: bool = True) -> subprocess.CompletedProcess:
    return subprocess.run(
        ["git", *args],
        capture_output=capture,
        text=True,
        check=check,
    )


def current_branch() -> str:
    return git("rev-parse", "--abbrev-ref", "HEAD", capture=True).stdout.strip()


def remote_url(remote: str = "origin") -> str:
    return git("remote", "get-url", remote, capture=True).stdout.strip()


def authed_url(url: str, token: str) -> str:
    """Embed the PAT as the username in an HTTPS remote URL."""
    p = urlparse(url)
    if p.scheme not in ("http", "https"):
        raise ValueError(
            f"Remote uses '{p.scheme}' — only HTTPS is supported for token auth. "
            "Switch the remote to an HTTPS URL."
        )
    netloc = f"{token}@{p.hostname}"
    if p.port:
        netloc += f":{p.port}"
    return urlunparse(p._replace(netloc=netloc))


def has_local_changes() -> bool:
    return bool(git("status", "--porcelain", capture=True).stdout.strip())


def unpushed_count(branch: str) -> int:
    r = git("rev-list", f"origin/{branch}..HEAD", "--count", capture=True, check=False)
    try:
        return int(r.stdout.strip())
    except ValueError:
        return 0


# ── main ───────────────────────────────────────────────────────────────────

def main() -> None:
    argv = sys.argv[1:]
    push_only = "--push-only" in argv
    msg_args  = [a for a in argv if a != "--push-only"]
    msg       = " ".join(msg_args).strip()

    token = os.environ.get("GITHUB_PERSONAL_ACCESS_TOKEN", "").strip()
    if not token:
        print("ERROR: GITHUB_PERSONAL_ACCESS_TOKEN is not set in the environment.")
        print("  export GITHUB_PERSONAL_ACCESS_TOKEN=ghp_yourtoken")
        sys.exit(1)

    branch = current_branch()
    url    = remote_url()
    push_url = authed_url(url, token)

    print(f"── PrivacyShield push → origin/{branch} ──")
    git("status")

    # ── commit step (skipped with --push-only) ───────────────────────────
    if not push_only:
        if has_local_changes():
            print("\nStaging all changes…")
            git("add", ".")
            if not msg:
                msg = f"chore: update [{datetime.now().strftime('%Y-%m-%d %H:%M')}]"
            print(f'Committing: "{msg}"')
            git("commit", "-m", msg)
        else:
            ahead = unpushed_count(branch)
            if ahead == 0:
                print("\nNothing to commit and nothing to push — already up to date.")
                sys.exit(0)
            print(f"\nWorking tree clean; {ahead} unpushed commit(s) found.")

    # ── push ─────────────────────────────────────────────────────────────
    ahead = unpushed_count(branch)
    if push_only and ahead == 0 and not has_local_changes():
        print(f"\nAlready up to date with origin/{branch}.")
        sys.exit(0)

    print(f"\nPushing to origin/{branch}…")
    result = subprocess.run(
        ["git", "push", push_url, f"HEAD:{branch}"],
        text=True,
        check=False,
    )
    if result.returncode != 0:
        print("\nPush failed. Check your token permissions and try again.")
        sys.exit(result.returncode)

    print(f"\n✓ Pushed successfully to origin/{branch}.")


if __name__ == "__main__":
    main()
