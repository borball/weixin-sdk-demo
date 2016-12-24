package com.riversoft.weixin.demo.mp;

import com.riversoft.weixin.common.oauth2.AccessToken;
import com.riversoft.weixin.common.oauth2.OpenUser;
import com.riversoft.weixin.mp.oauth2.MpOAuth2s;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Created by exizhai on 12/17/2015.
 */
public class MpOAuthCallbackController {

    private static org.slf4j.Logger logger = LoggerFactory.getLogger(WxCallbackController.class);

    /**
     * 公众号OAuth回调接口
     * @return
     */
    @RequestMapping("/wx/oauth/mp")
    public void mp(@RequestParam(value="code") String code, @RequestParam(value="state", required = false) String state) {

        logger.info("code:{}, state:{}", code, state);
        AccessToken accessToken = MpOAuth2s.defaultOAuth2s().getAccessToken(code);
        OpenUser openUser = MpOAuth2s.defaultOAuth2s().userInfo(accessToken.getAccessToken(), accessToken.getOpenId());
        //do your logic
    }
}
