package com.macro.mall.demo.service.impl;

import com.github.pagehelper.PageHelper;
import com.macro.mall.demo.dto.PmsBrandDto;
import com.macro.mall.demo.service.DemoService;
import com.macro.mall.mapper.PmsBrandMapper;
import com.macro.mall.model.PmsBrand;
import com.macro.mall.model.PmsBrandExample;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * DemoService实现类
 * Created by macro on 2019/4/8.
 */
@Service
public class DemoServiceImpl implements DemoService {
    /** 品牌Mapper */
    @Autowired
    private PmsBrandMapper brandMapper;

    /**
     * 获取所有品牌
     * @return 品牌列表
     */
    @Override
    public List<PmsBrand> listAllBrand() {
        return brandMapper.selectByExample(new PmsBrandExample());
    }

    /**
     * 创建品牌
     * @param pmsBrandDto 品牌DTO
     * @return 影响行数
     */
    @Override
    public int createBrand(PmsBrandDto pmsBrandDto) {
        PmsBrand pmsBrand = new PmsBrand();
        BeanUtils.copyProperties(pmsBrandDto,pmsBrand);
        return brandMapper.insertSelective(pmsBrand);
    }

    /**
     * 更新品牌
     * @param id 品牌ID
     * @param pmsBrandDto 品牌DTO
     * @return 影响行数
     */
    @Override
    public int updateBrand(Long id, PmsBrandDto pmsBrandDto) {
        PmsBrand pmsBrand = new PmsBrand();
        BeanUtils.copyProperties(pmsBrandDto,pmsBrand);
        pmsBrand.setId(id);
        return brandMapper.updateByPrimaryKeySelective(pmsBrand);
    }

    /**
     * 删除品牌
     * @param id 品牌ID
     * @return 影响行数
     */
    @Override
    public int deleteBrand(Long id) {
        return brandMapper.deleteByPrimaryKey(id);
    }

    /**
     * 分页查询品牌列表
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @return 品牌列表
     */
    @Override
    public List<PmsBrand> listBrand(int pageNum, int pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        return brandMapper.selectByExample(new PmsBrandExample());
    }

    /**
     * 根据ID获取品牌详情
     * @param id 品牌ID
     * @return 品牌信息
     */
    @Override
    public PmsBrand getBrand(Long id) {
        return brandMapper.selectByPrimaryKey(id);
    }
}
