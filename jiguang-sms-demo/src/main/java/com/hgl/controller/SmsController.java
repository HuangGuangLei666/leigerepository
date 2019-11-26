package com.hgl.controller;

import cn.jiguang.common.resp.APIConnectionException;
import cn.jiguang.common.resp.APIRequestException;
import com.alibaba.fastjson.JSONObject;
import com.hgl.model.CheckSmsCodeResp;
import com.hgl.model.SendSmsResp;
import com.hgl.utils.jiguang.SendSMSResult;
import com.hgl.utils.jiguang.ValidSMSResult;
import com.hgl.utils.jiguang.common.SMSClient;
import com.hgl.utils.jiguang.common.model.SMSPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author HuangGuangLei
 * @Date 2019/11/22
 */
@RestController
@RequestMapping(value = "/hgldemo")
public class SmsController {

    private static final Logger logger = LoggerFactory.getLogger(SmsController.class);


    /**
     * 发送验证码短信
     *
     * @return {
     * "retCode": 0,
     * "retDesc": "验证码发送成功",
     * "rateLimitQuota": 0,
     * "rateLimitRemaining": 0,
     * "rateLimitReset": 0,
     * "responseCode": 200,
     * "originalContent": "{\"msg_id\":\"848267378766\"}",
     * "resultOK": true
     * }
     */
    @RequestMapping(value = "/sendSms.do")
    @ResponseBody
    public SendSmsResp sendSMSCode() {
        SMSClient client = new SMSClient("4b9b1c29efd1109cfde08b84", "24286bbb7fde1ab272b71a6e");

        SendSmsResp resp = new SendSmsResp();
        SMSPayload payload = SMSPayload.newBuilder()
                .setMobileNumber("18750031121")
                .setTempId(1)
                .build();
        try {
            SendSMSResult smsResult = client.sendSMSCode(payload);

            String originalContent = smsResult.getOriginalContent();
            JSONObject jb = JSONObject.parseObject(originalContent);
            String msgId = jb.getString("msg_id");

            logger.info("=========msgId========" + smsResult.toString());
            resp.setRetCode(0);
            resp.setRetDesc("验证码发送成功");
            resp.setSmsId(msgId);
            return resp;
        } catch (APIConnectionException e) {
            logger.error("Connection error. Should retry later. ", e);
        } catch (APIRequestException e) {
            logger.error("Error response from JPush server. Should review and fix it. ", e);
            logger.info("HTTP Status: " + e.getStatus());
            logger.info("Error Message: " + e.getMessage());
        }
        resp.setRetCode(1);
        resp.setRetDesc("验证码发送失败");
        return resp;
    }


    /**
     * 验证码校验接口
     *
     * @param verifyCode
     * @param msgId
     * @return {
     * "retCode": 0,
     * "retDesc": "验证码校验成功"
     * }
     */
    @RequestMapping(value = "/checkSmsCode.do")
    @ResponseBody
    public CheckSmsCodeResp checkSmsCode(String verifyCode, String msgId) {
        CheckSmsCodeResp checkSmsCode = new CheckSmsCodeResp();
        try {
            SMSClient client = new SMSClient("4b9b1c29efd1109cfde08b84", "24286bbb7fde1ab272b71a6e");
            ValidSMSResult res = client.sendValidSMSCode(msgId, verifyCode);
            if (StringUtils.isEmpty(res)) {
                checkSmsCode.setRetCode(1);
                checkSmsCode.setRetDesc("你输入的验证码有误");
                return checkSmsCode;
            }

            logger.info("==========res.getIsValid()===========" + res.toString());
            checkSmsCode.setRetCode(0);
            checkSmsCode.setRetDesc("验证码校验成功");
            return checkSmsCode;
        } catch (APIConnectionException e) {
            logger.error("Connection error. Should retry later. ", e);
        } catch (APIRequestException e) {
            logger.error("Error response from JPush server. Should review and fix it. ", e);
            logger.info("HTTP Status: " + e.getStatus());
            logger.info("Error Message: " + e.getMessage());
        }

        checkSmsCode.setRetCode(1);
        checkSmsCode.setRetDesc("你输入的验证码有误");
        return checkSmsCode;
    }

}
