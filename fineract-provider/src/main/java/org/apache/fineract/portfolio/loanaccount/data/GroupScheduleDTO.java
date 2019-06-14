package org.apache.fineract.portfolio.loanaccount.data;

public class GroupScheduleDTO {
	final String loName;
	final String branchName;
	final String branchExternal;
	final String groupName;
	final String groupExternal;
	final String meeting_time;
	
	
	public GroupScheduleDTO(final String loName,final String branchName,final String branchExternal,final String groupName,final String groupExternal,final String meeting_time){
		this.loName=loName;
		this.branchName = branchName;
		this.branchExternal = branchExternal;
		this.groupName = groupName;
		this.groupExternal = groupExternal;	
		this.meeting_time = meeting_time;;
	}
	
	public String getMeeting_time() {
		return meeting_time;
	}

	public String getLoName() {
		return loName;
	}
	
	public String getBranchName() {
		return branchName;
	}
	
	public String getBranchExternal() {
		return branchExternal;
	}
	
	public String getGroupName() {
		return groupName;
	}
	
	public String getGroupExternal() {
		return groupExternal;
	}

}
