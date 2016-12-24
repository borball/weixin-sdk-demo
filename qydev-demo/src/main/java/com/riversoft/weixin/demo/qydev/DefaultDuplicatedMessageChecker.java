package com.riversoft.weixin.demo.qydev;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Created by exizhai on 11/15/2015.
 */
@Component
public class DefaultDuplicatedMessageChecker implements DuplicatedMessageChecker {

    private static Cache<String, String> cache = CacheBuilder.newBuilder().expireAfterWrite(15, TimeUnit.SECONDS).maximumSize(10000).build();

    @Override
    public boolean isDuplicated(String msgKey) {
        if(cache.getIfPresent(msgKey) == null) {
            cache.put(msgKey, msgKey);
            return false;
        } else {
            return true;
        }
    }
}
