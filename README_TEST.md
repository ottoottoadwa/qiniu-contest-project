# PR Review Bot - Auto Review Mode

## 🎯 What This Does

**Automatic PR Review** - No manual commands needed!

```
User creates/updates PR → Bot automatically reviews → Bot posts results
```

---

## Quick Start (2 Steps)

### Step 1: Set Environment Variables

```bash
set aliQwen_api=your_aliyun_api_key
set GITHUB_TOKEN=your_github_token
```

### Step 2: Start Application

**Double click `start-demo.bat`**

That's it! The bot is now running and will automatically review any PR.

---

## How It Works

### Automatic Triggers

The bot automatically reviews when:
- ✅ New PR is created (`opened` event)
- ✅ PR is updated with new commits (`synchronize` event)

### Manual Trigger (Optional)

You can also manually trigger by commenting `/review` on any PR.

---

## Testing

### Option 1: Test with Real PR (Recommended)

1. Start application: `start-demo.bat`
2. Create a new PR on GitHub
3. Bot will automatically review it!

### Option 2: Simulate PR Event (For Demo)

1. Start application: `start-demo.bat`
2. Run test script: `run-test.bat`
3. Script simulates a PR opened event

---

## Fast Demo Mode

- ✅ Analyzes only 3 files (faster)
- ✅ Skips detailed suggestions
- ✅ Review completes in 30s-1min

**Perfect for recording demos!**

---

## Recording Demo (5 Minutes)

### Scene 1: Show Setup (1min)
- Show environment variables
- Explain auto-review feature
- Start application

### Scene 2: Create PR (1min)
- Create a new branch
- Make some changes
- Create Pull Request

### Scene 3: Monitor (2min)
- Show application logs
- Show real-time progress
- Explain analysis process

### Scene 4: View Results (1min)
- Open GitHub PR
- Show bot comment
- Show risk items and suggestions

---

## Files

| File | Purpose |
|------|---------|
| `start-demo.bat` | Start bot (auto-review enabled) |
| `run-test.bat` | Simulate PR event (for testing) |
| `debug-test.bat` | Debug script |

---

## Configuration

### Enable/Disable Auto-Review

Edit `WebhookController.java`:

```java
// Auto-review enabled (default)
if ("pull_request".equals(eventType)) {
    boolean handled = webhookService.handlePullRequest(payload);
    ...
}

// To disable, comment out the above block
```

### Change Trigger Events

Edit `WebhookService.handlePullRequest()`:

```java
// Current: triggers on "opened" and "synchronize"
if (!"opened".equals(action) && !"synchronize".equals(action)) {
    return false;
}

// To trigger only on new PRs:
if (!"opened".equals(action)) {
    return false;
}
```

---

## Production Deployment

### With ngrok (Quick)

```bash
# Terminal 1: Start application
start-demo.bat

# Terminal 2: Expose to internet
ngrok http 8080
```

### Configure GitHub Webhook

1. Go to repository Settings → Webhooks
2. Add webhook:
   - **URL**: `https://your-domain/api/webhook/github`
   - **Content type**: `application/json`
   - **Events**: Select `Pull requests`
3. Save

Now every PR will be automatically reviewed!

---

## Troubleshooting

### Bot doesn't review automatically

1. Check application logs for errors
2. Verify GITHUB_TOKEN has `repo` permission
3. Check webhook delivery in GitHub Settings

### Review is too slow

1. Use demo mode (already enabled in `start-demo.bat`)
2. Or reduce `max-files` in `application-demo.yml`

### Bot posts multiple comments

This happens if you trigger review multiple times. Each trigger creates a new review.

---

## After Testing

```bash
git add -A
git commit -m "feat: add automatic PR review on PR open/update"
git push origin master
```

---

## Summary

- ✅ **One command**: Just run `start-demo.bat`
- ✅ **Automatic**: Reviews every PR automatically
- ✅ **Fast**: 30s-1min in demo mode
- ✅ **Simple**: No manual commands needed

Perfect for demos and production use!
