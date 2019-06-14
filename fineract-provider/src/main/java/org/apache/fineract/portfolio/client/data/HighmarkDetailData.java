package org.apache.fineract.portfolio.client.data;

import java.math.BigDecimal;
import java.util.Date;

public class HighmarkDetailData {
	
	private final long clientID;

	private final String loanAcc;

	private final String officeExtrenal;

	private final String loanType;
	
	private final long loanCycle;

	private final String loanExternal;

	private final String loanPurpose;

	private final String groupExternal;

	private final Date submittedDate;

	private final Date disbursedDate;

	private Date closedDate;
	
	private Date transactionDate;

	private final BigDecimal appliedAmount;

	private final BigDecimal approvedAmount;

	private final BigDecimal disbAmount;

	private final int installmentNumber;

	private final String termfrequency;
	
	private final BigDecimal outBalance;
	
	private final String clientExternal;

	private final BigDecimal installmentAmount;
	private final BigDecimal overDueAmount;
	private final int daysOverDUe;
	
	private final long writeoffAmount;
	private final String writeoffReason;
	private Date writeoffDate;
	
	
	private final int nomineeAge;
	private final String client_name;
	private final String mobileNo;
	private final String gender;
	private final String maritalStatus;
	private final String nomineeName;
	private final int nomineeRelation;
	private final String bankName;
	private final String bankAccount;
	private final String accHolderName;
	private final String voterId;
	private final String aadhaarId;
	private final String pancardId;
	private final Date activateDate;	
	private final Date dob;
	private int  age;	
	private final String occupation;
	private final long monthlyincome;
	private final long monthlyexpenses;
	private final String religion;
	private final String caste;
	

	private final String addressType;
	private final Long addressId;
	private final String street;
	private final String addressLine1;
	private final String addressLine2;
	private final String addressLine3;
	private final String townVillage;
	private final String city;
	private final String countyDistrict;
	private final String countryName;
	private final String stateName;
	private final String postalCode;
	private final String loanOfficer;


	public HighmarkDetailData(long clientID, String loanAcc, String officeExtrenal, String loanType, String loanExternal,
			String loanPurpose, String groupExternal, Date submittedDate, Date disbursedDate, Date closedDate,
			BigDecimal appliedAmount, BigDecimal approvedAmount, BigDecimal disbAmount, int installmentNumber,
			String termfrequency, BigDecimal outBalance, String clientExternal, BigDecimal installmentAmount,
			BigDecimal overDueAmount, int daysOverDUe, long loanCycle, Date transactionDate,
			long writeoffAmount, String writeoffReason, Date writeoffDate,int nomineeAge,String client_name,
			String mobileNo,String gender,String maritalStatus,String nomineeName,int nomineeRelation,
			String bankName,String bankAccount,String accHolderName,String voterId,String aadhaarId,
			String pancardId,Date activateDate,Date dob,int age,String occupation,long monthlyincome,
			long monthlyexpenses,String religion,String caste,String addressType,Long addressId,
			String street,String addressLine1,String addressLine2,String addressLine3,String townVillage,
			String city,String countyDistrict,String countryName,String stateName,String postalCode,String loanOfficer) {
		
		this.clientID = clientID;
		this.appliedAmount=appliedAmount;
		this.approvedAmount=approvedAmount;
		this.clientExternal=clientExternal;
		this.closedDate=closedDate;
		this.disbAmount=disbAmount;
		this.disbursedDate=disbursedDate;
		this.groupExternal=groupExternal;
		this.installmentAmount=installmentAmount;
		this.installmentNumber=installmentNumber;
		this.loanAcc=loanAcc;
		this.loanExternal=loanExternal;
		this.loanPurpose=loanPurpose;
		this.loanType=loanType;
		this.officeExtrenal=officeExtrenal;
		this.outBalance=outBalance;
		this.submittedDate=submittedDate;
		this.termfrequency=termfrequency;
		this.daysOverDUe=daysOverDUe;	
	    this.overDueAmount=overDueAmount;
	    this.loanCycle=loanCycle;
	    this.transactionDate = transactionDate;
	    
	    this.writeoffAmount=writeoffAmount;
	    this.writeoffReason=writeoffReason;
	    this.writeoffDate = writeoffDate;
	    
	    this.nomineeAge=nomineeAge;
	    this.client_name=client_name;
	    this.mobileNo=mobileNo;
	    this.gender=gender;
	    this.maritalStatus=maritalStatus;
	    this.nomineeName=nomineeName;
	    this.nomineeRelation=nomineeRelation;
	    this.bankName=bankName;
	    this.bankAccount=bankAccount;
	    this.accHolderName=accHolderName;
	    this.voterId=voterId;
	    this.aadhaarId=aadhaarId;
	    this.pancardId=pancardId;
	    this.activateDate=activateDate;
	    this.dob=dob;
	    this.age=age;
	    this.occupation=occupation;
	    this.monthlyincome=monthlyincome;
	    this.monthlyexpenses=monthlyexpenses;
	    this.religion=religion;
	    this.caste=caste;
	    this.addressType=addressType;
	    this.addressId=addressId;
	    this.street=street;
	    this.addressLine1=addressLine1;
	    this.addressLine2=addressLine2;
	    this.addressLine3=addressLine3;
	    this.townVillage=townVillage;
	    this.city=city;
	    this.countyDistrict=countyDistrict;
	    this.countryName=countryName;
	    this.postalCode=postalCode;
	    this.stateName=stateName;
	    this.loanOfficer=loanOfficer;
	    
	}

	public BigDecimal getOverDueAmount() {
		return overDueAmount;
	}

	public int getDaysOverDUe() {
		return daysOverDUe;
	}

	public String getLoanAcc() {
		return loanAcc;
	}

	public String getOfficeExtrenal() {
		return officeExtrenal;
	}

	public String getLoanType() {
		return loanType;
	}

	public String getLoanExternal() {
		return loanExternal;
	}

	public String getLoanPurpose() {
		return loanPurpose;
	}

	public String getGroupExternal() {
		return groupExternal;
	}

	public Date getSubmittedDate() {
		return submittedDate;
	}

	public Date getDisbursedDate() {
		return disbursedDate;
	}

	public Date getClosedDate() {
		return closedDate;
	}

	public Date setClosedDate(Date closedDate) {
		return this.closedDate=closedDate;
	}
	
	public BigDecimal getAppliedAmount() {
		return appliedAmount;
	}

	public BigDecimal getApprovedAmount() {
		return approvedAmount;
	}

	public BigDecimal getDisbAmount() {
		return disbAmount;
	}

	public int getInstallmentNumber() {
		return installmentNumber;
	}

	public String getTermfrequency() {
		return termfrequency;
	}

	public BigDecimal getOutBalance() {
		return outBalance;
	}

	public String getClientExternal() {
		return clientExternal;
	}

	public BigDecimal getInstallmentAmount() {
		return installmentAmount;
	}

	public long getClientID() {
		return clientID;
	}

	public long getLoanCycle() {
		return loanCycle;
	}

	public Date getTransactionDate() {
		return transactionDate;
	}

	public void setTransactionDate(Date transactionDate) {
		this.transactionDate = transactionDate;
	}

	public Date getWriteoffDate() {
		return writeoffDate;
	}

	public void setWriteoffDate(Date writeoffDate) {
		this.writeoffDate = writeoffDate;
	}

	public long getWriteoffAmount() {
		return writeoffAmount;
	}

	public String getWriteoffReason() {
		return writeoffReason;
	}

	public int getAge() {
		return age;
	}

	public void setAge(int age) {
		this.age = age;
	}

	public int getNomineeAge() {
		return nomineeAge;
	}

	public String getClient_name() {
		return client_name;
	}

	public String getMobileNo() {
		return mobileNo;
	}

	public String getGender() {
		return gender;
	}

	public String getMaritalStatus() {
		return maritalStatus;
	}

	public String getNomineeName() {
		return nomineeName;
	}

	public int getNomineeRelation() {
		return nomineeRelation;
	}

	public String getBankName() {
		return bankName;
	}

	public String getBankAccount() {
		return bankAccount;
	}

	public String getAccHolderName() {
		return accHolderName;
	}

	public String getVoterId() {
		return voterId;
	}

	public String getAadhaarId() {
		return aadhaarId;
	}

	public String getPancardId() {
		return pancardId;
	}

	public Date getActivateDate() {
		return activateDate;
	}

	public Date getDob() {
		return dob;
	}

	public String getOccupation() {
		return occupation;
	}

	public long getMonthlyincome() {
		return monthlyincome;
	}

	public long getMonthlyexpenses() {
		return monthlyexpenses;
	}

	public String getReligion() {
		return religion;
	}

	public String getCaste() {
		return caste;
	}

	public String getAddressType() {
		return addressType;
	}

	public Long getAddressId() {
		return addressId;
	}

	public String getStreet() {
		return street;
	}

	public String getAddressLine1() {
		return addressLine1;
	}

	public String getAddressLine2() {
		return addressLine2;
	}

	public String getAddressLine3() {
		return addressLine3;
	}

	public String getTownVillage() {
		return townVillage;
	}

	public String getCity() {
		return city;
	}

	public String getCountyDistrict() {
		return countyDistrict;
	}

	public String getCountryName() {
		return countryName;
	}

	public String getStateName() {
		return stateName;
	}

	public String getPostalCode() {
		return postalCode;
	}

	public String getLoanOfficer() {
		return loanOfficer;
	}
	
	

	
}
