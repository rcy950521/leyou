package com.leyou.user.controller;

import com.leyou.common.exceptions.LyException;
import com.leyou.user.domain.User;
import com.leyou.user.dto.UserDTO;
import com.leyou.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.stream.Collectors;

@RestController
@RequestMapping
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * @param user 必须加@Valid才能使用hibernate的验证
     * @param result BindingResult必须放在要验证的对象后面，才能接收验证结果
     * @param code
     * @return
     */
    @PostMapping("/register")
    public ResponseEntity<Void> register(@Valid User user,
                                         BindingResult result,
                                         @RequestParam("code") String code){
        //如果有异常，则抛出我们自定义的异常
        if (result.hasErrors()){
            String error = result.getFieldErrors()
                    .stream()
                    .map(FieldError::getDefaultMessage)
                    .collect(Collectors.joining("|"));
            throw new LyException(400, error);
        }
        userService.register(user,code);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    //http://api.leyou.com/api/user/check/444444/1
    //数据验证
    @GetMapping("/check/{data}/{type}")
    public ResponseEntity<Boolean> checkUsernameOrPhone(@PathVariable("data") String data,
                                                        @PathVariable("type") Integer type){
        Boolean result = userService.checkUsernameOrPhone(data,type);
        return ResponseEntity.ok(result);
    }

    /**
     * 发送短信验证码
     * POST /code
     */
    @PostMapping("/code")
    public ResponseEntity<Void> sendCheckCode(@RequestParam("phone")String phone){
        userService.sendCheckCode(phone);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    /**
     * 根据用户名和密码查询用户
     * GET /query
     */
    @GetMapping("/query")
    public ResponseEntity<UserDTO> findUserByUsernameAndPassword(@RequestParam("username") String username,
                                                                 @RequestParam("password") String Password){
        UserDTO userDTO = userService.findUserByUsernameAndPassword(username,Password);
        return ResponseEntity.ok(userDTO);
    }
}
