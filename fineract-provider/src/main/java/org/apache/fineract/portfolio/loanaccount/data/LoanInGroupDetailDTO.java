package org.apache.fineract.portfolio.loanaccount.data;

import java.math.BigDecimal;
import java.util.Date;

public class LoanInGroupDetailDTO {
	final Long id;
	final String loanExternal;
	final String loanPurpose;
	final String clientName;
	final String loanProduct;
	final long loanCycle;
	final Date loanDate;
	final String clientExternal;
	final String spouseName;
	final BigDecimal amount;
	final Long installment;
	final BigDecimal firstAmount;
	final BigDecimal lastAmount;
	final BigDecimal interestRate;
	
	public LoanInGroupDetailDTO(final Long id,final String loanProduct,final Date loanDate,final String loanExternal,final String loanPurpose,final String clientName,final String clientExternal,final String spouseName,final BigDecimal amount,final Long installment,final BigDecimal firstAmount,final BigDecimal lastAmount,final BigDecimal interestRate, final long loanCycle){
		this.id = id;
		this.loanExternal = loanExternal;
		this.loanPurpose = loanPurpose;
		this.clientName = clientName;
		this.loanDate = loanDate;
		this.loanProduct= loanProduct;
		this.clientExternal = clientExternal;
		this.spouseName = spouseName;
		this.amount = amount;
		this.firstAmount = firstAmount;
		this.lastAmount = lastAmount;
		this.installment = installment;
		this.interestRate = interestRate;
		this.loanCycle = loanCycle;
	}
	
	public Long getId() {
		return id;
	}
	public String getLoanProduct() {
		return loanProduct;
	}

	public Date getLoanDate() {
		return loanDate;
	}

	public String getLoanExternal() {
		return loanExternal;
	}
	public String getLoanPurpose() {
		return loanPurpose;
	}
	public String getClientName() {
		return clientName;
	}
	public String getClientExternal() {
		return clientExternal;
	}
	public String getSpouseName() {
		return spouseName;
	}
	public BigDecimal getAmount() {
		return amount;
	}
	public Long getInstallment() {
		return installment;
	}
	public BigDecimal getFirstAmount() {
		return firstAmount;
	}
	public BigDecimal getLastAmount() {
		return lastAmount;
	}
	public BigDecimal getInterestRate() {
		return interestRate;
	}

	public long getLoanCycle() {
		return loanCycle;
	}


}
