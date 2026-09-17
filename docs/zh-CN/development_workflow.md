# 开发工作流

## 1. 概述

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

## 2. 分支

### 2.1 `main`

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

### 2.2 Feature 分支

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

### 2.3 Fix 分支

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

### 2.4 其他分支类型

还可以使用以下分支命名规范：

```text
feature/*     新功能
fix/*         Bug 修复
refactor/*    重构
docs/*        文档
test/*        测试相关修改
release/*     Release 维护分支
```

例如：

```text
refactor/opentelemetry-config
docs/update-readme
test/add-integration-tests
```

---

## 3. Commit Message 规范

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

## 4. 合并到 `main` 的规则

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

## 5. 未完成或 Experimental 功能

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

## 6. 创建 Release 分支

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

## 7. 版本号

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

## 8. 两个层级的测试

整个工作流中有两个测试阶段。

这两个测试阶段的目的不同。

### 8.1 Feature 级别测试

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

### 8.2 Release 级别测试

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

## 9. Release Candidate

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

## 10. 正式发布

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

## 11. Patch Release

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

## 12. Release 之后发现 Bug

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

## 13. Backport

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

## 14. Forward Merge / Forward Port

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

## 15. 同时维护多个版本

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

## 16. Release 分支规则

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

## 17. 完整开发生命周期

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

## 18. 公共测试与私有测试

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

## 19. GitHub 与 Gitee

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

## 20. 核心原则

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

## 21. 示例

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
