package com.macro.mall.service.impl;

import cn.hutool.core.util.StrUtil;
import com.github.pagehelper.PageHelper;
import com.macro.mall.mapper.SmsFlashPromotionMapper;
import com.macro.mall.model.SmsFlashPromotion;
import com.macro.mall.model.SmsFlashPromotionExample;
import com.macro.mall.service.SmsFlashPromotionService;
import com.macro.mall.service.PortalHotCacheInvalidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * 限时购活动管理Service实现类
 * Created by macro on 2018/11/16.
 */
@Service
public class SmsFlashPromotionServiceImpl implements SmsFlashPromotionService {
    @Autowired
    private SmsFlashPromotionMapper flashPromotionMapper;
    @Autowired
    private PortalHotCacheInvalidator portalHotCacheInvalidator;

    @Override
    public int create(SmsFlashPromotion flashPromotion) {
        flashPromotion.setCreateTime(new Date());
        int count = flashPromotionMapper.insert(flashPromotion);
        portalHotCacheInvalidator.invalidateHomeCatalog();
        return count;
    }

    @Override
    public int update(Long id, SmsFlashPromotion flashPromotion) {
        flashPromotion.setId(id);
        int count = flashPromotionMapper.updateByPrimaryKey(flashPromotion);
        portalHotCacheInvalidator.invalidateHomeCatalog();
        return count;
    }

    @Override
    public int delete(Long id) {
        int count = flashPromotionMapper.deleteByPrimaryKey(id);
        portalHotCacheInvalidator.invalidateHomeCatalog();
        return count;
    }

    @Override
    public int updateStatus(Long id, Integer status) {
        SmsFlashPromotion flashPromotion = new SmsFlashPromotion();
        flashPromotion.setId(id);
        flashPromotion.setStatus(status);
        int count = flashPromotionMapper.updateByPrimaryKeySelective(flashPromotion);
        portalHotCacheInvalidator.invalidateHomeCatalog();
        return count;
    }

    @Override
    public SmsFlashPromotion getItem(Long id) {
        return flashPromotionMapper.selectByPrimaryKey(id);
    }

    @Override
    public List<SmsFlashPromotion> list(String keyword, Integer pageSize, Integer pageNum) {
        PageHelper.startPage(pageNum, pageSize);
        SmsFlashPromotionExample example = new SmsFlashPromotionExample();
        if (!StrUtil.isEmpty(keyword)) {
            example.createCriteria().andTitleLike("%" + keyword + "%");
        }
        return flashPromotionMapper.selectByExample(example);
    }
}
