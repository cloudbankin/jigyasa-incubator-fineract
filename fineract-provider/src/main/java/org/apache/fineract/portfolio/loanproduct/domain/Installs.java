package org.apache.fineract.portfolio.loanproduct.domain;


import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.*;

import org.apache.fineract.infrastructure.core.domain.AbstractPersistableCustom;
import org.apache.fineract.portfolio.fund.domain.Fund;



@Entity
@Table(name = "m_loan_ins_data")
public class Installs extends AbstractPersistableCustom<Long> {

	
    @ManyToOne
    @JoinColumn(name = "loan_product_id", nullable = false)
    private LoanProduct loanProduct;
    
	
	@Column(name = "original_loan", length = 100, unique = true)
	private long org_loan;
	
	@Column(name = "loan_installment", length = 10)
	private long ins_amt;
	
	
	public Installs(long ins_loan, long ins_amt) {
		this.org_loan = ins_loan;
		this.ins_amt = ins_amt;
	}
	
	public long getIns_loan() {
		return org_loan;
	}
	public void setIns_loan(long ins_loan) {
		this.org_loan = ins_loan;
	}
	public long getIns_amt() {
		return ins_amt;
	}
	public void setIns_amt(long ins_amt) {
		this.ins_amt = ins_amt;
	}

	public LoanProduct getLoanProduct() {
		return loanProduct;
	}

	public void setLoanProduct(LoanProduct loanProduct) {
		this.loanProduct = loanProduct;
	}

	
}