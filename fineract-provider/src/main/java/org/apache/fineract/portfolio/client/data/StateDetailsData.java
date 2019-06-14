package org.apache.fineract.portfolio.client.data;

public class StateDetailsData {

	private Long id;
	private String stateCode;
	private String stateName;
	
	
	public StateDetailsData(Long id, String stateCode, String stateName) {
		this.id = id;
		this.stateCode = stateCode;
		this.stateName = stateName;
	}

	public static StateDetailsData stateList(Long id, String stateCode, String stateName) {
		return new StateDetailsData(id,stateCode,stateName);
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getStateCode() {
		return stateCode;
	}

	public void setStateCode(String stateCode) {
		this.stateCode = stateCode;
	}

	public String getStateName() {
		return stateName;
	}

	public void setStateName(String stateName) {
		this.stateName = stateName;
	}

}
