"""Type-checks the Python examples against the SDK checkout and runs the webhook self-test.

    python check.py

Installs the SDK from $SDK_REPOS_DIR/quickbooks-desktop-python (default: <examples root>/sdk)
into python/.venv with the mise-pinned uv, runs mypy --strict on every example and runs
`async_webhooks.py --self-test`. It makes no API calls.
"""

from __future__ import annotations

import os
import subprocess
from pathlib import Path

HERE = Path(__file__).resolve().parent
MYPY = "mypy==1.19.1"


def run(args: list[str], env: dict[str, str] | None = None) -> None:
    print("+ " + " ".join(args), flush=True)
    subprocess.run(args, cwd=HERE, env=env, check=True)


def main() -> None:
    repos = Path(os.environ.get("SDK_REPOS_DIR") or HERE.parent / "sdk")
    sdk = repos / "quickbooks-desktop-python"
    if not (sdk / "pyproject.toml").is_file():
        raise SystemExit(f"Python SDK not found at {sdk}. Clone it there or set SDK_REPOS_DIR.")
    venv = HERE / ".venv"
    python = venv / ("Scripts/python.exe" if os.name == "nt" else "bin/python")
    run(["uv", "venv", "--quiet", "--allow-existing", str(venv)])
    run(
        [
            "uv",
            "pip",
            "install",
            "--quiet",
            "--python",
            str(python),
            "--reinstall-package",
            "desktopaccountingapi-quickbooks-desktop",
            str(sdk),
            MYPY,
        ]
    )
    examples = sorted(p.name for p in HERE.glob("*.py"))
    run([str(python), "-m", "mypy", "--strict", *examples])
    env = dict(os.environ)
    env["DAAPI_WEBHOOK_SECRET"] = "whsec_ZXhhbXBsZXMtc2VsZi10ZXN0LXNlY3JldC0zMmJ5dGVz"
    run([str(python), "async_webhooks.py", "--self-test"], env=env)
    print("python examples ok")


if __name__ == "__main__":
    main()
