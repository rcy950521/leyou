package com.leyou.gateway.filter;

import com.leyou.common.auth.domain.Payload;
import com.leyou.common.auth.domain.UserInfo;
import com.leyou.common.auth.utils.JwtUtils;
import com.leyou.common.utils.CookieUtils;
import com.leyou.gateway.config.FilterProperties;
import com.leyou.gateway.config.JwtProperties;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.netflix.zuul.filters.support.FilterConstants;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;

import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.PRE_TYPE;

@Slf4j
@Component
@EnableConfigurationProperties({JwtProperties.class, FilterProperties.class})
public class AuthFilter extends ZuulFilter {

    @Autowired
    private JwtProperties jwtProp;

    @Autowired
    private FilterProperties filterProp;

    /**
     * 过滤类型，过滤器执行的时机
     * @return
     */
    @Override
    public String filterType() {
        return PRE_TYPE;
    }

    /**
     * 过滤器执行的顺序
     * @return
     */
    @Override
    public int filterOrder() {
        return FilterConstants.FORM_BODY_WRAPPER_FILTER_ORDER+1;
    }

    /**
     * 过滤器开关，true表示过滤器起作用
     * @return
     */
    @Override
    public boolean shouldFilter() {
        return true;
    }

    /**
     * 具体业务
     * @return
     * @throws ZuulException
     */
    @Override
    public Object run() throws ZuulException {
        //获得request域
        RequestContext ctx = RequestContext.getCurrentContext();
        HttpServletRequest request = ctx.getRequest();
        //验证当前是否在白名单中，如果在 直接return null
        Boolean isWhite = handlerWhite(filterProp, request.getRequestURI());
        if (isWhite){
            return null;
        }
        //得到cookie
        String token = CookieUtils.getCookieValue(request, jwtProp.getUser().getCookieName());
        //解析token 如果token不合法 则组织访问微服务
        Payload<UserInfo> payload = null;
        try {
             payload = JwtUtils.getInfoFromToken(token, jwtProp.getPublicKey(), UserInfo.class);
        }catch (Exception e){
            log.error("用户权限不足");
            ctx.setSendZuulResponse(false);
            ctx.setResponseStatusCode(401);
            return null;
        }
        //得到token中角色信息 放入请求头中 传递给其他微服务
        String role = payload.getUserInfo().getRole();
        ctx.addZuulRequestHeader("USER_ROLE",role);
        return null;
    }

    /**
     * 返回为true 表示为在白名单中
     * @param filterProp
     * @param requestURI
     */
    private Boolean handlerWhite(FilterProperties filterProp, String requestURI) {

        for (String allowPath:filterProp.getAllowPaths()){
            if (requestURI.startsWith(allowPath)){
                return true;
            }
        }
        return false;
    }
}
