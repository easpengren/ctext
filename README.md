# CText Reader (Android)

Android client for exploring Chinese Text Project content using the CTP API.

## Status

This repository currently contains initial workspace scaffolding and MCP configuration. The full Android app module implementation is the next step.

## Goals

- Build a professional Android app in Kotlin
- Consume CTP JSON API data from https://api.ctext.org
- Support reading text by CTP URN
- Handle API errors and rate limits cleanly

## CTP API Notes

Primary docs:
- https://ctext.org/tools/api
- https://ctext.org/plugins/apilist

Useful functions:
- `getstatus` - check access/auth status
- `readlink` - convert URL to URN
- `getlink` - convert URN to direct URL
- `gettext` - fetch title/fulltext/subsections

Error model highlights:
- `ERR_REQUEST_LIMIT`
- `ERR_REQUIRES_AUTHENTICATION`
- `ERR_INVALID_URN`
- `ERR_INVALID_FUNCTION`

## Local Setup

Prerequisites:
- Android Studio (latest stable)
- JDK 17+
- Android SDK
- Git

Clone:

```bash
git clone https://github.com/easpengren/ctext.git
cd ctext
```

Open in Android Studio and sync Gradle.

## MCP Configuration

Mempalace MCP config is included in:
- `mcp.json`
- `.vscode/mcp.json`

If your local paths differ, update command and args accordingly.

## Next Build Steps

1. Create `app` module and Compose UI shell
2. Add Retrofit/OkHttp + Kotlin Serialization
3. Implement CTP API service and models
4. Add repository + use cases + ViewModel state
5. Add tests for API parsing and error handling

## Repository

- Owner: `easpengren`
- Repo: `ctext`
