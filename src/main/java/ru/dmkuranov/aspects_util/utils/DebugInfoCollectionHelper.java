package ru.dmkuranov.aspects_util.utils;

import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Collections;

public class DebugInfoCollectionHelper {
    public static StringBuilder getWebEnvironmentInfo(HttpSession httpSession,  HttpServletRequest httpServletRequest) {
        StringBuilder result = new StringBuilder();
        String serverVersionInfo = httpSession.getServletContext().getServerInfo();
        String deploymentPath = httpSession.getServletContext().getRealPath("");
        result.append("\n").append("Server version: ").append(serverVersionInfo);
        result.append("\n").append("Deployment path: ").append(deploymentPath);

        StringBuilder sessionAttrsInfo = new StringBuilder("Session attributes:\n");
        for(Object attrName : Collections.list(httpSession.getAttributeNames())) {
            try {
                sessionAttrsInfo.append("["+attrName+"]=").append(httpSession.getAttribute(attrName.toString())).append("\n");
            } catch (Exception e) {
                sessionAttrsInfo.append("["+attrName+"] retrieve failed: ").append(e).append("\n");
            }
        }
        result.append("\n\n").append(sessionAttrsInfo);

        StringBuilder requestHeadersInfo = new StringBuilder("Request headers:\n");
        for(Object attrName : Collections.<String>list(httpServletRequest.getHeaderNames())) {
            try {
                requestHeadersInfo.append("["+attrName+"]=")
                        .append(StringUtils.join(", ", Collections.<String>list(httpServletRequest.getHeaders(attrName.toString()))))
                        .append("\n");
            } catch (Exception e) {
                requestHeadersInfo.append("["+attrName+"] retrieve failed: ").append(e).append("\n");
            }
        }
        result.append("\n\n").append(requestHeadersInfo);

        StringBuilder requestParamsInfo = new StringBuilder("Request parameters:\n");
        for(Object attrName : Collections.<String>list(httpServletRequest.getParameterNames())) {
            try {
                requestParamsInfo.append("["+attrName+"]=").append(httpServletRequest.getParameter(attrName.toString())).append("\n");
            } catch (Exception e) {
                requestParamsInfo.append("["+attrName+"] retrieve failed: ").append(e).append("\n");
            }
        }
        result.append("\n\n").append(requestParamsInfo);

        StringBuilder requestAttrsInfo = new StringBuilder("Request attributes:\n");
        for(Object attrName : Collections.<String>list(httpServletRequest.getAttributeNames())) {
            try {
                requestAttrsInfo.append("["+attrName+"]=").append(httpServletRequest.getAttribute(attrName.toString())).append("\n");
            } catch (Exception e) {
                requestAttrsInfo.append("["+attrName+"] retrieve failed: ").append(e).append("\n");
            }
        }
        result.append("\n\n").append(requestAttrsInfo);

        StringBuilder cookiesInfo = new StringBuilder("Cookies:\n");
        for(Cookie cookie : httpServletRequest.getCookies()) {
            cookiesInfo.append("["+cookie.getName()+"]=").append(cookie.getValue()).append("\n");
        }
        result.append("\n\n").append(cookiesInfo);

        return result;
    }

    public static StringBuilder getJoinPointInfo(ProceedingJoinPoint pjp) {
        StringBuilder result = new StringBuilder("Join point: "+pjp.getSignature().toLongString()).append("\n");
        Object[] args = pjp.getArgs();
        if(args!=null) {
            result.append("Arguments:\n");
            for(int i=0; i<args.length;i++) {
                result.append("[").append(i).append("]=");
                try {
                    result.append(args[i]);
                } catch (Exception e) {
                    result.append("exception retreiving: "+e);
                }
                result.append("\n");
            }
        }
        return result;
    }
}

