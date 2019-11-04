package com.leyou.user.service;

import com.leyou.common.constants.MQConstants;
import com.leyou.common.enums.ExceptionEnum;
import com.leyou.common.exceptions.LyException;
import com.leyou.common.utils.BeanHelper;
import com.leyou.user.domain.User;
import com.leyou.user.dto.UserDTO;
import com.leyou.user.mapper.UserMapper;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@Transactional
public class UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private AmqpTemplate amqpTemplate;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    //REDIS中前缀 方便以后查询
    private static final String CHECK_CODE_PRE = "check:code:pre:";

    /**
     * 验证注册
     *
     * @param user
     * @param code
     */
    public void register(User user, String code) {

        //从redis中得到验证码
        String checkCode = redisTemplate.opsForValue().get(CHECK_CODE_PRE + user.getPhone());

        //判断验证码是否正确
        if (!StringUtils.equals(checkCode,code)) {
            //不正确的话就抛异常
            throw new LyException(ExceptionEnum.INVALID_VERIFY_CODE);
        }

        //将密码加密
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        //将user对象写入数据库
        userMapper.insertSelective(user);
    }

    /**
     * 判断用户名和手机号是否重复
     * @param data
     * @param type
     * @return
     */
    public Boolean checkUsernameOrPhone(String data, Integer type) {
        User user = new User();
        switch (type) {
            case 1:
                user.setUsername(data);
                break;
            case 2:
                user.setPhone(data);
                break;
            default:
                throw new LyException(ExceptionEnum.INVALID_PARAM_ERROR);
        }
        int count = userMapper.selectCount(user);
        //如果返回为0 则表示该数据在数据库没有相同
        return count==0;
    }

    /**
     * 发送短信验证码
     * @param phone
     */
    public void sendCheckCode(String phone) {
        //生成验证码
        String checkCode = RandomStringUtils.randomNumeric(6);
        //存入redis
        redisTemplate.opsForValue().set(CHECK_CODE_PRE+phone,checkCode,6, TimeUnit.HOURS);
        //将验证码发送到mq
        Map<String,String> codeMap = new HashMap<>();
        codeMap.put("phone",phone);
        codeMap.put("code",checkCode);
        amqpTemplate.convertAndSend(MQConstants.Exchange.SMS_EXCHANGE_NAME,
                                    MQConstants.RoutingKey.VERIFY_CODE_KEY,
                                    codeMap);
    }

    /**
     * 根据用户名和密码查询用户
     * @param username
     * @param password
     * @return
     */
    public UserDTO findUserByUsernameAndPassword(String username, String password) {
        try {
            //根据用户名查找用户
            User record = new User();
            record.setUsername(username);
            User user = userMapper.selectOne(record);
            //获取密码,判断密码是否正确
            if (passwordEncoder.matches(password,user.getPassword())){
                //密码正确
                return BeanHelper.copyProperties(user,UserDTO.class);
            }

            throw new LyException(ExceptionEnum.INVALID_USERNAME_PASSWORD);
        }catch (Exception e){
            throw new LyException(ExceptionEnum.CLIENT_TRANSFER_ERROR);

        }
    }
}