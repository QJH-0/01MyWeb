#!/usr/bin/env node
/**
 * Cursor hook adapter
 *
 * These hooks are executed as standalone node processes by Cursor.
 * This file provides small shared utilities:
 * - readStdin(): read Cursor-provided JSON payload from stdin
 * - hookEnabled(): tier/profile gate (minimal/standard/strict) + best-effort config check
 * - transformToClaude(): light payload transformation hook (kept permissive)
 * - runExistingHook(): execute another hook script if it exists (and avoid self-recursion)
 */

const fs = require('fs');
const path = require('path');
const { spawnSync } = require('child_process');

const HOOKS_JSON_PATH = path.join(__dirname, '..', 'hooks.json');

function safeReadJson(filePath) {
  try {
    if (!fs.existsSync(filePath)) return null;
    const raw = fs.readFileSync(filePath, 'utf8');
    return JSON.parse(raw);
  } catch {
    return null;
  }
}

let hooksConfigCache = undefined;
function getHooksConfig() {
  if (hooksConfigCache !== undefined) return hooksConfigCache;
  hooksConfigCache = safeReadJson(HOOKS_JSON_PATH) || {};
  return hooksConfigCache;
}

function getProfile() {
  // Default to the strictest profile so hooks stay enabled when no env is provided.
  return (
    process.env.CURSOR_HOOK_PROFILE ||
    process.env.HOOK_PROFILE ||
    process.env.ECC_HOOK_PROFILE ||
    'strict'
  );
}

function mapHookNameToConfigEvent(hookName) {
  if (!hookName || typeof hookName !== 'string') return null;
  if (hookName.startsWith('session:start')) return 'sessionStart';
  if (hookName.startsWith('session:end')) return 'sessionEnd';
  if (hookName.startsWith('pre:bash:') || hookName.startsWith('pre:')) return 'beforeShellExecution';
  if (hookName.startsWith('post:bash:') || hookName.startsWith('post:')) return 'afterShellExecution';
  if (hookName.startsWith('stop:')) return 'stop';
  return null;
}

function hookEnabled(hookName, tiers) {
  const profile = getProfile();

  // Tier/profile gating
  if (Array.isArray(tiers) && tiers.length > 0 && !tiers.includes(profile)) {
    return false;
  }

  // Best-effort config check: ensure the mapped top-level event exists.
  const cfg = getHooksConfig();
  const mappedEvent = mapHookNameToConfigEvent(hookName);
  if (mappedEvent && Array.isArray(cfg[mappedEvent]) && cfg[mappedEvent].length === 0) {
    return false;
  }

  return true;
}

function readStdin() {
  return new Promise(resolve => {
    // In Cursor hooks, stdin usually contains a JSON payload; treat empty as '{}'.
    let buf = '';
    try {
      process.stdin.setEncoding('utf8');
    } catch {}

    process.stdin.on('data', chunk => {
      buf += String(chunk);
    });
    process.stdin.on('end', () => resolve(buf));
    process.stdin.on('error', () => resolve(buf));
  });
}

function transformToClaude(input, extra = {}) {
  // Keep this permissive: downstream hooks (if present) can decide how to interpret.
  const base = input && typeof input === 'object' ? input : {};
  if (extra && typeof extra === 'object') {
    // Merge tool_input in a stable way when provided.
    if (extra.tool_input) {
      return {
        ...base,
        ...extra,
        tool_input: { ...(base.tool_input || {}), ...extra.tool_input },
      };
    }
    return { ...base, ...extra };
  }
  return { ...base };
}

function runExistingHook(targetFile, payload) {
  const hooksDir = __dirname;
  const targetPath = path.join(hooksDir, targetFile);

  // Avoid hard failures for missing sub-hooks.
  if (!fs.existsSync(targetPath)) return;

  // Avoid recursion when a hook delegates to itself.
  const currentPath = process.argv[1] ? path.resolve(process.argv[1]) : null;
  if (currentPath && path.resolve(targetPath) === currentPath) return;

  const stdin = typeof payload === 'string' ? payload : JSON.stringify(payload ?? {});
  const res = spawnSync('node', [targetPath], {
    input: stdin,
    encoding: 'utf8',
    stdio: ['pipe', 'pipe', 'pipe'],
    env: process.env,
  });

  // Keep the main hook output stable; surface errors only when there is something to debug.
  if (res.status !== 0 && (res.stderr || '').trim()) {
    console.error(`[ECC] Hook failed: ${targetFile}`);
    console.error(res.stderr.trim());
  }
}

module.exports = {
  readStdin,
  hookEnabled,
  transformToClaude,
  runExistingHook,
};

