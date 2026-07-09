package com.macro.mall.controller;

import com.macro.mall.common.api.CommonResult;
import com.macro.mall.dto.SeckillManageSummary;
import com.macro.mall.dto.SeckillManageWarmupResult;
import com.macro.mall.dto.SeckillOrderLogItem;
import com.macro.mall.service.SeckillManageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@Tag(name = "SeckillManageController", description = "Seckill management dashboard")
@RequestMapping("/seckill/manage")
public class SeckillManageController {
    @Autowired
    private SeckillManageService seckillManageService;

    @Operation(summary = "Warm up seckill stock from admin console")
    @RequestMapping(value = "/warmup/{relationId}", method = RequestMethod.POST)
    @ResponseBody
    public CommonResult<SeckillManageWarmupResult> warmup(@PathVariable Long relationId) {
        return CommonResult.success(seckillManageService.warmup(relationId));
    }

    @Operation(summary = "Get seckill dashboard summary")
    @RequestMapping(value = "/summary", method = RequestMethod.GET)
    @ResponseBody
    public CommonResult<SeckillManageSummary> summary(@RequestParam Long relationId) {
        return CommonResult.success(seckillManageService.summary(relationId));
    }

    @Operation(summary = "List seckill order logs")
    @RequestMapping(value = "/orderLogs", method = RequestMethod.GET)
    @ResponseBody
    public CommonResult<Map<String, Object>> orderLogs(@RequestParam Long relationId,
                                                       @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize,
                                                       @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum) {
        List<SeckillOrderLogItem> list = seckillManageService.listOrderLogs(relationId, pageSize, pageNum);
        Map<String, Object> data = new HashMap<>();
        data.put("pageNum", pageNum);
        data.put("pageSize", pageSize);
        data.put("total", seckillManageService.countOrderLogs(relationId));
        data.put("list", list);
        return CommonResult.success(data);
    }
}
