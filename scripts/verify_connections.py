#!/usr/bin/env python3
"""Verify connectivity to Odoo and OpenAI using environment variables.

Features:
- Optional .env loading (no third-party dependency)
- Clear validation errors for required variables
- Request timeouts for Odoo/OpenAI checks
- Safe logging (redacts secrets)
"""

from __future__ import annotations

import argparse
import json
import os
import socket
import sys
import urllib.error
import urllib.request
import xmlrpc.client
from dataclasses import dataclass
from pathlib import Path
from urllib.parse import urljoin


@dataclass
class CheckResult:
    service: str
    ok: bool
    message: str


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Verify Odoo/OpenAI connectivity")
    parser.add_argument("--env-file", default=".env", help="Path to .env file (default: .env)")
    parser.add_argument("--skip-env-file", action="store_true", help="Skip loading .env file")
    parser.add_argument("--timeout", type=float, default=20.0, help="Network timeout in seconds")
    parser.add_argument("--json", action="store_true", help="Force JSON output (default behavior)")
    return parser.parse_args()


def load_env_file(path: str) -> None:
    env_path = Path(path)
    if not env_path.exists():
        return

    for raw in env_path.read_text(encoding="utf-8").splitlines():
        line = raw.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, value = line.split("=", 1)
        key = key.strip()
        value = value.strip().strip('"').strip("'")
        os.environ.setdefault(key, value)


def require_env(name: str) -> str:
    value = os.getenv(name, "").strip()
    if not value:
        raise RuntimeError(f"Missing required environment variable: {name}")
    return value


def redact_secret(value: str) -> str:
    if len(value) <= 8:
        return "***"
    return f"{value[:4]}...{value[-4:]}"


class TimeoutTransport(xmlrpc.client.SafeTransport):
    def __init__(self, timeout: float) -> None:
        super().__init__()
        self._timeout = timeout

    def make_connection(self, host):  # type: ignore[override]
        connection = super().make_connection(host)
        connection.timeout = self._timeout
        return connection


def check_odoo(timeout: float) -> CheckResult:
    url = require_env("ODOO_URL").rstrip("/")
    db = require_env("ODOO_DB")
    username = require_env("ODOO_USERNAME")
    api_key = require_env("ODOO_API_KEY")

    common_endpoint = urljoin(url + "/", "xmlrpc/2/common")
    transport = TimeoutTransport(timeout=timeout)
    common = xmlrpc.client.ServerProxy(common_endpoint, transport=transport, allow_none=True)

    uid = common.authenticate(db, username, api_key, {})
    if not uid:
        return CheckResult("Odoo", False, "Authentication failed. Check DB / username / API key.")

    return CheckResult(
        "Odoo",
        True,
        (
            f"Connected successfully (uid={uid}, db={db}, username={username}, "
            f"api_key={redact_secret(api_key)})."
        ),
    )


def check_openai(timeout: float) -> CheckResult:
    api_key = require_env("OPENAI_API_KEY")
    req = urllib.request.Request(
        "https://api.openai.com/v1/models",
        headers={"Authorization": f"Bearer {api_key}"},
        method="GET",
    )

    try:
        with urllib.request.urlopen(req, timeout=timeout) as res:
            body = res.read().decode("utf-8")
            payload = json.loads(body)
    except urllib.error.HTTPError as exc:
        detail = exc.read().decode("utf-8", errors="ignore")[:300]
        return CheckResult("OpenAI", False, f"Request failed ({exc.code}): {detail}")

    model_count = len(payload.get("data", []))
    return CheckResult(
        "OpenAI",
        True,
        f"Connected successfully ({model_count} models visible, key={redact_secret(api_key)}).",
    )


def run_checks(timeout: float) -> list[CheckResult]:
    checks: list[CheckResult] = []

    for name, func in (("Odoo", check_odoo), ("OpenAI", check_openai)):
        try:
            checks.append(func(timeout))
        except (socket.timeout, TimeoutError):
            checks.append(CheckResult(name, False, f"Request timed out after {timeout}s"))
        except Exception as exc:  # noqa: BLE001
            checks.append(CheckResult(name, False, str(exc)))

    return checks


def main() -> int:
    args = parse_args()

    if not args.skip_env_file:
        load_env_file(args.env_file)

    results = run_checks(timeout=args.timeout)
    print(
        json.dumps(
            [{"service": r.service, "ok": r.ok, "message": r.message} for r in results],
            ensure_ascii=False,
            indent=2,
        )
    )
    return 0 if all(r.ok for r in results) else 1


if __name__ == "__main__":
    sys.exit(main())
