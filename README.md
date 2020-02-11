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
#### ①.主要配置项
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
#### ①.主要配置项
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
#### 1.pom.xml 文件中添加Redis依赖
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
	<version>2.9.0</version>
</dependency>
```
#### 2.application.yml属性配置

