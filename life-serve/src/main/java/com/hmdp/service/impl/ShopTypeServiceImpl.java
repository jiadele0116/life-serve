package com.hmdp.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {


    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Override
    public List<ShopType> redisList() {
        // 1.从redis中查询缓存
        String shopTypeJson = stringRedisTemplate.opsForValue().get("shopTypeList");
        
        // 2.判断是否存在
        if (StrUtil.isNotBlank(shopTypeJson)) {
            // 使用JSONArray解析列表
            JSONArray jsonArray = JSONUtil.parseArray(shopTypeJson);
            return jsonArray.toList(ShopType.class);
        }
        
        // 3.如果不存在，查询数据库
        List<ShopType> list = list();
        if (list == null || list.isEmpty()) {
            return new ArrayList<>();
        }

        // 4.将数据存入redis
        stringRedisTemplate.opsForValue().set("shopTypeList", JSONUtil.toJsonStr(list));
        
        // 5.返回
        return list;
    }
}
