# 开发

本文档定义本项目的开发、测试、发布和版本维护流程。

项目采用 **主干开发 + Release 分支** 的模式：

```text
feature/*
    │
    │ 开发
    │ 测试
    │ Code Review
    ▼
   main
    │
    ├─────────────────────→ release/v1.3.x
    │                           │
    │                           ├── RC
    │                           ├── Release 测试
    │                           ├── v1.3.0
    │                           ├── Bug 修复
    │                           ├── v1.3.1
    │                           └── v1.3.2
    │
    ▼
继续开发 1.4
```

核心原则是：

> `main` 应该只包含已经完成、经过测试，并且可以接受进入下一次发布版本的修改。

当某个版本进入发布稳定阶段时，从 `main` 创建对应的 Release 分支。

---

## 分支模型

本项目使用以下分支类型：

```text
main             主要开发分支

feature/*        新功能
fix/*            Bug 修复
refactor/*       重构
docs/*           文档
test/*           测试相关修改
release/*        Release 稳定和维护
```

正常开发流程为：

```text
feature/*
    │
    ▼
   main
    │
    ▼
release/vX.Y.x
```

Release 分支不作为普通 Feature 的集成分支。

---

## `main`

`main` 是项目的主要开发分支。

所有正常开发最终都应该进入 `main`。

例如：

* 新功能
* Bug 修复
* 重构
* 文档修改
* 测试修改
* 依赖升级

应尽量保证 `main` 始终处于可发布状态。

一个 Feature 通常只有满足以下条件后，才能合并到 `main`：

1. 功能已经开发完成。
2. 自动化测试通过。
3. Code Review 完成。
4. 不会破坏已有功能。
5. 可以接受该功能进入下一次 Release。

未完成的用户可见功能通常不应该直接合并到 `main`。

如果一个大型功能必须提前合并，则应该使用 Feature Flag，或者将其标记为 Experimental，并且默认关闭。

---

## Feature 分支

每个功能通常应该在独立的 Feature 分支中开发。

命名规范：

```text
feature/<name>
```

例如：

```text
feature/add-login
feature/add-opentelemetry-support
feature/add-sharding
```

典型开发流程：

```text
main
 │
 └── feature/add-a
          │
          ├── 开发
          ├── 单元测试
          ├── 集成测试
          ├── CI
          └── Code Review
                 │
                 ▼
                main
```

从最新的 `main` 创建 Feature 分支：

```bash
git switch main
git pull

git switch -c feature/add-a
```

开发和测试完成后，通过 Pull Request 合并到 `main`。

---

## Fix 分支

Bug 修复通常使用：

```text
fix/<name>
```

例如：

```text
fix/login-timeout
fix/null-pointer-exception
fix/database-connection-leak
```

对于开发过程中发现的普通 Bug：

```text
main
 │
 └── fix/xxx
        │
        ▼
       main
```

如果 Bug 同时影响已经发布的版本，则按照本文后面的 Backport 流程处理。

---

## 其他分支类型

其他常用的分支前缀包括：

```text
refactor/*
docs/*
test/*
```

例如：

```text
refactor/opentelemetry-config
docs/update-readme
test/add-integration-tests
```

---

## Commit Message 规范

项目建议尽量使用 Conventional Commits。

常见类型：

```text
feat:       新功能
fix:        Bug 修复
refactor:   代码重构
docs:       文档修改
test:       测试修改
build:      构建系统或依赖修改
ci:         CI 配置修改
chore:      维护性修改
```

例如：

```text
feat: add OpenTelemetry tracing support

fix: handle null configuration values

refactor: simplify database routing logic

docs: add release workflow documentation

test: add integration tests for authentication
```

---

## 合并到 `main` 的规则

一个 Pull Request 在合并到 `main` 之前，应满足项目规定的合并条件。

至少包括：

```text
功能开发完成
      │
      ▼
自动化测试通过
      │
      ▼
Code Review 通过
      │
      ▼
不存在已知 Regression
      │
      ▼
可以进入下一次 Release
      │
      ▼
     main
```

该规则的目的，是避免把 Release 分支变成一个临时挑选 Feature 的地方。

一个 Feature 是否已经准备好进入 Release，通常应该在它进入 `main` 之前决定。

---

## 未完成或 Experimental 功能

有些大型功能开发周期较长。

为了避免维护一个长期存在、与 `main` 差异越来越大的 Feature 分支，有时需要提前把代码合并到 `main`。

这种情况下，功能本身不应该作为普通生产功能直接开放。

可以采用：

```text
Feature Flag
Experimental 状态
默认关闭
仅内部使用
```

例如：

```properties
feature.c.enabled=false
```

表示 Feature C 的代码已经存在，但默认不启用。

例如：

```text
main
├── Feature A   enabled
├── Feature B   enabled
├── Feature C   disabled
└── Feature D   enabled
```

创建 Release 分支之后：

```text
release/v1.3.x
├── Feature A   enabled
├── Feature B   enabled
├── Feature C   disabled
└── Feature D   enabled
```

因此：

```text
代码进入 Release
        ≠
功能正式对用户开放
```

等 Feature C 稳定以后，再启用它。

---

# 测试

测试在两个不同阶段进行：

```text
Feature 测试
      ↓
合并到 main
      ↓
Release 测试
      ↓
正式发布
```

两个阶段可能会运行一部分相同的自动化测试，但它们要回答的问题不同。

---

## Feature 级别测试

一个 Feature 在进入 `main` 之前：

```text
feature/add-a
      │
      ├── 单元测试
      ├── Feature 相关集成测试
      ├── CI
      ├── 静态检查
      └── Code Review
             │
             ▼
            main
```

这里要回答的问题是：

> 这个独立修改是否已经可以进入 `main`？

典型检查包括：

```text
单元测试
与该 Feature 相关的集成测试
静态代码分析
构建验证
Code Review
基础 Regression 测试
```

---

## Release 级别测试

创建 `release/v1.3.x` 后，还需要再次对整个 Release 进行测试。

```text
release/v1.3.x
       │
       ├── RC Build
       ├── 完整 Regression 测试
       ├── 集成测试
       ├── 部署测试
       ├── 兼容性测试
       ├── 升级测试
       └── 类生产环境验证
              │
              ▼
           v1.3.0
```

这里需要回答的问题是：

> 完整的 `1.3.0` 是否已经可以进入生产环境？

即使所有 Feature 单独测试都通过：

```text
Feature A ✅
Feature B ✅
Feature C ✅
```

也不能保证：

```text
A + B + C ✅
```

因为多个功能组合在一起后，可能出现新的问题。

因此，同一套自动化测试在 Pull Request 阶段和 Release 阶段重复执行是正常的。

---

## 公共测试与私有测试

开源仓库应该运行所有可以公开的测试。

例如：

```text
Build
单元测试
公共集成测试
静态分析
代码风格检查
公共 CI
```

不能公开的内部测试继续保留在私有环境中。

因此，一个 Release 应该同时通过：

```text
公共仓库测试
      +
私有 / 内部测试
      =
Release 验证
```

最终将要创建 Tag 的那个公共 Commit 必须经过完整验证。

正确流程：

```text
公共 Release Commit
        │
        ├── Public CI ✅
        │
        ├── Private Tests ✅
        │
        ▼
      创建 Tag
```

最终测试完成之后，到创建 Tag 之前，不应该再修改代码。

---

# Release

---

## 创建 Release 分支

当项目准备发布 `1.3.0` 时，从 `main` 创建 Release 分支。

推荐命名：

```text
release/v1.3.x
```

这里的 `.x` 表示该分支负责维护整个 `1.3` 系列：

```text
v1.3.0
v1.3.1
v1.3.2
...
```

例如：

```bash
git switch main
git pull

git switch -c release/v1.3.x
```

此时分支关系为：

```text
                       release/v1.3.x
                      /
main ────────────────●
                      \
                       main 继续开发 1.4
```

创建 Release 分支之后：

```text
main
    → 开发下一个 Minor 版本
    → 例如 1.4.0-SNAPSHOT

release/v1.3.x
    → Release 稳定阶段
    → RC
    → v1.3.0
    → v1.3.1
    → v1.3.2
```

---

## 版本号

对于 Maven Java 项目，`main` 可以使用：

```text
1.3.0-SNAPSHOT
```

这样的开发版本号。

当创建 `1.3` Release 分支时：

```text
release/v1.3.x
    → 准备发布 1.3.0

main
    → 更新为 1.4.0-SNAPSHOT
```

概念上：

```text
创建 Release 分支之前：

main
└── 1.3.0-SNAPSHOT


创建 Release 分支之后：

release/v1.3.x
└── 准备 1.3.0

main
└── 1.4.0-SNAPSHOT
```

---

## Release Candidate

正式发布之前，可以创建一个或多个 Release Candidate。

例如：

```text
release/v1.3.x
       │
       ├── v1.3.0-RC1
       │
       ├── 测试
       │
       ├── Bug 修复
       │
       ├── v1.3.0-RC2
       │
       ├── 测试
       │
       └── v1.3.0
```

RC 应该从最终正式 Release 所使用的同一个 Release 分支构建。

进入 RC 阶段之后，应认为 Release 分支进入冻结状态。

通常只接受：

```text
严重 Bug 修复
Release Blocker 修复
文档修复
Build / Release 修复
```

新的 Feature 通常应该等待下一个版本。

---

## 正式发布与 Tag

Release 测试通过后：

```text
release/v1.3.x
       │
       ▼
   最终 Release Commit
       │
       ▼
     tag v1.3.0
```

例如：

```bash
git tag v1.3.0
git push <remote> v1.3.0
```

Tag 必须指向真正通过 Release 验证的那个 Commit。

正确流程：

```text
经过测试的 Commit
       │
       └── tag: v1.3.0
```

不要这样做：

```text
经过测试的 Commit
       │
       └── 又修改了一些代码
                │
                └── tag v1.3.0
```

如果在最终测试之后又修改了代码，就应该重新进行相应的 Release 验证。

---

## Release 分支规则

Release 分支创建后，应该主要用于：

```text
版本稳定
Release 准备
Patch 维护
```

通常允许：

```text
Bug 修复
Release Blocker 修复
文档修复
版本号修改
Build 修复
Release 配置修改
```

应该避免：

```text
大型重构
无关代码清理
新功能
Breaking API 修改
与当前 Release 无关的 Experimental 修改
```

这样可以减少 RC 之间的差异，并降低 Release 风险。

---

# 维护

---

## Patch Release

一个 Release 分支负责维护整个版本系列。

例如：

```text
release/v1.3.x
       │
       ├── v1.3.0
       │
       ├── fix A
       │
       ├── v1.3.1
       │
       ├── fix B
       │
       └── v1.3.2
```

Patch Release 原则上只应该包含 Bug 修复。

例如：

```text
v1.3.0 → 初始正式版本

v1.3.1 → Bug 修复

v1.3.2 → Bug 修复
```

应尽量避免在 Patch Release 中：

```text
增加新的公共功能
修改公共 API
加入不兼容修改
```

---

## Release 之后发现 Bug

假设：

```text
release/v1.3.x
       │
       └── v1.3.0

main
       └── 正在开发 1.4
```

现在发现 `v1.3.0` 中存在一个 Bug，并且这个 Bug 在 `main` 中也存在。

默认流程为：

```text
从 main 创建 fix 分支
        │
        ▼
修复并测试
        │
        ▼
合并到 main
        │
        ▼
Backport
        │
        ▼
release/v1.3.x
        │
        ▼
Release 测试
        │
        ▼
      v1.3.1
```

图示：

```text
main
A──B──C──D──F
             ↑
           Bug Fix
             │
             │ Backport
             ▼
release/v1.3.x
A──B──C──────F'
    │         │
 v1.3.0    v1.3.1
```

Bug Fix 优先进入 `main`，这样未来的新版本自然也会包含该修复。

之后再把需要的修复有选择地 Backport 到旧的维护分支。

通常不应该把整个 Release 分支重新 Merge 回 `main`。

---

## Backport

Backport 指的是：

> 把已经存在于较新开发分支中的修复，移植到较旧的 Release 维护分支。

方向是：

```text
较新
main
 │
 │ Backport
 ▼
release/v1.3.x
较旧
```

Git 层面上通常类似：

```bash
git switch release/v1.3.x
git cherry-pick <commit>
```

在 GitHub 或 Gitee 中，也可以通过 Backport Pull Request 完成：

```text
PR #100
fix: xxx
    │
    ▼
   main
    │
    │ Backport Workflow
    ▼
PR #105
[1.3.x backport] fix: xxx
    │
    ▼
release/v1.3.x
```

Backport PR 仍然需要经过：

```text
Code Review
CI
测试
```

因为同一个 Bug Fix 在旧版本代码中可能会产生不同的行为。

---

## Forward Merge / Forward Port

有些项目会采用相反的策略。

如果同一个 Bug 同时影响多个维护版本，例如：

```text
release/v1.2.x
release/v1.3.x
main
```

那么可以先在最老的受影响版本修复：

```text
release/v1.2.x
       │
       └── fix
            │
            ▼
release/v1.3.x
            │
            ▼
           main
```

这称为 Forward Merge 或 Forward Port。

方向是：

```text
旧版本 → 新版本
```

与 Backport 对比：

```text
Backport:
新版本 → 旧版本

Forward Port:
旧版本 → 新版本
```

本项目默认采用：

```text
优先修复 main
      ↓
必要时 Backport
      ↓
旧的维护分支
```

如果存在明确原因，也可以选择先修复最旧版本，然后 Forward Port。

---

## 同时维护多个版本

随着项目发展，可能同时存在：

```text
main
release/v1.4.x
release/v1.3.x
```

这些分支的职责分别是：

```text
main
    → 未来版本开发

release/v1.4.x
    → 维护 1.4 系列

release/v1.3.x
    → 维护 1.3 系列
```

如果一个 Bug 同时影响所有版本，默认先修复 `main`，然后有选择地 Backport：

```text
                    fix
                     │
                     ▼
main ────────────────F────────────>

                     │
                     ├── Backport
                     ▼
release/v1.4.x ──────F'───────────>

                     │
                     └── Backport
                         ▼
release/v1.3.x ──────F''──────────>
```

只有仍然处于支持周期内的版本才需要 Backport。

---

# 仓库与分发

---

## GitHub 与 Gitee

项目可以同时发布到 GitHub 和 Gitee。

两个公共仓库中的 Branch 和 Release Tag 应保持一致。

每个正式版本至少同步：

```text
main
release/vX.Y.x
vX.Y.Z tag
```

例如：

```text
GitHub
├── main
├── release/v1.3.x
└── v1.3.0

Gitee
├── main
├── release/v1.3.x
└── v1.3.0
```

不要在 GitHub 和 Gitee 上分别对同一个逻辑分支独立开发不同修改。

同一个 Branch 应该只有一条权威历史，然后同步到另一个公共 Remote。

---

## Release Tags

正式版本使用 Git Tag 表示：

```text
v1.3.0
v1.3.1
v1.3.2
```

Release 分支表示整个维护版本线：

```text
release/v1.3.x
```

Tag 表示具体且不可变的正式版本：

```text
release/v1.3.x
       │
       ├── v1.3.0
       ├── v1.3.1
       └── v1.3.2
```

---

## Docker 镜像与发布产物

Docker 镜像和其他 Release 产物应从经过测试的 Release Commit 或 Release Tag 构建。

推荐流程：

```text
Release Commit
      │
      ├── Release 测试 ✅
      │
      ▼
   v1.3.0 Tag
      │
      ▼
构建发布产物
      │
      ├── Docker 镜像
      ├── JAR
      └── 其他发布包
```

这样可以确保发布产物与 Release Tag 所标识的源代码完全一致。

工作流会发布三个支持 `linux/amd64` 和 `linux/arm64` 的多平台镜像：

| 组件 | Docker Hub | 阿里云 ACR |
| --- | --- | --- |
| Parser | `prismapm/prism-apm-parser` | `crpi-16i4tdv4pz11ple8.cn-beijing.personal.cr.aliyuncs.com/prism-apm/prism-apm-parser` |
| UI Server | `prismapm/prism-apm-ui-server` | `crpi-16i4tdv4pz11ple8.cn-beijing.personal.cr.aliyuncs.com/prism-apm/prism-apm-ui-server` |
| UI | `prismapm/prism-apm-ui` | `crpi-16i4tdv4pz11ple8.cn-beijing.personal.cr.aliyuncs.com/prism-apm/prism-apm-ui` |

Git Tag 与镜像标签的映射如下：

| Git Tag | 镜像标签 | 是否更新 `latest` |
| --- | --- | --- |
| `v0.1.0` | `0.1.0`、`latest` | 是 |
| `v0.2.0-rc.1` | `0.2.0-rc.1` | 否 |

Docker Hub 仓库页面分别为 [Parser](https://hub.docker.com/r/prismapm/prism-apm-parser/tags)、[UI Server](https://hub.docker.com/r/prismapm/prism-apm-ui-server/tags) 和 [UI](https://hub.docker.com/r/prismapm/prism-apm-ui/tags)。

---

# 参考

---

## 完整开发生命周期

完整流程可以总结为：

```text
Issue / Requirement
        │
        ▼
feature/add-a
        │
        ├── 开发
        ├── 单元测试
        ├── 集成测试
        ├── CI
        └── Code Review
               │
               ▼
              main
               │
               ├── Feature A
               ├── Feature B
               ├── Feature C（disabled）
               └── Fix D
               │
               ▼
       release/v1.3.x
               │
               ├── RC1
               ├── 完整 Release 测试
               ├── Release Fix
               ├── 必要时 RC2
               └── 最终验证
                       │
                       ▼
                    v1.3.0
                       │
                       ├── 发现 Bug
                       │
                       │
main ── fix bug ───────┘
        │
        │ Backport
        ▼
release/v1.3.x
        │
        ├── 测试
        └── v1.3.1
```

与此同时：

```text
main
 │
 └── 继续开发 1.4.0
```

---

## 核心原则

整个工作流遵循以下原则：

```text
1. 每个 Feature 使用短生命周期 Feature 分支开发。

2. 完成并经过测试的 Feature 合并到 main。

3. 尽量保持 main 随时可发布。

4. 对必须提前合并的大型未完成功能使用 Feature Flag。

5. Release 分支从 main 创建。

6. Release 分支只用于版本稳定和 Patch 维护，
   不作为普通 Feature 集成分支。

7. Feature 合并到 main 之前进行 Feature 级测试。

8. 正式发布之前对完整 Release 再次进行测试。

9. 使用 Tag 表示真正发布的版本。

10. 使用 release/vX.Y.x 维护一个完整的版本系列。

11. 默认优先在 main 修复 Bug。

12. 必要时将 Bug Fix Backport 到仍受支持的旧版本。

13. 通常不要把整个 Release 分支 Merge 回 main。

14. Public Tests 和 Private Tests 都应该针对真正的
    Release Candidate Commit 运行。

15. 只有通过最终验证的 Commit 才能创建正式 Release Tag。
```

---

## 示例

假设当前正在准备 `1.3.0`。

开发阶段：

```text
feature/add-a ──┐
                │
feature/add-b ──┼──→ main
                │
feature/add-c ──┘
```

Feature C 还没有完成，但由于开发周期较长，需要提前合并：

```properties
feature.c.enabled=false
```

此时：

```text
main
├── A
├── B
└── C（disabled）
```

准备发布时：

```text
main
 │
 ├────────────────→ release/v1.3.x
 │                       │
 │                       ├── v1.3.0-RC1
 │                       ├── Release 测试
 │                       └── v1.3.0
 │
 └── 继续开发 1.4.0-SNAPSHOT
```

后来在 `v1.3.0` 中发现 Bug：

```text
fix/xxx
   │
   ▼
 main
   │
   │ Backport
   ▼
release/v1.3.x
   │
   ├── 测试
   └── v1.3.1
```

通过这种方式，可以清晰地区分：

```text
正常开发
Release 稳定
旧版本维护
```

三种不同类型的工作。

---

# 创建 Release 分支的具体操作流程

本节以准备 `0.1.0` 为例，说明从 `main` 创建 Release 维护分支、准备 RC、正式发布，以及升级 `main` 开发版本的具体操作。

以下命令假设：

```text
GitHub Remote：github
当前开发版本：0.1.0-SNAPSHOT
Release 维护分支：release/v0.1.x
第一个 RC 分支：release/v0.1.0-RC1
正式 Release 分支：release/v0.1.0
正式版 Tag：v0.1.0
下一个开发版本：0.2.0-SNAPSHOT
```

如果本地使用的 GitHub Remote 名称不是 `github`，应将命令中的 `github` 替换为实际的 Remote 名称。可以使用以下命令确认：

```bash
git remote -v
```

## 发布前检查

开始操作前，应确认工作区干净，并将本地 `main` 更新到 GitHub 上的最新状态：

```bash
git status --short

git fetch github --prune
git switch main
git pull --ff-only github main
```

如果 `git status --short` 有输出，应先提交或暂存这些修改，不要带着未提交的修改创建 Release 分支。

## 创建 Release 维护分支

从最新的 `main` 创建负责维护整个 `0.1` 系列的分支：

```bash
git switch -c release/v0.1.x
git push -u github release/v0.1.x
```

`release/v0.1.x` 是 `0.1.0`、`0.1.1`、`0.1.2` 等版本共同使用的 Release 维护分支。正式发布时，还会通过发布脚本创建对应的版本分支和 Tag：

```text
release/v0.1.x
       │
       ├── release/v0.1.0 ── tag v0.1.0
       ├── release/v0.1.1 ── tag v0.1.1
       └── release/v0.1.2 ── tag v0.1.2
```

版本分支保存该次正式发布的分支状态；Tag 是正式版本的精确、不可变标识。后续 Patch 修复仍然从 `release/v0.1.x` 开始。

## 升级 `main` 的开发版本

Release 维护分支创建后，`main` 即可进入下一个 Minor 版本的开发。版本升级应在独立分支中完成，并通过 Pull Request 合并到 `main`：

```bash
git switch main
git switch -c chore/upgrade-version-to-0.2.0

./scripts/release/set-version.sh 0.2.0-SNAPSHOT

git diff --check
git add -u
git commit -m "chore: bump version to 0.2.0-SNAPSHOT"
git push -u github chore/upgrade-version-to-0.2.0
```

注意，Maven 的开发版本后缀必须写成大写的 `SNAPSHOT`。脚本会将 Maven 的 `0.2.0-SNAPSHOT` 映射为 `prism-apm-ui` 使用的 `0.2.0-dev`。

推送后，为该分支创建 Pull Request；CI 和 Code Review 通过后，再合并到 `main`。

此步骤可以和 `release/v0.1.x` 上的 RC 准备及测试并行进行。

## 创建 RC 分支

从 Release 维护分支创建 RC 分支。分支名使用连字符 `-`，不要使用下划线 `_`：

```bash
git switch release/v0.1.x
git pull --ff-only github release/v0.1.x
git switch -c release/v0.1.0-RC1

./scripts/release/set-version.sh 0.1.0-RC1

git diff --check
git add -u
git commit -m "chore(release): prepare 0.1.0-RC1"
git push -u github release/v0.1.0-RC1
```

然后针对该 RC 分支执行完整的 Release 测试。发现问题时，在 RC 分支上修复并重新测试。

如果需要新的 RC，可以在包含前一个 RC 修复的 Commit 上创建下一个 RC 分支，并更新版本号：

```bash
git switch release/v0.1.0-RC1
git switch -c release/v0.1.0-RC2

./scripts/release/set-version.sh 0.1.0-RC2

git diff --check
git add -u
git commit -m "chore(release): prepare 0.1.0-RC2"
git push -u github release/v0.1.0-RC2
```

Docker 镜像发布工作流接受 `vMAJOR.MINOR.PATCH` 格式的稳定版 Tag，以及 `vMAJOR.MINOR.PATCH-rc.N` 格式的 RC Tag。仅推送 RC 分支不会发布镜像；如果推送 RC Tag，工作流只会向 Docker Hub 和阿里云 ACR 发布对应的版本镜像标签，不会更新 `latest`。

## 确认最终 RC

假设最终通过验证的是 `release/v0.1.0-RC1`，正式发布前应更新本地分支，并再次确认工作区干净：

```bash
git fetch github --prune
git switch release/v0.1.0-RC1
git pull --ff-only github release/v0.1.0-RC1
git status --short
```

`git status --short` 应没有输出。还应确认以下名称均未被占用：

```bash
git branch --all --list 'release/v0.1.0' 'remotes/github/release/v0.1.0'
git tag --list v0.1.0
```

以上两个命令也应没有输出。然后针对这个最终 RC Commit 完成最后一轮 Release 验证。

## 创建正式 Release 分支、Commit 和 Tag

使用 `prepare-release-v1.sh`，从已验证的 RC 分支创建正式 Release 分支：

```bash
./scripts/release/prepare-release-v1.sh \
  release/v0.1.0-RC1 \
  release/v0.1.0 \
  0.1.0
```

该脚本会：

```text
1. 从 release/v0.1.0-RC1 创建并切换到 release/v0.1.0。
2. 将项目版本更新为 0.1.0。
3. 创建 Commit：chore(release): prepare v0.1.0。
4. 创建 annotated Tag：v0.1.0。
```

该脚本要求源分支存在于本地，且目标分支和 Tag 均不存在。它不会构建、测试或推送任何内容。

脚本执行后，应确认版本、Commit 和 Tag 正确，并对最终 Release Commit 重新执行 Release 验证：

```bash
git status --short
git show --stat --decorate HEAD
git show v0.1.0
```

`git status --short` 应没有输出，并且 `release/v0.1.0` 与 `v0.1.0` 应指向同一个 Release Commit。

## 更新 Release 维护分支

正式 Release Commit 验证通过后，应将 `release/v0.1.x` 快进到该 Commit，确保后续 Patch 版本基于已经发布的版本继续维护：

```bash
git switch release/v0.1.x
git pull --ff-only github release/v0.1.x
git merge --ff-only release/v0.1.0
```

如果不能进行 Fast-forward，说明 Release 维护分支和最终 RC 已经发生分叉。此时不要强制覆盖历史，应先通过 Pull Request 合并缺少的修改，完成 Code Review、CI 和 Release 验证后再继续发布。

## 推送到 GitHub

只有最终验证通过后，才能推送 Release 维护分支、正式 Release 分支和 Tag。Tag 最后推送，以触发正式发布工作流：

```bash
git push -u github release/v0.1.0
git push github release/v0.1.x
git push github v0.1.0
```

推送 `v0.1.0` Tag 后，GitHub Actions 中的 Docker 镜像发布工作流会先运行测试，然后构建 Parser、UI Server 和 UI 镜像，并将 `0.1.0` 和 `latest` 标签推送到上面列出的 Docker Hub 和阿里云 ACR 仓库。

如果测试或发布工作流失败，不要移动或覆盖已经公开的 Tag。应修复问题、重新完成验证，并根据问题性质创建新的版本。

## 同步到 Gitee

GitHub 发布和验证完成后，应将相同的 Release 分支和 Tag 同步到 Gitee，不要在 Gitee 上创建另一条独立历史：

```bash
git push gitee release/v0.1.x
git push gitee release/v0.1.0
git push gitee v0.1.0
```

同步后，可以确认两个公共仓库指向同一个 Commit：

```bash
git ls-remote github refs/heads/release/v0.1.x refs/heads/release/v0.1.0 refs/tags/v0.1.0
git ls-remote gitee refs/heads/release/v0.1.x refs/heads/release/v0.1.0 refs/tags/v0.1.0
```
