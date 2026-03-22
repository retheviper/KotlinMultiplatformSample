#!/usr/bin/env python3

import argparse
import json
import os
import sys
import urllib.error
import urllib.parse
import urllib.request
from pathlib import Path


DEFAULT_URL = "http://localhost:8080/mcp"
DEFAULT_PROTOCOL_VERSION = "2025-03-26"


def main() -> int:
    parser = build_parser()
    args = parser.parse_args()

    try:
        if args.command == "init":
            payload = initialize(args.url)
            print_json(payload)
            return 0

        if args.command == "tools":
            payload = tools_list(args.url)
            print_json(payload)
            return 0

        if args.command == "call":
            arguments = parse_arguments(args.arguments, args.arguments_json)
            payload = tools_call(args.url, args.tool_name, arguments)
            print_json(payload)
            return 0

        parser.print_help()
        return 1
    except RuntimeError as exc:
        print(f"error: {exc}", file=sys.stderr)
        return 1


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        description="Manual CLI for testing the messaging MCP server."
    )
    parser.add_argument(
        "--url",
        default=DEFAULT_URL,
        help=f"MCP server URL. Default: {DEFAULT_URL}",
    )

    subparsers = parser.add_subparsers(dest="command")

    subparsers.add_parser("init", help="Initialize an MCP session and print the server response.")
    subparsers.add_parser("tools", help="List tools from the MCP server.")

    call_parser = subparsers.add_parser("call", help="Call one MCP tool.")
    call_parser.add_argument("tool_name", help="Tool name to call.")
    call_parser.add_argument(
        "--arg",
        action="append",
        default=[],
        dest="arguments",
        help="Tool argument in key=value form. Repeat for multiple entries.",
    )
    call_parser.add_argument(
        "--arguments-json",
        help="Raw JSON object string for tool arguments.",
    )

    return parser


def initialize(url: str) -> dict:
    request_body = {
        "jsonrpc": "2.0",
        "id": 1,
        "method": "initialize",
        "params": {
            "protocolVersion": DEFAULT_PROTOCOL_VERSION,
            "capabilities": {},
            "clientInfo": {
                "name": "kotlin-multiplatform-sample-cli",
                "version": "1.0.0",
            },
        },
    }
    headers, payload = post_json(url, request_body)
    session_id = header_value(headers, "Mcp-Session-Id")
    protocol_version = payload.get("result", {}).get("protocolVersion", DEFAULT_PROTOCOL_VERSION)
    save_session(url, session_id=session_id, protocol_version=protocol_version)
    return payload


def tools_list(url: str) -> dict:
    session = ensure_session(url)
    notify_initialized(url, session)
    _, payload = post_json(
        url,
        {
            "jsonrpc": "2.0",
            "id": 2,
            "method": "tools/list",
            "params": {},
        },
        session_headers(session),
    )
    return payload


def tools_call(url: str, tool_name: str, arguments: dict) -> dict:
    session = ensure_session(url)
    notify_initialized(url, session)
    _, payload = post_json(
        url,
        {
            "jsonrpc": "2.0",
            "id": 3,
            "method": "tools/call",
            "params": {
                "name": tool_name,
                "arguments": arguments,
            },
        },
        session_headers(session),
    )
    return payload


def notify_initialized(url: str, session: dict) -> None:
    if session.get("initialized"):
        return
    post_json(
        url,
        {
            "jsonrpc": "2.0",
            "method": "notifications/initialized",
        },
        session_headers(session),
        expected_statuses={202},
    )
    session["initialized"] = True
    save_session(
        url,
        session_id=session.get("session_id"),
        protocol_version=session.get("protocol_version", DEFAULT_PROTOCOL_VERSION),
        initialized=True,
    )


def post_json(url: str, payload: dict, extra_headers: dict | None = None, expected_statuses: set[int] | None = None):
    headers = {
        "Content-Type": "application/json",
        "Accept": "application/json, text/event-stream",
    }
    if extra_headers:
        headers.update(extra_headers)

    request = urllib.request.Request(
        url,
        data=json.dumps(payload).encode("utf-8"),
        headers=headers,
        method="POST",
    )

    try:
        with urllib.request.urlopen(request) as response:
            status = response.status
            body = response.read().decode("utf-8")
            response_headers = {key.lower(): value for key, value in response.headers.items()}
    except urllib.error.HTTPError as exc:
        body = exc.read().decode("utf-8", errors="replace")
        raise RuntimeError(f"HTTP {exc.code}: {body}") from exc
    except urllib.error.URLError as exc:
        raise RuntimeError(f"connection failed: {exc.reason}") from exc

    allowed_statuses = expected_statuses or {200}
    if status not in allowed_statuses:
        raise RuntimeError(f"unexpected HTTP status {status}: {body}")

    if body.strip():
        return response_headers, json.loads(body)
    return response_headers, {}


def parse_arguments(argument_pairs: list[str], arguments_json: str | None) -> dict:
    if arguments_json:
        parsed = json.loads(arguments_json)
        if not isinstance(parsed, dict):
            raise RuntimeError("--arguments-json must be a JSON object")
        return parsed

    result = {}
    for pair in argument_pairs:
        if "=" not in pair:
            raise RuntimeError(f"invalid --arg value '{pair}', expected key=value")
        key, raw_value = pair.split("=", 1)
        result[key] = coerce_value(raw_value)
    return result


def coerce_value(raw_value: str):
    lowered = raw_value.lower()
    if lowered == "true":
        return True
    if lowered == "false":
        return False
    if lowered == "null":
        return None

    try:
        return json.loads(raw_value)
    except json.JSONDecodeError:
        return raw_value


def session_headers(session: dict) -> dict:
    headers = {
        "MCP-Protocol-Version": session.get("protocol_version", DEFAULT_PROTOCOL_VERSION),
    }
    session_id = session.get("session_id")
    if session_id:
        headers["MCP-Session-Id"] = session_id
    return headers


def header_value(headers: dict, name: str) -> str | None:
    return headers.get(name.lower())


def ensure_session(url: str) -> dict:
    session = load_session(url)
    if session is None:
        initialize(url)
        session = load_session(url)
    if session is None:
        raise RuntimeError("failed to initialize MCP session")
    return session


def session_file(url: str) -> Path:
    cache_dir = Path(".mcp-cli")
    cache_dir.mkdir(exist_ok=True)
    safe_name = urllib.parse.quote(url, safe="")
    return cache_dir / f"{safe_name}.json"


def save_session(url: str, session_id: str | None, protocol_version: str, initialized: bool = False) -> None:
    path = session_file(url)
    payload = {
        "session_id": session_id,
        "protocol_version": protocol_version,
        "initialized": initialized,
    }
    path.write_text(json.dumps(payload, indent=2) + os.linesep, encoding="utf-8")


def load_session(url: str) -> dict | None:
    path = session_file(url)
    if not path.exists():
        return None
    return json.loads(path.read_text(encoding="utf-8"))


def print_json(payload: dict) -> None:
    print(json.dumps(payload, indent=2, ensure_ascii=True))


if __name__ == "__main__":
    raise SystemExit(main())
