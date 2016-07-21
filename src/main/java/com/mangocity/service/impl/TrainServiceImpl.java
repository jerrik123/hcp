package com.mangocity.service.impl;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.mangocity.api.IApplicantService;
import com.mangocity.api.IChargeService;
import com.mangocity.api.IContactService;
import com.mangocity.api.ITrainOrderService;
import com.mangocity.api.ITrainPayDetailService;
import com.mangocity.api.ITrainPayInfoService;
import com.mangocity.api.ITrainPayService;
import com.mangocity.api.ITrainTicketService;
import com.mangocity.btms.api.IApprovalMessageManageService;
import com.mangocity.btms.api.IMessageManageService;
import com.mangocity.enums.ApprovalType;
import com.mangocity.enums.GoodsType;
import com.mangocity.model.Applicant;
import com.mangocity.model.Contact;
import com.mangocity.model.Order;
import com.mangocity.model.Ticket;
import com.mangocity.model.TrainPay;
import com.mangocity.model.TrainPayDetail;
import com.mangocity.model.TrainPayInfo;
import com.mangocity.response.ResponseMessage;
import com.mangocity.service.IPaymentService;
import com.mangocity.service.ITrainService;
import com.mangocity.utils.RefundFeeUtil;
import com.mangocity.vo.OrderDetailBean;
import com.mangocity.vo.PassStationVo;
import com.mangocity.vo.PayParamsVo;
import com.mangocity.vo.RefundFeeVo;
import com.mangocity.vo.RefundParamVo;
import com.mangocity.vo.TicketInfoVo;

/**
 * 火车票服务类
 * @author hongxiaodong
 *
 */
@Service("trainService")
public class TrainServiceImpl implements ITrainService {
	
	Logger logger = Logger.getLogger(TrainServiceImpl.class);
	
	@Autowired
	private ITrainTicketService trainTicketService;
	
	@Autowired
	private IChargeService chargeService;
	
	@Autowired
	private ITrainOrderService trainOrderService;
	
	@Autowired
	private ITrainPayDetailService trainPayDetailService;
	
	@Autowired
	private IApplicantService applicantService;
	
	@Autowired
	private IApprovalMessageManageService approvalMessageManageService;
	
	@Autowired
	private IContactService contactService;
	
	@Autowired
	private IMessageManageService messageManageService;
	
	@Autowired
	private IPaymentService paymentService;
	
	@Autowired
	private ITrainPayInfoService trainPayInfoService;
	
	@Autowired
	private ITrainPayService trainPayService;

	/**
	 * 2、接口：查询车次、余票及票价信息
	 * @param fromStation 出发站三字码
	 * @param toStation 到达站三字码
	 * @param trainDate 乘车日期（yyyy-MM-dd）
	 * @param routeType 行程类型：单程，往返
	 * @param channel 采购渠道
	 */
	@Override
	public ResponseMessage queryTrainTicket(Map<String,Object> map) throws Exception{
		return trainTicketService.queryTrainTicket(map);
	}
	
	/**
	 * 获取途经站点信息
	 * @param passStationVo
	 * @return
	 * @throws Exception
	 */
	@Override
	public ResponseMessage queryPassStations(PassStationVo passStationVo) throws Exception{
		return trainTicketService.queryPassStations(passStationVo);
	}

	/**
     * 火车票退票
     * @param orderItemId
     * @param goodsType
     * @param ticketNo
     * @return
     * @throws Exception
     */
	@Override
	public ResponseMessage createRefundTicket(PayParamsVo params,RefundFeeVo refundFeeVo) throws Exception {
		ResponseMessage responseMessage = new ResponseMessage();
		try {
			trainTicketService.createRefundTicket(params,refundFeeVo);
			responseMessage.setCode(ResponseMessage.SUCCESS);
			responseMessage.setMsg("成功！");
		} catch (Exception e) {
			e.printStackTrace();
			responseMessage.setCode(ResponseMessage.ERROR);
			responseMessage.setMsg("createRefundTicket接口异常！");
		}
		return responseMessage;
	}
	
	
	//取消订单，取消座位
	@Override
	public ResponseMessage checkTrainCancel(String orderCn) throws Exception{
		ResponseMessage responseMessage = new ResponseMessage();
		try {
			List<TicketInfoVo> list = trainTicketService.findTicketByOrderIdOrCd(null,orderCn,null,null);
			for(TicketInfoVo vo : list){
				if(StringUtils.isNotBlank(vo.getHbOrderId())){
					trainTicketService.checkTrainCancel(vo.getHbOrderId());
				}
			}
			Order order = trainOrderService.findOrderByOrderCn(orderCn);
			if(null != order){
				Order newOrder = new Order();
				newOrder.setId(order.getId());
				newOrder.setStatus("12");//已取消
				newOrder.setUpdateTime(new Date());
				trainOrderService.updateOrderStatus(newOrder);
			}
			responseMessage.setCode(ResponseMessage.SUCCESS);
			responseMessage.setMsg("成功！");
		} catch (Exception e) {
			e.printStackTrace();
			responseMessage.setCode(ResponseMessage.ERROR);
			responseMessage.setMsg("checkTrainCancel接口异常！");
		}
		return responseMessage;
	}
	
	/**
	 * 申请退票页面
	 * @param orderId
	 * @return
	 * @throws Exception
	 */
	@Override
	public OrderDetailBean queryRefundOrderApply(Long orderId) throws Exception{
		OrderDetailBean orderDetail = new OrderDetailBean();
		Order order = trainOrderService.find(orderId);
		orderDetail.setOrderBasic(order);
		//查询可以退票的火车票,类型 0-订票 1-改签 2-退票
		//查询类型不等于2的火车票
		List<TicketInfoVo> ticketList = trainTicketService.findCanRefundTicketByOrderId(orderId, "2");
		if(null != ticketList && ticketList.size() > 0){
			SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
			SimpleDateFormat formatTime = new SimpleDateFormat("yyyy-MM-dd HH:mm");
			for(TicketInfoVo vo : ticketList){
				BigDecimal rate = new BigDecimal("0.2");//默认给20%
				try {
					String startTime = format.format(vo.getStartTime()) + " " + vo.getCcsj();
					rate = RefundFeeUtil.caculateRate(formatTime.parse(startTime));
				} catch (Exception e) {
					e.printStackTrace();
				}
				vo.setRefundRate(rate);
				BigDecimal fee = vo.getSalePrice() != null ? vo.getSalePrice().multiply(rate) : new BigDecimal("0");
				fee = RefundFeeUtil.caculateFee(fee);
				vo.setRefundFee(fee);//退票费
				BigDecimal amount = vo.getSalePrice() != null ? (vo.getTmcPrice().subtract(fee).compareTo(BigDecimal.ZERO ) < 0 ? new BigDecimal("0") : vo.getTmcPrice().subtract(fee)) : new BigDecimal("0");
				vo.setReturnAmount(amount);//退还金额
			}
		}
		orderDetail.setTickets(ticketList);
		//查看预订票支付详情,4=已支付
		List<TrainPayDetail> detailList = trainPayDetailService.findPayDetailByOrderId(orderId, "0",null);
		orderDetail.setPaymentDetails(detailList);
		return orderDetail;
	}

	/**
	 * 根据订单ID获取退票订单详情
	 * 类型 0-订票 1-改签 2-退票
	 */
	@Override
	public OrderDetailBean queryRefundOrderDetail(Long orderId,String type,Long orderItemId) throws Exception {
		OrderDetailBean orderDetail = new OrderDetailBean();
		RefundFeeVo refundFeeVo = new RefundFeeVo();
		try {
			Order order = trainOrderService.find(orderId);
			//查询退票火车票
			List<TicketInfoVo> ticketList = trainTicketService.findRefundTicketByOrderIdOrCd(orderId,null,type,orderItemId);
			if(null != ticketList && ticketList.size() > 0){
				SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
				SimpleDateFormat formatTime = new SimpleDateFormat("yyyy-MM-dd HH:mm");
				TicketInfoVo ticketInfoVo = ticketList.get(0);
				//BigDecimal rate = Constants.refundRate.multiply(new BigDecimal("100"));
				BigDecimal retrunAmount = new BigDecimal("0.0");
				BigDecimal retrunFeeAmount = new BigDecimal("0.0");
				List<Long> goodsIds = new ArrayList<Long>();
				for(TicketInfoVo vo : ticketList){
					goodsIds.add(vo.getId());
					//vo.getTmcPrice()-(vo.getSalePrice()-vo.getFee())/vo.getPreSalePrice()
					BigDecimal rate = new BigDecimal("0.2");//默认给20%
					BigDecimal tmcPrice = vo.getTmcPrice() != null ? vo.getTmcPrice() : new BigDecimal("0");
					BigDecimal salePrice = vo.getSalePrice() != null ? vo.getSalePrice() : new BigDecimal("0");
					BigDecimal feeVo = vo.getFee() != null ? vo.getFee() : new BigDecimal("0");
					BigDecimal preSalePrice = vo.getPreSalePrice() != null ? vo.getPreSalePrice() : new BigDecimal("0");
					try {
						BigDecimal temp = tmcPrice.subtract(salePrice).add(feeVo);
						if(preSalePrice.compareTo(new BigDecimal("0.0")) != 0){
							rate = temp.divide(preSalePrice,2,BigDecimal.ROUND_HALF_UP);//四舍五入保留两位
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
					if(rate.compareTo(BigDecimal.ZERO ) < 0){
						rate = new BigDecimal("0.0");
					}else if(rate.compareTo(new BigDecimal("1.0")) > 0){
						rate = new BigDecimal("1.0");
					}
					vo.setRefundRate(rate);
					BigDecimal fee = vo.getPreSalePrice() != null ? vo.getPreSalePrice().multiply(rate) : new BigDecimal("0");
					fee = RefundFeeUtil.caculateFee(fee);
					vo.setRefundFee(RefundFeeUtil.caculateFee(fee));//退票费
					//BigDecimal amount = vo.getSalePrice() != null ? (vo.getTmcPrice().subtract(fee).compareTo(BigDecimal.ZERO ) < 0 ? new BigDecimal("0") : vo.getTmcPrice().subtract(fee)) : new BigDecimal("0");
					//BigDecimal returnFee = vo.getFee() != null ? vo.getFee() : new BigDecimal("0");//应退服务费
					//vo.setReturnAmount(amount.add(returnFee));//退还金额
					
					retrunAmount = retrunAmount.add(salePrice.subtract(feeVo));//票面应退
					retrunFeeAmount = retrunFeeAmount.add(feeVo);
				}
				refundFeeVo.setSalePrice(retrunAmount);
				refundFeeVo.setChargerFee(retrunFeeAmount);
				//100积分=1元
				refundFeeVo.setPointNum(refundFeeVo.getPoint().multiply(new BigDecimal("100")));
				BigDecimal totalPrice = refundFeeVo.getCash().add(refundFeeVo.getChargerFee()).add(refundFeeVo.getDeliveryFee()).add(refundFeeVo.getOtherFee()).add(refundFeeVo.getPoint()).add(refundFeeVo.getSalePrice());
				refundFeeVo.setTotalPrice(totalPrice);
			    //申请人
				Applicant applicant = applicantService.find(ticketInfoVo.getApplicantId());
			    orderDetail.setApplicant(applicant);
			    orderDetail.setRefundDesc(ticketInfoVo.getRefundDesc());//备注信息
			    orderDetail.setVerifyDesc(ticketInfoVo.getVerifyDesc());//审核备注
			  //1=退款
			   List<TrainPayInfo> list = trainPayInfoService.findPayInfoByItemIdCn(orderId, "1", goodsIds, GoodsType.TICKET.toString());
			   if(list != null && list.size() > 0){
				   TrainPayInfo payInfo = list.get(0);
				   if(payInfo != null){
					   order.setPaymentStatus(payInfo.getStatus());
					   TrainPay pay = trainPayService.findTrainPayByInfoId(payInfo.getId());
					   if(pay != null){
						   //退票支付详情
						   List<TrainPayDetail> detailList = trainPayDetailService.findByPayId(pay.getId(), null);
						   orderDetail.setPaymentDetails(detailList);
					   }
				   }
			   }
			}
			orderDetail.setOrderBasic(order);
			orderDetail.setTickets(ticketList);
			orderDetail.setRefundFeeVo(refundFeeVo);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return orderDetail;
	}

	/**
     * 退票订单详情保存
     * @param orderId
     * @param applicant
     * @param refundDesc
     * @param verifyDesc
     * @return
     * @throws Exception
     */
	@Override
	public ResponseMessage saveRefundOrderDetail(Long orderId,Applicant applicant, String refundDesc, String verifyDesc,RefundFeeVo refundFeeVo) throws Exception {
		//查询退票火车票
		//类型 0-订票 1-改签 2-退票
		ResponseMessage responseMessage = new ResponseMessage();
		try {
			if(null != refundFeeVo){
				List<RefundParamVo> refundParamVoList = refundFeeVo.getRefundParamVoList();
				if(null != refundParamVoList && refundParamVoList.size() > 0){
					Ticket ticketVo = trainTicketService.find(refundParamVoList.get(0).getTicketId());
					if(null != ticketVo){
						Long applicantId = ticketVo.getApplicantId();
						if(null != applicantId && null != applicant){
							//申请人
							applicant.setId(applicantId);
							applicantService.update(applicant);
						}
					}
					List<Long> goodsIds = new ArrayList<Long>();
					for(RefundParamVo vo : refundParamVoList){
						goodsIds.add(vo.getTicketId());
						Ticket ticket = new Ticket();
						ticket.setId(vo.getTicketId());
						ticket.setFee(vo.getFee());
						ticket.setSalePrice(vo.getReturnAmount());
						String rDesc = null;
						String vDesc = null;
						if(null != ticketVo){
							rDesc = ticketVo.getRefundDesc();
							vDesc = ticketVo.getVerifyDesc();
						}
						ticket.setRefundDesc(rDesc != null ? (rDesc + "\r\n" + refundDesc) : refundDesc);
						ticket.setVerifyDesc(vDesc != null ? (vDesc + "\r\n" + verifyDesc) : verifyDesc);
						trainTicketService.updateTicket(ticket);
					}
					//更新支付金额
					if(refundFeeVo.getTotalPrice() != null){
						//type=1退款
						List<TrainPayInfo> list = trainPayInfoService.findPayInfoByItemIdCn(orderId, "1", goodsIds, GoodsType.TICKET.toString());
						if(null != list && list.size() > 0){
							TrainPayInfo trainPayInfo = list.get(0);
							if(null != trainPayInfo){
								Date date = new Date();
								TrainPay trainPay = trainPayService.findTrainPayByInfoId(trainPayInfo.getId());
								if(null != trainPay){
									TrainPay pay = new TrainPay();
									pay.setId(trainPay.getId());
									pay.setAmount(refundFeeVo.getTotalPrice());
									pay.setModifyTime(date);
									trainPayService.update(pay);
									List<TrainPayDetail> detailList = trainPayDetailService.findByPayId(trainPay.getId(), null);
									if(null != detailList && detailList.size() > 0){
										TrainPayDetail detail = detailList.get(0);
										TrainPayDetail payDetail = new TrainPayDetail();
										payDetail.setId(detail.getId());
										payDetail.setPayAmount(refundFeeVo.getTotalPrice());
										payDetail.setModifyTime(date);
										trainPayDetailService.update(payDetail);
									}
								}
							}
						}
					}
					responseMessage.setCode(ResponseMessage.SUCCESS);
					responseMessage.setMsg("成功！");
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			responseMessage.setCode(ResponseMessage.ERROR);
			responseMessage.setMsg("saveRefundOrderDetail接口异常！");
		}
		return responseMessage;
	}
	
	/**
     * 退票-再次提交审核
     * @param orderId
     * @param applicant
     * @param refundDesc
     * @param verifyDesc
     * @return
     * @throws Exception
     */
	@Override
	public ResponseMessage updateRefundOrderDetail(Long orderId,String status,Applicant applicant, String refundDesc, String verifyDesc,RefundFeeVo refundFeeVo) throws Exception {
		//查询退票火车票
		//类型 0-订票 1-改签 2-退票
		ResponseMessage responseMessage = new ResponseMessage();
		try {
			if(null != refundFeeVo){
				List<RefundParamVo> refundParamVoList = refundFeeVo.getRefundParamVoList();
				if(null != refundParamVoList && refundParamVoList.size() > 0){
					Ticket ticketVo = trainTicketService.find(refundParamVoList.get(0).getTicketId());
					if(null != ticketVo){
						Long applicantId = ticketVo.getApplicantId();
						if(null != applicantId && null != applicant){
							//申请人
							applicant.setId(applicantId);
							applicantService.update(applicant);
						}
					}
					List<Long> goodsIds = new ArrayList<Long>();
					for(RefundParamVo vo : refundParamVoList){
						goodsIds.add(vo.getTicketId());
						Ticket ticket = new Ticket();
						ticket.setId(vo.getTicketId());
						ticket.setFee(vo.getFee());
						ticket.setSalePrice(vo.getReturnAmount());
						String rDesc = null;
						String vDesc = null;
						if(null != ticketVo){
							rDesc = ticketVo.getRefundDesc();
							vDesc = ticketVo.getVerifyDesc();
						}
						ticket.setRefundDesc(rDesc != null ? (rDesc + "\r\n" + refundDesc) : refundDesc);
						ticket.setVerifyDesc(vDesc != null ? (vDesc + "\r\n" + verifyDesc) : verifyDesc);
						ticket.setStatus(status);
						trainTicketService.updateTicket(ticket);
					}
					//更新支付金额
					if(refundFeeVo.getTotalPrice() != null){
						//type=1退款
						List<TrainPayInfo> list = trainPayInfoService.findPayInfoByItemIdCn(orderId, "1", goodsIds, GoodsType.TICKET.toString());
						if(null != list && list.size() > 0){
							TrainPayInfo trainPayInfo = list.get(0);
							if(null != trainPayInfo){
								Date date = new Date();
								TrainPay trainPay = trainPayService.findTrainPayByInfoId(trainPayInfo.getId());
								if(null != trainPay){
									TrainPay pay = new TrainPay();
									pay.setId(trainPay.getId());
									pay.setAmount(refundFeeVo.getTotalPrice());
									pay.setModifyTime(date);
									trainPayService.update(pay);
									List<TrainPayDetail> detailList = trainPayDetailService.findByPayId(trainPay.getId(), null);
									if(null != detailList && detailList.size() > 0){
										TrainPayDetail detail = detailList.get(0);
										TrainPayDetail payDetail = new TrainPayDetail();
										payDetail.setId(detail.getId());
										payDetail.setPayAmount(refundFeeVo.getTotalPrice());
										payDetail.setModifyTime(date);
										trainPayDetailService.update(payDetail);
									}
								}
							}
						}
					}
					responseMessage.setCode(ResponseMessage.SUCCESS);
					responseMessage.setMsg("成功！");
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			responseMessage.setCode(ResponseMessage.ERROR);
			responseMessage.setMsg("saveRefundOrderDetail接口异常！");
		}
		return responseMessage;
	}
	
	/**
	 * 退票审核
	 * @param refundDesc
	 * @param verifyDesc
	 * @param status
	 * @return
	 * @throws Exception
	 */
	@Override
	public ResponseMessage changeRefundOrder(Long orderId,String status,String refundDesc, String verifyDesc,RefundFeeVo refundFeeVo) throws Exception {
		//查询退票火车票
		//类型 0-订票 1-改签 2-退票
		ResponseMessage responseMessage = new ResponseMessage();
		try {
			if(null != refundFeeVo){
				List<RefundParamVo> refundParamVoList = refundFeeVo.getRefundParamVoList();
				Ticket ticketVo = trainTicketService.find(refundParamVoList.get(0).getTicketId());
				List<Long> ticketIds = new ArrayList<Long>();
				if(null != refundParamVoList && refundParamVoList.size() > 0){
					for(RefundParamVo vo : refundParamVoList){
						ticketIds.add(vo.getTicketId());
						Ticket ticket = new Ticket();
						ticket.setId(vo.getTicketId());
						String rDesc = null;
						String vDesc = null;
						if(null != ticketVo){
							rDesc = ticketVo.getRefundDesc();
							vDesc = ticketVo.getVerifyDesc();
						}
						ticket.setRefundDesc(rDesc != null ? (rDesc + "\r\n" + refundDesc) : refundDesc);
						ticket.setVerifyDesc(vDesc != null ? (vDesc + "\r\n" + verifyDesc) : verifyDesc);
						ticket.setStatus(status);
						trainTicketService.updateTicket(ticket);
					}
					if("4".equals(status)){//审核通过
						trainTicketService.checkRefundTicket(orderId, ticketIds);
						responseMessage.setCode(ResponseMessage.SUCCESS);
						responseMessage.setMsg("成功！");
					}else{
						responseMessage.setCode(ResponseMessage.SUCCESS);
						responseMessage.setMsg("成功！");
					}
					
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			responseMessage.setCode(ResponseMessage.ERROR);
			responseMessage.setMsg("saveRefundOrderDetail接口异常！");
		}
		return responseMessage;
	}
	

	@Override
	public ResponseMessage humanApprovalPayment(Long orderId, ApprovalType approvalType) throws Exception {
		ResponseMessage reponseMessage = approvalMessageManageService.humanApprovalOrder(orderId, approvalType);
		if(ResponseMessage.ERROR.equals(reponseMessage.getCode())){
			return reponseMessage;
		}
		Contact contact = contactService.findContactByOrderId(orderId);
		if(ApprovalType.REJECT.equals(approvalType)){//审批失败
			String smsContent = "订单号:"+orderId+"（审批通过/拒绝） 审批失败，失败原因：审批订单被拒绝，如有问题请致电4006620088";
			messageManageService.sendSingleSMS(smsContent, contact.getTelephone());
			return reponseMessage;
		}
		String smsContent = "订单号:"+orderId+"（审批通过/拒绝） 审批成功，我们会尽快帮助客人出票，感谢您对芒果网的支持！4006620088";
		messageManageService.sendSingleSMS(smsContent, contact.getTelephone());
		
		ResponseMessage reponseMessages = paymentService.createHBPay(orderId);
		if(ResponseMessage.ERROR.equals(reponseMessages.getCode())){
			reponseMessage.setCode(ResponseMessage.ERROR);
			reponseMessage.setMsg("通知出票失败");
			return reponseMessage;
		}
		
		return reponseMessage;
	}

}
