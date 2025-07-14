# 一个简单的 Paxos 实现

这是一个基于 Paxos 协议的分布式 Key-Value 存储的简化实现。项目旨在演示 Paxos 协议的核心思想，即如何在异步网络环境中，在多个节点之间对一个值达成共识。

## 项目概述

本项目实现了一个多 Proposer、多 Acceptor 的 Paxos 模型，用于对一个 KV 存储中的值进行读写。

*   **Proposer (提议者)**: 在 `KvClient.java` 中实现。负责发起提议，经过两阶段提交 (Prepare 和 Accept) 来尝试让一个值被大多数 Acceptor 接受。
*   **Acceptor (接受者)**: 在 `KVServer.java` 中实现。负责响应 Proposer 的提议。它会持久化自己已经 "承诺" (Promised) 和 "接受" (Accepted) 的提议。
*   **Learner (学习者)**: 本项目没有显式实现 Learner。在一个完整的 Paxos 实现中，Learner 负责从 Acceptor 那里学习已经被选定的值。

## 技术栈

*   **Java 11**: 主要编程语言。
*   **gRPC**: 用于 Proposer 和 Acceptor 之间的 RPC 通信。
*   **Protocol Buffers**: 用于定义服务接口和数据结构 (`paxoskv.proto`)。
*   **Maven**: 用于项目构建和依赖管理。

## 如何运行

1.  **构建项目**:
    ```bash
    mvn clean install
    ```
2.  **运行 Acceptors**:
    在 `TestSetAndGetByKeyVer.java` 中，有启动多个 Acceptor 的示例代码。
3.  **运行 Proposer (Client)**:
    通过 `KvClient.java` 来发起对 Paxos 集群的读写请求。