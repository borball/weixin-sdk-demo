package com.riversoft.weixin.demo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.riversoft.weixin.common.decrypt.AesException;
import com.riversoft.weixin.common.decrypt.MessageDecryption;
import com.riversoft.weixin.common.decrypt.SHA1;
import com.riversoft.weixin.common.event.ClickEvent;
import com.riversoft.weixin.common.message.XmlMessageHeader;
import com.riversoft.weixin.common.message.xml.TextXmlMessage;
import com.riversoft.weixin.common.util.XmlObjectMapper;
import com.riversoft.weixin.mp.base.AppSetting;
import com.riversoft.weixin.mp.message.MpXmlMessages;
import com.riversoft.weixin.mp.message.xml.Forward2CareXmlMessage;
import com.riversoft.weixin.qy.base.AgentSetting;
import com.riversoft.weixin.qy.base.DefaultSettings;
import com.riversoft.weixin.qy.message.QyXmlMessages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

/**
 * Created by exizhai on 10/7/2015.
 */
@Controller
public class WxCallbackController {

    private static Logger logger = LoggerFactory.getLogger(WxCallbackController.class);

    @Autowired
    private DuplicatedMessageChecker duplicatedMessageChecker;

    public void setDuplicatedMessageChecker(DuplicatedMessageChecker duplicatedMessageChecker) {
        this.duplicatedMessageChecker = duplicatedMessageChecker;
    }

    /**
     * 企业号回调接口
     * @param signature
     * @param timestamp
     * @param nonce
     * @param echostr
     * @param content
     * @return
     */
    @RequestMapping("/wx/qy")
    @ResponseBody
    public String qy(@RequestParam(value="msg_signature") String signature,
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
                XmlMessageHeader xmlRequest = QyXmlMessages.fromXml(messageDecryption.decrypt(signature, timestamp, nonce, content));
                qyDispatch(xmlRequest);
            }
        } catch (Exception e) {
            logger.error("callback failed.", e);
        }

        return "";
    }

    private void qyDispatch(XmlMessageHeader xmlRequest) {

    }

    /**
     * 公众号回调接口
     * @param signature
     * @param msg_signature
     * @param timestamp
     * @param nonce
     * @param echostr
     * @param encrypt_type
     * @param content
     * @return
     */
    @RequestMapping("/wx/mp")
    @ResponseBody
    public String mp(@RequestParam(value="signature") String signature,
                     @RequestParam(value="msg_signature", required = false) String msg_signature,
                     @RequestParam(value="timestamp") String timestamp,
                     @RequestParam(value="nonce") String nonce,
                     @RequestParam(value="echostr", required = false) String echostr,
                     @RequestParam(value="encrypt_type", required = false) String encrypt_type,
                     @RequestBody(required = false) String content) {

        logger.info("signature={}, msg_signature={}, timestamp={}, nonce={}, echostr={}, encrypt_type={}", signature, msg_signature, timestamp, nonce, echostr, encrypt_type);

        AppSetting appSetting = AppSetting.defaultSettings();
        try {
            if(!SHA1.getSHA1(appSetting.getToken(), timestamp, nonce).equals(signature)) {
                logger.warn("非法请求.");
                return "非法请求.";
            }
        } catch (AesException e) {
            logger.error("check signature failed:", e);
            return "非法请求.";
        }

        if (!StringUtils.isEmpty(echostr)) {
            return echostr;
        }

        XmlMessageHeader xmlRequest = null;
        if("aes".equals(encrypt_type)) {
            try {
                MessageDecryption messageDecryption = new MessageDecryption(appSetting.getToken(), appSetting.getAesKey(), appSetting.getAppId());
                xmlRequest = MpXmlMessages.fromXml(messageDecryption.decrypt(msg_signature, timestamp, nonce, content));
                XmlMessageHeader xmlResponse = mpDispatch(xmlRequest);

                if(xmlResponse != null) {
                    try {
                        return messageDecryption.encrypt(XmlObjectMapper.defaultMapper().toXml(xmlResponse), timestamp, nonce);
                    } catch (JsonProcessingException e) {
                    }
                }
            } catch (AesException e) {
            }
        } else {
            xmlRequest = MpXmlMessages.fromXml(content);
            XmlMessageHeader xmlResponse = mpDispatch(xmlRequest);
            if(xmlResponse != null) {
                try {
                    return XmlObjectMapper.defaultMapper().toXml(xmlResponse);
                } catch (JsonProcessingException e) {
                }
            }
        }

        return "";
    }

    private XmlMessageHeader mpDispatch(XmlMessageHeader xmlRequest) {
        String reply = "您好，正在为您接入客服。";
        if(!duplicatedMessageChecker.isDuplicated(xmlRequest.getFromUser() + xmlRequest.getCreateTime().getTime())) {

            if(xmlRequest instanceof ClickEvent) {
                ClickEvent clickEvent = (ClickEvent)xmlRequest;
                if("contact".equals(clickEvent.getEventKey())) {
                    Forward2CareXmlMessage kfMessage = new Forward2CareXmlMessage();
                    kfMessage.setFromUser(xmlRequest.getToUser());
                    kfMessage.setToUser(xmlRequest.getFromUser());
                    kfMessage.setCreateTime(xmlRequest.getCreateTime());
                    return kfMessage;
                }
            }
            TextXmlMessage textXmlMessage = new TextXmlMessage();
            textXmlMessage.content(reply).toUser(xmlRequest.getFromUser()).fromUser(xmlRequest.getToUser());
            return textXmlMessage;
        } else {
            logger.warn("Duplicated message: {} @ {}", xmlRequest.getMsgType(), xmlRequest.getFromUser());
        }

        return null;
    }


}
