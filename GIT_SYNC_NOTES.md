# Git sync notes — branding commit vs. origin/main

**Status as of 2026-07-16: NOT pushed.** Local `main` and `origin/main` have diverged;
pushing now would be rejected (non-fast-forward), and a blind merge/rebase would hit
conflicts in every file this commit touches. Nothing has been merged, rebased, or
pushed — this file just records the situation so it isn't lost.

## Where things stand

```
git status -sb
## main...origin/main [ahead 1, behind 9]
```

- **Local-only commit (`main`, not on `origin/main`):**
  `b22423a` — Replace default Android launcher icon and add StudyAI brand logo throughout app

- **Upstream-only commits (`origin/main`, not in local `main`)** — 9 commits, oldest first:

  | Commit | Summary |
  |---|---|
  | `0434d5a` | feat: add session_id to chat requests for multi-turn conversation memory |
  | `f3e9b89` | feat: add clear session button to ChatScreen with /clear-session API call |
  | `80fad40` | feat: add upload progress bar polling /upload-progress endpoint |
  | `50f09c1` | config: point backend to new Tailscale IP and port (100.95.45.33:8002) |
  | `2c0b886` | replace dashboard hub with persistent bottom navigation (Home, Chat, Quiz, Progress, Profile) |
  | `8baa063` | feat(android): wire Login and Signup to real backend /register and /login endpoints |
  | `a9f00c9` | feat(android): wire serverId through upload/chat/quiz, swap Progress screen to real /progress API |
  | `d3b93e5` | feat(android): scope docs-list to the signed-in user |
  | `fe5c177` | feat(android): let user pick how many quiz questions (3-20) |

## Why this needs care, not an auto-merge

The branding commit (`b22423a`) added the logo mark/banner to the *same* screens these
upstream commits restructured functionally. A textual merge would put conflict markers
in most of them:

| File | Branding commit did | Upstream did |
|---|---|---|
| `AppNavigation.kt` | — (untouched) | Routes reworked for persistent bottom nav (`2c0b886`) |
| `screens/DashboardScreen.kt` | Added `BrandLogoBadge` to the gradient header | Header logic gutted (113 lines removed) — most of the dashboard moved into the new bottom-nav scaffold (`2c0b886`, `d3b93e5`) |
| `screens/MainScaffold.kt` | doesn't exist locally | New file — the bottom-nav shell (`a9f00c9`) |
| `screens/HistoryScreen.kt` | doesn't exist locally | New file, 312 lines — Progress/history screen (`a9f00c9`) |
| `screens/ChatScreen.kt` | Swapped the emoji avatar for `BrandLogoMark` in the `TopAppBar` title | Session memory, clear-session button, serverId wiring (`0434d5a`, `f3e9b89`, `a9f00c9`, `2c0b886`) — ~130 lines changed |
| `screens/QuizScreen.kt` | Added `BrandLogoMark` next to the title | Question-count picker, serverId wiring, real quiz API (`fe5c177`, `a9f00c9`, `2c0b886`) — ~200 lines changed |
| `screens/UploadPdfScreen.kt` | Added `BrandLogoMark` next to the title | Upload-progress polling, serverId wiring (`80fad40`, `a9f00c9`) |
| `screens/LoginScreen.kt` | Replaced back-arrow row with a centered `BrandBanner` | Wired to real `/login` endpoint (`8baa063`) |
| `screens/SignupScreen.kt` | not modified by branding commit | Wired to real `/register` endpoint (`8baa063`) |
| `screens/ProfileScreen.kt` | Added `BrandLogoMark` next to the title | Adjusted for bottom-nav shell (`2c0b886`) |
| `network/ApiService.kt`, `network/NetworkConfig.kt`, `auth/UserManager.kt` | not touched | Real backend wiring, new Tailscale endpoint (multiple commits) |

None of these are simple line-level clashes — several (`DashboardScreen.kt`,
`ChatScreen.kt`, `QuizScreen.kt`) had their surrounding structure rewritten upstream, so
"resolve the conflict" really means "re-decide where the logo goes inside the new
bottom-nav layout," which needs a real look at the new screens, not a mechanical merge.

## Recommendation for later

When ready to reconcile (not done automatically, to avoid silently breaking either
side's work):

1. Pull `origin/main` in on its own first (fast-forward, no local commit in the way) so
   the new bottom-nav / real-backend code is in place and can be tested as-is.
2. Re-apply the branding pieces on top, screen by screen, against the *current* layout
   of each file (e.g. `BrandLogoBadge` likely belongs on the new `MainScaffold.kt` top
   bar rather than the old `DashboardScreen.kt` header; `BrandLogoMark` slots into
   whatever `TopAppBar`/title composable each screen now uses).
3. The shared component `ui/components/BrandLogo.kt` and the icon/drawable assets
   (`res/mipmap-*`, `res/drawable-xxhdpi/logo_*.png`, `res/values-v31/themes.xml`,
   `res/values/colors.xml` `ic_launcher_background`) are additive and don't conflict
   with anything upstream — those can carry over as-is.
4. Only then push.

No destructive git operations (rebase, reset, force-push) have been run. Local `main`
still contains only the original branding commit on top of the same base as
`origin/main`.
