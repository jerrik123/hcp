package com.mangocity.utils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.alibaba.dubbo.common.utils.StringUtils;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.mangocity.btms.adpater.vo.ApprovalManVO;
import com.mangocity.vo.PayParamsVo;

public class JsonUtil {
	
	public static String Object2JsonString(Object object){
		return JSON.toJSONString(object);
	}
	
	public static <T> Object JsonString2Object(String text,Class<T> clazz){
		if(StringUtils.isEmpty(text))return null;
		return  JSON.parseObject(text, clazz);
	} 
	
	public static JSONObject objectToJosnObject(Object object){
		JSONObject json = null;
		if(object!=null){
		   json = (JSONObject) JSON.toJSON(object);
		}
		return json;
	}
	
	public static JSONObject stringToJsonObject(String text){
		if(StringUtils.isBlank(text)){
			return null;
		}else{
			return JSON.parseObject(text);
		}
	}

	public static String ObjectToJsonString(Object object){
		if(object==null){
			return "";
		}else{
			 ObjectMapper mapper = new ObjectMapper();   
		     String json="";
			try {
			       mapper.configure(SerializationFeature.INDENT_OUTPUT, Boolean.TRUE);
                   json = mapper.writeValueAsString(object);
			} catch (JsonProcessingException e) {
				e.printStackTrace();
			}
		     return json;
		}  
		
	}
	
	
	public static void main(String[] args) {
	    PayParamsVo payParamsVo = new PayParamsVo();
	    List items = new ArrayList<>();
	    items.add(123);
	    items.add(124);
	    payParamsVo.setCreditCardId("123123412312");
	    payParamsVo.setOrderCn("2016321321");
	    payParamsVo.setOrderId(123L);
	    payParamsVo.setIntegralNum(0L);
	    payParamsVo.setOrderItems(items);
	    payParamsVo.setPayModel("TMON");
	    payParamsVo.setMembercd("46465456456");
	    payParamsVo.setCustomerRemark("4231421");
	    payParamsVo.setProjectCode("3412312");
	    payParamsVo.setCardPayType("ALL");
	    payParamsVo.setPayChannel("CC");
	    payParamsVo.setPrice(new BigDecimal(614));
	    payParamsVo.setPayInfoType("0");
	    List<ApprovalManVO> messageReceivers = new ArrayList<>();
	    ApprovalManVO a = new ApprovalManVO();
	    a.setEmail("31312@qq.com");
	    a.setFax("fasfds");
	    a.setIdentityType("会员");
	    a.setMobile("12398789486");
	    a.setName("4312321");
	    
	    payParamsVo.setMessageReceiver(messageReceivers);
		
	}

}
