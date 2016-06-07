package com.riversoft.weixin.demo;

import com.riversoft.weixin.common.decrypt.AesException;
import com.riversoft.weixin.common.decrypt.MessageDecryption;
import com.riversoft.weixin.common.decrypt.SHA1;
import com.riversoft.weixin.common.event.EventRequest;
import com.riversoft.weixin.common.exception.WxRuntimeException;
import com.riversoft.weixin.common.message.XmlMessageHeader;
import com.riversoft.weixin.mp.base.AppSetting;
import com.riversoft.weixin.mp.care.CareMessages;
import com.riversoft.weixin.mp.message.MpXmlMessages;
import com.riversoft.weixin.mp.user.Users;
import com.riversoft.weixin.qy.base.CorpSetting;
import com.riversoft.weixin.qy.message.QyXmlMessages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

/**
 * Created by exizhai on 10/7/2015.
 */
@Controller
public class WxCallbackController {

    private static Logger logger = LoggerFactory.getLogger(WxCallbackController.class);

    /**
     * token/aesKey 是和企业号应用绑定的，这里为了演示方便使用外部注入，实际使用的时候一个企业号可能有多个应用，这样的话需要有个分发逻辑。
     * 比如callback url可以定义为  /wx/qy/[应用的ID]，通过ID查询不同的token和aesKey
     */
    @Value("${agent.default.token}")
    private String token;

    @Value("${agent.default.aesKey}")
    private String aesKey;

    @Autowired
    private DuplicatedMessageChecker duplicatedMessageChecker;

    public void setDuplicatedMessageChecker(DuplicatedMessageChecker duplicatedMessageChecker) {
        this.duplicatedMessageChecker = duplicatedMessageChecker;
    }

    /**
     * 企业号回调接口
     * 这里为了演示方便使用单个URL，实际使用的时候一个企业号可能有多个应用，这样的话需要有个分发逻辑：
     * 比如callback url可以定义为  /wx/qy/[应用的ID]，通过ID查询不同的token和aesKey
     *
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

        CorpSetting corpSetting = CorpSetting.defaultSettings();

        try {
            MessageDecryption messageDecryption = new MessageDecryption(token, aesKey, corpSetting.getCorpId());
            if (!StringUtils.isEmpty(echostr)) {
                String echo = messageDecryption.decryptEcho(signature, timestamp, nonce, echostr);
                logger.info("消息签名验证成功.");
                return echo;
            } else {
                XmlMessageHeader xmlRequest = QyXmlMessages.fromXml(messageDecryption.decrypt(signature, timestamp, nonce, content));
                XmlMessageHeader xmlResponse = qyDispatch(xmlRequest);
                if(xmlResponse != null) {
                    try {
                        return messageDecryption.encrypt(QyXmlMessages.toXml(xmlResponse), timestamp, nonce);
                    } catch (WxRuntimeException e) {
                    }
                }
            }
        } catch (Exception e) {
            logger.error("callback failed.", e);
        }

        return "";
    }

    private XmlMessageHeader qyDispatch(XmlMessageHeader xmlRequest) {
        //添加处理逻辑

        //需要同步返回消息（被动响应消息）给用户则构造一个XmlMessageHeader类型，比较鸡肋，因为处理逻辑如果比较复杂响应太慢会影响用户感知，建议直接返回null；
        //如果有消息需要发送给用户则可以调用主动消息发送接口进行异步发送
        return null;
    }

    /**
     * 公众号回调接口
     * 这里为了演示方便使用单个固定URL，实际使用的时候一个一个系统可能有多个公众号，这样的话需要有个分发逻辑：
     * 比如callback url可以定义为  /wx/mp/[公众号的appId]，通过appId构造不同的AppSetting
     *
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
                        return messageDecryption.encrypt(MpXmlMessages.toXml(xmlResponse), timestamp, nonce);
                    } catch (WxRuntimeException e) {

                    }
                }
            } catch (AesException e) {
            }
        } else {
            xmlRequest = MpXmlMessages.fromXml(content);
            XmlMessageHeader xmlResponse = mpDispatch(xmlRequest);
            if(xmlResponse != null) {
                try {
                    return MpXmlMessages.toXml(xmlResponse);
                } catch (WxRuntimeException e) {
                }
            }
        }

        return "";
    }

    private XmlMessageHeader mpDispatch(XmlMessageHeader xmlRequest) {
        if(!duplicatedMessageChecker.isDuplicated(xmlRequest.getFromUser() + xmlRequest.getCreateTime().getTime())) {
            String welcome = "您好:" + Users.defaultUsers().get(xmlRequest.getFromUser()).getNickName();
            CareMessages.defaultCareMessages().text(xmlRequest.getFromUser(), welcome);

            if (xmlRequest instanceof EventRequest) {
                EventRequest eventRequest = (EventRequest) xmlRequest;
                logger.debug("事件请求[{}]", eventRequest.getEventType().name());
                CareMessages.defaultCareMessages().text(xmlRequest.getFromUser(), "事件请求:" + eventRequest.getEventType().name());
            } else {
                logger.debug("消息请求[{}]", xmlRequest.getMsgType().name());
                CareMessages.defaultCareMessages().text(xmlRequest.getFromUser(), "消息请求:" + xmlRequest.getMsgType().name());
            }
        } else {
            logger.warn("Duplicated message: {} @ {}", xmlRequest.getMsgType(), xmlRequest.getFromUser());
        }

        //需要同步返回消息（被动回复）给用户则构造一个XmlMessageHeader类型，比较鸡肋，因为处理逻辑如果比较复杂响应太慢会影响用户感知，建议直接返回null；
        //要发送消息给用户可以参考上面的例子使用客服消息接口进行异步发送
        return null;
    }


}
