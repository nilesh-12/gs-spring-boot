package com.ratnaafin.crm.common.config;

import com.ratnaafin.crm.common.service.AESEncryption;
import com.ratnaafin.crm.common.service.Utility;
import com.ratnaafin.crm.user.model.SysParaMst;
import com.ratnaafin.crm.user.service.UserService;
import org.apache.catalina.connector.CoyoteOutputStream;
import org.apache.catalina.connector.OutputBuffer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import javax.crypto.SecretKey;
import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.security.spec.AlgorithmParameterSpec;
import java.util.HashMap;

@WebFilter("/*")
public class EncryptionFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        Filter.super.init(filterConfig);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

        try {
            HttpServletRequest req = (HttpServletRequest) request;
            CustomHttpServletRequestWrapper customRequest = null;
            CustomHttpServletResponseWrapper wrappedResponse = null;
            HashMap<String, Object> keys = new HashMap<>();
            System.out.println("method " + req.getMethod());
            if ("POST".equals(req.getMethod())) {
                StringBuilder requestBody = new StringBuilder();
                try (BufferedReader reader = request.getReader()) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        requestBody.append(line);
                    }
                }
                String encryptRequestBody = requestBody.toString();
                String originalRequestBody = Utility.dataDecrypt(encryptRequestBody, keys);
                customRequest =
                        new CustomHttpServletRequestWrapper((HttpServletRequest) request, originalRequestBody);
                wrappedResponse = new CustomHttpServletResponseWrapper((HttpServletResponse) response, keys);
                chain.doFilter(customRequest, wrappedResponse);

            } else if ("GET".equals(req.getMethod())) {
                //customReques req;
                System.out.println("GET-SessKey : " + req.getHeader("sessKey"));
                System.out.println("GET-SessIv : " + req.getHeader("sessIv"));
                SecretKey key = AESEncryption.getKey(req.getHeader("sessKey"));
                AlgorithmParameterSpec iv = AESEncryption.getIv(req.getHeader("sessIv"));
                keys.put("key", key);
                keys.put("iv", iv);
                wrappedResponse = new CustomHttpServletResponseWrapper((HttpServletResponse) response, keys);
                // decrypt keys with rsa
                chain.doFilter(req, wrappedResponse);
            } else {
                chain.doFilter(request, response);
            }
        } catch (Exception e) {
            // for doc upload, when sessKey is not available, exception will be raised and it will continue the request
            chain.doFilter(request, response);
        }
    }

    @Override
    public void destroy() {
        Filter.super.destroy();
    }


}

