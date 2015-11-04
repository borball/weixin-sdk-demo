package com.riversoft.weixin.demo;

import com.riversoft.weixin.qy.base.AgentSetting;
import com.riversoft.weixin.qy.base.DefaultSettings;
import com.riversoft.weixin.qy.decrypt.MessageDecryption;
import com.riversoft.weixin.qy.message.XmlMessages;
import com.riversoft.weixin.qy.message.base.Message;
import com.riversoft.weixin.qy.message.request.XmlRequest;
import com.riversoft.weixin.qy.message.xml.XmlMessageHeader;
import com.riversoft.weixin.qy.message.xml.TextXmlMessage;
import com.riversoft.weixin.qy.util.XmlObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

/**
 * Created by exizhai on 10/7/2015.
 */
@Controller
public class WxCallbackController {

    private static Logger logger = LoggerFactory.getLogger(WxCallbackController.class);

    @RequestMapping("/wx/qy")
    @ResponseBody
    public String callback(@RequestParam(value="msg_signature") String signature,
                           @RequestParam(value="timestamp") String timestamp,
                           @RequestParam(value="nonce") String nonce,
                           @RequestParam(value="echostr", required = false) String echostr,
                           @RequestBody(required = false) String content) {

        logger.info("msg_signature={}, nonce={}, timestamp={}, echostr={}", signature, nonce, timestamp, echostr);

        AgentSetting agentSetting = DefaultSettings.defaultSettings().getAgentSetting();
        String corpId = DefaultSettings.defaultSettings().getCorpSetting().getCorpId();

        try {
            MessageDecryption messageDecryption = new MessageDecryption(agentSetting.getToken(), agentSetting.getAesKey(), corpId);
            if (!StringUtils.isEmpty(echostr)) {
                String echo = messageDecryption.decryptEcho(signature, timestamp, nonce, echostr);
                logger.info("消息签名验证成功.");
                return echo;
            } else {
                XmlRequest xmlRequest = XmlMessages.fromXml(messageDecryption.decrypt(signature, timestamp, nonce, content));
                Message message = dispatch(xmlRequest);
                if (message != null && isSyncResponse(message)) {
                    return messageDecryption.encrypt(XmlMessages.toXml((XmlMessageHeader) message), timestamp, nonce);
                }
            }
        } catch (Exception e) {
            logger.error("callback failed.", e);
        }

        return "";
    }

    private boolean isSyncResponse(Message message) {
        return message instanceof XmlMessageHeader;
    }

    private Message dispatch(XmlRequest xmlRequest) {
        try {
            String response = "收到" + xmlRequest.getFromUser() + "发到" + xmlRequest.getAgentId() + "的消息或者事件:\n" + XmlObjectMapper.defaultMapper().toXml(xmlRequest);
            return new TextXmlMessage().content(response).toUser(xmlRequest.getFromUser());
        } catch (Exception e) {
        }

        return null;
    }

}
