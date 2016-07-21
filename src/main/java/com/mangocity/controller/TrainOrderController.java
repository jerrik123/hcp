package com.mangocity.controller;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.annotation.Resource;
import javax.xml.rpc.ServiceException;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.mangocity.api.IMessageTemplateService;
import com.mangocity.btms.adpater.vo.ApprovalManVO;
import com.mangocity.btms.api.IAddressManageService;
import com.mangocity.btms.api.IApprovalManageService;
import com.mangocity.btms.api.ICorporationService;
import com.mangocity.btms.api.IMemberManageService;
import com.mangocity.btms.api.IMessageManageService;
import com.mangocity.btms.model.Corporation;
import com.mangocity.btms.organization.configuration.model.MessageConfiguration;
import com.mangocity.btms.organization.configuration.model.MessageType;
import com.mangocity.btms.organization.configuration.model.RecipientType;
import com.mangocity.btms.organization.configuration.service.CorporationConfigService;
import com.mangocity.btms.projectmanagement.model.Project;
import com.mangocity.btms.projectmanagement.service.ProjectService;
import com.mangocity.btms.vo.RoleTypes;
import com.mangocity.constant.Constants;
import com.mangocity.easy.workflow.model.FlowNode;
import com.mangocity.enums.ApprovalType;
import com.mangocity.framework.creditcard.SidGenerator;
import com.mangocity.member.adapter.model.Member;
import com.mangocity.member.adapter.model.MemberShipInfo;
import com.mangocity.model.address.AddressInfo;
import com.mangocity.response.ResponseMessage;
import com.mangocity.service.IBookPageService;
import com.mangocity.service.IPaymentService;
import com.mangocity.service.ITicketService;
import com.mangocity.service.ITrainService;
import com.mangocity.tmc.flight.adapter.btms.DeliveryAddressAdapter;
import com.mangocity.tmc.flight.adapter.btms.SatationAddressAdapter;
import com.mangocity.utils.JsonUtil;
import com.mangocity.vo.LinkmanInfo;
import com.mangocity.vo.OrderItemVo;
import com.mangocity.vo.OrderVo;
import com.mangocity.vo.PassengerVo;
import com.mangocity.vo.PayDetailBean;

@Controller
@RequestMapping("/order")
public class TrainOrderController extends BaseCantroller{
	
	private Logger logger = Logger.getLogger(this.getClass());
	
	@Autowired
	private ITicketService ticketService;
	@Autowired
	private IMessageManageService messageManageService;
	@Autowired
	private ICorporationService ducorporationService;
	@Autowired
    private IAddressManageService addressManageService;
    private List<ApprovalManVO> messageReceivers;
    @Autowired
	private IMemberManageService memberManageService;
    @Autowired
    private DeliveryAddressAdapter deliveryAddress;
    @Autowired
    private SatationAddressAdapter satationAddressAdapter;
    @Autowired
	private IPaymentService paymentService;
    @Autowired
    private IMessageTemplateService messageTemplateService;
    @Autowired
	private ITrainService trainService;
    @Autowired
	private IApprovalManageService approvalManageService;
    @Autowired
    private CorporationConfigService ducorporationConfigService;
    @Autowired
	private ProjectService duprojectService;
    @Autowired
	private IBookPageService bookPageService;
    
    @Resource
	private MessageSource messageSource;

    @RequestMapping(value="/test")
    public String test() throws Exception{
    	findMessageConfig("660009612699",189L);
    	//String r = messageTemplateService.showEmailTemplat(31L, 12,"");
    	//String sss = new String(ss,"UTF-8");
    	MemberShipInfo memberShipInfo = memberManageService.retrieveMbrShipByMbrshipCd("660009612699");
		System.out.println(memberShipInfo);
		Member member = memberManageService.retrieveMemberByMbrShipCd("660009921704");
		System.out.println(member);
    	//findMessageConfig("660009612699");
		LinkmanInfo LinkmanInfo = memberManageService.retrieveLinkManInfoByMbrShipCd("660009612699");
		System.out.println(LinkmanInfo);
		//findMessageConfig("660009612699",189L);
    	findDeliveryConfig("660009612699");
    	findHistoryAdd("660009612699");
    	Corporation corporation1 = ducorporationService.retrieveCorporationByNumOrCode(null,"PNT");
    	findSatationAddress(corporation1.getCorporationNum());
    	return "";
    }
    
    //个人支付成功之后调用号百支付接口，通知其出票
    //公司月结是审批通过之后调用号百支付接口，通知其出票
    @RequestMapping("/createHBPay")
	@ResponseBody
	public ResponseMessage createHBPay(@RequestParam("orderId")Long orderId) throws Exception{
    	ResponseMessage responseMessage = new ResponseMessage();
    	responseMessage = paymentService.createHBPay(orderId);
    	return responseMessage;
    }
    
	@RequestMapping("/createOrder")
	public ModelAndView createOrder(@RequestParam("orderInfo")String orderInfo) {
			ModelAndView model = new ModelAndView("TrainOrderPay");
			model.addObject("prevData", orderInfo);
			Long orderId=null;
			String memberCd="";
		try {
			logger.info(">>>>>>>>>>>>>>>>>>>createOrder:" + orderInfo);
			OrderVo orderVo = (OrderVo) JsonUtil.JsonString2Object(orderInfo, OrderVo.class);
			String projectCodeValue = orderVo.getOrder().getProjectCode();
			String pcode = "";
			if (StringUtils.isNotBlank(projectCodeValue) && projectCodeValue.contains("/")) {
				pcode = projectCodeValue.split("/")[1];
			}
			ResponseMessage repsonseMessage = ticketService.createOrder(orderVo);
			Map<String, Object> data = repsonseMessage.getData();
			if (StringUtils.equals(ResponseMessage.SUCCESS, repsonseMessage.getCode())) {
				orderId = (Long) repsonseMessage.getData().get("orderId");
				String orderCn = (String) repsonseMessage.getData().get("orderCn");
				memberCd = orderVo.getOrder().getMemberCn();
				String sid = SidGenerator.generate(Constants.CUSTOMER_NO);
				model.addObject("sid", sid);
				String cardUrl = messageSource.getMessage("MPM_CREDITCARD_URL", new Object[] {},LocaleContextHolder.getLocale());
				model.addObject("cardUrl", cardUrl);
				String operator = "";
				model.addObject("operator", operator);
				model.addObject("memberCd", memberCd);
				model.addObject("projectCode", pcode);
				model.addObject("orderId", orderId);
				model.addObject("orderCn", orderCn);
				model.addObject("passengers", data.get("succeedTicketInfo"));
				if (data.get("succeedTicketId") != null) {
					List<Long> succeedTicketId = (List<Long>) data.get("succeedTicketId");
					if (succeedTicketId.size() > 0) {
						model.addObject("payDetail", data.get("payDetail"));
					} else {
						OrderItemVo orderItemVo = orderVo.getOrderItemsVos().get(0);
						model.addObject("payDetail",
								calculateDetail(orderItemVo.getTrainTicketVos().get(0).getPassengerVos()));
					}
				}
				model.addObject("goodsIds", data.get("succeedTicketId"));
				model.addObject("message", data.get("message"));
			} else {
				model = new ModelAndView("/error/errorMsg");
				model.addObject("msg", "预定失败");
			}
		} catch (Exception e) {
			logger.error("createOrder预定订单异常", e);
		}
		try{
			List<ApprovalManVO> approvalManVos = this.findMessageConfig(memberCd, orderId);
			model.addObject("approvalManVos", approvalManVos);
		}catch(Exception e){
			logger.error("获取消息配置异常----", e);
		}
		try {
			String isMonthlyPay = ticketService.isMonthlyPay(memberCd);
			if (StringUtils.isNotBlank(isMonthlyPay)) {
				model.addObject("isMonthlyPay", 1);
			} else {
				model.addObject("isMonthlyPay", 0);
			}
		}catch(Exception e){
			logger.error("判断会员结算方式异常----", e);
		}
		return model;
	}
    
	//--消息配置--
	public List<ApprovalManVO> findMessageConfig(String memberCd,Long orderId) throws Exception{
		Corporation corporation = ducorporationService.retrieveCorporationByMbrShipCd(memberCd);
		//Member Member =dumemberAdapterService.retrieveMemberByMbrShipCd(memberCd);
		List<MessageConfiguration> messageConfigurationList = new ArrayList<MessageConfiguration>();
		if(null != corporation){
			messageConfigurationList = ducorporationConfigService.retrieveMessageConfigurationsByCorporationId(corporation.getCorporationId());
			/*String dataStr = organizationConfigAdapter.retrieveMessageConfigByCorporationId(corporation.getCorporationId());
			if(StringUtils.isNotBlank(dataStr)){
				JSONArray list = null;
				try {
					list = JSONArray.parseArray(dataStr);
				} catch (Exception e) {
					e.printStackTrace();
				}
				if(null != list && list.size() > 0){
					for(int i=0;i < list.size();i++){
						JSONObject obj = list.getJSONObject(i);
						MessageConfiguration messageConfiguration = obj.toJavaObject(obj, MessageConfiguration.class);
						messageConfigurationList.add(messageConfiguration);
					}
				}
			}*/
		}
		
		List<ApprovalManVO> receivers = genMessageReceivers(memberCd,orderId, messageConfigurationList);
		return receivers;
	}
	
	//统一配送
	public Object findDeliveryConfig(String memberCd) throws Exception{
		//统一配送地址列表
        String address = deliveryAddress.getDeliveryAddresses(memberCd);
        JSONArray array = null;
        if(StringUtils.isNotBlank(address)){
        	try {
				array = JSONArray.parseArray(address);
			} catch (Exception e) {
				e.printStackTrace();
			}
        }
        return array;
	}
	
	//历史配送地址
	public List<AddressInfo> findHistoryAdd(String memberCd) throws Exception{
        List<AddressInfo> list = null;
        try {
            list = addressManageService.queryAddressByCd(memberCd);
        } catch (Exception e) {
           // log.error("取历史配送地址失败", e);
        }
        return list;
	}
	
	//驻站地址查询
	public Object findSatationAddress(String corporationNum) throws Exception{
		String address = satationAddressAdapter.loadSatationAddressByCorporationNum(corporationNum);
		JSONObject obj = null;
		if(StringUtils.isNotBlank(address)){
			try {
				obj = JSONObject.parseObject(address);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return obj;
	}
	/**
	    * 取得信息接收人
	    *
	    * @throws ServiceException
	    */
   protected List<ApprovalManVO> genMessageReceivers(String memberCd,Long orderId, List<MessageConfiguration> messageConfigurationList) throws Exception {
	   logger.info(">>>>>>>genMessageReceivers>>>>>>>>memberCd:"+memberCd+">>>>>>orderId:"+orderId+">>>>>>>>messageConfigurationList:"+messageConfigurationList);
	   List<ApprovalManVO> receivers = messageManageService.getMessageReceivers(memberCd,orderId);
       for (ApprovalManVO approvalManVo : receivers) {
           approvalManVo.setSendMode("zs");
           for (MessageConfiguration messageConfiguration : messageConfigurationList) {
               // 联系人
               //这里让他不等于WEATHER_SMS，是因为天气预报这个功能不需要模板，模板是自己拼装，所以要排除
               if (isLinkMan(approvalManVo) && RecipientType.CONTACT_PERSON.name().equals(messageConfiguration.getReceiverType4Str()) &&!messageConfiguration.getMsgType4Str().equals("WEATHER_SMS")) {
                   assemble(approvalManVo, messageConfiguration);
               }
               // 乘机人
               //这里让他不等于WEATHER_SMS，是因为天气预报这个功能不需要模板，模板是自己拼装，所以要排除
               if (isPassager(approvalManVo) && RecipientType.PASS_PERSON.name().equals(messageConfiguration.getReceiverType4Str()) &&!messageConfiguration.getMsgType4Str().equals("WEATHER_SMS")) {
                   assemble(approvalManVo, messageConfiguration);
               }
           }
       }

       return receivers;
   }
	   
   private void assemble(ApprovalManVO approvalManVo, MessageConfiguration messageConfiguration) {
       if (StringUtils.isNotBlank(messageConfiguration.getMsgType4Str()) && !messageConfiguration.getMsgType4Str().equals(MessageType.APPROVE_EMAIL_AUTO.name()) && !messageConfiguration.getMsgType4Str().equals(MessageType.APPROVE_EMAIL_HANDAUDIT.name())) {
    	   if(messageConfiguration.getMsgType4Str().equals(MessageType.CONFIRM_SMS)){
    		    approvalManVo.setAffirm("Y");
    			approvalManVo.setAffirmTemplet(String.valueOf(messageConfiguration.getTemplateId()));
    	   }else if(messageConfiguration.getMsgType4Str().equals(MessageType.ISSUE_SMS)){
    		    approvalManVo.setIssut("Y");
    			approvalManVo.setIssutTemplet(String.valueOf(messageConfiguration.getTemplateId()));
    	   }else if(messageConfiguration.getMsgType4Str().equals(MessageType.TRAVEL_EMAIL)){
    		    approvalManVo.setTrip_mail("Y");
    			approvalManVo.setTripmailTemplet(String.valueOf(messageConfiguration.getTemplateId()));
    	        if(messageConfiguration.getContentType() != null) approvalManVo.setTripmailType(messageConfiguration.getContentType4Str());
    	   }
    	   
       }
   }
   
   private boolean isPassager(ApprovalManVO approvalManVo) {
       return approvalManVo.getIdentityType().equals(RoleTypes.PASSENGER);
   }

   private boolean isLinkMan(ApprovalManVO approvalManVo) {
       return approvalManVo.getIdentityType().equals(RoleTypes.LINKMAN)
               || approvalManVo.getIdentityType().equals(RoleTypes.MEMBER)
               || approvalManVo.getIdentityType().equals(RoleTypes.AUDITMAN)
               || approvalManVo.getIdentityType().equals(RoleTypes.SECRETARY);
   }
   
   
   /**
	 * 跳转到发消息页面
	 * @param memberCd  会稽号
	 * @param projectId 项目ID
	 * @param orderCn   订单号
	 * @return
	 */
	@RequestMapping("/messageConfig")
	public ModelAndView orderMessageConfig(@RequestParam("memberCd")String memberCd,String projectCode,@RequestParam("orderCn")String orderCn,@RequestParam("orderId")long orderId){
		ModelAndView modelAndView = new ModelAndView("messageconfig");
		try {
			String html = messageTemplateService.showEmailTemplat(orderCn, Constants.TICKET_EMAIL_NUM, Constants.TICKET_EMAIL_NAME);
			modelAndView.addObject("mailTemplet",html);
			Corporation corporation = bookPageService.getCorporation(memberCd);
			long projectId = 0l;
			
			String pCode = projectCode;
			if(StringUtils.isNotBlank(projectCode)){
				String[] s = projectCode.split("/");
				if(s.length>1){
					pCode = s[1];
				}
			}
			if(corporation!=null){
				Project project = duprojectService.retrieveProjectByCode(pCode,corporation.getCorporationId());
				if(project!=null){
					projectId = project.getProId();
				}
			}
			
			//判断是否需要审批
			boolean needApproval = false; 
			try {
				needApproval = approvalManageService.isNeedApproval(memberCd, projectId);
			} catch (Exception e1) {
				logger.error("查询是否需要审批出错",e1);
			}
			logger.info("needApproval:"+needApproval);
			modelAndView.addObject("needApproval",needApproval);
			modelAndView.addObject("memberCd",memberCd);
			modelAndView.addObject("orderCn",orderCn);
			modelAndView.addObject("orderId",orderId);
			modelAndView.addObject("projectCode",projectCode);
			
			//如果需要返回审批人（一级，二级，三级审批人）
			try{
				if(needApproval){
					Map<String, Map<FlowNode, List<ApprovalManVO>>> approvals = approvalManageService.retrieveApprovalMan(memberCd, projectId,true);
					modelAndView.addObject("approvals",sortFlow(approvals));
				}
				
			}catch(Exception e){
				logger.error("查询审批人失败",e);
			}
			
			//信息接收人
			List<ApprovalManVO> messageReceives = findMessageConfig(memberCd, orderId);
			modelAndView.addObject("messageReceives",messageReceives);
			
			//返回消息（邮件和短信）模板,最终发给用户的内容
			String smsMessage = messageManageService.viewIssuedSMSInfo(orderCn);
			modelAndView.addObject("messageTemplet",smsMessage);
			
			/*modelAndView.addObject("mailTemplet","<div id='u47' class='text'>"+
           "<p><span style='font-family:\'应用字体 Regular\', \'应用字体\';'>上海南站（19：41开</span><span style='font-family:\'应用字体 Regular\', \'应用字体\';'>)</span><span style='font-family:\'应用字体 Regular\', "
           + "\'应用字体\';'>--</span><span style='font-family:\'应用字体 Regular\', \'应用字体\';'>深圳站（次日13：22到）</span></p> </div>");
			*/
			
			
		} catch (Exception e) {
			logger.info("跳转到发消息页面出错", e);
		}		
		return modelAndView;
	}
	/**
	 * 将查询出来的审批进行排序
	 * @param map
	 * @return
	 */
	private Map<String, Map<FlowNode, List<ApprovalManVO>>> sortFlow(Map<String, Map<FlowNode, List<ApprovalManVO>>> map){
		Map<String, Map<FlowNode, List<ApprovalManVO>>> finalMap = new LinkedHashMap<String, Map<FlowNode, List<ApprovalManVO>>>();
		
		for (Map.Entry<String, Map<FlowNode, List<ApprovalManVO>>> mapping : map.entrySet()) {
			Map<FlowNode, List<ApprovalManVO>> approvalManVOMap = new TreeMap<FlowNode, List<ApprovalManVO>>(
					new Comparator<FlowNode>() {
						public int compare(FlowNode node1, FlowNode node2) {
							if (node1 == null || node2 == null)
								return 0;
							return String.valueOf(node1.getNodePosition())
									.compareTo(String.valueOf(node2.getNodePosition()));
						}
					});
			for(Map.Entry<FlowNode, List<ApprovalManVO>> mapping2 : mapping.getValue().entrySet()){
				approvalManVOMap.put(mapping2.getKey() ,mapping2.getValue());
				
			}
			finalMap.put(mapping.getKey(), approvalManVOMap);

		}
		return finalMap;
	}
	
	/**
	 *  人工审核 
	 * @param orderId 订单ID   
	 * @param approvalType  审核类型，0通过，1拒绝
	 * @return
	 * @throws Exception
	 */
	@RequestMapping("/humanApprovalOrder")
	@ResponseBody
	public ResponseMessage humanApprovalOrder(@RequestParam("orderId")Long orderId,int approvalType) throws Exception{
    	ResponseMessage responseMessage = new ResponseMessage();
    	ApprovalType type = null;
    	if(approvalType==0){
    		type = ApprovalType.PASS;
    	}else{
    		type = ApprovalType.REJECT;
    	}
    	try{
    		responseMessage = trainService.humanApprovalPayment(orderId,type);
    	}catch(Exception e){
    		logger.error("人工审核失败"+orderId+",approvalType:"+approvalType,e);
    	}
    	return responseMessage;
    }
   
	
	private PayDetailBean calculateDetail(List<PassengerVo> passengerVos){
		PayDetailBean payDetail = new PayDetailBean();
		BigDecimal sumTicketPrice = new BigDecimal(0);
		BigDecimal sumFee = new BigDecimal(0);
		BigDecimal sumTmcPrice = new BigDecimal(0);
		BigDecimal total = new BigDecimal(0);
		for (PassengerVo pvo : passengerVos) {
			sumTicketPrice = sumTicketPrice.add(pvo.getPrice() == null ? new BigDecimal(0) : new BigDecimal(pvo.getPrice()));
			sumFee = sumFee.add(pvo.getFee() == null ? new BigDecimal(0) : new BigDecimal(pvo.getFee()));
			sumTmcPrice = sumTmcPrice.add(pvo.getPrice() == null ? new BigDecimal(0) : new BigDecimal(pvo.getPrice()));
		}
		total = total.add(sumFee);
		total = total.add(sumTmcPrice);
		payDetail.setSumTicketPrice(sumTicketPrice);
		payDetail.setSumTmcPrice(sumTmcPrice);
		payDetail.setSumFee(sumFee);
		payDetail.setTotal(total);
		return payDetail;
	}
	
	
	public List<ApprovalManVO> getMessageReceivers() {
		return messageReceivers;
	}
	
	public void setMessageReceivers(List<ApprovalManVO> messageReceivers) {
		this.messageReceivers = messageReceivers;
	}
	
}
