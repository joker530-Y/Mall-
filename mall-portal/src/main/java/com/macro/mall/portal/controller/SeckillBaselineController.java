package com.macro.mall.portal.controller;

import com.macro.mall.common.api.CommonResult;
import com.macro.mall.portal.domain.BaselineSeckillOrderParam;
import com.macro.mall.portal.domain.BaselineSeckillOrderResult;
import com.macro.mall.portal.service.SeckillBaselineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@Tag(name = "SeckillBaselineController", description = "Baseline seckill order")
@RequestMapping("/seckill/baseline")
public class SeckillBaselineController {
    @Autowired
    private SeckillBaselineService seckillBaselineService;

    @Operation(summary = "Generate baseline seckill order")
    @RequestMapping(value = "/order", method = RequestMethod.POST)
    @ResponseBody
    public CommonResult<BaselineSeckillOrderResult> generateOrder(@RequestBody BaselineSeckillOrderParam orderParam) {
        BaselineSeckillOrderResult result = seckillBaselineService.generateOrder(orderParam);
        return CommonResult.success(result, "baseline seckill order created");
    }
}
