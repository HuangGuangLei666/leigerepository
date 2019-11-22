package com.hgl.controller;

import com.github.wxpay.sdk.WXPayUtil;
import com.hgl.model.XBMSConstant;
import com.hgl.utils.HttpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

/**
 * @author HGL
 * @Date 2019/11/21
 */
@RestController
@RequestMapping(value = "/busiManagement/wxgzh")
public class WxController {

    private static final Logger logger = LoggerFactory.getLogger(WxController.class);

    @Autowired


    /**
     * 微信支付接口(开通会员)
     *
     * @param request
     * @param openid
     * @param goodsId
     * @return
     */
    @RequestMapping(value = "/wxPay_openMembership.do", method = RequestMethod.GET)
    @ResponseBody
    public Map openMembership(HttpServletRequest request, String openid, Integer goodsId) {
//        TUserinfo tUserinfo = tUserinfoService.selectUserIdByOpenId(openid);
//        TMeal tMeal = tMealService.selectBygoodsId(goodsId);
//        BigDecimal decimal = new BigDecimal(tMeal.getPrice());
//        BigDecimal yibai = new BigDecimal(100);
//        BigDecimal bigDecimal = decimal.multiply(yibai);
//        BigInteger bigInteger = bigDecimal.toBigInteger();

//        logger.info("======tMeal.getPrice()={}=======", tMeal.getPrice());
        try {
            //获取请求ip地址
            String ip = request.getHeader("x-forwarded-for");
            if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeader("Proxy-Client-IP");
            }
            if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeader("WL-Proxy-Client-IP");
            }
            if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getRemoteAddr();
            }
            if (ip.indexOf(",") != -1) {
                String[] ips = ip.split(",");
                ip = ips[0].trim();
            }
            logger.info("=======ip={}=========", ip);

            String out_trade_no = WXPayUtil.generateNonceStr();
            logger.info("=======out_trade_no={}============", out_trade_no);
            //拼接统一下单地址参数
            Map<String, String> paraMap = new HashMap<String, String>();
            paraMap.put("appid", XBMSConstant.XBMS_WX_APPID);
            paraMap.put("body", "小兵秘书-开通会员支付");
            paraMap.put("mch_id", XBMSConstant.XBMS_WXPAY_MCH_ID);
            paraMap.put("nonce_str", WXPayUtil.generateNonceStr());
            paraMap.put("openid", openid);
            paraMap.put("out_trade_no", out_trade_no);//订单号
            paraMap.put("spbill_create_ip", ip);
            paraMap.put("total_fee", "0.01");
            paraMap.put("trade_type", XBMSConstant.XBMS_WXPAY_TRADE_TYPE);
            paraMap.put("notify_url", "https://ai.yousayido.net/busiManagement/wxgzh/callback.do");// 此路径是微信服务器调用支付结果通知路径随意写

            logger.info("=======paraMap={}=========", paraMap.toString());

            //商户号的密钥
            String paternerKey = XBMSConstant.xbms_wxpay_paternerkey;
            //生成签名
            String sign = WXPayUtil.generateSignature(paraMap, paternerKey);
            paraMap.put("sign", sign);
            logger.info("========paraMap={}==========", paraMap.toString());

            String xml = WXPayUtil.mapToXml(paraMap);//将所有参数(map)转xml格式
            logger.info("========xml={}==========", xml);


            //生成预支付订单
            /*TOrder tOrder = new TOrder();
            tOrder.setUserId(tUserinfo.getId());
            tOrder.setOpenid(openid);
            tOrder.setGoodsId(goodsId);
            tOrder.setPrice(tMeal.getPrice());
            tOrder.setNumber(1);
            tOrder.setPayMoney(tMeal.getPrice());
            tOrder.setStatus(1);
            tOrder.setTradeNo(out_trade_no);
            int i = tOrderService.addOrder(tOrder);*/

            //统一下单url: https://api.mch.weixin.qq.com/pay/unifiedorder
            String unifiedorder_url = "https://api.mch.weixin.qq.com/pay/unifiedorder";

            String xmlStr = HttpRequest.sendPost(unifiedorder_url, xml);//发送post请求"统一下单接口"返回预支付id:prepay_id
            logger.info("========xmlStr={}==========", xmlStr);

            //以下内容是返回前端页面的json数据
            String prepay_id = "";//预支付id
            if (xmlStr.indexOf("SUCCESS") != -1) {

                Map<String, String> map = WXPayUtil.xmlToMap(xmlStr);
                prepay_id = map.get("prepay_id");

            }

            Map<String, String> payMap = new HashMap<String, String>();

            payMap.put("appId", XBMSConstant.XBMS_WX_APPID);
            payMap.put("timeStamp", System.currentTimeMillis() / 1000 + "");
            payMap.put("nonceStr", WXPayUtil.generateNonceStr());
            payMap.put("signType", "MD5");
            payMap.put("package", "prepay_id=" + prepay_id);

            String paySign = WXPayUtil.generateSignature(payMap, paternerKey);
            payMap.put("paySign", paySign);

            return payMap;

        } catch (Exception e) {
            e.printStackTrace();

        }
        return null;
    }


    /**
     * 微信支付回调接口（开通会员）
     *
     * @param request
     * @param response
     * @return
     */
    @RequestMapping("/callback.do")
    @ResponseBody
    public String callBack(HttpServletRequest request, HttpServletResponse response) {

        logger.info("========进入回调接口==========");

        InputStream is = null;
        try {
            //获取请求的流信息(这里是微信发的xml格式所有只能使用流来读)
            is = request.getInputStream();
            String xml = iStreamToXML(is);

            //将微信发的xml转map
            Map<String, String> notifyMap = WXPayUtil.xmlToMap(xml);

            if (notifyMap.get("return_code").equals("SUCCESS")) {

                if (notifyMap.get("result_code").equals("SUCCESS")) {

                    //商户订单号
                    String ordersSn = notifyMap.get("out_trade_no");

                    //实际支付的订单金额:单位 分
                    String amountpaid = notifyMap.get("total_fee");

                    //将分转换成元-实际支付金额:元
                    BigDecimal amountPay = (new BigDecimal(amountpaid).divide(new BigDecimal("100"))).setScale(2);

                    String openid = notifyMap.get("openid");

                    String trade_type = notifyMap.get("trade_type");
                    logger.info("amountpaid:" + amountpaid);
                    logger.info("amountPay:" + amountPay);
                    logger.info("ordersSn:" + ordersSn);
                    logger.info("openid:" + openid);
                    logger.info("trade_type:" + trade_type);

                    //支付成功，更新支付状态
//                    tOrderService.updateOrderStatus(ordersSn);

                    //插入会员信息
////                    TMeal tMeal = tMealService.selectByTradeNo(ordersSn);
//                    Integer useDays = tMeal.getUseDays();
//                    if ("私人秘书".equals(tMeal.getType())) {
//                        tUserinfoService.updateMemberinfo(openid, useDays);
//                    }
//                    if ("私人定制".equals(tMeal.getType())) {
//                        tUserinfoService.updateMemberInfo(openid, useDays);
//                    }

                }

            }

            //告诉微信服务器收到信息了，不要在调用回调action了========这里很重要回复微信服务器信息用流发送一个xml即可
            response.getWriter().write("<xml><return_code><![CDATA[SUCCESS]]></return_code></xml>");
            is.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }


    public static String iStreamToXML(InputStream inputStream) {
        String str = "";
        try {
            StringBuilder sb = new StringBuilder();
            String line;

            BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            str = sb.toString();

        } catch (Exception e) {
            e.printStackTrace();
        }
        return str;
    }
}
