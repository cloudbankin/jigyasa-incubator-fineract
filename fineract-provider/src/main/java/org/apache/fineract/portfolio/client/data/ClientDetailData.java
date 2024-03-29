package org.apache.fineract.portfolio.client.data;

import java.util.Date;

public class ClientDetailData {


	private final Long client_id;

	private final String clientExternal;

	private final int nomineeAge;

	private final String officeExtrenal;

	private final String client_name;

	private final String mobileNo;

	private final String gender;
	
	private final String maritalStatus;

	private final String groupExternal;

	public String getMaritalStatus() {
		return maritalStatus;
	}

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
	
	
	public ClientDetailData(String clientExternal2, long client_id2, String officeExtrenal2, String client_name2,
			String mobileNo2, String gender2,String maritalStatus, String groupExternal2, String nomineeName2, int nomineeAge2,
			int nomineeRelation2, String bankName2, String bankAccount2, String accHolderName2, String voterId2,
			String aadhaarId2, String pancardId2, Date activateDate2, Date dob2,int age, String occupation2, long monthlyincome2, long monthlyexpenses2, String religion2, String caste2) {
		
	this.aadhaarId=aadhaarId2;
	this.accHolderName=accHolderName2;
	this.activateDate=activateDate2;
	this.bankAccount=bankAccount2;
	this.bankName=bankName2;
	this.client_id=client_id2;
	this.client_name=client_name2;
	this.clientExternal=clientExternal2;
	this.dob=dob2;
	this.gender=gender2;
	this.maritalStatus = maritalStatus;
	this.groupExternal=groupExternal2;
	this.mobileNo=mobileNo2;
	this.nomineeAge=nomineeAge2;
	this.nomineeName=nomineeName2;
	this.nomineeRelation=nomineeRelation2;
	this.officeExtrenal=officeExtrenal2;
	this.pancardId=pancardId2;
	this.voterId=voterId2;
    this.age=age;
    
    this.occupation = occupation2;
	this.monthlyincome = monthlyincome2;
	this.monthlyexpenses = monthlyexpenses2;
	this.religion = religion2;
    this.caste = caste2;
    
	}
	public int getAge() {
		return age;
	}
	public Long getClient_id() {
		return client_id;
	}

	public String getClientExternal() {
		return clientExternal;
	}

	public int getNomineeAge() {
		return nomineeAge;
	}

	public String getOfficeExtrenal() {
		return officeExtrenal;
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

	public String getGroupExternal() {
		return groupExternal;
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
	
}
