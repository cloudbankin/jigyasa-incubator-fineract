package org.apache.fineract.portfolio.loanaccount.api;


public class InstallmentsData {

	private long org_loan;
	private long ins_amt;
	
	public InstallmentsData(long org_loan, long ins_amt) {
		super();
		this.org_loan = org_loan;
		this.ins_amt = ins_amt;
	}
	
	public long getOrg_loan() {
		return org_loan;
	}
	public void setOrg_loan(long org_loan) {
		this.org_loan = org_loan;
	}
	public long getIns_amt() {
		return ins_amt;
	}
	public void setIns_amt(long ins_amt) {
		this.ins_amt = ins_amt;
	}
	
	

}
