package org.apache.fineract.portfolio.loanaccount.data;

import java.math.BigDecimal;
import java.util.Date;

public class GroupRepaymentDetailsDTO {

	final Date dueDate;
	final BigDecimal principleAmount;
	final Long installmentNumber;
	final BigDecimal interestAmount;
	
	public GroupRepaymentDetailsDTO(final BigDecimal principleAmount,final Long installmentNumber,final BigDecimal interestAmount,final Date dueDate){
		this.dueDate = dueDate;
		this.installmentNumber = installmentNumber;
		this.interestAmount = interestAmount;
		this.principleAmount = principleAmount;
	}
	
	public Date getDueDate() {
		return dueDate;
	}
	public BigDecimal getPrincipleAmount() {
		return principleAmount;
	}
	public Long getInstallmentNumber() {
		return installmentNumber;
	}
	public BigDecimal getInterestAmount() {
		return interestAmount;
	}
	
	
}
