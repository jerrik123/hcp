package com.mangocity.service;

import java.util.Map;

import com.mangocity.enums.ApprovalType;
import com.mangocity.model.Applicant;
import com.mangocity.response.ResponseMessage;
import com.mangocity.vo.OrderDetailBean;
import com.mangocity.vo.PassStationVo;
import com.mangocity.vo.PayParamsVo;
import com.mangocity.vo.RefundFeeVo;

/**
 * 火车票服务类
 * @author hongxiaodong
 *
 */
public interface ITrainService {
	
	/**
	 * 2、接口：查询车次、余票及票价信息
	 * @param fromStation 出发站三字码
	 * @param toStation 到达站三字码
	 * @param trainDate 乘车日期（yyyy-MM-dd）
	 * @param routeType 行程类型：单程，往返
	 * @param channel 采购渠道
	 */
	public ResponseMessage queryTrainTicket(Map<String,Object> map) throws Exception;
	
	/**
	 * 获取途经站点信息
	 * @param passStationVo
	 * @return
	 * @throws Exception
	 */
	public ResponseMessage queryPassStations(PassStationVo passStationVo) throws Exception;
	
	/**
     * 火车票退票
     * @param orderItemId
     * @param goodsType
     * @param ticketNo
     * @return
     * @throws Exception
     */
    public ResponseMessage createRefundTicket(PayParamsVo params,RefundFeeVo refundFeeVo) throws Exception;
    
    /**
     * 取消座位
     * @param orderCn
     * @return
     * @throws Exception
     */
    public ResponseMessage checkTrainCancel(String orderCn) throws Exception;
    
    /**
	 * 申请退票页面
	 * @param orderId
	 * @return
	 * @throws Exception
	 */
	public OrderDetailBean queryRefundOrderApply(Long orderId) throws Exception;
    
    /**
     * 根据订单ID获取退票订单详情
     * @param orderId
     * @return
     * @throws Exception
     */
    public OrderDetailBean queryRefundOrderDetail(Long orderId,String type,Long orderItemId) throws Exception;
    
    /**
     * 退票订单详情保存
     * @param orderId
     * @param applicant
     * @param refundDesc
     * @param verifyDesc
     * @return
     * @throws Exception
     */
    public ResponseMessage saveRefundOrderDetail(Long orderId,Applicant applicant,String refundDesc,String verifyDesc,RefundFeeVo refundFeeVo) throws Exception;
    
    /**
     * 退票-再次提交审核
     * @param orderId
     * @param applicant
     * @param refundDesc
     * @param verifyDesc
     * @return
     * @throws Exception
     */
    public ResponseMessage updateRefundOrderDetail(Long orderId,String status,Applicant applicant, String refundDesc, String verifyDesc,RefundFeeVo refundFeeVo) throws Exception;
    
    /**
     * 人工审批如果通过，通知出票并发送短信，如果失败就发送审核
     * @param orderId
     * @param approvalType
     * @return
     */
    public ResponseMessage humanApprovalPayment(Long orderId,ApprovalType approvalType)throws Exception;
    
    /**
	 * 退票审核
	 * @param refundDesc
	 * @param verifyDesc
	 * @param status
	 * @return
	 * @throws Exception
	 */
    public ResponseMessage changeRefundOrder(Long orderId,String status,String refundDesc, String verifyDesc,RefundFeeVo refundFeeVo) throws Exception;

}
