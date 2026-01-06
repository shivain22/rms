# Prettier Setup and Usage

## Summary

Prettier has been configured and run on all modified files. This document explains how to prevent formatting issues in the future.

## Current Configuration

Prettier is configured in `.prettierrc` with the following settings:

- **Print Width:** 140 characters
- **Single Quotes:** true
- **Tab Width:** 2 spaces
- **Arrow Parens:** avoid
- **Plugins:** prettier-plugin-packagejson, prettier-plugin-java

## Running Prettier

### Format All Files

```bash
npm run prettier:format
```

### Check Formatting (Without Fixing)

```bash
npm run prettier:check
```

### Format Specific Files

```bash
npx prettier --write "path/to/file.tsx"
```

## Files Formatted

The following files have been formatted:

- ✅ `src/main/webapp/app/shared/layout/footer/footer.tsx`
- ✅ `src/main/webapp/app/shared/layout/sidebar/sidebar.tsx`
- ✅ `src/main/webapp/app/shared/layout/header/header.tsx`
- ✅ `src/main/webapp/app/app.tsx`
- ✅ `src/main/webapp/app/app.scss`

## Preventing Future Issues

### Option 1: Run Prettier Before Committing

Always run Prettier before committing changes:

```bash
npm run prettier:format
```

### Option 2: Use lint-staged (Recommended)

The project already has `lint-staged` configured. It should automatically format files on commit. If it's not working, check:

1. **Husky is installed:**

   ```bash
   npm run prepare
   ```

2. **lint-staged configuration** in `.lintstagedrc.cjs` should include Prettier

### Option 3: IDE Integration

Configure your IDE (VS Code, WebStorm, etc.) to:

- Format on save
- Use Prettier as the default formatter
- Auto-format on paste

### VS Code Settings Example

Add to `.vscode/settings.json`:

```json
{
  "editor.defaultFormatter": "esbenp.prettier-vscode",
  "editor.formatOnSave": true,
  "editor.formatOnPaste": true,
  "[typescript]": {
    "editor.defaultFormatter": "esbenp.prettier-vscode"
  },
  "[typescriptreact]": {
    "editor.defaultFormatter": "esbenp.prettier-vscode"
  },
  "[javascript]": {
    "editor.defaultFormatter": "esbenp.prettier-vscode"
  },
  "[json]": {
    "editor.defaultFormatter": "esbenp.prettier-vscode"
  }
}
```

## Common Prettier Issues

### Issue: "Prettier keeps reformatting my code"

**Solution:** This is expected behavior. Prettier ensures consistent formatting across the codebase.

### Issue: "Prettier conflicts with ESLint"

**Solution:** The project uses `eslint-config-prettier` to disable ESLint rules that conflict with Prettier.

### Issue: "Prettier doesn't format on save"

**Solution:**

1. Install Prettier extension in your IDE
2. Set Prettier as default formatter
3. Enable format on save

## Pre-commit Hook

The project uses Husky for Git hooks. The pre-commit hook should run lint-staged, which includes Prettier. To verify:

```bash
cat .husky/pre-commit
```

If the hook doesn't exist or isn't working:

```bash
npm run prepare
```

## Best Practices

1. **Always run Prettier before committing:**

   ```bash
   npm run prettier:format
   ```

2. **Check formatting in CI/CD:**
   The `prettier:check` command can be used in CI to fail builds if files aren't formatted.

3. **Format on save:**
   Configure your IDE to format on save to prevent issues.

4. **Use consistent settings:**
   Don't override Prettier settings in individual files unless absolutely necessary.

## Troubleshooting

### Prettier not running automatically

- Check if Husky is installed: `npm run prepare`
- Verify lint-staged configuration
- Check Git hooks: `ls -la .git/hooks/`

### Formatting conflicts

- Run `npm run prettier:format` to fix all files
- Commit the formatted files
- If issues persist, check `.prettierrc` configuration

### IDE not formatting

- Install Prettier extension
- Set as default formatter
- Enable format on save
- Restart IDE

## Additional Resources

- [Prettier Documentation](https://prettier.io/docs/en/)
- [Prettier VS Code Extension](https://marketplace.visualstudio.com/items?itemName=esbenp.prettier-vscode)
- [lint-staged Documentation](https://github.com/okonet/lint-staged)
