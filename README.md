# snowflake-unlimited
A snowflake Java implemented，but this one unlimited time.

本 ID 生成器基于 Twitter Snowflake 算法，代码基于百度开源项目 uid-generator(https://github.com/baidu/uid-generator)，但都进行了一定改进。

传统 Snowflake 的结构如下：
> sign ==== delta seconds ==== data center id ==== worker id ==== sequence
>
> (其中 sign 为整数的正负标识，固定 1 bit，其余各位一共 63 bits。)

百度 uid-generator 结构如下：
> sign ==== delta seconds ==== worker id ==== sequence
>
> (其中 sign 为整数的正负标识，固定 1 bit，其余各位一共 63 bits。)

传统 Snowflake 缺点是最长 64 位，发号器可运行时间有限。
百度 uid-generator 缺点是缺少 data center id，导致跨服务无法做到发号排重。同时也拥有以上问题。
无法理解百度为何去掉 data center id，如果用户觉得没用，可以把 data center id 置为 0。


## 本 ID 生成器结构如下：

> **delta seconds ==== data center id ==== worker id ==== sequence**
>
> (其中 data center id + worker id + sequence < 63)

即取消 sign 作为正负标识，取消总位数限制。当总位数 >= 64 时，改为使用 BigInteger 及其位运算实现。
经测试，使用 BigInteger 的实现发号速度仍可达到单机 110w 左右。

所以这是个可以运行时间无限长的雪花算法的 Java 实现。



