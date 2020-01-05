package priv.howard.dubboconsumer.controller;

import org.apache.dubbo.config.annotation.Reference;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import priv.howard.dubboprovider.api.ProviderService;

@RestController
public class ServiceController {
    /**
     * @Description 服务消费者的Controller，通过Dubbo实现对于服务的远程调用，同时又注册到Nacos提供服务
     */
    @Reference(version = "1.0.0")
    private ProviderService providerService;

    @GetMapping("/hello/{msg}")
    public String getHello(@PathVariable("msg") String msg) {
        return providerService.sayHello(msg);
    }
}
