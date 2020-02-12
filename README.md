### Redis 哨兵模式

#### 1.服务器类型
| 服务器名称  | 节点类型       | IP地址 | 端口 
| -------| -------- | -------- | ---- 
| Node1 | Redis服务1(主节点Master) | 192.168.14.101   |   6379   |  
| Node2 | Redis服务2(从节点slave1) | 192.168.14.102   |   6380   |  
| Node3 | Redis服务3(从节点slave2) | 192.168.14.103   |   6381   |  
| Sentinel1 | 哨兵服务1 | 192.168.14.101   |   26379   |  
| Sentinel2 | 哨兵服务2 | 192.168.14.102   |   26380   |  
| Sentinel3 | 哨兵服务3 | 192.168.14.103   |   26381   |  


#### 2.搭建主从模式
##### ①.主要配置项
主服务器配置文件redis.conf配置项
```$xslt
#配置端口
port 6379
#以守护进程模式启动
daemonize yes
#pid的存放文件
pidfile /var/run/redis_6379.pid
#日志文件名
logfile "redis_6379.log"
#存放备份文件以及日志等文件的目录
dir "/opt/redis/data"  
```

从服务器配置文件主要配置项基本和主服务器保持一致,需要修改端口 port ;
另外存放位置和日志文件名也可以根据需要修改.
为了表示主从关系,还需要在从服务器配置文件中添加一行重要配置:
```$xslt
#配置主服务器IP,端口
slaveof 192.168.14.101 6379
```

#### 3.搭建哨兵模式
##### ①.主要配置项
```$xslt
#配置端口
port 26379
#以守护进程模式启动
daemonize yes
#日志文件名
logfile "sentinel_26379.log"
#存放备份文件以及日志等文件的目录
dir "/opt/redis/data" 
#监控的IP 端口号 名称 sentinel通过投票后认为mater宕机的数量，此处为至少2个
sentinel monitor mymaster 192.168.14.101 6379 2
#30秒ping不通主节点的信息，主观认为master宕机
sentinel down-after-milliseconds mymaster 30000
#故障转移后重新主从复制，1表示串行，>1并行
sentinel parallel-syncs mymaster 1
#故障转移开始，三分钟内没有完成，则认为转移失败
sentinel failover-timeout mymaster 180000
```
#### 4.Java客户端连接哨兵集群
##### 1.pom.xml 文件中添加Redis依赖
```$xslt
<!--spirngboot版本为2.x-->
<!-- 加载spring boot redis包,springboot2.0中直接使用jedis或者lettuce配置连接池，默认为lettuce连接池，这里使用jedis连接池 -->
<!-- 加载spring boot redis包 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
    <!-- 排除lettuce包，使用jedis代替-->
    <exclusions>
        <exclusion>
            <groupId>io.lettuce</groupId>
            <artifactId>lettuce-core</artifactId>
        </exclusion>
    </exclusions>
</dependency>

<dependency>
    <groupId>redis.clients</groupId>
    <artifactId>jedis</artifactId>
</dependency>
```
##### 2.application.yml属性配置
```$xslt
spring:
    redis:
      #哨兵节点配置
      sentinel:
        nodes: 192.168.14.101:26379,192.168.14.102:26380,192.168.14.103:26381
        master: mymaster
      #redis节点密码
      password: 123
      database: 0
```
##### 3.连接代码示例
 详细情况见项目 RedisSentinelApplicationTests.class

#### 5.Java客户端连接原理
连接步骤 
一.客户端遍历所有的 Sentinel 节点集合,获取一个可用的 Sentinel 节点.  
二.客户端向可用的 Sentinel 节点发送 get-master-addr-by-name 命令,获取Redis Master 节点.  
三.客户端向Redis Master节点发送role或role replication 命令,来确定其是否是Master节点,并且能够获取其 slave节点信息.  
四.客户端获取到确定的节点信息后,便可以向Redis发送命令来进行后续操作了  

#### 6.哨兵模式工作原理
　　①、三个定时任务
　　一.每10秒每个 sentinel 对master 和 slave 执行info 命令:该命令第一个是用来发现slave节点,第二个是确定主从关系.  
　　二.每2秒每个 sentinel 通过 master 节点的 channel(名称为_sentinel_:hello) 交换信息(pub/sub):用来交互对节点的看法(后面会介绍的节点主观下线和客观下线)以及自身信息.  
　　三.每1秒每个 sentinel 对其他 sentinel 和 redis 执行 ping 命令,用于心跳检测,作为节点存活的判断依据.  

　　②、主观下线和客观下线  
　　一.主观下线  
　　SDOWN:subjectively down,直接翻译的为”主观”失效,即当前sentinel实例认为某个redis服务为”不可用”状态.  
　　二.客观下线  
　　ODOWN:objectively down,直接翻译为”客观”失效,即多个sentinel实例都认为master处于”SDOWN”状态,那么此时master将处于ODOWN,ODOWN可以简单理解为master已经被集群确定为”不可用”,将会开启故障转移机制.  
```aidl
#监控的IP 端口号 名称 sentinel通过投票后认为mater宕机的数量，此处为至少2个
sentinel monitor mymaster 192.168.14.101 6379 2
```
   最后的 2 表示投票数,也就是说当一台 sentinel 发现一个 Redis 服务无法 ping 通时,就标记为 主观下线 sdown;同时另外的 sentinel 服务也发现该 Redis 服务宕机,也标记为 主观下线,当多台 sentinel (大于等于2,上面配置的最后一个)时,都标记该Redis服务宕机,这时候就变为客观下线了,然后进行故障转移.  
   ③、故障转移  
   故障转移是由 sentinel 领导者节点来完成的(只需要一个sentinel节点),关于 sentinel 领导者节点的选取也是每个 sentinel 向其他 sentinel 节点发送我要成为领导者的命令,超过半数sentinel 节点同意,并且也大于quorum ,那么他将成为领导者,如果有多个sentinel都成为了领导者,则会过段时间在进行选举.  
   sentinel 领导者节点选举出来后,会通过如下几步进行故障转移:  
   一.从 slave 节点中选出一个合适的 节点作为新的master节点.这里的合适包括如下几点:

　　　　1.选择 slave-priority(slave节点优先级)最高的slave节点,如果存在则返回,不存在则继续下一步判断.

　　　　2.选择复制偏移量最大的 slave 节点(复制的最完整),如果存在则返回,不存在则继续.

　　　　3.选择runId最小的slave节点(启动最早的节点)

　　二.对上面选出来的 slave 节点执行 slaveof no one 命令让其成为新的 master 节点.

　　三.向剩余的 slave 节点发送命令,让他们成为新master 节点的 slave 节点,复制规则和前面设置的 parallel-syncs 参数有关.

　　四.更新原来master 节点配置为 slave 节点,并保持对其进行关注,一旦这个节点重新恢复正常后,会命令它去复制新的master节点信息.(注意:原来的master节点恢复后是作为slave的角色)

　　可以从 sentinel 日志中出现的几个消息来进行查看故障转移:

　　1.+switch-master:表示切换主节点(从节点晋升为主节点)

　　2.+sdown:主观下线

　　3.+odown:客观下线

　　4.+convert-to-slave:切换从节点(原主节点降为从节点)
