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
所以这是个可以运行时间无限长的雪花算法的 Java 实现。

经测试，使用 BigInteger 的实现发号速度仍可达到单机 110w 左右。

具体实现中，data center id 需要手动置顶，worker id 是以 data center id 作为 key ，在 redis 中用 incr 指令获取的数字，再取模 max_worker_id 得到的。
max_worker_id 通过指定 worker id 的位数得到，如 worker id 位数为 8, max_worker_id 即 2^8 - 1 = 255。
取得当前 worker id 为 1，则 1%255 = 1；如取得为 256，则 256%255=1。即成一个环形的可复用的获取  worker id 的方式。

获取 worker id 的实现可根据需求自行修改。 


## 使用方式
- 引入除 Boostrap.java 外的文件。
- 配置 data.center.id，uid.timeBits，uid.dataCenterIdBits，uid.workerBits，uid.seqBits，uid.epochStr，或使用默认配置。
- 启动时使用 Spring 初始化需要的 Bean。
- 使用时注入 UidGenerator，调用 getUID() 方法即可。



