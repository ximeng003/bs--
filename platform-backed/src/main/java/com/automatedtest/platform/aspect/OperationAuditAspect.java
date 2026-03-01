package com.automatedtest.platform.aspect;

import com.automatedtest.platform.annotation.OperationAudit;
import com.automatedtest.platform.entity.User;
import com.automatedtest.platform.service.OperationLogService;
import com.automatedtest.platform.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;

@Slf4j
@Aspect
@Component
public class OperationAuditAspect {

    @Autowired
    private OperationLogService operationLogService;

    @Autowired
    private UserService userService;

    @Pointcut("@annotation(com.automatedtest.platform.annotation.OperationAudit)")
    public void auditPointcut() {}

    @Around("auditPointcut()")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        long startTime = System.currentTimeMillis();
        Object result = null;
        String errorMessage = null;

        try {
            result = point.proceed();
            
            // Check if Result object indicates failure
            if (result instanceof com.automatedtest.platform.common.Result) {
                com.automatedtest.platform.common.Result<?> r = (com.automatedtest.platform.common.Result<?>) result;
                if (r.getCode() != 200) {
                    errorMessage = r.getMessage();
                }
            }
            
            return result;
        } catch (Throwable e) {
            errorMessage = e.getMessage();
            throw e;
        } finally {
            try {
                // Wait for async execution if necessary or handle in a separate thread if performance critical
                // For simplicity, we record it here. Since service method is async, it should be fast.
                recordLog(point, errorMessage);
            } catch (Exception e) {
                log.error("Failed to record operation log", e);
            }
        }
    }

    private void recordLog(ProceedingJoinPoint point, String errorMessage) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) return;

        HttpServletRequest request = attributes.getRequest();
        String username = request.getHeader("X-User-Name");
        Long userId = null;

        if (username != null && !username.trim().isEmpty()) {
            User user = userService.lambdaQuery().eq(User::getUsername, username.trim()).one();
            if (user != null) {
                userId = user.getId();
            }
        }
        
        if (userId != null) {
            MethodSignature signature = (MethodSignature) point.getSignature();
            Method method = signature.getMethod();
            OperationAudit annotation = method.getAnnotation(OperationAudit.class);
            
            String module = "";
            String operation = "";
            
            if (annotation != null) {
                module = annotation.module();
                operation = annotation.operation();
            }
            
            if (module == null || module.isEmpty()) {
                module = point.getTarget().getClass().getSimpleName().replace("Controller", "");
            }
            
            if (operation == null || operation.isEmpty()) {
                operation = method.getName();
            }
            
            String target = request.getRequestURI();
            String details = errorMessage != null ? "Failed: " + errorMessage : "Success";
            String ipAddress = request.getRemoteAddr();

            operationLogService.log(userId, username, module, operation, target, details, ipAddress);
        }
    }
}
