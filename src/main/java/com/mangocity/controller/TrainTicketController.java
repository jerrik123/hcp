package com.mangocity.controller;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.annotation.Resource;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.mangocity.btms.adpater.vo.ApprovalManVO;
import com.mangocity.btms.api.IApprovalManageService;
import com.mangocity.btms.api.ICorporationService;
import com.mangocity.btms.model.Corporation;
import com.mangocity.btms.projectmanagement.model.Project;
import com.mangocity.btms.projectmanagement.service.ProjectService;
import com.mangocity.constant.Constants;
import com.mangocity.easy.workflow.model.FlowNode;
import com.mangocity.enums.GoodsType;
import com.mangocity.framework.creditcard.SidGenerator;
import com.mangocity.model.Applicant;
import com.mangocity.model.Contact;
import com.mangocity.model.Order;
import com.mangocity.response.ResponseMessage;
import com.mangocity.service.IBookPageService;
import com.mangocity.service.IPaymentService;
import com.mangocity.service.ITicketService;
import com.mangocity.service.ITrainService;
import com.mangocity.utils.JsonUtil;
import com.mangocity.vo.OrderBasisVo;
import com.mangocity.vo.OrderDetailBean;
import com.mangocity.vo.PageOrderParameter;
import com.mangocity.vo.PageQueryResult;
import com.mangocity.vo.PassStationVo;
import com.mangocity.vo.PayParamsVo;
import com.mangocity.vo.RefundFeeVo;
import com.mangocity.vo.RefundParamVo;
import com.mangocity.vo.TicketInfoVo;

/**
 * 火车票控制类
 * 
 * @author hongxiaodong
 *
 */
@Controller
@RequestMapping("/train")
public class TrainTicketController extends BaseCantroller {

	Logger logger = Logger.getLogger(TrainTicketController.class);
	
	@Autowired
	private ITrainService trainService;
	@Autowired
	private ITicketService ticketService;
	@Autowired
	private IPaymentService paymentService;
	@Autowired
	private IBookPageService bookPageService;
	@Autowired
	private ProjectService duprojectService;
	@Autowired
	private ICorporationService ducorporationService;
	@Autowired
	private IApprovalManageService approvalManageService;
	
	@Resource
	private MessageSource messageSource;
		
	/**
	 * 2、接口：查询车次、余票及票价信息
	 * @param fromStation 出发站三字码
	 * @param toStation 到达站三字码
	 * @param trainDate 乘车日期（yyyy-MM-dd）
	 */
	@RequestMapping(value="/queryTrainTicket", method=RequestMethod.GET)
	@ResponseBody
	public Object queryTrainTicket(@RequestParam("fromStation")String fromStation,@RequestParam("toStation")String toStation,@RequestParam("trainDate")String trainDate) throws Exception{
		Map<String,Object> map = new HashMap<String,Object>();
		map.put("fromStation", fromStation);
		map.put("toStation", toStation);
		map.put("trainDate", trainDate);
		ResponseMessage responseMessage = trainService.queryTrainTicket(map);
		return responseMessage;
	}
	
	//获取途经站点信息
	@RequestMapping(value="/queryPassStations")
	@ResponseBody
	public Object queryPassStations(PassStationVo passStationVo) throws Exception{
		ResponseMessage responseMessage = trainService.queryPassStations(passStationVo);
		return responseMessage;
	}
	
	@RequestMapping("/queryOrders")
	@ResponseBody
	public Object queryOrders(PageOrderParameter pageOrderParameter) {
		ResponseMessage responseMessage = new ResponseMessage();
		try {
			PageQueryResult<OrderBasisVo> result = ticketService.queryOrders(pageOrderParameter);
			Map<String, Object> map = new HashMap<>();
			map.put("ordersVo", result);
			responseMessage.setData(map);
			responseMessage.setCode(ResponseMessage.SUCCESS);
		} catch (Exception e) {
			responseMessage.setCode(ResponseMessage.ERROR);
			responseMessage.setMsg("系统异常");
			logger.error("queryOrders查询订单异常：", e);
		}
		return responseMessage;
	}
	
	@RequestMapping("/queryBindTickets")
	@ResponseBody
	public Object queryBindTickets(String orderCd, Long orderId) {
		ResponseMessage responseMessage = new ResponseMessage();
		try {
			List<TicketInfoVo> result = ticketService.queryBindTicket(orderId, orderCd,null,null);
			if (result != null) {
				Map<String, Object> map = new HashMap<>();
				responseMessage.setData(map);
				map.put("tickets", result);
				responseMessage.setCode(ResponseMessage.SUCCESS);
			} else {
				responseMessage.setCode(ResponseMessage.ERROR);
				responseMessage.setMsg("获取失败");
			}
		} catch (Exception e) {
			responseMessage.setCode(ResponseMessage.ERROR);
			responseMessage.setMsg("系统异常");
			logger.error("queryBindTickets查询票异常：", e);
		}
		return responseMessage;
	}
	
	@RequestMapping("/queryOrderDetail")
	public ModelAndView queryOrderDetail(@RequestParam("orderCd") String orderCd) {
		ModelAndView modelAndView = new ModelAndView("orderdetail");
		OrderDetailBean orderDetail = null;
		try {
			orderDetail = ticketService.queryOrderDetail(orderCd);
			modelAndView.addObject("orderVo", orderDetail);
		} catch (Exception e) {
			logger.error("queryOrderDetail查询订单明细异常", e);
		}
		try {
			if (orderDetail != null) {
				Corporation corporation = ducorporationService.retrieveCorporationByMbrShipCd(orderDetail.getOrderBasic().getMemberCn());
				if (corporation != null) {
					modelAndView.addObject("corporation", corporation.getZhName());
				}
			}
		} catch (Exception e) {
			logger.error("查询公司名称异常", e);
		}
		return modelAndView;
	}
	
	@RequestMapping("/redirectUpdateOrder")
	public ModelAndView redirectUpdateOrder(@RequestParam("orderCd")String orderCd){
		ModelAndView modelAndView = new ModelAndView("updateorder");
		try {
			OrderDetailBean orderDetail = ticketService.queryOrderDetail(orderCd);
			modelAndView.addObject("orderVo",orderDetail);
			String sid = SidGenerator.generate(Constants.CUSTOMER_NO);
			modelAndView.addObject("sid",sid);
			String cardUrl = messageSource.getMessage("MPM_CREDITCARD_URL", new Object[] {},LocaleContextHolder.getLocale());
			modelAndView.addObject("cardUrl",cardUrl);
			String operator = "";
			modelAndView.addObject("operator",operator);
			try {
				if (orderDetail != null) {
					Corporation corporation = ducorporationService.retrieveCorporationByMbrShipCd(orderDetail.getOrderBasic().getMemberCn());
					if (corporation != null) {
						modelAndView.addObject("corporation", corporation.getZhName());
					}
				}
			} catch (Exception e) {
				logger.error("查询公司名称异常", e);
			}
			try {
				String isMonthlyPay = ticketService.isMonthlyPay(orderDetail.getOrderBasic().getMemberCn());
				if (StringUtils.isNotBlank(isMonthlyPay)) {
					modelAndView.addObject("isMonthlyPay", 1);
				} else {
					modelAndView.addObject("isMonthlyPay", 0);
				}
			}catch(Exception e){
				logger.error("判断会员结算方式异常----", e);
			}
		} catch (Exception e) {
			logger.error("queryOrderDetail查询订单明细异常", e);
		}		
		return modelAndView;
	}
	
	@RequestMapping("/queryDifferentStatusOrderNum")
	@ResponseBody
	public Object queryDifferentStatusNum(String status,String payStatus){
		ResponseMessage responseMessage = new ResponseMessage();
		try{
			Map result = ticketService.queryDifferentStatusOrderNum(status,payStatus);
			responseMessage.setData(result);
			responseMessage.setCode(ResponseMessage.SUCCESS);
		}catch(Exception e){
			responseMessage.setCode(ResponseMessage.ERROR);
			responseMessage.setMsg("系统异常");
			logger.error("queryDifferentStatusOrderNum查询不同状态订单数量异常", e);
		}
		return responseMessage;
	}
	
	/**
	 * 查询退票订单
	 * @param pageOrderParameter
	 * @return
	 */
	@RequestMapping("/queryRefundOrders")
	@ResponseBody
	public Object queryRefundOrders(PageOrderParameter pageOrderParameter) {
		ResponseMessage responseMessage = new ResponseMessage();
		//类型 0-订票 1-改签 2-退票
		pageOrderParameter.setType("2");
		try {
			PageQueryResult<OrderBasisVo> result = ticketService.queryRefundOrders(pageOrderParameter);
			Map<String, Object> map = new HashMap<>();
			map.put("ordersVo", result);
			responseMessage.setData(map);
			responseMessage.setCode(ResponseMessage.SUCCESS);
		} catch (Exception e) {
			responseMessage.setCode(ResponseMessage.ERROR);
			responseMessage.setMsg("系统异常");
			logger.error("queryOrders查询订单异常：", e);
		}
		return responseMessage;
	}
	/**
	 * 查询退票各种状态数
	 * @return
	 */
	@RequestMapping("/queryDifferentStatusRefundOrderNum")
	@ResponseBody
	public Object queryDifferentStatusRefundOrderNum(String status){
		ResponseMessage responseMessage = new ResponseMessage();
		try{
			Map result = ticketService.queryDifferentStatusRefundOrderNum(status);
			responseMessage.setData(result);
			responseMessage.setCode(ResponseMessage.SUCCESS);
		}catch(Exception e){
			responseMessage.setCode(ResponseMessage.ERROR);
			responseMessage.setMsg("系统异常");
			logger.error("queryDifferentStatusOrderNum查询不同状态订单数量异常", e);
		}
		return responseMessage;
	}
	
	//按订单项查询退票火车票
	@RequestMapping("/queryBindRefundTickets")
	@ResponseBody
	public Object queryBindRefundTickets(String orderCd, Long orderId,Long orderItemId) {
		ResponseMessage responseMessage = new ResponseMessage();
		try {
			//类型 0-订票 1-改签 2-退票
			List<TicketInfoVo> result = ticketService.queryBindTicket(orderId, orderCd,"2",orderItemId);
			if (result != null) {
				Map<String, Object> map = new HashMap<>();
				responseMessage.setData(map);
				map.put("tickets", result);
				responseMessage.setCode(ResponseMessage.SUCCESS);
			} else {
				responseMessage.setCode(ResponseMessage.ERROR);
				responseMessage.setMsg("获取失败");
			}
		} catch (Exception e) {
			responseMessage.setCode(ResponseMessage.ERROR);
			responseMessage.setMsg("系统异常");
			logger.error("queryBindTickets查询票异常：", e);
		}
		return responseMessage;
	}
	
	/**
	 * 退票申请页面
	 * @param orderCd
	 * @return
	 */
	@RequestMapping("/showRefundOrderApply")
    public ModelAndView showRefundOrderApply(@RequestParam("orderId")Long orderId){
		ModelAndView modelAndView = new ModelAndView("refundticket");
		try {
			OrderDetailBean orderDetail = trainService.queryRefundOrderApply(orderId);
			modelAndView.addObject("orderDetail",orderDetail);
		} catch (Exception e) {
			logger.error("showRefundOrderApply退票申请页面异常", e);
		}		
		return modelAndView;
	}
	
	/**
	 * 退票详情页面
	 * @param orderCd
	 * @return
	 */
	@RequestMapping("/showRefundOrderDetail")
    public ModelAndView showRefundOrderDetail(@RequestParam("orderId")Long orderId,@RequestParam("orderItemId")Long orderItemId) throws Exception{
		ModelAndView modelAndView = new ModelAndView("refundorderdetail");
		try {
			//类型 0-订票 1-改签 2-退票
			OrderDetailBean orderDetail = trainService.queryRefundOrderDetail(orderId,"2",orderItemId);
			modelAndView.addObject("orderDetail",orderDetail);
		} catch (Exception e) {
			logger.error("showRefundOrderDetail退票详情页面异常", e);
		}		
		return modelAndView;
	}
	
	/**
	 * 跳转到修改退票详情页面
	 * @param orderCd
	 * @return
	 */
	@RequestMapping("/sendUpdateRefundOrder")
    public ModelAndView sendUpdateRefundOrder(@RequestParam("orderId")Long orderId,@RequestParam("orderItemId")Long orderItemId) throws Exception{
		ModelAndView modelAndView = new ModelAndView("updaterefundticket");
		try {
			//类型 0-订票 1-改签 2-退票
			OrderDetailBean orderDetail = trainService.queryRefundOrderDetail(orderId,"2",orderItemId);
			modelAndView.addObject("orderDetail",orderDetail);
		} catch (Exception e) {
			logger.error("sendUpdateRefundOrder跳转退票修改页面异常", e);
		}		
		return modelAndView;
	}
	
	
	/**
	 * 退票详情页面--保存
	 * @param orderId
	 * @param applicant
	 * @param refundDesc
	 * @param verifyDesc
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value="/saveRefundOrderDetail")
	@ResponseBody
	public Object saveRefundOrderDetail(Long orderId,Applicant applicant,
			String refundDesc,String verifyDesc,RefundFeeVo refundFeeVo
			,String refundParamVoJson) throws Exception{
		logger.info("refundParamVoJson:"+refundParamVoJson);
		ResponseMessage responseMessage = new ResponseMessage();
		try{
			JSONArray jsonArray = JSONArray.parseArray(refundParamVoJson);  
			if(jsonArray==null||jsonArray.size()==0){
				throw new Exception("参数有误");
			}
			List<RefundParamVo> travelInfoVO = new ArrayList<RefundParamVo>(jsonArray.size());
	        for(int i = 0; i < jsonArray.size(); ++i){
	        	RefundParamVo t = JSON.parseObject(jsonArray.getString(i), RefundParamVo.class);    
	        	travelInfoVO.add(t);
	        }
			refundFeeVo.setRefundParamVoList(travelInfoVO);
			responseMessage = trainService.saveRefundOrderDetail(orderId, applicant, refundDesc, verifyDesc,refundFeeVo);
		}catch(Exception e){
			logger.error("修改退改订单失败",e);
			responseMessage.setCode(ResponseMessage.ERROR);
			responseMessage.setMsg("系统异常");
			logger.info("保存失败", e);
		}
		return responseMessage;
	}
	
	/**
	 * 退票详情页面--再次提交审核
	 * @param orderId
	 * @param applicant
	 * @param refundDesc
	 * @param verifyDesc
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value="/updateRefundOrderDetail")
	@ResponseBody
	public Object updateRefundOrderDetail(Long orderId,Applicant applicant,
			String refundDesc,String verifyDesc,RefundFeeVo refundFeeVo
			,String refundParamVoJson) throws Exception{
		logger.info("refundParamVoJson:"+refundParamVoJson);
		ResponseMessage responseMessage = new ResponseMessage();
		try{
			JSONArray jsonArray = JSONArray.parseArray(refundParamVoJson);  
			if(jsonArray==null||jsonArray.size()==0){
				throw new Exception("参数有误");
			}
			List<RefundParamVo> travelInfoVO = new ArrayList<RefundParamVo>(jsonArray.size());
	        for(int i = 0; i < jsonArray.size(); ++i){
	        	RefundParamVo t = JSON.parseObject(jsonArray.getString(i), RefundParamVo.class);    
	        	travelInfoVO.add(t);
	        }
	        String status = "2";//待审核
			refundFeeVo.setRefundParamVoList(travelInfoVO);
			responseMessage = trainService.updateRefundOrderDetail(orderId, status, applicant, refundDesc, verifyDesc,refundFeeVo);
		}catch(Exception e){
			logger.error("修改退改订单失败",e);
			responseMessage.setCode(ResponseMessage.ERROR);
			responseMessage.setMsg("系统异常");
			logger.info("保存失败", e);
		}
		return responseMessage;
	}
	
	/**
	 * 火车票退票
	 * @throws Exception
	 */
	@RequestMapping(value="/createRefundTicket")
	@ResponseBody
	public Object createRefundTicket(PayParamsVo params,RefundFeeVo refundFeeVo,String refundParamVoJson) throws Exception{
		logger.info("refundParamVoJson:"+refundParamVoJson);
		ResponseMessage responseMessage = new ResponseMessage();
		try{
			JSONArray jsonArray = JSONArray.parseArray(refundParamVoJson);
			if(jsonArray==null||jsonArray.size()==0){
				throw new Exception("参数有误");
			}
			List<RefundParamVo> travelInfoVO = new ArrayList<RefundParamVo>(jsonArray.size());
	        for(int i = 0; i < jsonArray.size(); ++i){
	        	RefundParamVo t = JSON.parseObject(jsonArray.getString(i), RefundParamVo.class);    
	        	travelInfoVO.add(t);
	        }
			refundFeeVo.setRefundParamVoList(travelInfoVO);
			responseMessage = trainService.createRefundTicket(params,refundFeeVo);
		}catch(Exception e){
			logger.error("修改退改订单失败",e);
			responseMessage.setCode(ResponseMessage.ERROR);
			responseMessage.setMsg("系统异常");
		}
		return responseMessage;
	}
	
	
	/**
	 * 退票审核拒绝
	 * @param refundDesc
	 * @param verifyDesc
	 * @param status
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value="/refuseRefundTicket")
	@ResponseBody
	public Object changeRefundTicket(@RequestParam("orderId")Long orderId,@RequestParam("refundDesc")String refundDesc,@RequestParam("verifyDesc")String verifyDesc,
			RefundFeeVo refundFeeVo,String refundParamVoJson) throws Exception{
		logger.info("refundParamVoJson:"+refundParamVoJson);
		ResponseMessage responseMessage = new ResponseMessage();
		try{
			JSONArray jsonArray = JSONArray.parseArray(refundParamVoJson);
			if(jsonArray==null||jsonArray.size()==0){
				throw new Exception("参数有误");
			}
			List<RefundParamVo> travelInfoVO = new ArrayList<RefundParamVo>(jsonArray.size());
	        for(int i = 0; i < jsonArray.size(); ++i){
	        	RefundParamVo t = JSON.parseObject(jsonArray.getString(i), RefundParamVo.class);    
	        	travelInfoVO.add(t);
	        }
			refundFeeVo.setRefundParamVoList(travelInfoVO);
			String status = "3";//审核失败
			responseMessage = trainService.changeRefundOrder(orderId,status, refundDesc, verifyDesc, refundFeeVo);
		}catch(Exception e){
			logger.error("审核退改订单失败",e);
			responseMessage.setCode(ResponseMessage.ERROR);
			responseMessage.setMsg("系统异常");
		}
		return responseMessage;
	}
	
	/**
	 * 退票审核通过
	 * @param refundDesc
	 * @param verifyDesc
	 * @param status
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value="/passRefundTicket")
	@ResponseBody
	public Object passRefundTicket(@RequestParam("orderId")Long orderId,@RequestParam("refundDesc")String refundDesc,@RequestParam("verifyDesc")String verifyDesc,
			RefundFeeVo refundFeeVo,String refundParamVoJson) throws Exception{
		
		logger.info("refundParamVoJson:"+refundParamVoJson);
		ResponseMessage responseMessage = new ResponseMessage();
		try{
			JSONArray jsonArray = JSONArray.parseArray(refundParamVoJson);
			if(jsonArray==null||jsonArray.size()==0){
				throw new Exception("参数有误");
			}
			List<RefundParamVo> travelInfoVO = new ArrayList<RefundParamVo>(jsonArray.size());
	        for(int i = 0; i < jsonArray.size(); ++i){
	        	RefundParamVo t = JSON.parseObject(jsonArray.getString(i), RefundParamVo.class);    
	        	travelInfoVO.add(t);
	        }
			refundFeeVo.setRefundParamVoList(travelInfoVO);
			String status = "4";//已审核
			responseMessage = trainService.changeRefundOrder(orderId,status, refundDesc, verifyDesc, refundFeeVo);
		}catch(Exception e){
			logger.error("审核退改订单失败",e);
			responseMessage.setCode(ResponseMessage.ERROR);
			responseMessage.setMsg("系统异常");
		}
		return responseMessage;
	}

	//根据订单号取消座位
	@RequestMapping(value="/trainCancel")
	@ResponseBody
	public Object trainCancel(@RequestParam("orderCn")String orderCn) throws Exception{
		ResponseMessage responseMessage = trainService.checkTrainCancel(orderCn);
		return responseMessage;
	}
	
	/*
	 *  在线支付,包括了网站在线支付、3G在线支付、网站快捷支付
	 */
	@RequestMapping(value="/createPayProcess", method=RequestMethod.GET)
	@ResponseBody
	public ResponseMessage createPayProcess(@RequestParam("params")PayParamsVo params) throws Exception{
		ResponseMessage responseMessage = new ResponseMessage();
		if("TMON".equals(params.getPayModel())){//公司月结
			responseMessage = paymentService.createCompanyPay(params);
		}else{
			responseMessage = paymentService.createPayProcess(params);
		}
		return responseMessage;
	}
	
	//离线支付,包括了信用卡支付，上门收款（包括上门POS）、现金、3G信用卡支付、CC信用卡支付
	@RequestMapping(value="/createOffPayProcess", method=RequestMethod.POST)
	//@ResponseBody
	public ModelAndView createOffPayProcess(@RequestParam("payInfo") String payInfo) throws Exception {
		ModelAndView model = new ModelAndView("TrainOrderSuccess");
		logger.info("-------createOffPayProcess-----"+payInfo);
		try {
			ResponseMessage responseMessage = null;
			PayParamsVo params = (PayParamsVo) JsonUtil.JsonString2Object(payInfo, PayParamsVo.class);
			if (params == null) {
				model = new ModelAndView("error/errorMsg");
				model.addObject("msg", "缺少必要的参数");
				return model;
			}
			params.setGoodsType(GoodsType.TICKET.toString());
			if ("TMON".equals(params.getPayModel())) {// 公司月结
				responseMessage = paymentService.createCompanyPay(params);
			} else {
				responseMessage = paymentService.createOffPayProcess(params);
			}
			logger.info("支付返回结果" + responseMessage.getCode() + "---" + responseMessage.getMsg());
			// 保存预定信息通知人员
			List<ApprovalManVO> approvals = params.getMessageReceiver();
			bookPageService.saveMessageReeivers(approvals, params.getMembercd(), params.getOrderId());
			buildModel(model, params);
			Order order = new Order();
			order.setId(params.getOrderId());
			order.setPaymentMethod(params.getPayModel());
			order.setFrontRemark(params.getFrontRemark());
			// 判断是否需要审批
			Corporation corporation = ducorporationService.retrieveCorporationByMbrShipCd(params.getMembercd());
			if (null != corporation) {
				Project project = duprojectService.retrieveProjectByCode(params.getProjectCode(),
						corporation.getCorporationId());
				if (ticketService.isNeedAudit(params.getMembercd(), project)) {
					model.addObject("isNeedAudit", "1");
					order.setIsNeedAudit(1);
					order.setStatus("3");
				} else {
					if ("TMON".equals(params.getPayModel())) {
						ticketService.updateBindTicketStatus(params.getOrderCn());
						order.setStatus("6");
					}else{
						order.setStatus("3");
					}
					model.addObject("isNeedAudit", "0");
					order.setIsNeedAudit(0);
					order.setUpdateTime(new Date());
				}
				ticketService.updateOrderPaymethod(order);
				long proId = 0L;
				if(project!=null){
					proId = project.getProId();
				}
				Map<String, Map<FlowNode, List<ApprovalManVO>>> approvalMans = approvalManageService
						.retrieveApprovalMan(params.getMembercd(), proId, true);
				model.addObject("approvals", sortFlow(approvalMans));
			}
		} catch (Exception e) {
			logger.error("下单确认付款时异常----", e);
		}
		return model;
	}
	
	@RequestMapping("/updateOrderBaiscInfo")
	@ResponseBody
	public ModelAndView updateOrderBaiscInfo(OrderDetailBean orderDetail,PayParamsVo params){
		ModelAndView  model = new ModelAndView("success");
		ResponseMessage responseMessage = new ResponseMessage();
		try{
			Contact contact = orderDetail.getContact();
			if(StringUtils.isEmpty(contact.getIsForeign())){
				orderDetail.getContact().setIsForeign("0");
			}
			ticketService.updateOrderBasicInfo(orderDetail);	
			if(params!=null){
				if ("TMON".equals(params.getPayModel())) {
					responseMessage = paymentService.createCompanyPay(params);
				}
				if(StringUtils.isNotBlank(params.getCreditCardId())){
					paymentService.createOffPayProcess(params);
				}
			}
			responseMessage.setCode(ResponseMessage.SUCCESS);
			model.addObject("result","更新成功");
		}catch(Exception e){
			logger.error("updateOrderBaiscInfo更新异常", e);
			responseMessage.setCode(ResponseMessage.ERROR);
			responseMessage.setMsg("更新异常");
			model.addObject("result","更新异常"+e.fillInStackTrace());
		}
		return model;
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
	
	private void buildModel(ModelAndView model,PayParamsVo params) throws Exception{
		Order orderInfo = ticketService.getOrderBasicInfo(params.getOrderId());
		List<TicketInfoVo> tickets = ticketService.queryBindTicket(params.getOrderId(), params.getOrderCn(), "0",null);
		if (tickets != null && tickets.size() > 0) {
			model.addObject("travelInfo", tickets.get(0));
			long endTime = tickets.get(0).getEndTime().getTime();
			long startTime = tickets.get(0).getStartTime().getTime();
			long gapDays = (endTime - startTime) / (1000 * 60 * 60 * 24);
			model.addObject("gapDays", gapDays);
		}
		model.addObject("tickets", tickets);
		model.addObject("order", orderInfo);
	}
	
}
