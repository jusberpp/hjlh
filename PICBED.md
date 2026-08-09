# GitHub Image Hosting

This repository is prepared as a lightweight image host.

## Folder Convention

Put images under:

```text
images/YYYY/MM/file-name.webp
```

Example:

```text
images/2026/08/avatar.webp
```

## Recommended Image Links

Replace `FILE_PATH` with the path of your uploaded image.

### jsDelivr CDN

```text
https://cdn.jsdelivr.net/gh/jusberpp/hjlh@main/FILE_PATH
```

Example:

```text
https://cdn.jsdelivr.net/gh/jusberpp/hjlh@main/images/2026/08/avatar.webp
```

### GitHub Raw

```text
https://raw.githubusercontent.com/jusberpp/hjlh/main/FILE_PATH
```

Example:

```text
https://raw.githubusercontent.com/jusberpp/hjlh/main/images/2026/08/avatar.webp
```

## More Stable Production Links

For long-lived blog posts or documentation, create a Git tag after uploading a stable batch of images, then use the tag in the URL:

```text
https://cdn.jsdelivr.net/gh/jusberpp/hjlh@v2026.08/FILE_PATH
```

Tagged links are safer for long-term references than `@main`, because they will not change when the main branch changes.

## PicGo Settings

Use these settings if you upload with PicGo:

```text
Repo: jusberpp/hjlh
Branch: main
Path: images/
Custom domain: https://cdn.jsdelivr.net/gh/jusberpp/hjlh@main
```

## Stability Rules

- Keep this repository public if images need to be loaded by blogs, Markdown notes, or websites.
- Prefer compressed `.webp`, `.jpg`, or `.png` images.
- Keep individual images below 1 MB when possible.
- Avoid videos, archives, installers, or very large original photos.
- Do not use Git LFS for image hosting unless you understand its storage and bandwidth quota.
- Keep a local backup of important original images.
