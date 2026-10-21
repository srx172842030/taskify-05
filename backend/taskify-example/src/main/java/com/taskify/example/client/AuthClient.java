package com.taskify.example.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Map;

/**
 * OpenFeign 同步调用示例：通过 Nacos 服务发现调用认证服务。
 */
@FeignClient(name = "taskify-auth")
public interface AuthClient {

    @GetMapping("/health")
    Map<String, String> health();
}
