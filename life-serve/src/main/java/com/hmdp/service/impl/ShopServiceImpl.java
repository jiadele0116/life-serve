package com.hmdp.service.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.CacheClient;
import com.hmdp.utils.RedisData;
import com.hmdp.utils.SystemConstants;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;

import static com.hmdp.utils.RedisConstants.CACHE_SHOP_KEY;
import static com.hmdp.utils.RedisConstants.LOCK_SHOP_KEY;
import static java.util.concurrent.TimeUnit.MINUTES;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
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
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {


    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private CacheClient cacheClient;

    @Value("${amap.key}")
    private String amapKey;

    @Value("${amap.nearby-search-url}")
    private String amapNearbySearchUrl;

    private final RestTemplate restTemplate = new RestTemplate();



    @Override
    public Result queryById(Long id) {
        //缓存穿透
        Shop shop =cacheClient.queryWithPassThrough(CACHE_SHOP_KEY, id, Shop.class, this::getById, 30L, MINUTES);
        //互斥锁解决缓存击穿
        //Shop shop = cacheClient.queryWithPassThrough(CACHE_SHOP_KEY, id, Shop.class, this::getById, 30L, MINUTES);
        //逻辑过期解决缓存击穿
        //Shop shop = cacheClient.queryWithLogicalExpire(CACHE_SHOP_KEY, id, Shop.class, this::getById, 30L, MINUTES);
        if (shop == null){
            return Result.fail("店铺不存在");
        }
        //返回
        return Result.ok(shop);
    }

    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);
/*    private Shop queryWithLogicalExpire(Long id) {
        String key = CACHE_SHOP_KEY + id;
        //从redis中查询缓存
        String shopJson = stringRedisTemplate.opsForValue().get(key);

        //判断是非存在
        if (StrUtil.isBlank(shopJson)) {
            //存在，直接返回
            return null;
        }
        //命中，需要先把json反序列化为对象
        RedisData redisData = JSONUtil.toBean(shopJson, RedisData.class);
        JSONObject data = (JSONObject) redisData.getData();
        Shop shop = JSONUtil.toBean(data, Shop.class);
        LocalDateTime expireTime = redisData.getExpireTime();
        //判断是否过期
        if (expireTime.isAfter(LocalDateTime.now())) {
            //未过期，直接返回
            return shop;
        }
        //过期，需要重建
        //缓存重建
        //获取互斥锁
        String lockKey = LOCK_SHOP_KEY + id;
        boolean isLock = tryLock(lockKey);
        //判断是否成功
        if (isLock) {
            //成功，开启独立线程，实现缓存重建
            CACHE_REBUILD_EXECUTOR.submit(() -> {
                //重建缓存
                try {
                    this.saveShop2Redis(id, 30L);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    //释放锁
                    unlock(lockKey);
                }
            });
        }
        //返回过期的数据
        return shop;
    }*/

/*    public Shop queryWithMutex(Long id){
        String key = CACHE_SHOP_KEY + id;
        //从redis中查询缓存
        String shopJson = stringRedisTemplate.opsForValue().get(key);

        //判断是非存在
        if (StrUtil.isNotBlank(shopJson)) {
            //存在，直接返回
            Shop shop = JSONUtil.toBean(shopJson, Shop.class);
            return shop;
        }
        if (shopJson != null) {
            //空值缓存
            return null;
        }
        //实现缓存重建
        //获取互斥锁
        String lockKey = LOCK_SHOP_KEY + id;
        Shop shop = null;
        try {
            boolean isLock = tryLock(lockKey);
            //判断是否成功
            while (!isLock){
                //失败，休眠并重试
                Thread.sleep(50);
                isLock = tryLock(lockKey);
            }

            //成功,根据id查询数据库
            shop = getById(id);
            //模拟重建延时
            Thread.sleep(200);

            //不存在，返回错误
            if (shop == null) {

                stringRedisTemplate.opsForValue().set(key, "", 2, MINUTES);
                return null;
            }
            //存在，将数据存入redis
            stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(shop), 30, MINUTES);


        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }finally {
            //释放互斥锁
            unlock(lockKey);
        }
        // 返回
        return shop;
    }*/
    private Shop queryWithPassThrough(Long id){
        String key = CACHE_SHOP_KEY + id;
        //从redis中查询缓存
        String shopJson = stringRedisTemplate.opsForValue().get(key);

        //判断是非存在
        if (StrUtil.isNotBlank(shopJson)) {
            //存在，直接返回
            Shop shop = JSONUtil.toBean(shopJson, Shop.class);
            return shop;
        }
        if (shopJson != null) {
            //空值缓存
            return null;
        }

        //如果不存在,根据id查询数据库
        Shop shop = getById(id);

        //不存在，返回错误
        if (shop == null) {

            stringRedisTemplate.opsForValue().set(key, "", 2, MINUTES);
            return null;
        }
        //存在，将数据存入redis
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(shop), 30, MINUTES);
        //返回

        return shop;
    }



/*    private boolean tryLock(String key){
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(flag);
    }

    private void unlock(String key){
        stringRedisTemplate.delete(key);
    }

    public void saveShop2Redis(Long id, Long expireSeconds) throws InterruptedException {
        //查询店铺数据
        Shop shop = getById(id);
        Thread.sleep(200);
        //封装过期时间
        RedisData redisData = new RedisData();
        redisData.setData(shop);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(expireSeconds));
        //写入redis
        stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id, JSONUtil.toJsonStr(redisData));
    }*/

    @Override
    @Transactional
    public Result update(Shop shop) {
        Long id = shop.getId();
        if (id == null) {
            return Result.fail("店铺id不能为空");
        }
        //1更新数据库
        updateById(shop);
        //2删除缓存
        stringRedisTemplate.delete(CACHE_SHOP_KEY + id);
        return Result.ok();
    }

    @Override
    public Result queryShopByNearby(Integer typeId, Integer current, Double x, Double y) {
        // 1.参数校验
        if (x == null || y == null) {
            return Result.fail("用户位置不能为空");
        }

        // 2.调用高德地图周边搜索API，获取指定范围内的POI
        // 中心点坐标格式：经度,纬度
        String location = x + "," + y;
        // 搜索半径，默认5000米
        int radius = 5000;
        // 请求高德API
        String url = String.format("%s?key=%s&location=%s&radius=%d&offset=25&page=%d&extensions=all",
                amapNearbySearchUrl, amapKey, location, radius, current);

        // 如果指定了商铺类型，添加类型过滤条件
        if (typeId != null) {
            url += "&types=" + getAmapTypeByTypeId(typeId);
        }

        String response = null;
        try {
            response = restTemplate.getForObject(url, String.class);
        } catch (Exception e) {
            // 高德API调用失败，降级为数据库按距离排序查询
            return fallbackQueryByDistance(typeId, current, x, y);
        }

        // 3.解析高德API返回结果
        if (StrUtil.isBlank(response)) {
            return Result.fail("调用高德地图API失败");
        }

        JSONObject jsonObject = JSONUtil.parseObj(response);
        String status = jsonObject.getStr("status");
        if (!"1".equals(status)) {
            // 高德API返回错误，降级为数据库查询
            return fallbackQueryByDistance(typeId, current, x, y);
        }

        JSONArray pois = jsonObject.getJSONArray("pois");
        if (pois == null || pois.isEmpty()) {
            return Result.ok(new ArrayList<>());
        }

        // 4.从高德返回的POI中提取商铺名称和位置，匹配数据库中的商铺
        List<Shop> shops = new ArrayList<>();
        for (int i = 0; i < pois.size(); i++) {
            JSONObject poi = pois.getJSONObject(i);
            String poiName = poi.getStr("name");
            String poiLocation = poi.getStr("location"); // 格式：经度,纬度
            Double poiDistance = poi.getDouble("distance"); // 单位：米

            // 根据名称在数据库中模糊匹配商铺
            if (StrUtil.isNotBlank(poiName)) {
                Shop shop = lambdaQuery()
                        .like(Shop::getName, poiName)
                        .last("LIMIT 1")
                        .one();
                if (shop != null) {
                    shop.setDistance(poiDistance);
                    shops.add(shop);
                }
            }
        }

        // 5.返回结果
        return Result.ok(shops);
    }

    /**
     * 降级方案：当高德API不可用时，使用数据库中的经纬度计算距离
     */
    private Result fallbackQueryByDistance(Integer typeId, Integer current, Double x, Double y) {
        // 查询所有商铺（可按类型筛选）
        List<Shop> shops;
        if (typeId != null) {
            shops = lambdaQuery().eq(Shop::getTypeId, typeId).list();
        } else {
            shops = list();
        }

        // 计算每个商铺到用户的距离
        for (Shop shop : shops) {
            if (shop.getX() != null && shop.getY() != null) {
                double distance = calculateDistance(y, x, shop.getY(), shop.getX());
                shop.setDistance(Math.round(distance * 100.0) / 100.0); // 保留两位小数
            }
        }

        // 按距离升序排序
        shops.sort(Comparator.comparingDouble(s -> s.getDistance() != null ? s.getDistance() : Double.MAX_VALUE));

        // 手动分页
        int pageSize = SystemConstants.DEFAULT_PAGE_SIZE;
        int fromIndex = (current - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, shops.size());

        if (fromIndex >= shops.size()) {
            return Result.ok(new ArrayList<>());
        }

        List<Shop> pageRecords = shops.subList(fromIndex, toIndex);
        return Result.ok(pageRecords);
    }

    /**
     * 使用Haversine公式计算两个经纬度坐标之间的距离（单位：米）
     */
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final double EARTH_RADIUS = 6371000; // 地球半径，单位：米
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS * c;
    }

    /**
     * 将系统中的商铺类型ID映射为高德地图POI类型编码
     */
    private String getAmapTypeByTypeId(Integer typeId) {
        // 高德地图POI类型编码，可根据实际业务映射
        // 参考文档：https://lbs.amap.com/api/webservice/download
        switch (typeId) {
            case 1: return "050000"; // 餐饮服务
            case 2: return "060000"; // 购物服务
            case 3: return "080000"; // 体育休闲服务
            case 4: return "090000"; // 医疗保健服务
            case 5: return "100000"; // 住宿服务
            case 6: return "110000"; // 风景名胜
            case 7: return "120000"; // 商务住宅
            case 8: return "140000"; // 科教文化服务
            case 9: return "150000"; // 交通设施服务
            case 10: return "170000"; // 公司企业
            default: return "050000"; // 默认餐饮服务
        }
    }
}
