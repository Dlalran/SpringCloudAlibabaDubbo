## Dubbo

​		官网：[Apache Dubbo](http://dubbo.apache.org/zh-cn/index.html), [Dubbo GitHub](https://github.com/apache/dubbo)

​		**一种思想是内部服务间远程调用可以使用Dubbo进行RPC实现，外部的访问再通过REST(RestTemplate、Feign)进行实现。**

​		*此时的Dubbo仅作为RPC的一种实现，一般不使用其如负载均衡、服务降级等附加功能，因为这些功能将通过Spring Cloud Alibaba的组件进行实现。*

#### 统一依赖管理

​		创建统一的依赖管理模块，其中包含Spring Boot、Spring Cloud、Spring Cloud Alibaba、Dubbo的以及他们之间的依赖。

```xml
    <dependencyManagement>
        <dependencies>
<!--            Spring Boot BOM-->
            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-dependencies</artifactId>
                <version>${spring-boot.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
<!--            Spring Cloud BOM-->
            <dependency>
                <groupId>org.springframework.cloud</groupId>
                <artifactId>spring-cloud-dependencies</artifactId>
                <version>${spring-cloud.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
<!--            Spring Cloud Alibaba BOM-->
            <dependency>
                <groupId>com.alibaba.cloud</groupId>
                <artifactId>spring-cloud-alibaba-dependencies</artifactId>
                <version>${spring-cloud-alibaba.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
<!--            Dubbo SpringBoot Starter-->
            <dependency>
                <groupId>org.apache.dubbo</groupId>
                <artifactId>dubbo-spring-boot-starter</artifactId>
                <version>${dubbo.version}</version>
            </dependency>
<!--            Dubbo-->
            <dependency>
                <groupId>org.apache.dubbo</groupId>
                <artifactId>dubbo</artifactId>
                <version>${dubbo.version}</version>
                <exclusions>
                    <exclusion>
                        <groupId>org.springframework</groupId>
                        <artifactId>spring</artifactId>
                    </exclusion>
                    <exclusion>
                        <groupId>javax.servlet</groupId>
                        <artifactId>servlet-api</artifactId>
                    </exclusion>
                    <exclusion>
                        <groupId>log4j</groupId>
                        <artifactId>log4j</artifactId>
                    </exclusion>
                </exclusions>
            </dependency>
<!--            Dubbo Actuator-->
            <dependency>
                <groupId>org.apache.dubbo</groupId>
                <artifactId>dubbo-spring-boot-actuator</artifactId>
                <version>${dubbo.version}</version>
            </dependency>
<!--            Alibaba Spring Context Support-->
            <dependency>
                <groupId>com.alibaba.spring</groupId>
                <artifactId>spring-context-support</artifactId>
                <version>${alibaba-spring-context-support.version}</version>
            </dependency>
        </dependencies>
    </dependencyManagement>
```



#### 服务接口

​		不同于Feign中的声明式服务接口仅提供给所在的服务使用，使用Dubbo时，接口提供给消费者以及服务提供者本身使用，因此建议将其独立化作为一个模块。

- 编写接口，示例如下

```java
package priv.howard.dubboprovider.api;

public interface ProviderService {
    /**
     * @Description 服务的API接口，基于Dubbo使用
     */
    String sayHello(String msg);
}
```



#### 服务提供者

1. 加入依赖

   ​		**如果是模块化开发，注意还要加入Spring Boot以及服务接口的依赖**

```xml
<!--        Dubbo-->
        <dependency>
            <groupId>org.apache.dubbo</groupId>
            <artifactId>dubbo</artifactId>
        </dependency>
        <dependency>
            <groupId>org.apache.dubbo</groupId>
            <artifactId>dubbo-spring-boot-starter</artifactId>
        </dependency>
<!--        Dubbo Nacos-->
        <dependency>
            <groupId>org.apache.dubbo</groupId>
            <artifactId>dubbo-registry-nacos</artifactId>
        </dependency>
<!--        Nacos Client-->
        <dependency>
            <groupId>com.alibaba.nacos</groupId>
            <artifactId>nacos-client</artifactId>
        </dependency>
<!--        Alibaba Spring Context Support-->
        <dependency>
            <groupId>com.alibaba.spring</groupId>
            <artifactId>spring-context-support</artifactId>
        </dependency>
<!--        服务接口-->
        <dependency>
            <groupId>priv.howard</groupId>
            <artifactId>DubboAPI</artifactId>
            <version>1.0-SNAPSHOT</version>
        </dependency>
```

2. 配置文件中进行Dubbo相关配置

```yml
dubbo:
#  扫描Dubbo注解的包，已在主配置类注解中指定
  #  scan:
  #    base-packages: priv.howard.dubboprovider.service
#  使用的RPC协议以及端口，端口号可以设置为-1以自动分配，一般默认为20880
  protocol:
    name: dubbo
    port: 20880
#    注册中心的地址，使用Nacos代替Zookeeper
  registry:
    address: nacos://localhost:8848
```

3. 编写服务实现

​		实现服务接口，并添加**Dubbo提供的注解**`@Service`，使其可以被远程调用，

​		*其中interfaceName参数指定实现的接口名，version指定服务版本，均可省略，详细查看Dubbo的Demo项目*

```java
package priv.howard.dubboprovider.service;

import org.apache.dubbo.config.annotation.Service;
import priv.howard.dubboprovider.api.ProviderService;

@Service(interfaceName = "priv.howard.dubboprovider.api.ProviderService", version = "1.0.0")
public class ProviderServiceImpl implements ProviderService {
    @Override
    public String sayHello(String msg) {
        return "Hello, " + msg + "!";
    }
}
```

4. 在主配置类中添加注解指定扫描注解的包名(也可以在配置文件中指定，见2中的代码注释)

```java
@EnableDubbo(scanBasePackages = "priv.howard.dubboprovider.service")
```



#### 服务消费者

​		服务消费者使用Dubbo通过接口对服务提供者提供的服务进行远程调用，并注册在Nacos通过REST对外提供服务。

1. 添加依赖与服务提供者相同，**注意在添加Spring Boot和服务接口依赖之外还要额外添加Spring Boot Web**，需要监控服务状况则再添加Spring Boot Actuator和Dubbo Spring Boot Actuator

2. 配置文件也与服务提供者相同，以下额外给出服务监控相关的配置

```yml
dubbo:
  protocol:
    name: dubbo
    port: 20880
  registry:
    address: nacos://localhost:8848

#暴露Dubbo监控端点
endpoints:
  dubbo:
    enabled: true

management:
#  开启Dubbo服务健康监控
  health:
    dubbo:
      status:
        defaults: memory
        extras: load,threadpool
#  暴露所有监控端点
  endpoints:
    web:
      exposure:
        include: "*"
```

3. 访问服务消费者测试是否能够间接调用服务提供者的服务，还可以通过`http://服务提供者地址/acturtor`查看所有可以查看的Dubbo监控端点，如``http://服务提供者地址/acturtor/dubbo/configs``等



#### 高速序列化

​		Dubbo RPC核心的是一种高性能、高吞吐量的远程调用方式，消费者与提供者间采用单一多路复用的TCP长连接进行数据传输。Dubbo默认的序列化协议(不同于通信协议)是基于Netty + Hessain的序列化方式，支持的序列化协议包括针对Java语言的Kryo、FST等，跨语言的ProtoStuff、ProtoBuf、Thrift、Avro等、针对JSON的Fastjson等。

​		**因此需要注意，涉及Dubbo远程调用的实体类都要实现序列化接口(如`public class User implements Serializable`)**

​		这里使用其中性能最优之一的Kryo进行序列化，Dubbo对其进行了整合并提供了jar包，并且对于常用的Java类(如ArrayList、HashMap、Object[]等)进行了序列化注册，因此使用Kryo序列化时仅需要对自定义的实体类(DTO、DO、PO)实现序列化接口即可(即`implements Serializable`)。

1. 统一依赖控制中添加Dubbo Kryo依赖，并在服务提供者和消费者应用中添加该依赖

```xml
<dependency>
    <groupId>org.apache.dubbo</groupId>
    <artifactId>dubbo-serialization-kryo</artifactId>
    <version>${dubbo.version}</version>
</dependency>
```

2. 服务提供者和消费者中加入配置`dubbo.protocol.serialization=kryo`，使得两者分别通过Kryo进行序列化和反序列化



#### 负载均衡

​		Dubbo自带的负载均衡包括四种策略，默认的是Random，即基于权重的随机负载均衡策略；还有RoundRobin，即基于权重的轮询负载均衡策略；以及LeastActive，即最少活跃调用数优先的负载均衡策略；最后是ConsistentHash，即一致性哈希负载均衡策略，对于相同参数的请求总是分发到相同的提供者。

​		由于Nacos默认的是轮询策略，因此可以将Dubbo的策略也指定为轮询以保持整体策略一致。

- 对指定服务指定负载均衡策略，在服务实现的注解中指定，如`@Service(loadbalance = "roundrobin")`

- 对整个服务提供者应用指定负载均衡策略，在配置文件中加入`dubbo.provider.loadbalance=roundrobin`

  *其他的策略名为上述策略名的小写形式。*



​		*Dubbo还有许多其他功能，如服务降级(Mock)、集群容错、服务分组等，由于这里Dubbo仅作为RPC的实现，因此不予赘述，详细查看Dubbo的Spring Boot Demo项目内容。*
