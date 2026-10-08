package com.hmdp;



import com.hmdp.entity.Shop;
import com.hmdp.service.IShopService;
import com.hmdp.utils.RedisConstants;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@SpringBootTest
class ShopServiceImplTest {

    @Resource
    private IShopService shopService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Test
    void testSaveShop2Redis() {
        // 预热 id=1 的店铺，逻辑过期时间 10 秒
        shopService.saveShop2Redis(1L, 10L);
    }
    @Test
    void loadShopData() {
        // 1. 查询所有商铺
        List<Shop> shops = shopService.list();

        // 2. 按 typeId 分组
        Map<Long, List<Shop>> map = shops.stream()
                .collect(Collectors.groupingBy(Shop::getTypeId));

        // 3. 分批写入 Redis GEO
        for (Map.Entry<Long, List<Shop>> entry : map.entrySet()) {
            Long typeId = entry.getKey();
            String key = RedisConstants.SHOP_GEO_KEY + typeId;

            List<RedisGeoCommands.GeoLocation<String>> locations = new ArrayList<>();
            for (Shop shop : entry.getValue()) {
                // 每个店铺：id + 坐标
                locations.add(new RedisGeoCommands.GeoLocation<>(
                        shop.getId().toString(),
                        new Point(shop.getX(), shop.getY())
                ));
            }

            // 批量写入
            stringRedisTemplate.opsForGeo().add(key, locations);
        }

        System.out.println("GEO 数据导入完成！");
    }

}