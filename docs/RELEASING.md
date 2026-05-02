# Releasing

Workflow file: `.github/workflows/android.yml`

## How the pipeline works

| Trigger             | What runs                                                       |
| ------------------- | --------------------------------------------------------------- |
| Push / PR to `main` | Unit tests + debug APK build (artifact kept 7 days)             |
| `v*.*.*` tag push   | Release APK build -> optional signing -> GitHub Release created |

## Creating a release

```bash
git tag v1.0.0
git push origin v1.0.0
```

Release notes are auto-generated from commits since the previous tag.

## Signing setup (one-time)

Without signing secrets the pipeline publishes an **unsigned APK**. Android will not install an unsigned APK, so add the signing secrets before using GitHub Releases as an install source.

### 1. Generate a keystore

```bash
keytool -genkey -v -keystore wandercraft.jks \
  -alias wandercraft -keyalg RSA -keysize 2048 -validity 10000
```

Keep `wandercraft.jks` somewhere safe - **never commit it**.

### 2. Add secrets to GitHub

Go to **repo -> Settings -> Secrets and variables -> Actions** and add:

| Secret              | Value                                   |
| ------------------- | --------------------------------------- |
| `KEYSTORE_BASE64`   | base64-encoded `.jks` file              |
| `KEYSTORE_PASSWORD` | keystore password                       |
| `KEY_ALIAS`         | alias chosen above (e.g. `wandercraft`) |
| `KEY_PASSWORD`      | key password                            |
| `MAPS_API_KEY`      | Google Maps API key (can be blank)      |

Generate `KEYSTORE_BASE64` with a single-line value:

```bash
# macOS
base64 < wandercraft.jks | tr -d '\n'

# Linux (GNU coreutils)
base64 -w0 wandercraft.jks
```

Once set, the next tag push will produce a signed `wandercraft-v<tag>.apk` in the GitHub Release.

## Re-releasing the current tag

If `v1.0.0` already exists and was released without signing secrets, you have two practical options after adding the secrets:

### Option A: Re-run the existing release workflow

1. Open **GitHub -> Actions -> Android CI**.
2. Open the run that was triggered by `v1.0.0`.
3. Use **Re-run all jobs**.

This rebuilds the same commit with the new secrets. Because the signed asset is named `wandercraft-v1.0.0.apk`, you may end up with both the old unsigned asset and the new signed asset on the same release. Delete the unsigned asset from the GitHub Release page if you want a clean release.

### Option B: Delete and recreate the same tag

Use this if you want the release page recreated from scratch:

```bash
git tag -d v1.0.0
git push origin :refs/tags/v1.0.0
git tag v1.0.0 1c65e33
git push origin v1.0.0
```

That re-triggers the workflow for the same commit currently tagged in this repo.

## When to bump the version instead

If you want this to be a new public release rather than a corrected upload of the same build, bump `versionCode` and `versionName` first, then create a new tag such as `v1.0.1`.

## Multiple GitHub accounts

Per-path credential storage lets different accounts coexist:

```bash
git config --global credential.useHttpPath true
# push once with token embedded to register credentials
git push https://<username>:<token>@github.com/<owner>/<repo>.git HEAD:main
```

Subsequent pushes via `git push origin main` will use the cached token.
