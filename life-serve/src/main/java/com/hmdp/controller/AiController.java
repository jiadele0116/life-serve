package com.hmdp.controller;

import com.hmdp.ai.AiAssistant;
import com.hmdp.dto.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * AI 智能客服控制器
 */
@Slf4j
@RestController
@RequestMapping("/ai")
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST})
public class AiController {

    @Resource
    private AiAssistant aiAssistant;

    /**
     * 与 AI 智能客服对话
     * @param message 用户消息
     * @return AI 回复
     */
    @PostMapping("/chat")
    public Result chat(@RequestBody String message) {
        log.info("用户发送消息: {}", message);
        try {
            String response = aiAssistant.chat(message);
            log.info("AI 回复: {}", response);
            return Result.ok(response);
        } catch (Exception e) {
            log.error("AI 对话失败", e);
            return Result.fail("AI 服务暂时不可用，请稍后再试");
        }
    }

    /**
     * 简单的 GET 接口，用于测试
     */
    @GetMapping("/chat")
    public Result chatGet(@RequestParam("message") String message) {
        log.info("用户发送消息(GET): {}", message);
        try {
            String response = aiAssistant.chat(message);
            return Result.ok(response);
        } catch (Exception e) {
            log.error("AI 对话失败", e);
            return Result.fail("AI 服务暂时不可用，请稍后再试");
        }
    }
}
