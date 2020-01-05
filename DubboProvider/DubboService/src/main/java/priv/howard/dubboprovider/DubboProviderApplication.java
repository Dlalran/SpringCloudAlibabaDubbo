package priv.howard.dubboprovider;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

//指定要扫描注解的包名
@EnableDubbo(scanBasePackages = "priv.howard.dubboprovider.service")
@SpringBootApplication
public class DubboProviderApplication {
    public static void main(String[] args) {
        SpringApplication.run(DubboProviderApplication.class, args);
    }
}
