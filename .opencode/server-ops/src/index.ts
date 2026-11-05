import { Plugin } from "@opencode/plugin"
import { Client } from "ssh2"
import { readFile, stat } from "node:fs/promises"
import { homedir } from "node:os"
import { isAbsolute, join, resolve } from "node:path"

type ServerConfig = {
  label?: string
  host: string
  port: number
  username: string
  password?: string
  privateKeyPath?: string
  passphrase?: string
  defaultCwd?: string
  readyTimeout?: number
  maxOutputChars?: number
}

type ExecResult = {
  stdout: string
  stderr: string
  code: number | null
  signal?: string
  durationMs: number
}

const CONFIG_CANDIDATES = [
  process.env.OPENCODE_SERVER_OPS_CONFIG,
  join(homedir(), ".config", "opencode", "server-ops.json"),
  join(homedir(), ".opencode", "server-ops.json"),
]

const DEFAULT_MAX_OUTPUT = 200_000
const DEFAULT_TIMEOUT_MS = 120_000

function expandHome(path: string) {
  if (path === "~") return homedir()
  if (path.startsWith("~/") || path.startsWith("~\\")) return join(homedir(), path.slice(2))
  return path
}

async function readJsonConfig(): Promise<Partial<ServerConfig> | undefined> {
  for (const candidate of CONFIG_CANDIDATES) {
    if (!candidate) continue
    try {
      const raw = await readFile(expandHome(candidate), "utf8")
      return JSON.parse(raw) as Partial<ServerConfig>
    } catch (error) {
      const code = (error as NodeJS.ErrnoException).code
      if (code === "ENOENT") continue
      throw new Error(`读取服务器配置失败（${candidate}）：${(error as Error).message}`)
    }
  }
  return undefined
}

function pick<T>(...values: (T | undefined)[]): T | undefined {
  return values.find((value) => value !== undefined && value !== "")
}

async function resolveConfig(options: Record<string, unknown>): Promise<ServerConfig> {
  const file = await readJsonConfig()
  const merged = { ...(file ?? {}), ...options } as Record<string, unknown>
  const cfg: ServerConfig = {
    label: (pick(merged.label, process.env.OPENCODE_SERVER_LABEL) as string) ?? "remote",
    host: pick(merged.host, process.env.OPENCODE_SERVER_HOST) as string,
    port: Number(pick(merged.port, process.env.OPENCODE_SERVER_PORT, 22)),
    username: pick(merged.username, merged.user, process.env.OPENCODE_SERVER_USER) as string,
    password: pick(merged.password, process.env.OPENCODE_SERVER_PASSWORD) as string | undefined,
    privateKeyPath: pick(merged.privateKeyPath, merged.privateKey, process.env.OPENCODE_SERVER_KEY) as
      | string
      | undefined,
    passphrase: pick(merged.passphrase, process.env.OPENCODE_SERVER_PASSPHRASE) as string | undefined,
    defaultCwd: (pick(merged.defaultCwd, merged.cwd) as string | undefined) ?? undefined,
    readyTimeout: Number(pick(merged.readyTimeout, 20_000)),
    maxOutputChars: Number(pick(merged.maxOutputChars, DEFAULT_MAX_OUTPUT)),
  }

  if (!cfg.host) throw new Error("未配置服务器地址：请设置 host 或环境变量 OPENCODE_SERVER_HOST")
  if (!cfg.username) throw new Error("未配置服务器用户名：请设置 username 或环境变量 OPENCODE_SERVER_USER")
  if (!Number.isFinite(cfg.port) || cfg.port <= 0) throw new Error(`服务器端口无效：${cfg.port}`)
  if (!cfg.password && !cfg.privateKeyPath) {
    throw new Error("未配置认证方式：请设置 password 或 privateKeyPath")
  }
  return cfg
}

function quote(value: string) {
  return `'${value.replace(/'/g, `'\\''`)}'`
}

function truncate(value: string, limit: number) {
  if (value.length <= limit) return value
  const head = value.slice(0, Math.floor(limit * 0.7))
  const tail = value.slice(-Math.floor(limit * 0.25))
  const omitted = value.length - head.length - tail.length
  return `${head}\n\n... [输出过长，已省略 ${omitted} 字符] ...\n\n${tail}`
}

async function connect(cfg: ServerConfig): Promise<Client> {
  let privateKey: Buffer | undefined
  if (cfg.privateKeyPath) {
    privateKey = await readFile(expandHome(cfg.privateKeyPath))
  }

  return new Promise<Client>((resolvePromise, rejectPromise) => {
    const conn = new Client()
    let settled = false

    conn
      .on("ready", () => {
        settled = true
        resolvePromise(conn)
      })
      .on("error", (error) => {
        if (!settled) {
          settled = true
          rejectPromise(
            new Error(
              `连接服务器 ${cfg.username}@${cfg.host}:${cfg.port} 失败：${error.message}` +
                (error.level ? `（${error.level}）` : ""),
            ),
          )
        }
      })

    conn.connect({
      host: cfg.host,
      port: cfg.port,
      username: cfg.username,
      password: cfg.password,
      privateKey,
      passphrase: cfg.passphrase,
      readyTimeout: cfg.readyTimeout ?? 20_000,
      keepaliveInterval: 10_000,
    })
  })
}

async function runCommand(
  cfg: ServerConfig,
  command: string,
  options: { cwd?: string; timeoutMs?: number; signal?: AbortSignal } = {},
): Promise<ExecResult> {
  const timeoutMs = options.timeoutMs && options.timeoutMs > 0 ? options.timeoutMs : DEFAULT_TIMEOUT_MS
  const cwd = options.cwd ? expandHome(options.cwd) : cfg.defaultCwd
  const fullCommand = cwd ? `cd ${quote(cwd)} && ${command}` : command

  const conn = await connect(cfg)
  const startedAt = Date.now()
  const maxOutput = cfg.maxOutputChars ?? DEFAULT_MAX_OUTPUT

  return await new Promise<ExecResult>((resolvePromise, rejectPromise) => {
    let stdout = ""
    let stderr = ""
    let settled = false

    const finish = (fn: () => void) => {
      if (settled) return
      settled = true
      clearTimeout(timer)
      options.signal?.removeEventListener("abort", onAbort)
      fn()
      conn.end()
    }

    const onAbort = () => {
      finish(() => rejectPromise(new Error("命令已被取消")))
    }

    if (options.signal?.aborted) {
      conn.end()
      rejectPromise(new Error("命令已被取消"))
      return
    }
    options.signal?.addEventListener("abort", onAbort, { once: true })

    const timer = setTimeout(() => {
      finish(() =>
        rejectPromise(
          new Error(
            `命令执行超时（${timeoutMs}ms）。已收集到的输出：\n${truncate(
              `${stdout}${stderr ? `\n[stderr]\n${stderr}` : ""}`,
              maxOutput,
            )}`,
          ),
        ),
      )
    }, timeoutMs)

    conn.exec(fullCommand, { pty: false }, (error, stream) => {
      if (error) {
        finish(() => rejectPromise(new Error(`启动远程命令失败：${error.message}`)))
        return
      }

      stream
        .on("data", (chunk: Buffer) => {
          stdout += chunk.toString("utf8")
        })
        .on("close", (code: number | null, signal?: string) => {
          finish(() =>
            resolvePromise({
              stdout: truncate(stdout, maxOutput),
              stderr: truncate(stderr, maxOutput),
              code,
              signal,
              durationMs: Date.now() - startedAt,
            }),
          )
        })

      stream.stderr.on("data", (chunk: Buffer) => {
        stderr += chunk.toString("utf8")
      })
    })
  })
}

function localPath(directory: string, path: string) {
  const expanded = expandHome(path)
  return isAbsolute(expanded) ? expanded : resolve(directory, expanded)
}

async function ensureRemoteDir(sftp: any, dir: string) {
  if (!dir || dir === "/" || dir === ".") return
  const parts = dir.split("/").filter(Boolean)
  let current = dir.startsWith("/") ? "/" : ""
  for (const part of parts) {
    current = current === "/" ? `/${part}` : current ? `${current}/${part}` : part
    await new Promise<void>((ok) => {
      sftp.mkdir(current, (error: Error | undefined) => {
        if (error && !/exist/i.test(error.message) && (error as any).code !== 4) {
          // 忽略权限/已存在之外的错误，后续 fastPut 会给出更明确的失败信息
        }
        ok()
      })
    })
  }
}

async function withSftp<T>(
  cfg: ServerConfig,
  options: { signal?: AbortSignal },
  fn: (sftp: any) => Promise<T>,
): Promise<T> {
  const conn = await connect(cfg)
  return await new Promise<T>((resolvePromise, rejectPromise) => {
    conn.sftp(async (error, sftp) => {
      if (error) {
        conn.end()
        rejectPromise(new Error(`建立 SFTP 会话失败：${error.message}`))
        return
      }
      try {
        const result = await fn(sftp)
        resolvePromise(result)
      } catch (err) {
        rejectPromise(err)
      } finally {
        conn.end()
      }
    })
  })
}

function formatResult(title: string, result: ExecResult) {
  const parts = [`$ ${title}`, `exit: ${result.code ?? "?"}${result.signal ? ` (${result.signal})` : ""}`]
  if (result.stdout.trim()) parts.push(`--- stdout ---\n${result.stdout.trimEnd()}`)
  if (result.stderr.trim()) parts.push(`--- stderr ---\n${result.stderr.trimEnd()}`)
  parts.push(`--- ${result.durationMs}ms ---`)
  return parts.join("\n")
}

export default Plugin.define({
  id: "opencode.server-ops",
  async setup(ctx) {
    const cfg = await resolveConfig(ctx.options)
    const target = `${cfg.username}@${cfg.host}:${cfg.port}`
    const label = cfg.label ?? "remote"

    await ctx.tool.transform((editor) => {
      editor.namespace({
        name: "server",
        description: `远程服务器直连操作（${label}: ${target}），通过 SSH 执行命令、上传和下载文件`,
      })

      editor.add({
        name: "exec",
        description: [
          `在远程服务器 ${label} (${target}) 上通过 SSH 执行 shell 命令。`,
          cfg.defaultCwd ? `默认工作目录：${cfg.defaultCwd}。` : "",
          "返回 stdout、stderr 和退出码。命令以配置的用户身份运行，请谨慎执行破坏性操作。",
        ]
          .filter(Boolean)
          .join(" "),
        input: {
          type: "object",
          properties: {
            command: { type: "string", description: "要执行的 shell 命令" },
            cwd: { type: "string", description: "执行前切换到的远程工作目录（可选）" },
            timeoutMs: {
              type: "number",
              description: `超时毫秒数，默认 ${DEFAULT_TIMEOUT_MS}`,
            },
          },
          required: ["command"],
          additionalProperties: false,
        },
        options: { namespace: "server" },
        execute: async (input, context) => {
          const { command, cwd, timeoutMs } = input as {
            command: string
            cwd?: string
            timeoutMs?: number
          }
          if (!command?.trim()) throw new Error("command 不能为空")
          await context.progress({ status: `在 ${target} 执行命令` })
          const result = await runCommand(cfg, command, {
            cwd,
            timeoutMs,
            signal: context.signal,
          })
          return { content: formatResult(command, result) }
        },
      })

      editor.add({
        name: "upload",
        description: `把本地文件上传到远程服务器 ${label} (${target})，自动创建远程目录`,
        input: {
          type: "object",
          properties: {
            localPath: { type: "string", description: "本地文件路径，支持相对于项目目录的路径" },
            remotePath: { type: "string", description: "远程目标文件路径" },
            mode: { type: "string", description: "上传后设置的权限，例如 644（可选）" },
          },
          required: ["localPath", "remotePath"],
          additionalProperties: false,
        },
        options: { namespace: "server" },
        execute: async (input, context) => {
          const { localPath: source, remotePath, mode } = input as {
            localPath: string
            remotePath: string
            mode?: string
          }
          const sourcePath = localPath(ctx.location.directory, source)
          const info = await stat(sourcePath).catch(() => undefined)
          if (!info?.isFile()) throw new Error(`本地文件不存在：${sourcePath}`)

          await context.progress({ status: `上传到 ${target}:${remotePath}` })
          const result = await withSftp(cfg, { signal: context.signal }, async (sftp) => {
            const parent = remotePath.replace(/\/[^/]*$/, "")
            if (parent && parent !== remotePath) await ensureRemoteDir(sftp, parent)
            await new Promise<void>((ok, fail) =>
              sftp.fastPut(sourcePath, remotePath, (err: Error | undefined) => (err ? fail(err) : ok())),
            )
            if (mode) {
              await new Promise<void>((ok, fail) =>
                sftp.chmod(remotePath, parseInt(mode, 8), (err: Error | undefined) =>
                  err ? fail(err) : ok(),
                ),
              )
            }
            return sftp.stat(remotePath)
          })

          return {
            content: `已上传 ${sourcePath} -> ${target}:${remotePath}（${result.size ?? info.size} 字节）`,
          }
        },
      })

      editor.add({
        name: "download",
        description: `从远程服务器 ${label} (${target}) 下载文件到本地，自动创建本地目录`,
        input: {
          type: "object",
          properties: {
            remotePath: { type: "string", description: "远程文件路径" },
            localPath: { type: "string", description: "本地目标路径，支持相对于项目目录的路径" },
          },
          required: ["remotePath", "localPath"],
          additionalProperties: false,
        },
        options: { namespace: "server" },
        execute: async (input, context) => {
          const { remotePath, localPath: destination } = input as {
            remotePath: string
            localPath: string
          }
          const destinationPath = localPath(ctx.location.directory, destination)
          await context.progress({ status: `从 ${target}:${remotePath} 下载` })
          const result = await withSftp(cfg, { signal: context.signal }, async (sftp) => {
            const { mkdir } = await import("node:fs/promises")
            const parent = destinationPath.replace(/[\\/][^\\/]*$/, "")
            if (parent && parent !== destinationPath) await mkdir(parent, { recursive: true })
            await new Promise<void>((ok, fail) =>
              sftp.fastGet(remotePath, destinationPath, (err: Error | undefined) =>
                err ? fail(err) : ok(),
              ),
            )
            return sftp.stat(remotePath)
          })
          return {
            content: `已下载 ${target}:${remotePath} -> ${destinationPath}（${result.size ?? "?"} 字节）`,
          }
        },
      })

      editor.add({
        name: "status",
        description: `查看远程服务器 ${label} (${target}) 的基础状态：主机名、负载、内存、磁盘、Docker 容器`,
        input: { type: "object", properties: {}, additionalProperties: false },
        options: { namespace: "server" },
        execute: async (_input, context) => {
          await context.progress({ status: `读取 ${target} 状态` })
          const command = [
            "hostname",
            "uptime",
            "free -h",
            "df -h /",
            "(docker ps --format 'table {{.Names}}\\t{{.Status}}\\t{{.Ports}}' 2>/dev/null || true)",
          ].join("; echo '---'; ")
          const result = await runCommand(cfg, command, { signal: context.signal })
          return { content: formatResult("server status", result) }
        },
      })
    })

    await ctx.storage.set("lastConfig", {
      label,
      target,
      defaultCwd: cfg.defaultCwd ?? null,
    })
  },
})
