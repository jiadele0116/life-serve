package com.hmdp.processor;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.hmdp.dto.UserDTO;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.concurrent.TimeUnit;
@Slf4j
public class RefreshTokenInterceptor implements HandlerInterceptor {

    private StringRedisTemplate stringRedisTemplate;

    public RefreshTokenInterceptor(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }


    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //获取请求头中的token
        String token = request.getHeader("authorization");
        if (StrUtil.isBlank(token)) {
            //如果没有携带token，则返回401
           return true;
        }
        //log.info("拦截到请求，token:{}", token);

        //基于token获取redis中的用户
        Map<Object, Object> userMap = stringRedisTemplate.opsForHash().entries(
                RedisConstants.LOGIN_USER_KEY + token);
        if (userMap == null || userMap.isEmpty()) {
           return true;
        }
        //查询到的Hash值转换为UserDTO对象
        Object idObj = userMap.get("id");
        if (idObj == null) {
            // id为空，说明数据有问题，清除后放行
            stringRedisTemplate.delete(RedisConstants.LOGIN_USER_KEY + token);
            return true;
        }
        UserDTO userDTO = new UserDTO();
        userDTO.setId(Long.valueOf(idObj.toString()));
        userDTO.setNickName((String) userMap.get("nickName"));
        userDTO.setIcon((String) userMap.get("icon"));

        UserHolder.saveUser(userDTO);
        //刷新token有效期
        stringRedisTemplate.expire(RedisConstants.LOGIN_USER_KEY + token, 30, TimeUnit.MINUTES);
        //放行
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        UserHolder.removeUser();
    }
}
