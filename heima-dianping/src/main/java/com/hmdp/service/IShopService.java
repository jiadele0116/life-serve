package com.hmdp.service;

import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface IShopService extends IService<Shop> {



    Result queryById(Long id);

    Result update(Shop shop);

    /**
     * 根据用户位置查询附近商铺
     * @param typeId 商铺类型ID（可选）
     * @param current 页码
     * @param x 用户经度
     * @param y 用户纬度
     * @return 商铺列表（含距离）
     */
    Result queryShopByNearby(Integer typeId, Integer current, Double x, Double y);
}
