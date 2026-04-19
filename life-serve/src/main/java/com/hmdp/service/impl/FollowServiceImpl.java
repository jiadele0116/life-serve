package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Follow;
import com.hmdp.entity.User;
import com.hmdp.mapper.FollowMapper;
import com.hmdp.service.IFollowService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.service.IUserService;
import com.hmdp.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class FollowServiceImpl extends ServiceImpl<FollowMapper, Follow> implements IFollowService {

    @Resource
    private FollowMapper followMapper;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private IUserService userService;

    @Override
    public Result follow(Long followUserId, Boolean isFollow) {
        //获取用户
        Long userId = UserHolder.getUser().getId();
        String key = "follows" + userId;
        //判断是否关注
        if (isFollow) {
            //关注，新增数据
            Follow follow = new Follow();
            follow.setUserId(userId);
            follow.setFollowUserId(followUserId);
            boolean isSuccess = save(follow);
            if(isSuccess){
                //把关注用户的id，放入redis的set集合中
                stringRedisTemplate.opsForSet().add(key, followUserId.toString());
            }
        } else {
            //取关，删除数据
            followMapper.deleteFollow(userId, followUserId);
            //把关注的用户id，重redis的set集合中删除
            stringRedisTemplate.opsForSet().remove(key, followUserId.toString());
        }

        return Result.ok();
    }

    @Override
    public Result isFollow(Long followUserId) {
        //获取用户
        Long userId = UserHolder.getUser().getId();
        //查询是否关注
        Integer count = followMapper.selectIsFollow(userId, followUserId);
        //判断
        if (count > 0) {
            return Result.ok(true);
        }
        return Result.ok(false);
    }

    @Override
    public Result followCommons(Long id) {
        //获取当前用户
        Long userId = UserHolder.getUser().getId();
        String key1 = "follows" + userId;
        //求交集
        String key2 = "follows" + id;
        Set<String> intersect = stringRedisTemplate.opsForSet().intersect(key1, key2);
        if(intersect == null || intersect.isEmpty()){
            return Result.ok(Collections.emptyList());
        }
        //解析id集合
        List<Long> ids = intersect.stream().map(Long::valueOf).collect(Collectors.toList());
        //查询用户
        List<User> users = userService.listByIds(ids);
        List<UserDTO> userDTOS = users.stream()
                .map(user -> {
                    UserDTO dto = new UserDTO();
                    BeanUtil.copyProperties(user, dto);
                    return dto;
                })
                .collect(Collectors.toList());
        /*List<UserDTO> userDTOS = new ArrayList<>();
        for (User user : users) {
            UserDTO userDTO = new UserDTO();
            BeanUtil.copyProperties(user, userDTO);
            userDTOS.add(userDTO);
        }*/
        return Result.ok(userDTOS);
    }

    @Override
    public List<Long> queryFansIds(Long userId) {
        // 查询关注该用户的所有粉丝
        List<Follow> follows = query().eq("follow_user_id", userId).list();
        return follows.stream()
                .map(Follow::getUserId)
                .collect(Collectors.toList());
    }
}
