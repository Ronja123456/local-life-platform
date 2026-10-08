package com.hmdp.controller;

import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.service.IShopService;
import com.hmdp.utils.DeepSeekClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@Slf4j
@RestController
@RequestMapping("/ai")
public class AiChatController {

    @Resource
    private DeepSeekClient deepSeekClient;

    @Resource
    private IShopService shopService;

    @PostMapping("/chat")
    public Result chat(@RequestParam Long shopId, @RequestParam String question) {
        // 1. 查商铺信息
        Shop shop = shopService.getById(shopId);
        if (shop == null) {
            return Result.fail("商铺不存在");
        }

        // 2. 拼 prompt
        String prompt = String.format(
                "你是商铺智能客服。以下是商铺信息：\n" +
                        "名称：%s\n地址：%s\n营业时间：%s\n人均消费：%s\n" +
                        "用户问题：%s\n" +
                        "请基于以上商铺信息回答，不要编造信息。",
                shop.getName(), shop.getAddress(), shop.getOpenHours(),
                shop.getAvgPrice(), question
        );

        // 3. 调大模型
        String answer = deepSeekClient.chat(prompt);

        // 4. 返回
        return Result.ok(answer);
    }
}