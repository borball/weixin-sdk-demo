package com.riversoft.weixin.demo.pay;

/**
 * Created by exizhai on 11/15/2015.
 */
public interface DuplicatedMessageChecker {

    boolean isDuplicated(String msgKey);

}
