package com.macro.mall.portal.service.impl;

import com.macro.mall.common.exception.ApiException;
import com.macro.mall.mapper.OmsCartItemMapper;
import com.macro.mall.model.OmsCartItem;
import com.macro.mall.model.UmsMember;
import com.macro.mall.portal.service.UmsMemberService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OmsCartItemServiceSecurityTest {

    @InjectMocks
    private OmsCartItemServiceImpl cartItemService;

    @Mock
    private OmsCartItemMapper cartItemMapper;
    @Mock
    private UmsMemberService memberService;

    @Test
    void updateAttr_shouldRejectCartItemOwnedByAnotherMember() {
        UmsMember currentMember = member(1L);
        OmsCartItem existing = cartItem(10L, 2L, 0);
        OmsCartItem request = cartItem(10L, null, null);

        when(memberService.getCurrentMember()).thenReturn(currentMember);
        when(cartItemMapper.selectByPrimaryKey(10L)).thenReturn(existing);

        ApiException exception = assertThrows(ApiException.class, () -> cartItemService.updateAttr(request));

        assertTrue(exception.getMessage().contains("购物车项不存在"));
        verify(cartItemMapper, never()).updateByPrimaryKeySelective(existing);
    }

    private UmsMember member(Long id) {
        UmsMember member = new UmsMember();
        member.setId(id);
        member.setNickname("member-" + id);
        return member;
    }

    private OmsCartItem cartItem(Long id, Long memberId, Integer deleteStatus) {
        OmsCartItem cartItem = new OmsCartItem();
        cartItem.setId(id);
        cartItem.setMemberId(memberId);
        cartItem.setDeleteStatus(deleteStatus);
        return cartItem;
    }
}
