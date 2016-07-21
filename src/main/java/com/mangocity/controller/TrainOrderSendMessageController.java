package com.mangocity.controller;

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.mangocity.btms.api.IApprovalMessageManageService;
import com.mangocity.btms.api.IMessageManageService;
import com.mangocity.btms.approval.model.SelectedApprovalMan;
import com.mangocity.btms.vo.TravelInfoVO;
import com.mangocity.constant.Constants;
import com.mangocity.response.ResponseMessage;
import com.mangocity.utils.JsonUtil;
import com.mangocity.vo.MailMessage;

/**
 * 
 * @author yeminlong
 * 订单查询页面  --->点击“发送消息”，可再次发审核、出票和退票  短信或邮件
 */
@Controller
@RequestMapping("/train")
public class TrainOrderSendMessageController extends BaseCantroller{
	
	Logger logger = Logger.getLogger(TrainOrderSendMessageController.class);
	
	@Autowired
	private IMessageManageService messageManageService;
	
	@Autowired
	private IApprovalMessageManageService  approvalMessageManageService;
	

	
	/**
	 * 发送出票邮件
	 * @param membercd
	 * @param mailMessage   只要orderCn参数就可以
	 * @param mailTo
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value="/sendEmail")
	@ResponseBody
	public Object sendEmail(String membercd,String orderCn, String[] mailTo) throws Exception{
		ResponseMessage responseMessage = new  ResponseMessage();
		//membercd = "660009612699";
		MailMessage mailMessage = new MailMessage();
		mailMessage.setOrderCn(orderCn);
		//mailMessage.setOrderCn("201607038866897V");
		//mailTo = new String[1];
		//mailTo[0] = "minlong.ye@mangocity.com";
		try{
			boolean tag = messageManageService.sendEmail(membercd, mailMessage, mailTo,Constants.TICKET_EMAIL_NUM ,Constants.TICKET_EMAIL_NAME );
			if(!tag){
				throw new Exception();
			}
			responseMessage.setCode("0");
			responseMessage.setMsg("发送成功");
		}catch(Exception e){
			logger.error("邮件发送失败",e);
			responseMessage.setCode("-1");
			responseMessage.setMsg("邮件发送失败");
		}
		return responseMessage;
	}
	
	/**
	 * 发送出票邮件
	 * @param membercd
	 * @param mailMessage   只要orderCn参数就可以
	 * @param mailTo
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value="/sendMsg")
	@ResponseBody
	public Object sendIssuedMsg(String orderCn, String[] msgTo) throws Exception{
		ResponseMessage responseMessage = new  ResponseMessage();
		//mailTo = new String[1];
		//mailTo[0] = "minlong.ye@mangocity.com";
		try{
			for(String phone:msgTo){
				boolean tag = messageManageService.sendissuedSMS(orderCn, phone);
				if(!tag){
					logger.error("发送短信失败："+phone+",orderCn"+orderCn);
				}
				
			}
			responseMessage.setCode("0");
			responseMessage.setMsg("发送成功");
		}catch(Exception e){
			logger.error("短信发送失败",e);
			responseMessage.setCode("-1");
			responseMessage.setMsg("短信发送失败");
		}
		return responseMessage;
	}
	
	
	@RequestMapping(value="/sendApprovalSMS")
	@ResponseBody
	public Object sendIssuedsendApprovalSMS(Long orderId,String approvalJosn) throws Exception{
		
		logger.info(">>>>>>>>>orderId:"+orderId+">>>>>>>>>>approvalJosn:"+approvalJosn);
		
		ResponseMessage responseMessage = new  ResponseMessage();
		if(null == orderId || null == approvalJosn){
			responseMessage.setCode("-1");
			responseMessage.setMsg("审批短信发送失败");
			return responseMessage;
		}
		try{
			
			TravelInfoVO travelInfoVO = (TravelInfoVO) JsonUtil.JsonString2Object(approvalJosn, TravelInfoVO.class);
			
			if(null == travelInfoVO || travelInfoVO.getApprovalManSet().isEmpty()){
				
				responseMessage.setCode("-1");
				responseMessage.setMsg("审批短信发送失败");
				return responseMessage;
			}
			
			responseMessage = approvalMessageManageService.sendApprovalMessage(orderId, travelInfoVO);
			
			responseMessage.setCode("0");
			
			responseMessage.setMsg("发送成功");
		}catch(Exception e){
			logger.error("审批短信发送失败",e);
			responseMessage.setCode("-1");
			responseMessage.setMsg("审批短信发送失败");
		}
		return responseMessage;
	}
}
