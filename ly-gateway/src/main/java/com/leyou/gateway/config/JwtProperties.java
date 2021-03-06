package com.leyou.gateway.config;

import com.leyou.common.auth.utils.RsaUtils;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;

import javax.annotation.PostConstruct;
import java.security.PublicKey;

@Data
@Slf4j
@ConfigurationProperties(prefix = "ly.jwt")
public class JwtProperties {

    private String pubKeyPath;
    private PublicKey publicKey;
    private JwtUser user = new JwtUser();

    @Data
    public class JwtUser{
        private String cookieName;
    }

    //<bean id="" class="" init-method="" destroy-method=""/>
    //在对象彻底创建成功之后执行
    @PostConstruct
    public void init() throws Exception {
        publicKey = RsaUtils.getPublicKey(pubKeyPath);
    }

}