package com.riversoft.weixin.demo.app;

import com.riversoft.weixin.app.base.AppSetting;
import com.riversoft.weixin.app.care.CareMessages;
import com.riversoft.weixin.app.message.AppXmlMessages;
import com.riversoft.weixin.app.template.Message;
import com.riversoft.weixin.app.template.Templates;
import com.riversoft.weixin.common.decrypt.AesException;
import com.riversoft.weixin.common.decrypt.MessageDecryption;
import com.riversoft.weixin.common.decrypt.SHA1;
import com.riversoft.weixin.common.message.XmlMessageHeader;
import com.riversoft.weixin.demo.commons.DuplicatedMessageChecker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @borball on 12/29/2016.
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
     * 小程序回调接口
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
    @RequestMapping("/wx/app")
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
                logger.warn("invalid request.");
                return "invalid request.";
            }
        } catch (AesException e) {
            logger.error("check signature failed:", e);
            return "invalid request.";
        }

        if (!StringUtils.isEmpty(echostr)) {
            return echostr;
        }

        XmlMessageHeader xmlRequest = null;
        if("aes".equals(encrypt_type)) {
            try {
                MessageDecryption messageDecryption = new MessageDecryption(appSetting.getToken(), appSetting.getAesKey(), appSetting.getAppId());
                xmlRequest = AppXmlMessages.fromXml(messageDecryption.decrypt(msg_signature, timestamp, nonce, content));
            } catch (AesException e) {
            }
        } else {
            xmlRequest = AppXmlMessages.fromXml(content);
        }

        dispatch(xmlRequest);

        return "";
    }

    /**
     * 具体业务逻辑
     * @param xmlRequest
     */
    private void dispatch(XmlMessageHeader xmlRequest) {
        if(!duplicatedMessageChecker.isDuplicated(xmlRequest.getFromUser() + xmlRequest.getCreateTime().getTime())) {
            //如果有需要可以调用客服接口或者模板消息接口发送消息给用户
            //Message message = new Message();
            //Templates.defaultTemplates().send(message);

            //CareMessages.defaultCareMessages().text(xmlRequest.getFromUser(), "Hello!");
            //CareMessages.defaultCareMessages().image(xmlRequest.getFromUser(), "image_media_id");
        } else {
            logger.warn("Duplicated message: {} @ {}", xmlRequest.getMsgType(), xmlRequest.getFromUser());
        }

    }

}
