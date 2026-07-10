package com.macro.mall.common.log;

import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.macro.mall.common.domain.WebLog;
import com.macro.mall.common.util.RequestUtil;
import io.swagger.v3.oas.annotations.Operation;
import net.logstash.logback.marker.Markers;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 统一日志处理切面
 * Created by macro on 2018/4/26.
 */
@Aspect
@Component
@Order(1)
public class WebLogAspect {
    private static final Logger LOGGER = LoggerFactory.getLogger(WebLogAspect.class);
    private static final String MASKED_VALUE = "***";
    private static final Set<String> SENSITIVE_KEYS = new HashSet<>(List.of(
            "password",
            "oldpassword",
            "newpassword",
            "token",
            "tokenhead",
            "authorization",
            "secret",
            "secretkey",
            "accesskey",
            "accesskeyid",
            "accesskeysecret",
            "apikey",
            "api-key",
            "privatekey",
            "appprivatekey"
    ));

    /**
     * 定义切点：拦截controller包下的所有public方法
     */
    @Pointcut("execution(public * com.macro.mall.controller.*.*(..))||execution(public * com.macro.mall.*.controller.*.*(..))")
    public void webLog() {
    }

    /**
     * 前置通知（暂不处理日志记录）
     * @param joinPoint 切点
     */
    @Before("webLog()")
    public void doBefore(JoinPoint joinPoint) throws Throwable {
    }

    /**
     * 后置返回通知（暂不处理日志记录）
     * @param ret 返回值
     */
    @AfterReturning(value = "webLog()", returning = "ret")
    public void doAfterReturning(Object ret) throws Throwable {
    }

    /**
     * 环绕通知：记录请求日志信息（耗时、IP、参数、返回值等）
     * @param joinPoint 切点
     * @return 方法执行结果
     */
    @Around("webLog()")
    public Object doAround(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        //获取当前请求对象
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes.getRequest();
        //记录请求信息(通过Logstash传入Elasticsearch)
        WebLog webLog = new WebLog();
        Object result = joinPoint.proceed();
        Signature signature = joinPoint.getSignature();
        MethodSignature methodSignature = (MethodSignature) signature;
        Method method = methodSignature.getMethod();
        if (method.isAnnotationPresent(Operation.class)) {
            Operation log = method.getAnnotation(Operation.class);
            webLog.setDescription(log.summary());
        }
        long endTime = System.currentTimeMillis();
        String urlStr = request.getRequestURL().toString();
        webLog.setBasePath(StrUtil.removeSuffix(urlStr, URLUtil.url(urlStr).getPath()));
        webLog.setUsername(request.getRemoteUser());
        webLog.setIp(RequestUtil.getRequestIp(request));
        webLog.setMethod(request.getMethod());
        webLog.setParameter(maskSensitiveData(getParameter(method, joinPoint.getArgs())));
        webLog.setResult(maskSensitiveData(result));
        webLog.setSpendTime((int) (endTime - startTime));
        webLog.setStartTime(startTime);
        webLog.setUri(request.getRequestURI());
        webLog.setUrl(request.getRequestURL().toString());
        Map<String,Object> logMap = new HashMap<>();
        logMap.put("url",webLog.getUrl());
        logMap.put("method",webLog.getMethod());
        logMap.put("parameter",webLog.getParameter());
        logMap.put("spendTime",webLog.getSpendTime());
        logMap.put("description",webLog.getDescription());
//        LOGGER.info("{}", JSONUtil.parse(webLog));
        LOGGER.info(Markers.appendEntries(logMap), JSONUtil.parse(webLog).toString());
        return result;
    }

    /**
     * 根据方法和传入的参数获取请求参数
     */
    private Object getParameter(Method method, Object[] args) {
        List<Object> argList = new ArrayList<>();
        Parameter[] parameters = method.getParameters();
        for (int i = 0; i < parameters.length; i++) {
            //将RequestBody注解修饰的参数作为请求参数
            RequestBody requestBody = parameters[i].getAnnotation(RequestBody.class);
            if (requestBody != null) {
                argList.add(args[i]);
            }
            //将RequestParam注解修饰的参数作为请求参数
            RequestParam requestParam = parameters[i].getAnnotation(RequestParam.class);
            if (requestParam != null) {
                Map<String, Object> map = new HashMap<>();
                String key = parameters[i].getName();
                if (!StrUtil.isEmpty(requestParam.value())) {
                    key = requestParam.value();
                }
                if(args[i]!=null){
                    map.put(key, args[i]);
                    argList.add(map);
                }
            }
        }
        if (argList.size() == 0) {
            return null;
        } else if (argList.size() == 1) {
            return argList.get(0);
        } else {
            return argList;
        }
    }

    private Object maskSensitiveData(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof JSONObject jsonObject) {
            JSONObject masked = new JSONObject();
            for (String key : jsonObject.keySet()) {
                Object item = jsonObject.get(key);
                masked.set(key, isSensitiveKey(key) ? MASKED_VALUE : maskSensitiveData(item));
            }
            return masked;
        }
        if (value instanceof JSONArray jsonArray) {
            JSONArray masked = new JSONArray();
            for (Object item : jsonArray) {
                masked.add(maskSensitiveData(item));
            }
            return masked;
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> masked = new HashMap<>();
            map.forEach((key, item) -> {
                String keyText = String.valueOf(key);
                masked.put(keyText, isSensitiveKey(keyText) ? MASKED_VALUE : maskSensitiveData(item));
            });
            return masked;
        }
        if (value instanceof Collection<?> collection) {
            List<Object> masked = new ArrayList<>();
            for (Object item : collection) {
                masked.add(maskSensitiveData(item));
            }
            return masked;
        }
        if (value.getClass().isArray()) {
            JSONArray jsonArray = JSONUtil.parseArray(value);
            return maskSensitiveData(jsonArray);
        }
        if (isSimpleValue(value)) {
            return value;
        }
        try {
            return maskSensitiveData(JSONUtil.parse(value));
        } catch (Exception e) {
            return value.getClass().getSimpleName();
        }
    }

    private boolean isSensitiveKey(String key) {
        if (StrUtil.isBlank(key)) {
            return false;
        }
        String normalized = key.replace("_", "").replace("-", "").toLowerCase();
        if (SENSITIVE_KEYS.contains(key.toLowerCase()) || SENSITIVE_KEYS.contains(normalized)) {
            return true;
        }
        return normalized.contains("password")
                || normalized.contains("token")
                || normalized.contains("authorization")
                || normalized.contains("secret")
                || normalized.contains("apikey")
                || normalized.contains("privatekey");
    }

    private boolean isSimpleValue(Object value) {
        return value instanceof String
                || value instanceof Number
                || value instanceof Boolean
                || value instanceof Character
                || value instanceof Enum<?>;
    }
}
