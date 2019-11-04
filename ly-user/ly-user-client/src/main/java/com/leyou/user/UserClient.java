package com.leyou.user;

import com.leyou.user.dto.UserDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient("user-service")
public interface UserClient {

    /**
     * 根据用户名和密码查询用户
     * @param username
     * @param Password
     * @return
     */
    @GetMapping("/query")
    public UserDTO findUserByUsernameAndPassword(@RequestParam("username") String username,
                                                 @RequestParam("password") String Password);
}
