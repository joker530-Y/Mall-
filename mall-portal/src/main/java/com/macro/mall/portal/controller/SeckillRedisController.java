package com.macro.mall.portal.controller;

import com.macro.mall.common.api.CommonResult;
import com.macro.mall.portal.domain.SeckillOrderParam;
import com.macro.mall.portal.domain.SeckillOrderResult;
import com.macro.mall.portal.domain.SeckillOrderSubmitResult;
import com.macro.mall.portal.domain.SeckillWarmupResult;
import com.macro.mall.portal.service.SeckillRedisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@Tag(name = "SeckillRedisController", description = "Redis Lua MQ seckill")
@RequestMapping("/seckill/redis")
public class SeckillRedisController {
    @Autowired
    private SeckillRedisService seckillRedisService;

    @Operation(summary = "Warm up seckill stock into Redis")
    @RequestMapping(value = "/warmup/{relationId}", method = RequestMethod.POST)
    @ResponseBody
    public CommonResult<SeckillWarmupResult> warmup(@PathVariable Long relationId) {
        return CommonResult.forbidden(null);
    }

    @Operation(summary = "Submit seckill order through Redis Lua and MQ")
    @RequestMapping(value = "/order", method = RequestMethod.POST)
    @ResponseBody
    public CommonResult<SeckillOrderSubmitResult> submitOrder(@RequestBody SeckillOrderParam orderParam) {
        return CommonResult.success(seckillRedisService.submitOrder(orderParam), "QUEUING");
    }

    @Operation(summary = "Query seckill order result")
    @RequestMapping(value = "/result", method = RequestMethod.GET)
    @ResponseBody
    public CommonResult<SeckillOrderResult> getResult(@RequestParam Long relationId) {
        return CommonResult.success(seckillRedisService.getResult(relationId));
    }
}
