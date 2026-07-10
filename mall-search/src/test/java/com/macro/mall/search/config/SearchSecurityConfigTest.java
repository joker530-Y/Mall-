package com.macro.mall.search.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class SearchSecurityConfigTest {

    @Test
    void writeEndpoint_shouldRejectWhenTokenMissing() throws Exception {
        SearchSecurityConfig.ManageTokenFilter filter = new SearchSecurityConfig.ManageTokenFilter("secret-token");
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/esProduct/importAll");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatus());
        verify(chain, never()).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void writeEndpoint_shouldAcceptValidManageToken() throws Exception {
        SearchSecurityConfig.ManageTokenFilter filter = new SearchSecurityConfig.ManageTokenFilter("secret-token");
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/esProduct/importAll");
        request.addHeader("X-Manage-Token", "secret-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        try {
            filter.doFilter(request, response, chain);
            assertEquals(200, response.getStatus() == 0 ? 200 : response.getStatus());
            assertNotNull(SecurityContextHolder.getContext().getAuthentication());
            verify(chain).doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void readEndpoint_shouldPassWithoutToken() throws Exception {
        SearchSecurityConfig.ManageTokenFilter filter = new SearchSecurityConfig.ManageTokenFilter("secret-token");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/esProduct/search/simple");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }
}
