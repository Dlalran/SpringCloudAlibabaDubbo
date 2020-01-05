package priv.howard.dubboprovider.service;

import org.apache.dubbo.config.annotation.Service;
import priv.howard.dubboprovider.api.ProviderService;

//将其作为Dubbo远程调用服务
@Service(interfaceName = "priv.howard.dubboprovider.api.ProviderService", version = "1.0.0")
public class ProviderServiceImpl implements ProviderService {
    @Override
    public String sayHello(String msg) {
        return "Hello, " + msg + "!";
    }
}
