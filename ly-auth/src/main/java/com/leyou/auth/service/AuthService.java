package com.leyou.auth.service;

import com.leyou.auth.properties.JwtProperties;
import com.leyou.common.auth.domain.Payload;
import com.leyou.common.auth.domain.UserInfo;
import com.leyou.common.auth.utils.JwtUtils;
import com.leyou.common.enums.ExceptionEnum;
import com.leyou.common.exceptions.LyException;
import com.leyou.common.utils.CookieUtils;
import com.leyou.user.UserClient;
import com.leyou.user.dto.UserDTO;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@Service
public class AuthService {

    @Autowired
    private UserClient userClient;

    @Autowired
    private JwtProperties jwtProp;

    @Autowired
    private StringRedisTemplate redisTemplate;
    /**
     * 登录验证
     *
     * @param username
     * @param password
     */
    public void login(String username, String password, HttpServletResponse response) {
        //判断用户名和密码是否正确
        UserDTO userDTO = userClient.findUserByUsernameAndPassword(username, password);

        if (userDTO == null) {
            //为空 抛异常
            throw new LyException(ExceptionEnum.INVALID_USERNAME_PASSWORD);
        }
        //创建载荷使用的用户对象
        UserInfo userInfo = new UserInfo(userDTO.getId(), userDTO.getUsername(), "ROLE_ADMIN");
        //生成token并把token写入到cookie中
        writeTokenToCookie(response, userInfo);
    }

    //生成token并将token写入到cookie中
    private void writeTokenToCookie(HttpServletResponse response, UserInfo userInfo) {
        //生成token
        String token = JwtUtils.generateTokenExpireInMinutes(userInfo,
                jwtProp.getPrivateKey(), jwtProp.getUser().getExpire());

        //将token写入cookie中
        CookieUtils.newCookieBuilder()
                .response(response)
                .name(jwtProp.getUser().getCookieName())
                .domain(jwtProp.getUser().getCookieDomain())
                .value(token)
                .httpOnly(true)
                .build();
    }

    /**
     * 验证用户是否登录
     * @param request
     * @param response
     * @return
     */
    public UserInfo verifyUser(HttpServletRequest request, HttpServletResponse response) {
        //获取token
        String token = CookieUtils.getCookieValue(request, jwtProp.getUser().getCookieName());
        if(StringUtils.isBlank(token)){
            throw new LyException(ExceptionEnum.UNAUTHORIZED);
        }
        //校验token是否合法
        Payload<UserInfo> infoPayload = null;
        try {
            infoPayload = JwtUtils.getInfoFromToken(token, jwtProp.getPublicKey(), UserInfo.class);
        }catch (Exception e){
            throw new LyException(ExceptionEnum.UNAUTHORIZED);
        }
        //获取UserInfo
        UserInfo userInfo = infoPayload.getUserInfo();

        // 获取token的id，校验黑名单
        String id = infoPayload.getId();
        Boolean boo = redisTemplate.hasKey(id);
        if (boo != null && boo) {
            // 抛出异常，证明token无效，直接返回401
            throw new LyException(ExceptionEnum.UNAUTHORIZED);
        }
        //得到当前token的过期时间
        Date expTime = infoPayload.getExpiration();
        //得到刷新token的时间点
        DateTime refreshTime = new DateTime(expTime).minusMinutes(jwtProp.getUser().getRefreshTime());
        //如果当前时间在刷新时间之后触发刷新token操作
        if(refreshTime.isBefore(System.currentTimeMillis())){
            //生成token并将token写入到Cookie中
            writeTokenToCookie(response, userInfo);
        }

        return userInfo;
    }

    /**
     * 登出
     * @param request
     * @param response
     */
    public void logout(HttpServletRequest request, HttpServletResponse response) {

        // 获取token
        String token = CookieUtils.getCookieValue(request, jwtProp.getUser().getCookieName());
        // 解析token
        Payload<UserInfo> payload = JwtUtils.getInfoFromToken(token, jwtProp.getPublicKey());
        // 获取id和有效期剩余时长
        String id = payload.getId();
        long time = payload.getExpiration().getTime() - System.currentTimeMillis();
        // 写入redis, 剩余时间超过5秒以上才写
        if (time > 5000) {
            redisTemplate.opsForValue().set(id, "", time, TimeUnit.MILLISECONDS);
        }
        // 删除cookie
        CookieUtils.deleteCookie(jwtProp.getUser().getCookieName(),
                                 jwtProp.getUser().getCookieDomain(),
                                 response);

    }
}
