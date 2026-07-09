package com.macro.mall.portal.controller;

import com.macro.mall.common.api.CommonResult;
import com.macro.mall.portal.domain.AiChatResponse;
import com.macro.mall.portal.domain.AiCustomerChatParam;
import com.macro.mall.portal.domain.AiProviderStatus;
import com.macro.mall.portal.service.AiCustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@Tag(name = "AiCustomerController", description = "AI智能客服")
@RequestMapping("/ai/customer")
public class AiCustomerController {
    private final AiCustomerService aiCustomerService;

    public AiCustomerController(AiCustomerService aiCustomerService) {
        this.aiCustomerService = aiCustomerService;
    }

    @Operation(summary = "AI客服问答")
    @RequestMapping(value = "/chat", method = RequestMethod.POST)
    @ResponseBody
    public CommonResult<AiChatResponse> chat(@RequestBody AiCustomerChatParam param) {
        return CommonResult.success(aiCustomerService.chat(param));
    }

    @Operation(summary = "AI provider状态")
    @RequestMapping(value = "/status", method = RequestMethod.GET)
    @ResponseBody
    public CommonResult<AiProviderStatus> status() {
        return CommonResult.success(aiCustomerService.status());
    }
}
