# BayMcPatrol

BayMcPatrol 是一个用于 Velocity 群组服的 Paper/Folia 后端跨服随机巡查插件, 支持管理员从全群组在线玩家中随机选取目标并自动切服传送到其当前位置

## 功能

- `/patrol` 和 `/patrol next` 随机巡查下一个玩家
- `/patrol back` 回退到上一个巡查历史目标
- `/patrol status` 查看插件运行状态
- `/patrol reload` 重载配置和语言文件
- Redis 正常时支持全群组跨服巡查
- Redis 异常时自动降级为本服巡查
- 支持 `redis.enabled=false` 永久本服模式
- 支持 Paper 和 Folia
- 支持一轮内不重复随机
- 支持动态在线池
- 所有消息使用 `lang/zh_CN.yml` 和 MiniMessage

## 部署

BayMcPatrol 只安装在后端 Paper/Folia 子服, 不安装在 Velocity 代理端

所有参与巡查的后端子服都需要安装本插件, 并连接同一个 Redis

Velocity 需要开启 BungeeCord 插件消息兼容

```toml
bungee-plugin-message-channel = true
```

## 配置

每个后端子服都需要正确配置 `server.id` 和 `server.alias`

```yaml
server:
  id: "survival-1"
  alias: "生存一区"
```

`server.id` 必须和 Velocity 配置中的服务器名完全一致, 例如

```toml
[servers]
survival-1 = "127.0.0.1:25566"
resource-1 = "127.0.0.1:25567"
```

跨服切换时插件会向 Velocity 发送

```text
Connect -> server.id
```

## 命令

```text
/baymcpatrol
/baymcpatrol next
/baymcpatrol back
/baymcpatrol status
/baymcpatrol reload
/patrol
/patrol next
/patrol back
/patrol status
/patrol reload
```

`/patrol` 等价于 `/patrol next`

## 权限

```text
baymcpatrol.use
baymcpatrol.back
baymcpatrol.status
baymcpatrol.reload
baymcpatrol.bypass
```

`baymcpatrol.bypass` 只影响随机候选池, 拥有该权限的玩家不会被随机巡查选中, 但不影响其自己使用巡查命令

## 随机巡查范围

随机巡查会排除

- 管理员自己
- 拥有 `baymcpatrol.bypass` 权限的玩家
- 离线玩家
- Redis 在线数据过期的玩家
- 当前管理员本轮 seen 中已经巡查过的玩家

随机巡查不会排除

- OP
- 管理员
- AFK 玩家
- 创造模式玩家
- 旁观模式玩家
- 不同世界玩家
- 不同维度玩家

## Redis 模式

Redis 正常时支持

- 全群组在线池
- 跨服随机巡查
- 跨服 back
- seen/history/cursor 会话
- active 防重复
- pending 跨服传送任务
- 上次巡查时间永久记录

Redis 异常时, 插件自动降级为本服巡查, 此时 `/patrol` 不额外提示 Redis 异常, `/patrol status` 会显示 Redis 状态和运行模式

如果配置

```yaml
redis:
  enabled: false
```

插件会进入永久本服巡查模式

## 构建

```bash
./gradlew build
```

Windows

```powershell
.\gradlew.bat build
```

构建产物

```text
build/libs/BayMcPatrol-1.0-SNAPSHOT.jar
```

## 技术栈

```text
Java 25
Gradle Kotlin DSL
ShadowJar
Paper API
Lettuce
Gson
Adventure + MiniMessage
```
