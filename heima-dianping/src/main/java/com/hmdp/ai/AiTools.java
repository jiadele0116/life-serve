package com.hmdp.ai;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.hmdp.entity.Shop;
import com.hmdp.entity.Voucher;
import com.hmdp.service.IShopService;
import com.hmdp.service.IVoucherService;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AI 智能客服工具类
 * 提供商铺查询、优惠券查询等功能供 AI 调用
 */
@Slf4j
@Component
public class AiTools {

    @Resource
    private IShopService shopService;

    @Resource
    private IVoucherService voucherService;

    /**
     * 根据商铺ID查询商铺详情
     */
    @Tool("根据商铺ID查询商铺的详细信息，包括名称、地址、评分、营业时间等")
    public String getShopById(Long shopId) {
        log.info("AI调用工具: getShopById, shopId={}", shopId);
        try {
            Shop shop = shopService.getById(shopId);
            if (shop == null) {
                return "未找到该商铺";
            }
            return formatShopInfo(shop);
        } catch (Exception e) {
            log.error("查询商铺失败", e);
            return "查询商铺信息失败: " + e.getMessage();
        }
    }

    /**
     * 根据商铺名称模糊搜索商铺
     */
    @Tool("根据商铺名称关键字搜索商铺列表，返回匹配的商铺信息")
    public String searchShopByName(String name) {
        log.info("AI调用工具: searchShopByName, name={}", name);
        try {
            QueryWrapper<Shop> wrapper = new QueryWrapper<>();
            wrapper.like("name", name).last("LIMIT 10");
            List<Shop> shops = shopService.list(wrapper);
            if (shops.isEmpty()) {
                return "未找到匹配的商铺";
            }
            return shops.stream()
                    .map(this::formatShopInfo)
                    .collect(Collectors.joining("\n---\n"));
        } catch (Exception e) {
            log.error("搜索商铺失败", e);
            return "搜索商铺失败: " + e.getMessage();
        }
    }

    /**
     * 根据商铺类型查询商铺列表
     */
    @Tool("根据商铺类型ID查询商铺列表，类型ID: 1-餐饮, 2-购物, 3-体育休闲, 4-医疗保健, 5-住宿, 6-风景名胜, 7-商务住宅, 8-科教文化, 9-交通设施, 10-公司企业")
    public String getShopsByType(Integer typeId) {
        log.info("AI调用工具: getShopsByType, typeId={}", typeId);
        try {
            QueryWrapper<Shop> wrapper = new QueryWrapper<>();
            wrapper.eq("type_id", typeId).last("LIMIT 10");
            List<Shop> shops = shopService.list(wrapper);
            if (shops.isEmpty()) {
                return "该类型下暂无商铺";
            }
            return shops.stream()
                    .map(this::formatShopInfo)
                    .collect(Collectors.joining("\n---\n"));
        } catch (Exception e) {
            log.error("查询商铺类型失败", e);
            return "查询商铺类型失败: " + e.getMessage();
        }
    }

    /**
     * 查询热门商铺（按销量排序）
     */
    @Tool("查询热门商铺列表，按销量排序，返回最受欢迎的商铺")
    public String getHotShops() {
        log.info("AI调用工具: getHotShops");
        try {
            QueryWrapper<Shop> wrapper = new QueryWrapper<>();
            wrapper.orderByDesc("sold").last("LIMIT 10");
            List<Shop> shops = shopService.list(wrapper);
            if (shops.isEmpty()) {
                return "暂无商铺数据";
            }
            return shops.stream()
                    .map(this::formatShopInfo)
                    .collect(Collectors.joining("\n---\n"));
        } catch (Exception e) {
            log.error("查询热门商铺失败", e);
            return "查询热门商铺失败: " + e.getMessage();
        }
    }

    /**
     * 查询商铺的优惠券
     */
    @Tool("查询指定商铺当前可用的优惠券信息，包括折扣力度、使用条件等")
    public String getShopVouchers(Long shopId) {
        log.info("AI调用工具: getShopVouchers, shopId={}", shopId);
        try {
            List<Voucher> vouchers = voucherService.lambdaQuery()
                    .eq(Voucher::getShopId, shopId)
                    .list();
            if (vouchers.isEmpty()) {
                return "该商铺暂无优惠券";
            }
            return vouchers.stream()
                    .map(this::formatVoucherInfo)
                    .collect(Collectors.joining("\n---\n"));
        } catch (Exception e) {
            log.error("查询优惠券失败", e);
            return "查询优惠券失败: " + e.getMessage();
        }
    }

    /**
     * 查询高评分商铺
     */
    @Tool("查询高评分商铺列表，返回评分最高的商铺推荐")
    public String getHighScoreShops() {
        log.info("AI调用工具: getHighScoreShops");
        try {
            QueryWrapper<Shop> wrapper = new QueryWrapper<>();
            wrapper.orderByDesc("score").last("LIMIT 10");
            List<Shop> shops = shopService.list(wrapper);
            if (shops.isEmpty()) {
                return "暂无商铺数据";
            }
            return shops.stream()
                    .map(this::formatShopInfo)
                    .collect(Collectors.joining("\n---\n"));
        } catch (Exception e) {
            log.error("查询高评分商铺失败", e);
            return "查询高评分商铺失败: " + e.getMessage();
        }
    }

    /**
     * 格式化商铺信息
     */
    private String formatShopInfo(Shop shop) {
        Map<String, Object> info = new HashMap<>();
        info.put("商铺ID", shop.getId());
        info.put("商铺名称", shop.getName());
        info.put("地址", shop.getAddress());
        info.put("商圈", shop.getArea());
        info.put("评分", shop.getScore() != null ? shop.getScore() / 10.0 + "分" : "暂无评分");
        info.put("均价", shop.getAvgPrice() != null ? "￥" + shop.getAvgPrice() + "/人" : "暂无数据");
        info.put("销量", shop.getSold());
        info.put("营业时间", shop.getOpenHours());
        return JSONUtil.toJsonPrettyStr(info);
    }

    /**
     * 格式化优惠券信息
     */
    private String formatVoucherInfo(Voucher voucher) {
        Map<String, Object> info = new HashMap<>();
        info.put("优惠券ID", voucher.getId());
        info.put("标题", voucher.getTitle());
        info.put("副标题", voucher.getSubTitle());
        info.put("类型", voucher.getType() == 1 ? "普通券" : "秒杀券");
        // payValue: 支付金额, actualValue: 抵扣金额 (单位: 分)
        if (voucher.getPayValue() != null && voucher.getActualValue() != null) {
            info.put("支付金额", "￥" + (voucher.getPayValue() / 100.0));
            info.put("抵扣金额", "￥" + (voucher.getActualValue() / 100.0));
        }
        info.put("使用规则", voucher.getRules());
        return JSONUtil.toJsonPrettyStr(info);
    }
}
