package com.mangocity.utils;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;

public class RefundFeeUtil {
	
	//退票费百分比
	public static BigDecimal caculateRate(Date startDate) throws Exception{
		BigDecimal refundRate = new BigDecimal("0.0");
		if(null != startDate){
			long currentTime = System.currentTimeMillis();
			long startTime = startDate.getTime();
			if(startTime > currentTime){
				long time = startTime - currentTime;
				int startMonth = startDate.getMonth();
				int startDay = startDate.getDay();
				//乘车日期(1月24日至3月3日)在春运期间的，退票时一律按开车时间20%
				if(startMonth == 2 || (startMonth == 1 && startDay >= 24) || (startMonth == 3 && startDay <= 3)){
					refundRate = refundRate.add(new BigDecimal("0.2"));
				}else{
					//开车前15天以上不收退票费
					long fday = 15 * 24 * 60 * 60 * 1000;
					//开车前48小时以上按票价5%
					long fhour = 2 * 24 * 60 * 60 * 1000;
					//开车前24小时以上不足48小时按票价10%
					long thour = 24 * 60 * 60 * 1000;
					//开车前24小时以内按20%
					if(time > fhour && time <= fday){
						refundRate = refundRate.add(new BigDecimal("0.05"));
					}else if(time > thour && time <= fhour){
						refundRate = refundRate.add(new BigDecimal("0.1"));
					}else if(time < thour){
						refundRate = refundRate.add(new BigDecimal("0.2"));
					}
				}
			}else{
				refundRate = refundRate.add(new BigDecimal("1"));
			}
		}else{
			refundRate = refundRate.add(new BigDecimal("1"));
		}
		return refundRate;
	}
	
	//退票价格
	public static BigDecimal caculateFee(BigDecimal price) throws Exception{
		BigDecimal refundPrice = new BigDecimal("0.00");
		if(null != price){
			//退票费最低按2元收，尾数以5角为单位，尾数小于2.5角的舍去、2.5角以上且小于7.5角的计为5角、7.5角以上的进为1元
			if(price.compareTo(new BigDecimal("2.00")) < 0 && price.compareTo(new BigDecimal("0.00")) > 0){
				refundPrice = refundPrice.add(new BigDecimal("2.00"));
			}else{
				refundPrice = price.divideToIntegralValue(new BigDecimal("1.00"));
				BigDecimal tmp = price.subtract(refundPrice);
				if(tmp.compareTo(new BigDecimal("0.25")) <= 0){
					refundPrice = refundPrice.add(new BigDecimal("0.00"));
				}else if(tmp.compareTo(new BigDecimal("0.25")) > 0 && tmp.compareTo(new BigDecimal("0.75")) <= 0){
					refundPrice = refundPrice.add(new BigDecimal("0.5"));
				}else if(tmp.compareTo(new BigDecimal("0.75")) > 0){
					refundPrice = refundPrice.add(new BigDecimal("1.00"));
				}
			}
		}
		return refundPrice;
	}
}
