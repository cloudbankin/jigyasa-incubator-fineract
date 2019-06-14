/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.fineract.portfolio.client.service;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.apache.fineract.infrastructure.codes.data.CodeValueData;
import org.apache.fineract.infrastructure.codes.service.CodeValueReadPlatformService;
import org.apache.fineract.infrastructure.configuration.data.GlobalConfigurationPropertyData;
import org.apache.fineract.infrastructure.configuration.service.ConfigurationReadPlatformService;
import org.apache.fineract.infrastructure.core.api.ApiParameterHelper;
import org.apache.fineract.infrastructure.core.data.EnumOptionData;
import org.apache.fineract.infrastructure.core.domain.JdbcSupport;
import org.apache.fineract.infrastructure.core.service.Page;
import org.apache.fineract.infrastructure.core.service.PaginationHelper;
import org.apache.fineract.infrastructure.core.service.RoutingDataSource;
import org.apache.fineract.infrastructure.core.service.SearchParameters;
import org.apache.fineract.infrastructure.dataqueries.data.DatatableData;
import org.apache.fineract.infrastructure.dataqueries.data.EntityTables;
import org.apache.fineract.infrastructure.dataqueries.data.StatusEnum;
import org.apache.fineract.infrastructure.dataqueries.service.EntityDatatableChecksReadService;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.infrastructure.security.utils.ColumnValidator;
import org.apache.fineract.organisation.office.data.OfficeData;
import org.apache.fineract.organisation.office.service.OfficeReadPlatformService;
import org.apache.fineract.organisation.staff.data.StaffData;
import org.apache.fineract.organisation.staff.service.StaffReadPlatformService;
import org.apache.fineract.portfolio.address.data.AddressData;
import org.apache.fineract.portfolio.address.service.AddressReadPlatformService;
import org.apache.fineract.portfolio.client.api.ClientApiConstants;
import org.apache.fineract.portfolio.client.data.ClientAddressData;
import org.apache.fineract.portfolio.client.data.ClientData;
import org.apache.fineract.portfolio.client.data.ClientDetailData;
import org.apache.fineract.portfolio.client.data.ClientNonPersonData;
import org.apache.fineract.portfolio.client.data.ClientTimelineData;
import org.apache.fineract.portfolio.client.data.FamilyDetailData;
import org.apache.fineract.portfolio.client.data.HighmarkDetailData;
import org.apache.fineract.portfolio.client.data.LoanDetailData;
import org.apache.fineract.portfolio.client.data.StateDetailsData;
import org.apache.fineract.portfolio.client.domain.ClientEnumerations;
import org.apache.fineract.portfolio.client.domain.ClientStatus;
import org.apache.fineract.portfolio.client.domain.LegalForm;
import org.apache.fineract.portfolio.client.exception.ClientNotFoundException;
import org.apache.fineract.portfolio.group.data.GroupGeneralData;
import org.apache.fineract.portfolio.savings.data.SavingsProductData;
import org.apache.fineract.portfolio.savings.service.SavingsProductReadPlatformService;
import org.apache.fineract.useradministration.domain.AppUser;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.joda.time.LocalDate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Service
public class ClientReadPlatformServiceImpl implements ClientReadPlatformService {

	private final JdbcTemplate jdbcTemplate;
	private final PlatformSecurityContext context;
	private final OfficeReadPlatformService officeReadPlatformService;
	private final StaffReadPlatformService staffReadPlatformService;
	private final CodeValueReadPlatformService codeValueReadPlatformService;
	private final SavingsProductReadPlatformService savingsProductReadPlatformService;
	// data mappers
	private final PaginationHelper<ClientData> paginationHelper = new PaginationHelper<>();
	private final ClientMapper clientMapper = new ClientMapper();
	private final ClientLookupMapper lookupMapper = new ClientLookupMapper();
	private final ClientMembersOfGroupMapper membersOfGroupMapper = new ClientMembersOfGroupMapper();
	private final ParentGroupsMapper clientGroupsMapper = new ParentGroupsMapper();

	private final AddressReadPlatformService addressReadPlatformService;
	private final ConfigurationReadPlatformService configurationReadPlatformService;
	private final EntityDatatableChecksReadService entityDatatableChecksReadService;
	private final ColumnValidator columnValidator;

	@Autowired
	public ClientReadPlatformServiceImpl(final PlatformSecurityContext context, final RoutingDataSource dataSource,
			final OfficeReadPlatformService officeReadPlatformService,
			final StaffReadPlatformService staffReadPlatformService,
			final CodeValueReadPlatformService codeValueReadPlatformService,
			final SavingsProductReadPlatformService savingsProductReadPlatformService,
			final AddressReadPlatformService addressReadPlatformService,
			final ConfigurationReadPlatformService configurationReadPlatformService,
			final EntityDatatableChecksReadService entityDatatableChecksReadService,
			final ColumnValidator columnValidator) {
		this.context = context;
		this.officeReadPlatformService = officeReadPlatformService;
		this.jdbcTemplate = new JdbcTemplate(dataSource);
		this.staffReadPlatformService = staffReadPlatformService;
		this.codeValueReadPlatformService = codeValueReadPlatformService;
		this.savingsProductReadPlatformService = savingsProductReadPlatformService;
		this.addressReadPlatformService = addressReadPlatformService;
		this.configurationReadPlatformService = configurationReadPlatformService;
		this.entityDatatableChecksReadService = entityDatatableChecksReadService;
		this.columnValidator = columnValidator;
	}

	@Override
	public ClientData retrieveTemplate(final Long officeId, final boolean staffInSelectedOfficeOnly) {
		this.context.authenticatedUser();

		final Long defaultOfficeId = defaultToUsersOfficeIfNull(officeId);
		AddressData address = null;

		final Collection<OfficeData> offices = this.officeReadPlatformService.retrieveAllOfficesForDropdown();

		final Collection<SavingsProductData> savingsProductDatas = this.savingsProductReadPlatformService
				.retrieveAllForLookupByType(null);

		final GlobalConfigurationPropertyData configuration = this.configurationReadPlatformService
				.retrieveGlobalConfiguration("Enable-Address");

		final Boolean isAddressEnabled = configuration.isEnabled();
		if (isAddressEnabled) {
			address = this.addressReadPlatformService.retrieveTemplate();
		}

		Collection<StaffData> staffOptions = null;

		final boolean loanOfficersOnly = false;
		if (staffInSelectedOfficeOnly) {
			staffOptions = this.staffReadPlatformService.retrieveAllStaffForDropdown(defaultOfficeId);
		} else {
			staffOptions = this.staffReadPlatformService
					.retrieveAllStaffInOfficeAndItsParentOfficeHierarchy(defaultOfficeId, loanOfficersOnly);
		}
		if (CollectionUtils.isEmpty(staffOptions)) {
			staffOptions = null;
		}
		final List<CodeValueData> genderOptions = new ArrayList<>(
				this.codeValueReadPlatformService.retrieveCodeValuesByCode(ClientApiConstants.GENDER));

		final List<CodeValueData> clientTypeOptions = new ArrayList<>(
				this.codeValueReadPlatformService.retrieveCodeValuesByCode(ClientApiConstants.CLIENT_TYPE));

		final List<CodeValueData> clientClassificationOptions = new ArrayList<>(
				this.codeValueReadPlatformService.retrieveCodeValuesByCode(ClientApiConstants.CLIENT_CLASSIFICATION));

		final List<CodeValueData> clientNonPersonConstitutionOptions = new ArrayList<>(this.codeValueReadPlatformService
				.retrieveCodeValuesByCode(ClientApiConstants.CLIENT_NON_PERSON_CONSTITUTION));

		final List<CodeValueData> clientNonPersonMainBusinessLineOptions = new ArrayList<>(
				this.codeValueReadPlatformService
						.retrieveCodeValuesByCode(ClientApiConstants.CLIENT_NON_PERSON_MAIN_BUSINESS_LINE));

		final List<EnumOptionData> clientLegalFormOptions = ClientEnumerations.legalForm(LegalForm.values());

		final List<DatatableData> datatableTemplates = this.entityDatatableChecksReadService
				.retrieveTemplates(StatusEnum.CREATE.getCode().longValue(), EntityTables.CLIENT.getName(), null);

		return ClientData.template(defaultOfficeId, new LocalDate(), offices, staffOptions, null, genderOptions,
				savingsProductDatas, clientTypeOptions, clientClassificationOptions, clientNonPersonConstitutionOptions,
				clientNonPersonMainBusinessLineOptions, clientLegalFormOptions, address, isAddressEnabled,
				datatableTemplates);
	}

	// high Mark
	@Override
	public Workbook retrievenewHighMarkData(String asOnDate, String closedFrom) {
		SimpleDateFormat sdf3 = new SimpleDateFormat("EEE MMM dd yyyy HH:mm:ss zzz ", Locale.ENGLISH);
		Date asDate = null;
		Date fromDate = null;
		try {
			Date tempDate = sdf3.parse(asOnDate);
			SimpleDateFormat format1 = new SimpleDateFormat("yyyy-MM-dd");
			String date1 = format1.format(tempDate);
			asDate = format1.parse(date1);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			Date tempDate = sdf3.parse(closedFrom);
			SimpleDateFormat format1 = new SimpleDateFormat("yyyy-MM-dd");
			String date1 = format1.format(tempDate);
			fromDate = format1.parse(date1);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		final AppUser currentUser = this.context.authenticatedUser();
		final String hierarchy = currentUser.getOffice().getHierarchy();
		final AddressDetailsMapper rm = new AddressDetailsMapper();
		final List<ClientAddressData> addressDetails = this.jdbcTemplate.query(rm.schema(), rm, new Object[] {});
		final ClientDetailsMapper cm = new ClientDetailsMapper();
		final List<ClientDetailData> clientDetails = this.jdbcTemplate.query(cm.schema(), cm, new Object[] {});
		final LoansDetailsMapper lm = new LoansDetailsMapper();
		final List<LoanDetailData> loanDetails = this.jdbcTemplate.query(lm.schema(), lm, new Object[] { asDate, asDate,
				asDate, asDate, asDate, asDate, fromDate, asDate, asDate, asDate, fromDate, asDate });

		Workbook workbook = new HSSFWorkbook();
		CreationHelper creationHelper = workbook.getCreationHelper();
		CellStyle style = workbook.createCellStyle(); // Create new style
		style.setWrapText(true); // Set wordwrap
		CellStyle style1 = workbook.createCellStyle();
		style1.setDataFormat(creationHelper.createDataFormat().getFormat("ddMMyyyy"));

		Sheet accountSheet = workbook.createSheet("Account Info");
		Sheet clientSheet = workbook.createSheet("Member Info");
		Sheet addressSheet = workbook.createSheet("Address Info");

		Row headerLoan = accountSheet.createRow(0);
		headerLoan.setRowStyle(style);
		headerLoan.createCell(0).setCellValue("BANK_ID");
		headerLoan.createCell(1).setCellValue("SEGMENT_IDENTIFIER");
		headerLoan.createCell(2).setCellValue("HM_UNIQ_RFR_NBR");
		headerLoan.createCell(3).setCellValue("ACCOUNT_NUMBER");
		headerLoan.createCell(4).setCellValue("BRANCH_IDENTIFIER");
		headerLoan.createCell(5).setCellValue("KENDRA_CENTRE_IDENTIFIER");
		headerLoan.createCell(6).setCellValue("LOAN_OFFICER_ORIG_LOAN");
		headerLoan.createCell(7).setCellValue("DATE_OF_ACCOUNT_INFORMATION");
		headerLoan.createCell(8).setCellValue("LOAN_CATEGORY");
		headerLoan.createCell(9).setCellValue("GROUP_ID");
		headerLoan.createCell(10).setCellValue("LOAN_CYCLE_ID");
		headerLoan.createCell(11).setCellValue("LOAN_PURPOSE");
		headerLoan.createCell(12).setCellValue("ACCOUNT_STATUS");
		headerLoan.createCell(13).setCellValue("APPLICATION_DATE");
		headerLoan.createCell(14).setCellValue("SANCTIONED_DATE");
		headerLoan.createCell(15).setCellValue("DATE_OPENED");
		headerLoan.createCell(16).setCellValue("DATE_CLOSED");
		headerLoan.createCell(17).setCellValue("DATE_OF_LAST_PAYMENT");
		headerLoan.createCell(18).setCellValue("APPLIED_FOR_AMOUNT");
		headerLoan.createCell(19).setCellValue("LOAN_AMOUNT_SANCTIONED");
		headerLoan.createCell(20).setCellValue("TOTAL_AMOUNT_DISBURSED");
		headerLoan.createCell(21).setCellValue("NUMBER_OF_INSTALLMENTS");
		headerLoan.createCell(22).setCellValue("REPAYMENT_FREQUENCY");
		headerLoan.createCell(23).setCellValue("MINIMUM_AMT_DUE");
		headerLoan.createCell(24).setCellValue("CURRENT_BALANCE");
		headerLoan.createCell(25).setCellValue("AMOUNT_OVERDUE");
		headerLoan.createCell(26).setCellValue("DPD");
		headerLoan.createCell(27).setCellValue("WRITE_OFF");
		headerLoan.createCell(28).setCellValue("DATE_WRITE_OFF");
		headerLoan.createCell(29).setCellValue("WRITE_OFF_REASON");
		headerLoan.createCell(30).setCellValue("NO_OF_MEETING_HELD");
		headerLoan.createCell(31).setCellValue("NO_OF_ABSENTEES_IN_MEETING");
		headerLoan.createCell(32).setCellValue("INSURANCE_INDICATOR");
		headerLoan.createCell(33).setCellValue("TYPE_OF_INSURANCE");
		headerLoan.createCell(34).setCellValue("SUM_ASSURED");
		headerLoan.createCell(35).setCellValue("AGREED_MEETING_WEEK_DAY");
		headerLoan.createCell(36).setCellValue("AGREED_MEETING_DAY_TIME");
		headerLoan.createCell(37).setCellValue("RESERVED_FOR_FUTURE_USE");
		headerLoan.createCell(38).setCellValue("OLD_MEMBER_CODE");
		headerLoan.createCell(39).setCellValue("OLD_MEMBER_SHRT_NM");
		headerLoan.createCell(40).setCellValue("OLD_ACCOUNT_NBR");
		headerLoan.createCell(41).setCellValue("CIBIL_ACT_STATUS");
		headerLoan.createCell(42).setCellValue("ASSET_CLASIFICATION");
		headerLoan.createCell(43).setCellValue("MEMBER_CODE");
		headerLoan.createCell(44).setCellValue("MEMBER_SHRT_NM");
		headerLoan.createCell(45).setCellValue("ACCOUNT_TYPE");
		headerLoan.createCell(46).setCellValue("OWNERSHIP_IND");
		headerLoan.createCell(47).setCellValue("PARENT_ID");
		headerLoan.createCell(48).setCellValue("EXTRACTION_FILE_ID");
		headerLoan.createCell(49).setCellValue("SEVERITY");

		List<StateDetailsData> stateDetails = retrieveStateDetails();
		Map<String, String> stateCodeAndName = new HashMap<String, String>();
		for (StateDetailsData state : stateDetails) {
			stateCodeAndName.put(state.getStateName(), state.getStateCode());
		}

		for (int k = 0; k < loanDetails.size(); k++) {

			LoanDetailData tempLoan = loanDetails.get(k);
			Row loanRow = accountSheet.createRow(k + 1);
			try {
				loanRow.setRowStyle(style);
				
				loanRow.createCell(0).setCellValue(tempLoan.getLoanAcc());
				loanRow.createCell(1).setCellValue("ACTCRD");
				loanRow.createCell(2).setCellValue(tempLoan.getLoanAcc());
				loanRow.createCell(3).setCellValue(tempLoan.getLoanAcc());
				loanRow.createCell(4).setCellValue(tempLoan.getOfficeExtrenal());
				loanRow.createCell(5).setCellValue(tempLoan.getOfficeExtrenal());
//        	DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
//            String strDate = dateFormat.format(new Date());
				loanRow.createCell(6).setCellValue("Virendar Singh");

				Cell dateCell1 = loanRow.createCell(7);
				dateCell1.setCellValue(asDate);
				dateCell1.setCellStyle(style1);

				if (tempLoan.getLoanType() != null) {
					if (tempLoan.getLoanType().equals("GROUP")) {
						loanRow.createCell(8).setCellValue("T01");
					} else if (tempLoan.getLoanType().equals("JLG")) {
						loanRow.createCell(8).setCellValue("T02");
					} else {
						loanRow.createCell(8).setCellValue("T03");
					}
				}

				if (tempLoan.getGroupExternal() != null) {
					loanRow.createCell(9).setCellValue(tempLoan.getGroupExternal());
				} else {
					loanRow.createCell(9).setCellValue(tempLoan.getClientExternal());
				}
				String str = tempLoan.getLoanExternal();
				/*
				 * if(str!=null && !str.isEmpty()) {
				 * loanRow.createCell(10).setCellValue(str.substring(Math.max(str.length() - 2,
				 * 0))); } else { loanRow.createCell(10).setCellValue(tempLoan.getLoanCycle());
				 * }
				 */
//        	DateFormat dateFormat1 = new SimpleDateFormat("yyyy-MM-dd");
//            String strDate1= dateFormat1.format(tempLoan.getSubmittedDate());

				loanRow.createCell(10).setCellValue(tempLoan.getLoanCycle());

				if (tempLoan.getClosedDate() != null) {
					Date cdate = tempLoan.getClosedDate();
					if (cdate.after(asDate)) {
						cdate = tempLoan.setClosedDate(null);
					}

				}

				if (tempLoan.getOverDueAmount() != null && tempLoan.getOverDueAmount().floatValue() > 0) {
					loanRow.createCell(12).setCellValue("S05");
				} else {
					if (tempLoan.getClosedDate() != null) {
						loanRow.createCell(12).setCellValue("S07");
					} else {
						loanRow.createCell(12).setCellValue("S04");
					}
				}
				Cell dateCell = loanRow.createCell(13);
				dateCell.setCellValue(tempLoan.getSubmittedDate());
				dateCell.setCellStyle(style1);
//        	DateFormat dateFormat2 = new SimpleDateFormat("yyyy-MM-dd");
//            String strDate2= dateFormat2.format(tempLoan.getDisbursedDate());
				Cell cell1 = loanRow.createCell(14);
				cell1.setCellValue(tempLoan.getDisbursedDate());
				cell1.setCellStyle(style1);
//        	DateFormat dateFormat3 = new SimpleDateFormat("yyyy-MM-dd");
//            String strDate3 = dateFormat3.format(tempLoan.getDisbursedDate());
				Cell cell = loanRow.createCell(15);
				cell.setCellValue(tempLoan.getDisbursedDate());
				cell.setCellStyle(style1);

				if (tempLoan.getTransactionDate() != null) {
					Cell Cell17 = loanRow.createCell(17);
					Cell17.setCellValue(tempLoan.getTransactionDate());
					Cell17.setCellStyle(style1);
				}

				if (tempLoan.getClosedDate() != null) {
//           	DateFormat dateFormat4 = new SimpleDateFormat("yyyy-MM-dd");
//            String strDate4 = dateFormat4.format(tempLoan.getClosedDate());

//        		DateTime CloseDate = new DateTime(tempLoan.getClosedDate());
//        	     DateTime currentDate = new DateTime();      	    
//        	     int diffInDays = Days.daysBetween(CloseDate, currentDate).getDays();
//        	     if(diffInDays < 30 )
//        	     {

					Cell cell16 = loanRow.createCell(16);
					cell16.setCellValue(tempLoan.getClosedDate());
					cell16.setCellStyle(style1);
//        	     }
				}
				loanRow.createCell(11).setCellValue(tempLoan.getLoanPurpose());
				loanRow.createCell(18).setCellValue(tempLoan.getAppliedAmount().floatValue());
				loanRow.createCell(19).setCellValue(tempLoan.getApprovedAmount().floatValue());
				loanRow.createCell(20).setCellValue(tempLoan.getDisbAmount().floatValue());
				loanRow.createCell(21).setCellValue(tempLoan.getInstallmentNumber());

				if (tempLoan.getTermfrequency().contains("Months")) {
					loanRow.createCell(22).setCellValue("F03");
				} else if (tempLoan.getTermfrequency().contains("Weeks")) {
					loanRow.createCell(22).setCellValue("F01");
				}
				loanRow.createCell(23).setCellValue(tempLoan.getInstallmentAmount().floatValue());
				loanRow.createCell(24).setCellValue(tempLoan.getOutBalance().floatValue());
				if (tempLoan.getOverDueAmount() != null)
					loanRow.createCell(25).setCellValue(tempLoan.getOverDueAmount().floatValue());
				else
					loanRow.createCell(25).setCellValue(0);
				loanRow.createCell(26).setCellValue(tempLoan.getDaysOverDUe());

				loanRow.createCell(27).setCellValue(tempLoan.getWriteoffAmount());
				if (tempLoan.getWriteoffAmount() != 0) {
					loanRow.createCell(28).setCellValue(tempLoan.getWriteoffDate());
					loanRow.createCell(29).setCellValue(tempLoan.getWriteoffReason());
				}

				loanRow.createCell(47).setCellValue(tempLoan.getClientExternal());

//        	edit

				List<ClientDetailData> client = clientDetails.stream()
						.filter(p -> p.getClient_id() == tempLoan.getClientID()).collect(Collectors.toList());

				Row clientRow = clientSheet.createRow(k + 1);
				clientRow.setRowStyle(style);

				if (client != null) {
					ClientDetailData clientobj = null;

					Iterator<ClientDetailData> iterator = client.iterator();
					while (iterator.hasNext()) {
						clientobj = iterator.next();
					}

					final FamilyDetailsMapper fm = new FamilyDetailsMapper();
					final List<FamilyDetailData> familyDetails = this.jdbcTemplate.query(fm.schema(), fm,
							new Object[] { clientobj.getClient_id() });
					if (familyDetails != null) {
						int familySize = familyDetails.size();
						if (familySize > 4)
							familySize = 4;
						int fdc = 17;
						if (familySize != 0) {
							for (int f = 0; f < familySize; f++) {
								FamilyDetailData fdetail = familyDetails.get(f);
								if (fdetail.getRelationType() == 79) {
									clientRow.createCell(15).setCellValue(fdetail.getName());
									clientRow.createCell(19).setCellValue(fdetail.getName());
									clientRow.createCell(21).setCellValue(fdetail.getName());
									clientRow.createCell(23).setCellValue(fdetail.getName());
									if (clientobj.getGender() != null && clientobj.getGender().contains("Female")) {
										clientRow.createCell(16).setCellValue("K02");
										
									} else if (clientobj.getGender() != null
											&& clientobj.getGender().contains("Male")) {
										clientRow.createCell(16).setCellValue("K06");
									}
								} else {
									clientRow.createCell(fdc).setCellValue(fdetail.getName());

									if (fdetail.getRelationType() == 113) {
										clientRow.createCell(fdc + 1).setCellValue("K04");
									} else if (fdetail.getRelationType() == 99) {
										clientRow.createCell(fdc + 1).setCellValue("K05");
									} else if (fdetail.getRelationType() == 100) {
										clientRow.createCell(fdc + 1).setCellValue("K07");
									} else if (fdetail.getRelationType() == 112) {
										clientRow.createCell(fdc + 1).setCellValue("K03");
									} else if (fdetail.getRelationType() == 77) {
										clientRow.createCell(fdc + 1).setCellValue("K01");
									} else {
										clientRow.createCell(fdc + 1).setCellValue("K15");
									}

									fdc = fdc + 2;
								}
							}
						}
					}
					clientRow.createCell(0).setCellValue(clientobj.getClientExternal());
					clientRow.createCell(1).setCellValue("CNSCRD");
					clientRow.createCell(2).setCellValue(clientobj.getClientExternal());
					clientRow.createCell(3).setCellValue(clientobj.getOfficeExtrenal());
					clientRow.createCell(4).setCellValue(clientobj.getOfficeExtrenal());
					if (clientobj.getGroupExternal() != null) {
						clientRow.createCell(5).setCellValue(clientobj.getGroupExternal());
					} else {
						clientRow.createCell(5).setCellValue(clientobj.getClientExternal());
					}
					clientRow.createCell(6).setCellValue(clientobj.getClient_name());
					if (clientobj.getDob() != null) {
//        	DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
//            String strDate = dateFormat.format(tempClient.getDob());
						Cell clientCell = clientRow.createCell(10);
						clientCell.setCellValue(clientobj.getDob());
						clientCell.setCellStyle(style1);
					}
//        	DateFormat dateFormat1 = new SimpleDateFormat("yyyy-MM-dd");
//            String strDate1 = dateFormat1.format(tempClient.getActivateDate());
					if (clientobj.getAge() != 0 && clientobj.getDob() != null) {
						Cell active = clientRow.createCell(12);
						active.setCellValue(clientobj.getActivateDate());
						active.setCellStyle(style1);
					}
					if (clientobj.getAge() != 0)
						clientRow.createCell(11).setCellValue(clientobj.getAge());
					if (clientobj.getGender() != null) {

						if (clientobj.getGender().equals("Female")) {
							clientRow.createCell(13).setCellValue("F");
						} else {
							clientRow.createCell(13).setCellValue("M");

						}

					}

					if (clientobj.getMaritalStatus() != null) {
						switch (clientobj.getMaritalStatus()) {
						case "Married":
							clientRow.createCell(14).setCellValue("M01");
							break;
						case "Unmarried":
							clientRow.createCell(14).setCellValue("M05");
							break;
						case "Deprived":
							clientRow.createCell(14).setCellValue("M06");
							break;
						case "Divorcee":
							clientRow.createCell(14).setCellValue("M03");
							break;
						case "Widow":
							clientRow.createCell(14).setCellValue("M04");
							break;

						}
					}

					clientRow.createCell(25).setCellValue(clientobj.getNomineeName());

					if (clientobj.getNomineeRelation() == 113) {
						clientRow.createCell(26).setCellValue("K04");
					} else if (clientobj.getNomineeRelation() == 99) {
						clientRow.createCell(26).setCellValue("K05");
					} else if (clientobj.getNomineeRelation() == 100) {
						clientRow.createCell(26).setCellValue("K07");
					} else if (clientobj.getNomineeRelation() == 112) {
						clientRow.createCell(26).setCellValue("K03");
					} else if (clientobj.getNomineeRelation() == 77) {
						clientRow.createCell(26).setCellValue("K01");
					} else {
						clientRow.createCell(26).setCellValue("K15");
					}

					if (clientobj.getNomineeAge() != 0)
						clientRow.createCell(27).setCellValue(clientobj.getNomineeAge());
					clientRow.createCell(28).setCellValue(clientobj.getVoterId());
					clientRow.createCell(29).setCellValue(clientobj.getAadhaarId());
					clientRow.createCell(30).setCellValue(clientobj.getPancardId());
					clientRow.createCell(39).setCellValue(clientobj.getMobileNo());
					clientRow.createCell(45).setCellValue(clientobj.getAccHolderName());
					clientRow.createCell(46).setCellValue(clientobj.getBankName());
					clientRow.createCell(47).setCellValue(clientobj.getBankAccount());

					clientRow.createCell(48).setCellValue(clientobj.getOccupation());
					if (clientobj.getOccupation() != null) {
						clientRow.createCell(49).setCellValue(clientobj.getMonthlyincome());
						clientRow.createCell(50).setCellValue(clientobj.getMonthlyexpenses());
					}

					if (clientobj.getReligion() != null) {
						switch (clientobj.getReligion()) {
						case "Hindu":
							clientRow.createCell(51).setCellValue("R01");
							break;
						case "Muslim":
							clientRow.createCell(51).setCellValue("R02");
							break;
						case "Christian":
							clientRow.createCell(51).setCellValue("R03");
							break;
						case "Sikh":
							clientRow.createCell(51).setCellValue("R04");
							break;
						case "Buddhist":
							clientRow.createCell(51).setCellValue("R05");
							break;
						case "Jain":
							clientRow.createCell(51).setCellValue("R06");
							break;
						case "Bahai":
							clientRow.createCell(51).setCellValue("R07");
							break;
						case "Others":
							clientRow.createCell(51).setCellValue("R08");
							break;
						case "Religion not stated":
							clientRow.createCell(51).setCellValue("R09");
							break;
						}
					}

					// clientRow.createCell(51).setCellValue(clientobj.getReligion());
					clientRow.createCell(52).setCellValue(clientobj.getCaste());

				}

//        	edit
				
				

				List<ClientAddressData> clientAdd = addressDetails.stream()
						.filter(p -> p.getClient_id() == tempLoan.getClientID()).collect(Collectors.toList());

				Row rowNext = addressSheet.createRow(k + 1);
				rowNext.setRowStyle(style);

				if (clientAdd != null) {
					ClientAddressData clientAddobj = null;

					Iterator<ClientAddressData> iterator = clientAdd.iterator();
					while (iterator.hasNext()) {
						clientAddobj = iterator.next();
					}

					if (clientAddobj != null) {
						rowNext.createCell(0).setCellValue(clientAddobj.getClientExternal());
						rowNext.createCell(1).setCellValue("ADRCRD");
						rowNext.createCell(2)
								.setCellValue(clientAddobj.getAddressLine1() + clientAddobj.getAddressLine2()
										+ clientAddobj.getAddressLine3() + clientAddobj.getStreet()
										+ clientAddobj.getTownVillage() + clientAddobj.getCity());
						String stateCodeValue = "";
						if (clientAddobj.getStateName() != null
								&& stateCodeAndName.containsKey(clientAddobj.getStateName())) {
							stateCodeValue = stateCodeAndName.get(clientAddobj.getStateName());
						}
						rowNext.createCell(3).setCellValue(stateCodeValue);
						rowNext.createCell(4).setCellValue(clientAddobj.getPostalCode());
						rowNext.createCell(5)
								.setCellValue(clientAddobj.getAddressLine1() + clientAddobj.getAddressLine2()
										+ clientAddobj.getAddressLine3() + clientAddobj.getStreet()
										+ clientAddobj.getTownVillage() + clientAddobj.getCity());
						rowNext.createCell(6).setCellValue(stateCodeValue);
						rowNext.createCell(7).setCellValue(clientAddobj.getPostalCode());
						rowNext.createCell(9).setCellValue(clientAddobj.getClientExternal());
					}
				}

			} catch (Exception e) {
				loanRow.createCell(6).setCellValue("Error Occured" + e.getMessage());
			}
		}
		accountSheet.autoSizeColumn(0);
		accountSheet.autoSizeColumn(1);
		accountSheet.autoSizeColumn(2);
		accountSheet.autoSizeColumn(3);
		accountSheet.autoSizeColumn(4);
		accountSheet.autoSizeColumn(5);
		accountSheet.autoSizeColumn(6);
		accountSheet.autoSizeColumn(7);
		accountSheet.autoSizeColumn(8);
		accountSheet.autoSizeColumn(9);
		accountSheet.autoSizeColumn(10);
		accountSheet.autoSizeColumn(11);
		accountSheet.autoSizeColumn(12);
		accountSheet.autoSizeColumn(13);
		accountSheet.autoSizeColumn(14);
		accountSheet.autoSizeColumn(15);
		accountSheet.autoSizeColumn(16);
		accountSheet.autoSizeColumn(17);
		accountSheet.autoSizeColumn(18);
		accountSheet.autoSizeColumn(19);
		accountSheet.autoSizeColumn(20);
		accountSheet.autoSizeColumn(21);
		accountSheet.autoSizeColumn(22);
		accountSheet.autoSizeColumn(23);
		accountSheet.autoSizeColumn(24);
		accountSheet.autoSizeColumn(25);
		accountSheet.autoSizeColumn(26);
		accountSheet.autoSizeColumn(27);
		accountSheet.autoSizeColumn(28);
		accountSheet.autoSizeColumn(29);
		accountSheet.autoSizeColumn(30);
		accountSheet.autoSizeColumn(31);
		accountSheet.autoSizeColumn(32);
		accountSheet.autoSizeColumn(33);
		accountSheet.autoSizeColumn(34);
		accountSheet.autoSizeColumn(35);
		accountSheet.autoSizeColumn(36);
		accountSheet.autoSizeColumn(37);
		accountSheet.autoSizeColumn(38);
		accountSheet.autoSizeColumn(39);
		accountSheet.autoSizeColumn(40);
		accountSheet.autoSizeColumn(41);
		accountSheet.autoSizeColumn(42);
		accountSheet.autoSizeColumn(43);
		accountSheet.autoSizeColumn(44);
		accountSheet.autoSizeColumn(45);
		accountSheet.autoSizeColumn(46);
		accountSheet.autoSizeColumn(47);
		accountSheet.autoSizeColumn(48);
		accountSheet.autoSizeColumn(49);

		Row headerClient = clientSheet.createRow(0);
		headerClient.setRowStyle(style);
		headerClient.createCell(0).setCellValue("BANK_ID");
		headerClient.createCell(1).setCellValue("SEGMENT_IDENTIFIER");
		headerClient.createCell(2).setCellValue("MEMBER_ID");
		headerClient.createCell(3).setCellValue("BRANCH_IDENTIFIER");
		headerClient.createCell(4).setCellValue("KENDRA_CENTRE_IDENTIFIER");
		headerClient.createCell(5).setCellValue("GROUP_ID");
		headerClient.createCell(6).setCellValue("MEMBER_NAME_1");
		headerClient.createCell(7).setCellValue("MEMBER_NAME_2");
		headerClient.createCell(8).setCellValue("MEMBER_NAME_3");
		headerClient.createCell(9).setCellValue("ALTERNATE_NAME_OF_CONSUMER");
		headerClient.createCell(10).setCellValue("DATE_OF_BIRTH");
		headerClient.createCell(11).setCellValue("MEMBER_AGE");
		headerClient.createCell(12).setCellValue("MEMBER_AGE_AS_ON_DATE");
		headerClient.createCell(13).setCellValue("GENDER");
		headerClient.createCell(14).setCellValue("MARITAL_STATUS_TYPE");
		headerClient.createCell(15).setCellValue("KEY_PERSON_NAME");
		headerClient.createCell(16).setCellValue("KEY_PERSON_RELATIONSHIP");
		headerClient.createCell(17).setCellValue("MBR_REL_NM_1");
		headerClient.createCell(18).setCellValue("MBR_REL_TYP_1");
		headerClient.createCell(19).setCellValue("MBR_REL_NM_2");
		headerClient.createCell(20).setCellValue("MBR_REL_TYP_2");
		headerClient.createCell(21).setCellValue("MBR_REL_NM_3");
		headerClient.createCell(22).setCellValue("MBR_REL_TYP_3");
		headerClient.createCell(23).setCellValue("MBR_REL_NM_4");
		headerClient.createCell(24).setCellValue("MBR_REL_TYP_4");
		headerClient.createCell(25).setCellValue("NOMINEE_NAME");
		headerClient.createCell(26).setCellValue("NOMINEE_REL_TYP");
		headerClient.createCell(27).setCellValue("NOMINEE_AGE");
		headerClient.createCell(28).setCellValue("VOTERS_ID_NUMBER");
		headerClient.createCell(29).setCellValue("U_ID");
		headerClient.createCell(30).setCellValue("PAN");
		headerClient.createCell(31).setCellValue("RATION_CARD");
		headerClient.createCell(32).setCellValue("OTHER_ID_TYPE_1_DESC");
		headerClient.createCell(33).setCellValue("MEMBER_OTHER_ID_1");
		headerClient.createCell(34).setCellValue("MEMBER_OTHER_ID_2_DESC");
		headerClient.createCell(35).setCellValue("MEMBER_OTHER_ID_2");
		headerClient.createCell(36).setCellValue("MEMBER_OTHER_ID_3_DESC");
		headerClient.createCell(37).setCellValue("MEMBER_OTHER_ID_3");
		headerClient.createCell(38).setCellValue("TELEPHONE_NUMBER_1_IND");
		headerClient.createCell(39).setCellValue("MEMBER_TELEPHONE_NUMBER_1");
		headerClient.createCell(40).setCellValue("TELEPHONE_NUMBER_2_IND");
		headerClient.createCell(41).setCellValue("MEMBER_TELEPHONE_NUMBER_2");
		headerClient.createCell(42).setCellValue("POVERTY_INDEX");
		headerClient.createCell(43).setCellValue("ASSET_OWNERSHIP_INDICATOR");
		headerClient.createCell(44).setCellValue("NUMBER_OF_DEPENDENTS");
		headerClient.createCell(45).setCellValue("MBR_BANK_NM");
		headerClient.createCell(46).setCellValue("MBR_BANK_BRNCH_NM");
		headerClient.createCell(47).setCellValue("MBR_BANK_ACT_NBR");
		headerClient.createCell(48).setCellValue("OCCUPATION");
		headerClient.createCell(49).setCellValue("TOTAL_MONTHLY_INCOME");
		headerClient.createCell(50).setCellValue("MONTHLY_FAMILY_EXPENSES");
		headerClient.createCell(51).setCellValue("MBR_RELIGION");
		headerClient.createCell(52).setCellValue("MBR_CASTE");
		headerClient.createCell(53).setCellValue("GRP_LDR_IND");
		headerClient.createCell(54).setCellValue("CNTR_LDR_IND");
		headerClient.createCell(55).setCellValue("RESERVED_FOR_FUTURE_USE");
		headerClient.createCell(56).setCellValue("MEMBER_NAME_4");
		headerClient.createCell(57).setCellValue("MEMBER_NAME_5");
		headerClient.createCell(58).setCellValue("PASSPORT_NBR");
		headerClient.createCell(59).setCellValue("PARENT_ID");
		headerClient.createCell(60).setCellValue("EXTRACTION_FILE_ID");
		headerClient.createCell(61).setCellValue("SEVERITY");

		/*
		 * for(int j=0;j<clientDetails.size();j++){ ClientDetailData tempClient =
		 * clientDetails.get(j); Row clientRow = clientSheet.createRow(j+1);
		 * clientRow.setRowStyle(style);
		 * 
		 * final FamilyDetailsMapper fm = new FamilyDetailsMapper(); final List
		 * <FamilyDetailData> familyDetails = this.jdbcTemplate.query(fm.schema(),fm,
		 * new Object[] {tempClient.getClient_id()}); int familySize =
		 * familyDetails.size(); if(familySize>4) familySize=4; int fdc=17;
		 * if(familySize!=0){ for(int f=0;f<familySize;f++){ FamilyDetailData fdetail =
		 * familyDetails.get(f); if(fdetail.getRelationType()==79){
		 * clientRow.createCell(15).setCellValue(fdetail.getName());
		 * clientRow.createCell(16).setCellValue(fdetail.getRelation()); } else{
		 * clientRow.createCell(fdc).setCellValue(fdetail.getName());
		 * clientRow.createCell(fdc+1).setCellValue(fdetail.getRelation()); fdc=fdc+2; }
		 * } } clientRow.createCell(0).setCellValue(tempClient.getClientExternal());
		 * clientRow.createCell(1).setCellValue("CNSCRD");
		 * clientRow.createCell(2).setCellValue(tempClient.getClientExternal());
		 * clientRow.createCell(3).setCellValue(tempClient.getOfficeExtrenal());
		 * if(tempClient.getGroupExternal()!=null){
		 * clientRow.createCell(5).setCellValue(tempClient.getGroupExternal()); } else {
		 * clientRow.createCell(5).setCellValue(tempClient.getClientExternal()); }
		 * clientRow.createCell(6).setCellValue(tempClient.getClient_name());
		 * if(tempClient.getDob()!=null){ // DateFormat dateFormat = new
		 * SimpleDateFormat("yyyy-MM-dd"); // String strDate =
		 * dateFormat.format(tempClient.getDob()); Cell clientCell =
		 * clientRow.createCell(10); clientCell.setCellValue(tempClient.getDob());
		 * clientCell.setCellStyle(style1); } // DateFormat dateFormat1 = new
		 * SimpleDateFormat("yyyy-MM-dd"); // String strDate1 =
		 * dateFormat1.format(tempClient.getActivateDate()); Cell active =
		 * clientRow.createCell(12); active.setCellValue(tempClient.getActivateDate());
		 * active.setCellStyle(style1); if(tempClient.getAge()!=0)
		 * clientRow.createCell(11).setCellValue(tempClient.getAge());
		 * clientRow.createCell(13).setCellValue(tempClient.getGender());
		 * clientRow.createCell(25).setCellValue(tempClient.getNomineeName());
		 * clientRow.createCell(26).setCellValue(tempClient.getNomineeRelation());
		 * if(tempClient.getNomineeAge()!=0)
		 * clientRow.createCell(27).setCellValue(tempClient.getNomineeAge());
		 * clientRow.createCell(28).setCellValue(tempClient.getVoterId());
		 * clientRow.createCell(29).setCellValue(tempClient.getAadhaarId());
		 * clientRow.createCell(30).setCellValue(tempClient.getPancardId());
		 * clientRow.createCell(39).setCellValue(tempClient.getMobileNo());
		 * clientRow.createCell(45).setCellValue(tempClient.getAccHolderName());
		 * clientRow.createCell(46).setCellValue(tempClient.getBankName());
		 * clientRow.createCell(47).setCellValue(tempClient.getBankAccount()); }
		 */

		clientSheet.autoSizeColumn(0);
		clientSheet.autoSizeColumn(1);
		clientSheet.autoSizeColumn(2);
		clientSheet.autoSizeColumn(3);
		clientSheet.autoSizeColumn(4);
		clientSheet.autoSizeColumn(5);
		clientSheet.autoSizeColumn(6);
		clientSheet.autoSizeColumn(7);
		clientSheet.autoSizeColumn(8);
		clientSheet.autoSizeColumn(9);
		clientSheet.autoSizeColumn(10);
		clientSheet.autoSizeColumn(11);
		clientSheet.autoSizeColumn(12);
		clientSheet.autoSizeColumn(13);
		clientSheet.autoSizeColumn(14);
		clientSheet.autoSizeColumn(15);
		clientSheet.autoSizeColumn(16);
		clientSheet.autoSizeColumn(17);
		clientSheet.autoSizeColumn(18);
		clientSheet.autoSizeColumn(19);
		clientSheet.autoSizeColumn(20);
		clientSheet.autoSizeColumn(21);
		clientSheet.autoSizeColumn(22);
		clientSheet.autoSizeColumn(23);
		clientSheet.autoSizeColumn(24);
		clientSheet.autoSizeColumn(25);
		clientSheet.autoSizeColumn(26);
		clientSheet.autoSizeColumn(27);
		clientSheet.autoSizeColumn(28);
		clientSheet.autoSizeColumn(29);
		clientSheet.autoSizeColumn(30);
		clientSheet.autoSizeColumn(31);
		clientSheet.autoSizeColumn(32);
		clientSheet.autoSizeColumn(33);
		clientSheet.autoSizeColumn(34);
		clientSheet.autoSizeColumn(35);
		clientSheet.autoSizeColumn(36);
		clientSheet.autoSizeColumn(37);
		clientSheet.autoSizeColumn(38);
		clientSheet.autoSizeColumn(39);
		clientSheet.autoSizeColumn(40);
		clientSheet.autoSizeColumn(41);
		clientSheet.autoSizeColumn(42);
		clientSheet.autoSizeColumn(43);
		clientSheet.autoSizeColumn(44);
		clientSheet.autoSizeColumn(45);
		clientSheet.autoSizeColumn(46);
		clientSheet.autoSizeColumn(47);
		clientSheet.autoSizeColumn(48);
		clientSheet.autoSizeColumn(49);
		clientSheet.autoSizeColumn(50);
		clientSheet.autoSizeColumn(51);
		clientSheet.autoSizeColumn(52);
		clientSheet.autoSizeColumn(53);
		clientSheet.autoSizeColumn(54);
		clientSheet.autoSizeColumn(55);
		clientSheet.autoSizeColumn(56);
		clientSheet.autoSizeColumn(57);
		clientSheet.autoSizeColumn(58);
		clientSheet.autoSizeColumn(59);
		clientSheet.autoSizeColumn(60);
		clientSheet.autoSizeColumn(61);

		Row header = addressSheet.createRow(0);
		header.setRowStyle(style);
		header.createCell(0).setCellValue("BANK_ID");
		header.createCell(1).setCellValue("SEGMENT_IDENTIFIER");
		header.createCell(2).setCellValue("MBR_PERM_ADDR");
		header.createCell(3).setCellValue("MBR_PERM_ST_CD");
		header.createCell(4).setCellValue("MBR_PERM_PIN_CD");
		header.createCell(5).setCellValue("MBR_CURR_ADDR");
		header.createCell(6).setCellValue("MBR_CURR_ST_CD");
		header.createCell(7).setCellValue("MBR_CURR_PIN_CD");
		header.createCell(8).setCellValue("RESERVED_FOR_FUTURE_USE");
		header.createCell(9).setCellValue("PARENT_ID");
		header.createCell(10).setCellValue("EXTRACTION_FILE_ID");
		header.createCell(11).setCellValue("SEVERITY");

		/*
		 * for(int i=0;i<addressDetails.size();i++){ ClientAddressData tempAddr =
		 * addressDetails.get(i); Row rowNext = addressSheet.createRow(i+1);
		 * rowNext.setRowStyle(style);
		 * rowNext.createCell(0).setCellValue(tempAddr.getClientExternal());
		 * rowNext.createCell(1).setCellValue("ADRCRD");
		 * rowNext.createCell(2).setCellValue(tempAddr.getAddressLine1()+tempAddr.
		 * getAddressLine2()+tempAddr.getAddressLine3()+tempAddr.getStreet()+tempAddr.
		 * getTownVillage()+tempAddr.getCity());
		 * rowNext.createCell(3).setCellValue(tempAddr.getStateName());
		 * rowNext.createCell(4).setCellValue(tempAddr.getPostalCode());
		 * rowNext.createCell(9).setCellValue(tempAddr.getClientExternal()); }
		 */
		addressSheet.autoSizeColumn(0);
		addressSheet.autoSizeColumn(1);
		addressSheet.autoSizeColumn(2);
		addressSheet.autoSizeColumn(3);
		addressSheet.autoSizeColumn(4);
		addressSheet.autoSizeColumn(5);
		addressSheet.autoSizeColumn(6);
		addressSheet.autoSizeColumn(7);
		addressSheet.autoSizeColumn(8);
		addressSheet.autoSizeColumn(9);
		addressSheet.autoSizeColumn(10);
		addressSheet.autoSizeColumn(11);

		return workbook;
	}

	private static final class FamilyDetailsMapper implements RowMapper<FamilyDetailData> {

		public String schema() {
			return "select fd.client_id as clientId,fd.`Name` as name,mcv.code_value as relation,fd.`Relationship with Borrowers` as relationType  from `family detail` fd join m_code_value mcv on mcv.id=fd.`Relationship with Borrowers` where fd.client_id=?";
		}

		@Override
		public FamilyDetailData mapRow(final ResultSet rs, @SuppressWarnings("unused") final int rowNum)
				throws SQLException {

			final long clientId = rs.getLong("clientId");

			final String name = rs.getString("name");

			final String relation = rs.getString("relation");

			final int relationType = rs.getInt("relationType");

			return new FamilyDetailData(clientId, name, relation, relationType);

		}
	}

	private static final class AddressDetailsMapper implements RowMapper<ClientAddressData> {

		public String schema() {
			return "select distinct cv2.code_value as addressType,ca.client_id as client_id,addr.id as id,addr.street as street,addr.address_line_1 as address_line_1,addr.address_line_2 as address_line_2,addr.address_line_3 as address_line_3,addr.town_village as town_village,addr.city as city,addr.county_district as county_district,cv.code_value as state_name,c.code_value as country_name,addr.postal_code as postal_code,mc.account_no as clientExternal from m_client mc join m_client_address ca  on mc.id=ca.client_id inner join m_address addr on addr.id= ca.address_id left join m_code_value cv on addr.state_province_id=cv.id left join  m_code_value c on addr.country_id=c.id left join m_code_value cv2 on ca.address_type_id=cv2.id where ca.is_active=1 group by mc.id ";
		}

		@Override
		public ClientAddressData mapRow(final ResultSet rs, @SuppressWarnings("unused") final int rowNum)
				throws SQLException {

			final String addressType = rs.getString("addressType");
			final long addressId = rs.getLong("id");

			final long client_id = rs.getLong("client_id");

			final String street = rs.getString("street");

			final String address_line_1 = rs.getString("address_line_1");

			final String address_line_2 = rs.getString("address_line_2");

			final String address_line_3 = rs.getString("address_line_3");

			final String town_village = rs.getString("town_village");

			final String city = rs.getString("city");

			final String county_district = rs.getString("county_district");

			final String country_name = rs.getString("country_name");

			final String state_name = rs.getString("state_name");

			final String postal_code = rs.getString("postal_code");

			final String external = rs.getString("clientExternal");

			return new ClientAddressData(addressType, client_id, addressId, street, address_line_1, address_line_2,
					address_line_3, town_village, city, county_district, state_name, country_name, postal_code,
					external);

		}
	}

	private static final class ClientDetailsMapper implements RowMapper<ClientDetailData> {

		public String schema() {
			return "select c.id as clientId,c.account_no as external, getAge(c.submittedon_date,c.date_of_birth) as age, o.external_id as officeExternal,c.activation_date as activateDate,c.display_name as name,c.mobile_no as mobileNo, c.date_of_birth as dob,mcv.code_value as gender, mcs.code_value as marital_status ,mg.account_no as groupExternal,nd.`Name` as nomineeName, nd.`Age` as nomineeAge,mcv1.id as nomineeRelation,bd.`Bank Name` as bankName, bd.`Account Number` as bankAccount, bd.`Account Holder Name` as accHolderName,`getVoterId`(c.id) as voterId, `getAadhaarId`(c.id) as aadhaarId,`getPanCardId`(c.id) as pancardId ,mcvo.code_value as 'Occupation', ifnull(ROUND(iae.`Income of the Family`,2),0) as 'monthlyincome', ifnull(ROUND(iae.`Expenditure of the Family`,2),0) as 'monthlyexpenses', mcvr.code_value as 'Religion',mcvc.code_value as 'Caste' from m_client c join m_office o on o.id=c.office_id left join m_code_value mcv on mcv.id=c.gender_cv_id left join m_group_client mgc on mgc.client_id= c.id left join m_group mg on mg.id= mgc.group_id left join `bank detail` bd on bd.client_id= c.id left join `nominee detail` nd on nd.client_id = c.id left join m_code_value mcv1 on mcv1.id=nd.`Relationship Type` left join `social status` ss on ss.client_id = c.id left join m_code_value mcs on mcs.id = ss.`Marital Status` left join `income and expense` iae on iae.id=(select max(iae1.id) from `income and expense` iae1 where iae1.client_id=c.id) left join m_code_value mcvo on mcvo.id=iae.Occupation left join `social status` ss12 on ss12.id=(select max(ss1.id) from `social status` ss1 where ss1.client_id=c.id) left join m_code_value mcvr on ss12.Religion=mcvr.id left join `social status` ss11 on ss11.id=(select max(ss2.id) from `social status` ss2 where ss2.client_id=c.id) left join m_code_value mcvc on ss11.Caste=mcvc.id";
		}

		@Override
		public ClientDetailData mapRow(final ResultSet rs, @SuppressWarnings("unused") final int rowNum)
				throws SQLException {

			final String clientExternal = rs.getString("external");
			final long client_id = rs.getLong("clientId");
			final String officeExtrenal = rs.getString("officeExternal");
			final String client_name = rs.getString("name");
			final String mobileNo = rs.getString("mobileNo");
			final String gender = rs.getString("gender");
			final String maritalStatus = rs.getString("marital_status");
			final String groupExternal = rs.getString("groupExternal");
			final String nomineeName = rs.getString("nomineeName");
			final int nomineeAge = rs.getInt("nomineeAge");
			final int nomineeRelation = rs.getInt("nomineeRelation");
			final String bankName = rs.getString("bankName");
			final String bankAccount = rs.getString("bankAccount");
			final String accHolderName = rs.getString("accHolderName");
			final String voterId = rs.getString("voterId");
			final String aadhaarId = rs.getString("aadhaarId");
			final String pancardId = rs.getString("pancardId");
			final Date activateDate = rs.getDate("activateDate");
			final Date dob = rs.getDate("dob");
			final int age = rs.getInt("age");

			final String occupation = rs.getString("Occupation");
			final long monthlyincome = rs.getLong("monthlyincome");
			final long monthlyexpenses = rs.getLong("monthlyexpenses");
			final String religion = rs.getString("Religion");
			final String caste = rs.getString("Caste");

			return new ClientDetailData(clientExternal, client_id, officeExtrenal, client_name, mobileNo, gender,
					maritalStatus, groupExternal, nomineeName, nomineeAge, nomineeRelation, bankName, bankAccount,
					accHolderName, voterId, aadhaarId, pancardId, activateDate, dob, age, occupation, monthlyincome,
					monthlyexpenses, religion, caste);

		}
	}

	private static final class LoansDetailsMapper implements RowMapper<LoanDetailData> {

		public String schema() {
			return "select c.id as clientID, l.account_no as loanAcc, ifnull( ifnull( l.loan_counter, l.loan_product_counter ), 1 ) as loanCycle, o.external_id as officeExternal, re.enum_value as loanType, g.account_no as groupExternal, l.external_id as loanExternal, mcv.code_value as loanPurpose, l.submittedon_date as submittedDate, ifnull(max(date(ldd.disbursedon_date)),date(l.disbursedon_date)) as disbursedDate, l.closedon_date as closedDate, l.principal_amount_proposed as appliedAmount, l.approved_principal as approvedAmount, SUM(if( l.product_id in (39), ldd.principal, l.`principal_amount`)) as disbAmount, l.number_of_repayments as installmentNumber, re1.enum_value as termfrequency, ( SUM(if( l.product_id in (39), ldd.principal, l.`principal_amount`)) - `getPrincipalReceived`(l.id, ?) ) as outBalance, c.account_no as clientExternal, `getPrincipalOverdue`(l.id, ?) as overDue, `getNoOfDaysOverDue`(l.id, ?) as noOfDaysDue, (ifnull(mlr.principal_amount,0)+ ifnull(mlr.interest_amount,0)) as installmentAmount, getTransactionDate(l.id, ?) as transactionDate, (l.principal_writtenoff_derived + l.interest_writtenoff_derived) as writeoff_amount, cv.code_value as writeoff_reason, l.writtenoffon_date as writeoff_date, ROUND( getInsurance(l.id), 2 ) as Insurance from m_loan l join m_client c on c.id = l.client_id left join m_office o on o.id = c.office_id left join r_enum_value re on re.enum_id = l.loan_type_enum and re.enum_name = 'account_type_type_enum' left join m_group g on g.id = l.group_id left join m_code_value mcv on mcv.id = l.loanpurpose_cv_id left join r_enum_value re1 on re1.enum_id = l.repayment_period_frequency_enum and re1.enum_name = 'repayment_period_frequency_enum' left join m_loan_repayment_schedule mlr on mlr.loan_id = l.id and mlr.installment = 1 left join m_loan_arrears_aging mla on mla.loan_id = l.id left join m_code_value cv on cv.id = l.writeoff_reason_cv_id left join m_loan_disbursement_detail ldd on ldd.loan_id = l.id and ldd.disbursedon_date is not null where l.loan_status_id not in(100, 200, 500)  and ifnull(date(ldd.disbursedon_date),date(l.disbursedon_date)) <= ? AND ( ( ifnull(l.closedon_date, ?) between ? and ? ) or ( if( l.closedon_date > ?, ?, l.closedon_date ) between ? and ? ) ) group by l.id";
		}

		@Override
		public LoanDetailData mapRow(final ResultSet rs, @SuppressWarnings("unused") final int rowNum)
				throws SQLException {

			final long clientID = rs.getInt("clientID");
			final String loanAcc = rs.getString("loanAcc");
			final String officeExtrenal = rs.getString("officeExternal");
			final String loanType = rs.getString("loanType");
			final String loanExternal = rs.getString("loanExternal");
			final String loanPurpose = rs.getString("loanPurpose");
			final String groupExternal = rs.getString("groupExternal");
			final Date submittedDate = rs.getDate("submittedDate");
			final Date disbursedDate = rs.getDate("disbursedDate");
			final Date closedDate = rs.getDate("closedDate");
			final BigDecimal appliedAmount = rs.getBigDecimal("appliedAmount");
			final BigDecimal approvedAmount = rs.getBigDecimal("approvedAmount");
			final BigDecimal disbAmount = rs.getBigDecimal("disbAmount");
			final int installmentNumber = rs.getInt("installmentNumber");
			final String termfrequency = rs.getString("termfrequency");
			final BigDecimal outBalance = rs.getBigDecimal("outBalance");
			final String clientExternal = rs.getString("clientExternal");
			final BigDecimal installmentAmount = rs.getBigDecimal("installmentAmount");
			final BigDecimal overDueAmount = rs.getBigDecimal("overDue");
			final int daysOverDUe = rs.getInt("noOfDaysDue");
			final long loanCycle = rs.getLong("loanCycle");
			final Date transactionDate = rs.getDate("transactionDate");

			final long writeoffAmount = rs.getLong("writeoff_amount");
			final String writeoffReason = rs.getString("writeoff_reason");
			final Date writeoffDate = rs.getDate("writeoff_date");

			return new LoanDetailData(clientID, loanAcc, officeExtrenal, loanType, loanExternal, loanPurpose,
					groupExternal, submittedDate, disbursedDate, closedDate, appliedAmount, approvedAmount, disbAmount,
					installmentNumber, termfrequency, outBalance, clientExternal, installmentAmount, overDueAmount,
					daysOverDUe, loanCycle, transactionDate, writeoffAmount, writeoffReason, writeoffDate);

		}
	}

	@Override
	// @Transactional(readOnly=true)
	public Page<ClientData> retrieveAll(final SearchParameters searchParameters) {

		final String userOfficeHierarchy = this.context.officeHierarchy();
		final String underHierarchySearchString = userOfficeHierarchy + "%";
		final String appUserID = String.valueOf(context.authenticatedUser().getId());

		// if (searchParameters.isScopedByOfficeHierarchy()) {
		// this.context.validateAccessRights(searchParameters.getHierarchy());
		// underHierarchySearchString = searchParameters.getHierarchy() + "%";
		// }
		List<Object> paramList = new ArrayList<>(Arrays.asList(underHierarchySearchString, underHierarchySearchString));
		final StringBuilder sqlBuilder = new StringBuilder(200);
		sqlBuilder.append("select SQL_CALC_FOUND_ROWS ");
		sqlBuilder.append(this.clientMapper.schema());
		sqlBuilder.append(" where (o.hierarchy like ? or transferToOffice.hierarchy like ?) ");

		if (searchParameters.isSelfUser()) {
			sqlBuilder.append(
					" and c.id in (select umap.client_id from m_selfservice_user_client_mapping as umap where umap.appuser_id = ? ) ");
			paramList.add(appUserID);
		}

		final String extraCriteria = buildSqlStringFromClientCriteria(this.clientMapper.schema(), searchParameters,
				paramList);

		if (StringUtils.isNotBlank(extraCriteria)) {
			sqlBuilder.append(" and (").append(extraCriteria).append(")");
		}

		if (searchParameters.isOrderByRequested()) {
			sqlBuilder.append(" order by ").append(searchParameters.getOrderBy());

			if (searchParameters.isSortOrderProvided()) {
				sqlBuilder.append(' ').append(searchParameters.getSortOrder());
			}
		}

		if (searchParameters.isLimited()) {
			sqlBuilder.append(" limit ").append(searchParameters.getLimit());
			if (searchParameters.isOffset()) {
				sqlBuilder.append(" offset ").append(searchParameters.getOffset());
			}
		}

		final String sqlCountRows = "SELECT FOUND_ROWS()";
		return this.paginationHelper.fetchPage(this.jdbcTemplate, sqlCountRows, sqlBuilder.toString(),
				paramList.toArray(), this.clientMapper);
	}

	private String buildSqlStringFromClientCriteria(String schemaSql, final SearchParameters searchParameters,
			List<Object> paramList) {

		String sqlSearch = searchParameters.getSqlSearch();
		final Long officeId = searchParameters.getOfficeId();
		final String externalId = searchParameters.getExternalId();
		final String displayName = searchParameters.getName();
		final String firstname = searchParameters.getFirstname();
		final String lastname = searchParameters.getLastname();

		String extraCriteria = "";
		if (sqlSearch != null) {
			sqlSearch = sqlSearch.replaceAll(" display_name ", " c.display_name ");
			sqlSearch = sqlSearch.replaceAll("display_name ", "c.display_name ");
			extraCriteria = " and (" + sqlSearch + ")";
			this.columnValidator.validateSqlInjection(schemaSql, sqlSearch);
		}

		if (officeId != null) {
			extraCriteria += " and c.office_id = ? ";
			paramList.add(officeId);
		}

		if (externalId != null) {
			paramList.add(ApiParameterHelper.sqlEncodeString(externalId));
			extraCriteria += " and c.external_id like ? ";
		}

		if (displayName != null) {
			// extraCriteria += " and concat(ifnull(c.firstname, ''), if(c.firstname > '','
			// ', '') , ifnull(c.lastname, '')) like "
			paramList.add("%" + displayName + "%");
			extraCriteria += " and c.display_name like ? ";
		}

		if (firstname != null) {
			paramList.add(ApiParameterHelper.sqlEncodeString(firstname));
			extraCriteria += " and c.firstname like ? ";
		}

		if (lastname != null) {
			paramList.add(ApiParameterHelper.sqlEncodeString(lastname));
			extraCriteria += " and c.lastname like ? ";
		}

		if (searchParameters.isScopedByOfficeHierarchy()) {
			paramList.add(ApiParameterHelper.sqlEncodeString(searchParameters.getHierarchy() + "%"));
			extraCriteria += " and o.hierarchy like ? ";
		}

		if (searchParameters.isOrphansOnly()) {
			extraCriteria += " and c.id NOT IN (select client_id from m_group_client) ";
		}

		if (StringUtils.isNotBlank(extraCriteria)) {
			extraCriteria = extraCriteria.substring(4);
		}
		return extraCriteria;
	}

	@Override
	public ClientData retrieveOne(final Long clientId) {
		try {
			final String hierarchy = this.context.officeHierarchy();
			final String hierarchySearchString = hierarchy + "%";

			final String sql = "select " + this.clientMapper.schema()
					+ " where ( o.hierarchy like ? or transferToOffice.hierarchy like ?) and c.id = ?";
			final ClientData clientData = this.jdbcTemplate.queryForObject(sql, this.clientMapper,
					new Object[] { hierarchySearchString, hierarchySearchString, clientId });

			final String clientGroupsSql = "select " + this.clientGroupsMapper.parentGroupsSchema();

			final Collection<GroupGeneralData> parentGroups = this.jdbcTemplate.query(clientGroupsSql,
					this.clientGroupsMapper, new Object[] { clientId });

			return ClientData.setParentGroups(clientData, parentGroups);

		} catch (final EmptyResultDataAccessException e) {
			throw new ClientNotFoundException(clientId);
		}
	}

	@Override
	public Collection<ClientData> retrieveAllForLookup(final String extraCriteria) {

		String sql = "select " + this.lookupMapper.schema();

		if (StringUtils.isNotBlank(extraCriteria)) {
			sql += " and (" + extraCriteria + ")";
			this.columnValidator.validateSqlInjection(sql, extraCriteria);
		}
		return this.jdbcTemplate.query(sql, this.lookupMapper, new Object[] {});
	}

	@Override
	public Collection<ClientData> retrieveAllForLookupByOfficeId(final Long officeId) {

		final String sql = "select " + this.lookupMapper.schema() + " where c.office_id = ? and c.status_enum != ?";

		return this.jdbcTemplate.query(sql, this.lookupMapper,
				new Object[] { officeId, ClientStatus.CLOSED.getValue() });
	}

	@Override
	public Collection<ClientData> retrieveClientMembersOfGroup(final Long groupId) {

		final AppUser currentUser = this.context.authenticatedUser();
		final String hierarchy = currentUser.getOffice().getHierarchy();
		final String hierarchySearchString = hierarchy + "%";

		final String sql = "select " + this.membersOfGroupMapper.schema()
				+ " where o.hierarchy like ? and pgc.group_id = ?";

		return this.jdbcTemplate.query(sql, this.membersOfGroupMapper, new Object[] { hierarchySearchString, groupId });
	}

	@Override
	public Collection<ClientData> retrieveActiveClientMembersOfGroup(final Long groupId) {

		final AppUser currentUser = this.context.authenticatedUser();
		final String hierarchy = currentUser.getOffice().getHierarchy();
		final String hierarchySearchString = hierarchy + "%";

		final String sql = "select " + this.membersOfGroupMapper.schema()
				+ " where o.hierarchy like ? and pgc.group_id = ? and c.status_enum = ? ";

		return this.jdbcTemplate.query(sql, this.membersOfGroupMapper,
				new Object[] { hierarchySearchString, groupId, ClientStatus.ACTIVE.getValue() });
	}

	private static final class ClientMembersOfGroupMapper implements RowMapper<ClientData> {

		private final String schema;

		public ClientMembersOfGroupMapper() {
			final StringBuilder sqlBuilder = new StringBuilder(200);

			sqlBuilder.append(
					"c.id as id, c.account_no as accountNo, c.external_id as externalId, c.status_enum as statusEnum,c.sub_status as subStatus, ");
			sqlBuilder.append(
					"cvSubStatus.code_value as subStatusValue,cvSubStatus.code_description as subStatusDesc,c.office_id as officeId, o.name as officeName, ");
			sqlBuilder.append(
					"c.transfer_to_office_id as transferToOfficeId, transferToOffice.name as transferToOfficeName, ");
			sqlBuilder.append("c.firstname as firstname, c.middlename as middlename, c.lastname as lastname, ");
			sqlBuilder.append("c.fullname as fullname, c.display_name as displayName, ");
			sqlBuilder.append("c.mobile_no as mobileNo, ");
			sqlBuilder.append("c.is_staff as isStaff, ");
			sqlBuilder.append("c.date_of_birth as dateOfBirth, ");
			sqlBuilder.append("c.gender_cv_id as genderId, ");
			sqlBuilder.append("cv.code_value as genderValue, ");
			sqlBuilder.append("c.client_type_cv_id as clienttypeId, ");
			sqlBuilder.append("cvclienttype.code_value as clienttypeValue, ");
			sqlBuilder.append("c.client_classification_cv_id as classificationId, ");
			sqlBuilder.append("cvclassification.code_value as classificationValue, ");
			sqlBuilder.append("c.legal_form_enum as legalFormEnum, ");
			sqlBuilder.append("c.activation_date as activationDate, c.image_id as imageId, ");
			sqlBuilder.append("c.staff_id as staffId, s.display_name as staffName,");
			sqlBuilder.append("c.default_savings_product as savingsProductId, sp.name as savingsProductName, ");
			sqlBuilder.append("c.default_savings_account as savingsAccountId, ");

			sqlBuilder.append("c.submittedon_date as submittedOnDate, ");
			sqlBuilder.append("sbu.username as submittedByUsername, ");
			sqlBuilder.append("sbu.firstname as submittedByFirstname, ");
			sqlBuilder.append("sbu.lastname as submittedByLastname, ");

			sqlBuilder.append("c.closedon_date as closedOnDate, ");
			sqlBuilder.append("clu.username as closedByUsername, ");
			sqlBuilder.append("clu.firstname as closedByFirstname, ");
			sqlBuilder.append("clu.lastname as closedByLastname, ");

			sqlBuilder.append("acu.username as activatedByUsername, ");
			sqlBuilder.append("acu.firstname as activatedByFirstname, ");
			sqlBuilder.append("acu.lastname as activatedByLastname, ");

			sqlBuilder.append("cnp.constitution_cv_id as constitutionId, ");
			sqlBuilder.append("cvConstitution.code_value as constitutionValue, ");
			sqlBuilder.append("cnp.incorp_no as incorpNo, ");
			sqlBuilder.append("cnp.incorp_validity_till as incorpValidityTill, ");
			sqlBuilder.append("cnp.main_business_line_cv_id as mainBusinessLineId, ");
			sqlBuilder.append("cvMainBusinessLine.code_value as mainBusinessLineValue, ");
			sqlBuilder.append("cnp.remarks as remarks ");

			sqlBuilder.append("from m_client c ");
			sqlBuilder.append("join m_office o on o.id = c.office_id ");
			sqlBuilder.append("left join m_client_non_person cnp on cnp.client_id = c.id ");
			sqlBuilder.append("join m_group_client pgc on pgc.client_id = c.id ");
			sqlBuilder.append("left join m_staff s on s.id = c.staff_id ");
			sqlBuilder.append("left join m_savings_product sp on sp.id = c.default_savings_product ");
			sqlBuilder.append("left join m_office transferToOffice on transferToOffice.id = c.transfer_to_office_id ");

			sqlBuilder.append("left join m_appuser sbu on sbu.id = c.submittedon_userid ");
			sqlBuilder.append("left join m_appuser acu on acu.id = c.activatedon_userid ");
			sqlBuilder.append("left join m_appuser clu on clu.id = c.closedon_userid ");
			sqlBuilder.append("left join m_code_value cv on cv.id = c.gender_cv_id ");
			sqlBuilder.append("left join m_code_value cvclienttype on cvclienttype.id = c.client_type_cv_id ");
			sqlBuilder.append(
					"left join m_code_value cvclassification on cvclassification.id = c.client_classification_cv_id ");
			sqlBuilder.append("left join m_code_value cvSubStatus on cvSubStatus.id = c.sub_status ");
			sqlBuilder.append("left join m_code_value cvConstitution on cvConstitution.id = cnp.constitution_cv_id ");
			sqlBuilder.append(
					"left join m_code_value cvMainBusinessLine on cvMainBusinessLine.id = cnp.main_business_line_cv_id ");

			this.schema = sqlBuilder.toString();
		}

		public String schema() {
			return this.schema;
		}

		@Override
		public ClientData mapRow(final ResultSet rs, @SuppressWarnings("unused") final int rowNum) throws SQLException {

			final String accountNo = rs.getString("accountNo");

			final Integer statusEnum = JdbcSupport.getInteger(rs, "statusEnum");
			final EnumOptionData status = ClientEnumerations.status(statusEnum);

			final Long subStatusId = JdbcSupport.getLong(rs, "subStatus");
			final String subStatusValue = rs.getString("subStatusValue");
			final String subStatusDesc = rs.getString("subStatusDesc");
			final boolean isActive = false;
			final CodeValueData subStatus = CodeValueData.instance(subStatusId, subStatusValue, subStatusDesc,
					isActive);

			final Long officeId = JdbcSupport.getLong(rs, "officeId");
			final String officeName = rs.getString("officeName");

			final Long transferToOfficeId = JdbcSupport.getLong(rs, "transferToOfficeId");
			final String transferToOfficeName = rs.getString("transferToOfficeName");

			final Long id = JdbcSupport.getLong(rs, "id");
			final String firstname = rs.getString("firstname");
			final String middlename = rs.getString("middlename");
			final String lastname = rs.getString("lastname");
			final String fullname = rs.getString("fullname");
			final String displayName = rs.getString("displayName");
			final String externalId = rs.getString("externalId");
			final String mobileNo = rs.getString("mobileNo");
			final boolean isStaff = rs.getBoolean("isStaff");
			final LocalDate dateOfBirth = JdbcSupport.getLocalDate(rs, "dateOfBirth");
			final Long genderId = JdbcSupport.getLong(rs, "genderId");
			final String genderValue = rs.getString("genderValue");
			final CodeValueData gender = CodeValueData.instance(genderId, genderValue);

			final Long clienttypeId = JdbcSupport.getLong(rs, "clienttypeId");
			final String clienttypeValue = rs.getString("clienttypeValue");
			final CodeValueData clienttype = CodeValueData.instance(clienttypeId, clienttypeValue);

			final Long classificationId = JdbcSupport.getLong(rs, "classificationId");
			final String classificationValue = rs.getString("classificationValue");
			final CodeValueData classification = CodeValueData.instance(classificationId, classificationValue);

			final LocalDate activationDate = JdbcSupport.getLocalDate(rs, "activationDate");
			final Long imageId = JdbcSupport.getLong(rs, "imageId");
			final Long staffId = JdbcSupport.getLong(rs, "staffId");
			final String staffName = rs.getString("staffName");

			final Long savingsProductId = JdbcSupport.getLong(rs, "savingsProductId");
			final String savingsProductName = rs.getString("savingsProductName");

			final Long savingsAccountId = JdbcSupport.getLong(rs, "savingsAccountId");

			final LocalDate closedOnDate = JdbcSupport.getLocalDate(rs, "closedOnDate");
			final String closedByUsername = rs.getString("closedByUsername");
			final String closedByFirstname = rs.getString("closedByFirstname");
			final String closedByLastname = rs.getString("closedByLastname");

			final LocalDate submittedOnDate = JdbcSupport.getLocalDate(rs, "submittedOnDate");
			final String submittedByUsername = rs.getString("submittedByUsername");
			final String submittedByFirstname = rs.getString("submittedByFirstname");
			final String submittedByLastname = rs.getString("submittedByLastname");

			final String activatedByUsername = rs.getString("activatedByUsername");
			final String activatedByFirstname = rs.getString("activatedByFirstname");
			final String activatedByLastname = rs.getString("activatedByLastname");

			final Integer legalFormEnum = JdbcSupport.getInteger(rs, "legalFormEnum");
			EnumOptionData legalForm = null;
			if (legalFormEnum != null)
				legalForm = ClientEnumerations.legalForm(legalFormEnum);

			final Long constitutionId = JdbcSupport.getLong(rs, "constitutionId");
			final String constitutionValue = rs.getString("constitutionValue");
			final CodeValueData constitution = CodeValueData.instance(constitutionId, constitutionValue);
			final String incorpNo = rs.getString("incorpNo");
			final LocalDate incorpValidityTill = JdbcSupport.getLocalDate(rs, "incorpValidityTill");
			final Long mainBusinessLineId = JdbcSupport.getLong(rs, "mainBusinessLineId");
			final String mainBusinessLineValue = rs.getString("mainBusinessLineValue");
			final CodeValueData mainBusinessLine = CodeValueData.instance(mainBusinessLineId, mainBusinessLineValue);
			final String remarks = rs.getString("remarks");

			final ClientNonPersonData clientNonPerson = new ClientNonPersonData(constitution, incorpNo,
					incorpValidityTill, mainBusinessLine, remarks);

			final ClientTimelineData timeline = new ClientTimelineData(submittedOnDate, submittedByUsername,
					submittedByFirstname, submittedByLastname, activationDate, activatedByUsername,
					activatedByFirstname, activatedByLastname, closedOnDate, closedByUsername, closedByFirstname,
					closedByLastname);

			return ClientData.instance(accountNo, status, subStatus, officeId, officeName, transferToOfficeId,
					transferToOfficeName, id, firstname, middlename, lastname, fullname, displayName, externalId,
					mobileNo, dateOfBirth, gender, activationDate, imageId, staffId, staffName, timeline,
					savingsProductId, savingsProductName, savingsAccountId, clienttype, classification, legalForm,
					clientNonPerson, isStaff);

		}
	}

	@Override
	public Collection<ClientData> retrieveActiveClientMembersOfCenter(final Long centerId) {

		final AppUser currentUser = this.context.authenticatedUser();
		final String hierarchy = currentUser.getOffice().getHierarchy();
		final String hierarchySearchString = hierarchy + "%";

		final String sql = "select " + this.membersOfGroupMapper.schema()
				+ " left join m_group g on pgc.group_id=g.id where o.hierarchy like ? and g.parent_id = ? and c.status_enum = ? group by c.id";

		return this.jdbcTemplate.query(sql, this.membersOfGroupMapper,
				new Object[] { hierarchySearchString, centerId, ClientStatus.ACTIVE.getValue() });
	}

	private static final class ClientMapper implements RowMapper<ClientData> {

		private final String schema;

		public ClientMapper() {
			final StringBuilder builder = new StringBuilder(400);

			builder.append(
					"c.id as id, c.account_no as accountNo, c.external_id as externalId, c.status_enum as statusEnum,c.sub_status as subStatus, ");
			builder.append(
					"cvSubStatus.code_value as subStatusValue,cvSubStatus.code_description as subStatusDesc,c.office_id as officeId, o.name as officeName, ");
			builder.append(
					"c.transfer_to_office_id as transferToOfficeId, transferToOffice.name as transferToOfficeName, ");
			builder.append("c.firstname as firstname, c.middlename as middlename, c.lastname as lastname, ");
			builder.append("c.fullname as fullname, c.display_name as displayName, ");
			builder.append("c.mobile_no as mobileNo, ");
			builder.append("c.is_staff as isStaff, ");
			builder.append("c.date_of_birth as dateOfBirth, ");
			builder.append("c.gender_cv_id as genderId, ");
			builder.append("cv.code_value as genderValue, ");
			builder.append("c.client_type_cv_id as clienttypeId, ");
			builder.append("cvclienttype.code_value as clienttypeValue, ");
			builder.append("c.client_classification_cv_id as classificationId, ");
			builder.append("cvclassification.code_value as classificationValue, ");
			builder.append("c.legal_form_enum as legalFormEnum, ");

			builder.append("c.submittedon_date as submittedOnDate, ");
			builder.append("sbu.username as submittedByUsername, ");
			builder.append("sbu.firstname as submittedByFirstname, ");
			builder.append("sbu.lastname as submittedByLastname, ");

			builder.append("c.closedon_date as closedOnDate, ");
			builder.append("clu.username as closedByUsername, ");
			builder.append("clu.firstname as closedByFirstname, ");
			builder.append("clu.lastname as closedByLastname, ");

			// builder.append("c.submittedon as submittedOnDate, ");
			builder.append("acu.username as activatedByUsername, ");
			builder.append("acu.firstname as activatedByFirstname, ");
			builder.append("acu.lastname as activatedByLastname, ");

			builder.append("cnp.constitution_cv_id as constitutionId, ");
			builder.append("cvConstitution.code_value as constitutionValue, ");
			builder.append("cnp.incorp_no as incorpNo, ");
			builder.append("cnp.incorp_validity_till as incorpValidityTill, ");
			builder.append("cnp.main_business_line_cv_id as mainBusinessLineId, ");
			builder.append("cvMainBusinessLine.code_value as mainBusinessLineValue, ");
			builder.append("cnp.remarks as remarks, ");

			builder.append("c.activation_date as activationDate, c.image_id as imageId, ");
			builder.append("c.staff_id as staffId, s.display_name as staffName, ");
			builder.append("c.default_savings_product as savingsProductId, sp.name as savingsProductName, ");
			builder.append("c.default_savings_account as savingsAccountId ");
			builder.append("from m_client c ");
			builder.append("join m_office o on o.id = c.office_id ");
			builder.append("left join m_client_non_person cnp on cnp.client_id = c.id ");
			builder.append("left join m_staff s on s.id = c.staff_id ");
			builder.append("left join m_savings_product sp on sp.id = c.default_savings_product ");
			builder.append("left join m_office transferToOffice on transferToOffice.id = c.transfer_to_office_id ");
			builder.append("left join m_appuser sbu on sbu.id = c.submittedon_userid ");
			builder.append("left join m_appuser acu on acu.id = c.activatedon_userid ");
			builder.append("left join m_appuser clu on clu.id = c.closedon_userid ");
			builder.append("left join m_code_value cv on cv.id = c.gender_cv_id ");
			builder.append("left join m_code_value cvclienttype on cvclienttype.id = c.client_type_cv_id ");
			builder.append(
					"left join m_code_value cvclassification on cvclassification.id = c.client_classification_cv_id ");
			builder.append("left join m_code_value cvSubStatus on cvSubStatus.id = c.sub_status ");
			builder.append("left join m_code_value cvConstitution on cvConstitution.id = cnp.constitution_cv_id ");
			builder.append(
					"left join m_code_value cvMainBusinessLine on cvMainBusinessLine.id = cnp.main_business_line_cv_id ");

			this.schema = builder.toString();
		}

		public String schema() {
			return this.schema;
		}

		@Override
		public ClientData mapRow(final ResultSet rs, @SuppressWarnings("unused") final int rowNum) throws SQLException {

			final String accountNo = rs.getString("accountNo");

			final Integer statusEnum = JdbcSupport.getInteger(rs, "statusEnum");
			final EnumOptionData status = ClientEnumerations.status(statusEnum);

			final Long subStatusId = JdbcSupport.getLong(rs, "subStatus");
			final String subStatusValue = rs.getString("subStatusValue");
			final String subStatusDesc = rs.getString("subStatusDesc");
			final boolean isActive = false;
			final CodeValueData subStatus = CodeValueData.instance(subStatusId, subStatusValue, subStatusDesc,
					isActive);

			final Long officeId = JdbcSupport.getLong(rs, "officeId");
			final String officeName = rs.getString("officeName");

			final Long transferToOfficeId = JdbcSupport.getLong(rs, "transferToOfficeId");
			final String transferToOfficeName = rs.getString("transferToOfficeName");

			final Long id = JdbcSupport.getLong(rs, "id");
			final String firstname = rs.getString("firstname");
			final String middlename = rs.getString("middlename");
			final String lastname = rs.getString("lastname");
			final String fullname = rs.getString("fullname");
			final String displayName = rs.getString("displayName");
			final String externalId = rs.getString("externalId");
			final String mobileNo = rs.getString("mobileNo");
			final boolean isStaff = rs.getBoolean("isStaff");
			final LocalDate dateOfBirth = JdbcSupport.getLocalDate(rs, "dateOfBirth");
			final Long genderId = JdbcSupport.getLong(rs, "genderId");
			final String genderValue = rs.getString("genderValue");
			final CodeValueData gender = CodeValueData.instance(genderId, genderValue);

			final Long clienttypeId = JdbcSupport.getLong(rs, "clienttypeId");
			final String clienttypeValue = rs.getString("clienttypeValue");
			final CodeValueData clienttype = CodeValueData.instance(clienttypeId, clienttypeValue);

			final Long classificationId = JdbcSupport.getLong(rs, "classificationId");
			final String classificationValue = rs.getString("classificationValue");
			final CodeValueData classification = CodeValueData.instance(classificationId, classificationValue);

			final LocalDate activationDate = JdbcSupport.getLocalDate(rs, "activationDate");
			final Long imageId = JdbcSupport.getLong(rs, "imageId");
			final Long staffId = JdbcSupport.getLong(rs, "staffId");
			final String staffName = rs.getString("staffName");

			final Long savingsProductId = JdbcSupport.getLong(rs, "savingsProductId");
			final String savingsProductName = rs.getString("savingsProductName");
			final Long savingsAccountId = JdbcSupport.getLong(rs, "savingsAccountId");

			final LocalDate closedOnDate = JdbcSupport.getLocalDate(rs, "closedOnDate");
			final String closedByUsername = rs.getString("closedByUsername");
			final String closedByFirstname = rs.getString("closedByFirstname");
			final String closedByLastname = rs.getString("closedByLastname");

			final LocalDate submittedOnDate = JdbcSupport.getLocalDate(rs, "submittedOnDate");
			final String submittedByUsername = rs.getString("submittedByUsername");
			final String submittedByFirstname = rs.getString("submittedByFirstname");
			final String submittedByLastname = rs.getString("submittedByLastname");

			final String activatedByUsername = rs.getString("activatedByUsername");
			final String activatedByFirstname = rs.getString("activatedByFirstname");
			final String activatedByLastname = rs.getString("activatedByLastname");

			final Integer legalFormEnum = JdbcSupport.getInteger(rs, "legalFormEnum");
			EnumOptionData legalForm = null;
			if (legalFormEnum != null)
				legalForm = ClientEnumerations.legalForm(legalFormEnum);

			final Long constitutionId = JdbcSupport.getLong(rs, "constitutionId");
			final String constitutionValue = rs.getString("constitutionValue");
			final CodeValueData constitution = CodeValueData.instance(constitutionId, constitutionValue);
			final String incorpNo = rs.getString("incorpNo");
			final LocalDate incorpValidityTill = JdbcSupport.getLocalDate(rs, "incorpValidityTill");
			final Long mainBusinessLineId = JdbcSupport.getLong(rs, "mainBusinessLineId");
			final String mainBusinessLineValue = rs.getString("mainBusinessLineValue");
			final CodeValueData mainBusinessLine = CodeValueData.instance(mainBusinessLineId, mainBusinessLineValue);
			final String remarks = rs.getString("remarks");

			final ClientNonPersonData clientNonPerson = new ClientNonPersonData(constitution, incorpNo,
					incorpValidityTill, mainBusinessLine, remarks);

			final ClientTimelineData timeline = new ClientTimelineData(submittedOnDate, submittedByUsername,
					submittedByFirstname, submittedByLastname, activationDate, activatedByUsername,
					activatedByFirstname, activatedByLastname, closedOnDate, closedByUsername, closedByFirstname,
					closedByLastname);

			return ClientData.instance(accountNo, status, subStatus, officeId, officeName, transferToOfficeId,
					transferToOfficeName, id, firstname, middlename, lastname, fullname, displayName, externalId,
					mobileNo, dateOfBirth, gender, activationDate, imageId, staffId, staffName, timeline,
					savingsProductId, savingsProductName, savingsAccountId, clienttype, classification, legalForm,
					clientNonPerson, isStaff);

		}
	}

	private static final class ParentGroupsMapper implements RowMapper<GroupGeneralData> {

		public String parentGroupsSchema() {
			return "gp.id As groupId , gp.account_no as accountNo, gp.display_name As groupName from m_client cl JOIN m_group_client gc ON cl.id = gc.client_id "
					+ "JOIN m_group gp ON gp.id = gc.group_id WHERE cl.id  = ?";
		}

		@Override
		public GroupGeneralData mapRow(final ResultSet rs, @SuppressWarnings("unused") final int rowNum)
				throws SQLException {

			final Long groupId = JdbcSupport.getLong(rs, "groupId");
			final String groupName = rs.getString("groupName");
			final String accountNo = rs.getString("accountNo");

			return GroupGeneralData.lookup(groupId, accountNo, groupName);
		}
	}

	private static final class ClientLookupMapper implements RowMapper<ClientData> {

		private final String schema;

		public ClientLookupMapper() {
			final StringBuilder builder = new StringBuilder(200);

			builder.append("c.id as id, c.display_name as displayName, ");
			builder.append("c.office_id as officeId, o.name as officeName ");
			builder.append("from m_client c ");
			builder.append("join m_office o on o.id = c.office_id ");

			this.schema = builder.toString();
		}

		public String schema() {
			return this.schema;
		}

		@Override
		public ClientData mapRow(final ResultSet rs, @SuppressWarnings("unused") final int rowNum) throws SQLException {

			final Long id = rs.getLong("id");
			final String displayName = rs.getString("displayName");
			final Long officeId = rs.getLong("officeId");
			final String officeName = rs.getString("officeName");

			return ClientData.lookup(id, displayName, officeId, officeName);
		}
	}

	@Override
	public ClientData retrieveClientByIdentifier(final Long identifierTypeId, final String identifierKey) {
		try {
			final ClientIdentifierMapper mapper = new ClientIdentifierMapper();

			final String sql = "select " + mapper.clientLookupByIdentifierSchema();

			return this.jdbcTemplate.queryForObject(sql, mapper, new Object[] { identifierTypeId, identifierKey });
		} catch (final EmptyResultDataAccessException e) {
			return null;
		}
	}

	private static final class ClientIdentifierMapper implements RowMapper<ClientData> {

		public String clientLookupByIdentifierSchema() {
			return "c.id as id, c.account_no as accountNo, c.firstname as firstname, c.middlename as middlename, c.lastname as lastname, "
					+ "c.fullname as fullname, c.display_name as displayName,"
					+ "c.office_id as officeId, o.name as officeName "
					+ " from m_client c, m_office o, m_client_identifier ci "
					+ "where o.id = c.office_id and c.id=ci.client_id "
					+ "and ci.document_type_id= ? and ci.document_key like ?";
		}

		@Override
		public ClientData mapRow(final ResultSet rs, @SuppressWarnings("unused") final int rowNum) throws SQLException {

			final Long id = rs.getLong("id");
			final String accountNo = rs.getString("accountNo");

			final String firstname = rs.getString("firstname");
			final String middlename = rs.getString("middlename");
			final String lastname = rs.getString("lastname");
			final String fullname = rs.getString("fullname");
			final String displayName = rs.getString("displayName");

			final Long officeId = rs.getLong("officeId");
			final String officeName = rs.getString("officeName");

			return ClientData.clientIdentifier(id, accountNo, firstname, middlename, lastname, fullname, displayName,
					officeId, officeName);
		}
	}

	private Long defaultToUsersOfficeIfNull(final Long officeId) {
		Long defaultOfficeId = officeId;
		if (defaultOfficeId == null) {
			defaultOfficeId = this.context.authenticatedUser().getOffice().getId();
		}
		return defaultOfficeId;
	}

	@Override
	public ClientData retrieveAllNarrations(final String clientNarrations) {
		final List<CodeValueData> narrations = new ArrayList<>(
				this.codeValueReadPlatformService.retrieveCodeValuesByCode(clientNarrations));
		final Collection<CodeValueData> clientTypeOptions = null;
		final Collection<CodeValueData> clientClassificationOptions = null;
		final Collection<CodeValueData> clientNonPersonConstitutionOptions = null;
		final Collection<CodeValueData> clientNonPersonMainBusinessLineOptions = null;
		final List<EnumOptionData> clientLegalFormOptions = null;
		return ClientData.template(null, null, null, null, narrations, null, null, clientTypeOptions,
				clientClassificationOptions, clientNonPersonConstitutionOptions, clientNonPersonMainBusinessLineOptions,
				clientLegalFormOptions, null, null, null);
	}

	public void insertIntoNomineeDetails(long clientID, String nomineeName) {
		// TODO Auto-generated method stub
//		this.jdbcTemplate.query("INSERT INTO `mifostenant-default`.`nominee detail` (`client_id`, `Name`) VALUES (?, ?);", null,  new Object[] {clientID, nomineeName});	 

		this.jdbcTemplate.execute(
				"INSERT INTO `mifostenant-default`.`nominee detail` (`client_id`, `Name`, `Relationship Type`) VALUES (\""
						+ clientID + "\",\"" + nomineeName + "\",\"79\");");
	}

	public List<StateDetailsData> retrieveStateDetails() {
		// TODO Auto-generated method stub
		this.context.authenticatedUser();

		final StateDetailsMapper ptm = new StateDetailsMapper();
		final String sql = "select " + ptm.schema();

		return this.jdbcTemplate.query(sql, ptm, new Object[] {});
	}

	private static final class StateDetailsMapper implements RowMapper<StateDetailsData> {

		public String schema() {
			return "h.id as id,h.state_code as stateCode,h.state_name as stateName  from hab_state_code_highmark h";
		}

		@Override
		public StateDetailsData mapRow(final ResultSet rs, @SuppressWarnings("unused") final int rowNum)
				throws SQLException {

			final Long id = rs.getLong("id");
			final String stateCode = rs.getString("stateCode");

			final String stateName = rs.getString("stateName");
			;

			return StateDetailsData.stateList(id, stateCode, stateName);
		}
	}
	
	
	
	
	
	
	
	// new highmark
	
	@Override
	public Workbook retrieveHighMarkData(String asOnDate, String closedFrom) {
		SimpleDateFormat sdf3 = new SimpleDateFormat("EEE MMM dd yyyy HH:mm:ss zzz ", Locale.ENGLISH);
		Date asDate = null;
		Date fromDate = null;
		try {
			Date tempDate = sdf3.parse(asOnDate);
			SimpleDateFormat format1 = new SimpleDateFormat("yyyy-MM-dd");
			String date1 = format1.format(tempDate);
			asDate = format1.parse(date1);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			Date tempDate = sdf3.parse(closedFrom);
			SimpleDateFormat format1 = new SimpleDateFormat("yyyy-MM-dd");
			String date1 = format1.format(tempDate);
			fromDate = format1.parse(date1);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		final AppUser currentUser = this.context.authenticatedUser();
		final String hierarchy = currentUser.getOffice().getHierarchy();
		final HighmarkDetailsMapper lm = new HighmarkDetailsMapper();
		final List<HighmarkDetailData> loanDetails = this.jdbcTemplate.query(lm.schema(), lm, new Object[] { asDate, asDate,
				asDate, asDate, asDate, asDate, fromDate, asDate, asDate, asDate, fromDate, asDate });

		Workbook workbook = new HSSFWorkbook();
		CreationHelper creationHelper = workbook.getCreationHelper();
		CellStyle style = workbook.createCellStyle(); // Create new style
		style.setWrapText(true); // Set wordwrap
		CellStyle style1 = workbook.createCellStyle();
		style1.setDataFormat(creationHelper.createDataFormat().getFormat("ddMMyyyy"));

		Sheet accountSheet = workbook.createSheet("Highmark data");

		Row headerLoan = accountSheet.createRow(0);
		headerLoan.setRowStyle(style);
		headerLoan.createCell(0).setCellValue("SEGMENT_IDENTIFIER");
		headerLoan.createCell(1).setCellValue("MEMBER_IDENTIFIER");
		headerLoan.createCell(2).setCellValue("BRANCH_IDENTIFIER");
		headerLoan.createCell(3).setCellValue("KENDRA_CENTRE_IDENTIFIER");
		headerLoan.createCell(4).setCellValue("GROUP_IDENTIFIER");
		headerLoan.createCell(5).setCellValue("MEMBER_NAME_1");
		headerLoan.createCell(6).setCellValue("MEMBER_NAME_2");
		headerLoan.createCell(7).setCellValue("MEMBER_NAME_3");
		headerLoan.createCell(8).setCellValue("ALTERNATE_NAME_OF_MEMBER");
		headerLoan.createCell(9).setCellValue("MEMBER_DATE_OF_BIRTH");
		headerLoan.createCell(10).setCellValue("MEMBER_AGE");
		headerLoan.createCell(11).setCellValue("MEMBER_AGE_AS_ON_DATE");
		headerLoan.createCell(12).setCellValue("MEMBER_GENDER_TYPE");
		headerLoan.createCell(13).setCellValue("MARITAL_STATUS_TYPE");
		headerLoan.createCell(14).setCellValue("KEY_PERSON_NAME");
		headerLoan.createCell(15).setCellValue("KEY_PERSON_RELATIONSHIP");
		headerLoan.createCell(16).setCellValue("MBR_REL_NM_1");
		headerLoan.createCell(17).setCellValue("MBR_REL_TYP_1");
		headerLoan.createCell(18).setCellValue("MBR_REL_NM_2");
		headerLoan.createCell(19).setCellValue("MBR_REL_TYP_2");
		headerLoan.createCell(20).setCellValue("MBR_REL_NM_3");
		headerLoan.createCell(21).setCellValue("MBR_REL_TYP_3");
		headerLoan.createCell(22).setCellValue("MBR_REL_NM_4");
		headerLoan.createCell(23).setCellValue("MBR_REL_TYP_4");
		headerLoan.createCell(24).setCellValue("NOMINEE_NAME");
		headerLoan.createCell(25).setCellValue("NOMINEE_REL_TYP");
		headerLoan.createCell(26).setCellValue("NOMINEE_AGE");
		headerLoan.createCell(27).setCellValue("VOTERS_ID_NUMBER");
		headerLoan.createCell(28).setCellValue("U_ID");
		headerLoan.createCell(29).setCellValue("PAN");
		headerLoan.createCell(30).setCellValue("RATION_CARD");
		headerLoan.createCell(31).setCellValue("MEMBER_OTHER_ID_1_DESC");
		headerLoan.createCell(32).setCellValue("MEMBER_OTHER_ID_1");
		headerLoan.createCell(33).setCellValue("MEMBER_OTHER_ID_2_DESC");
		headerLoan.createCell(34).setCellValue("MEMBER_OTHER_ID_2");
		headerLoan.createCell(35).setCellValue("MEMBER_OTHER_ID_3_DESC");
		headerLoan.createCell(36).setCellValue("MEMBER_OTHER_ID_3");
		headerLoan.createCell(37).setCellValue("TELEPHONE_NUMBER_1_INDICATOR");
		headerLoan.createCell(38).setCellValue("MEMBER_TELEPHONE_NUMBER_1");
		headerLoan.createCell(39).setCellValue("TELEPHONE_NUMBER_2_INDICATOR");
		headerLoan.createCell(40).setCellValue("MEMBER_TELEPHONE_NUMBER_2");
		headerLoan.createCell(41).setCellValue("POVERTY_INDEX");
		headerLoan.createCell(42).setCellValue("ASSET_OWNERSHIP_INDICATOR");
		headerLoan.createCell(43).setCellValue("NUMBER_OF_DEPENDENTS");
		headerLoan.createCell(44).setCellValue("BANK_ACCOUNT-BANK_NAME");
		headerLoan.createCell(45).setCellValue("BANK_ACCOUNT-BRANCH_NAME");
		headerLoan.createCell(46).setCellValue("BANK_ACCOUNT-ACCOUNT_NUMBER");
		headerLoan.createCell(47).setCellValue("OCCUPATION");
		headerLoan.createCell(48).setCellValue("TOTAL_MONTHLY_FAMILY_INCOME");
		headerLoan.createCell(49).setCellValue("MONTHLY_FAMILY_EXPENSES");
		headerLoan.createCell(50).setCellValue("MBR_RELIGION");
		headerLoan.createCell(51).setCellValue("MBR_CASTE");
		headerLoan.createCell(52).setCellValue("GROUP_LEADER_INDICATOR");
		headerLoan.createCell(53).setCellValue("CENTER_LEADER_INDICATOR");
		headerLoan.createCell(54).setCellValue("DUMMY");
		headerLoan.createCell(55).setCellValue("SEGMENT_IDENTIFIER");
		headerLoan.createCell(56).setCellValue("MBR_PERM_ADDR");
		headerLoan.createCell(57).setCellValue("MBR_PERM_ST_CD");
		headerLoan.createCell(58).setCellValue("MBR_PERM_PIN_CD");
		headerLoan.createCell(59).setCellValue("MBR_CURR_ADDR");
		headerLoan.createCell(60).setCellValue("MBR_CURR_ST_CD");
		headerLoan.createCell(61).setCellValue("MBR_CURR_PIN_CD");
		headerLoan.createCell(62).setCellValue("ADDRESS");
		headerLoan.createCell(63).setCellValue("SEGMENT_IDENTIFIER");
		headerLoan.createCell(64).setCellValue("UNIQUE_ACCOUNT_REFERENCE_NO");
		headerLoan.createCell(65).setCellValue("ACCOUNT_NUMBER");
		headerLoan.createCell(66).setCellValue("BRANCH_IDENTIFIER");
		headerLoan.createCell(67).setCellValue("KENDRA_CENTRE_IDENTIFIER");
		headerLoan.createCell(68).setCellValue("LOAN_OFFICER_FOR_ORIGINATING_THE_LOAN");
		headerLoan.createCell(69).setCellValue("DATE_OF_ACCOUNT_INFORMATION");
		headerLoan.createCell(70).setCellValue("LOAN_CATEGORY");
		headerLoan.createCell(71).setCellValue("GROUP_IDENTIFIER");
		headerLoan.createCell(72).setCellValue("LOAN_CYCLE_ID");
		headerLoan.createCell(73).setCellValue("LOAN_PURPOSE");
		headerLoan.createCell(74).setCellValue("ACCOUNT_STATUS");
		headerLoan.createCell(75).setCellValue("APPLICATION_DATE");
		headerLoan.createCell(76).setCellValue("SANCTIONED_DATE");
		headerLoan.createCell(77).setCellValue("DATE_OPENED_DISBURSED");
		headerLoan.createCell(78).setCellValue("DATE_CLOSED");
		headerLoan.createCell(79).setCellValue("DATE_OF_LAST_PAYMENT");
		headerLoan.createCell(80).setCellValue("APPLIED_FOR_AMOUNT");
		headerLoan.createCell(81).setCellValue("LOAN_AMOUNT_SANCTIONED");
		headerLoan.createCell(82).setCellValue("TOTAL_LOAN_DISBURSED");
		headerLoan.createCell(83).setCellValue("NO_OF_INSTALLMENTS");
		headerLoan.createCell(84).setCellValue("REPAYMENT_FREQUENCY");
		headerLoan.createCell(85).setCellValue("MINIMUM_AMOUNT_DUE_INSTALLMENT_DUE");
		headerLoan.createCell(86).setCellValue("CURRENT_BALANCE");
		headerLoan.createCell(87).setCellValue("AMOUNT_OVERDUE");
		headerLoan.createCell(88).setCellValue("DAYS_PAST_DUE(DPD)");
		headerLoan.createCell(89).setCellValue("WRITE_OFF_AMOUNT");
		headerLoan.createCell(90).setCellValue("DATE_WRITE_OFF");
		headerLoan.createCell(91).setCellValue("WRITE_OFF_REASON");
		headerLoan.createCell(92).setCellValue("NO_OF_MEETTINGS_HELD");
		headerLoan.createCell(93).setCellValue("NO_OF_MEETINGS_MISSED");
		headerLoan.createCell(94).setCellValue("INSURANCE_INDICATOR");
		headerLoan.createCell(95).setCellValue("TYPE_OF_INSURANCE");
		headerLoan.createCell(96).setCellValue("SUM_ASSURED_COVERAGE");
		headerLoan.createCell(97).setCellValue("AGREED_MEETING_DAY_OF_THE_WEEK");
		headerLoan.createCell(98).setCellValue("AGREED_MEETING_TIME_OF_THE_DAY");
		headerLoan.createCell(99).setCellValue("DUMMY");
				
		

		List<StateDetailsData> stateDetails = retrieveStateDetails();
		Map<String, String> stateCodeAndName = new HashMap<String, String>();
		for (StateDetailsData state : stateDetails) {
			stateCodeAndName.put(state.getStateName(), state.getStateCode());
		}

		for (int k = 0; k < loanDetails.size(); k++) {

			HighmarkDetailData tempLoan = loanDetails.get(k);
			Row loanRow = accountSheet.createRow(k + 1);
			try {
				loanRow.setRowStyle(style);
				loanRow.createCell(0).setCellValue("CNSCRD");
				loanRow.createCell(1).setCellValue(tempLoan.getClientExternal());
				loanRow.createCell(2).setCellValue(tempLoan.getOfficeExtrenal());
				loanRow.createCell(3).setCellValue(tempLoan.getOfficeExtrenal());
				
				if (tempLoan.getGroupExternal() != null) {
					loanRow.createCell(4).setCellValue(tempLoan.getGroupExternal());
				} else {
					loanRow.createCell(4).setCellValue(tempLoan.getClientExternal());
				}
				
				loanRow.createCell(5).setCellValue(tempLoan.getClient_name());
				if (tempLoan.getDob() != null) {
								Cell clientCell = loanRow.createCell(9);
								clientCell.setCellValue(tempLoan.getDob());
								clientCell.setCellStyle(style1);
				}
				
				if (tempLoan.getAge() != 0){
					loanRow.createCell(10).setCellValue(tempLoan.getAge());
				}
				
				if (tempLoan.getAge() != 0 && tempLoan.getDob() != null) {
					Cell active = loanRow.createCell(11);
					active.setCellValue(tempLoan.getActivateDate());
					active.setCellStyle(style1);
				}
				
				if (tempLoan.getGender() != null) {

					if (tempLoan.getGender().equals("Female")) {
						loanRow.createCell(12).setCellValue("F");
					} else {
						loanRow.createCell(12).setCellValue("M");

					}

				}
				
				if (tempLoan.getMaritalStatus() != null) {
					switch (tempLoan.getMaritalStatus()) {
					case "Married":
						loanRow.createCell(13).setCellValue("M01");
						break;
					case "Unmarried":
						loanRow.createCell(13).setCellValue("M05");
						break;
					case "Deprived":
						loanRow.createCell(13).setCellValue("M06");
						break;
					case "Divorcee":
						loanRow.createCell(13).setCellValue("M03");
						break;
					case "Widow":
						loanRow.createCell(13).setCellValue("M04");
						break;

					}
				}
				
				final FamilyDetailsMapper fm = new FamilyDetailsMapper();
				final List<FamilyDetailData> familyDetails = this.jdbcTemplate.query(fm.schema(), fm,
						new Object[] { tempLoan.getClientID() });
				if (familyDetails != null) {
					int familySize = familyDetails.size();
					if (familySize > 4)
						familySize = 4;
					int fdc = 16;
					if (familySize != 0) {
						for (int f = 0; f < familySize; f++) {
							FamilyDetailData fdetail = familyDetails.get(f);
							if (fdetail.getRelationType() == 79) {
								loanRow.createCell(14).setCellValue(fdetail.getName());
//								loanRow.createCell(18).setCellValue(fdetail.getName());
//								loanRow.createCell(20).setCellValue(fdetail.getName());
//								loanRow.createCell(22).setCellValue(fdetail.getName());
								if (tempLoan.getGender() != null && tempLoan.getGender().contains("Female")) {
									loanRow.createCell(15).setCellValue("K02");
									
								} else if (tempLoan.getGender() != null
										&& tempLoan.getGender().contains("Male")) {
									loanRow.createCell(15).setCellValue("K06");
								}
							} else {
								loanRow.createCell(fdc).setCellValue(fdetail.getName());

								if (fdetail.getRelationType() == 113) {
									loanRow.createCell(fdc + 1).setCellValue("K04");
								} else if (fdetail.getRelationType() == 99) {
									loanRow.createCell(fdc + 1).setCellValue("K05");
								} else if (fdetail.getRelationType() == 100) {
									loanRow.createCell(fdc + 1).setCellValue("K07");
								} else if (fdetail.getRelationType() == 112) {
									loanRow.createCell(fdc + 1).setCellValue("K03");
								} else if (fdetail.getRelationType() == 77) {
									loanRow.createCell(fdc + 1).setCellValue("K01");
								} else {
									loanRow.createCell(fdc + 1).setCellValue("K15");
								}

								fdc = fdc + 2;
							}
						}
					}
				}
				
				loanRow.createCell(24).setCellValue(tempLoan.getNomineeName());
				
				if (tempLoan.getNomineeRelation() == 113) {
					loanRow.createCell(25).setCellValue("K04");
				} else if (tempLoan.getNomineeRelation() == 99) {
					loanRow.createCell(25).setCellValue("K05");
				} else if (tempLoan.getNomineeRelation() == 100) {
					loanRow.createCell(25).setCellValue("K07");
				} else if (tempLoan.getNomineeRelation() == 112) {
					loanRow.createCell(25).setCellValue("K03");
				} else if (tempLoan.getNomineeRelation() == 77) {
					loanRow.createCell(25).setCellValue("K01");
				} else {
					loanRow.createCell(25).setCellValue("K15");
				}
				
				if (tempLoan.getNomineeAge() != 0)
					loanRow.createCell(26).setCellValue(tempLoan.getNomineeAge());
				
				loanRow.createCell(27).setCellValue(tempLoan.getVoterId());
				loanRow.createCell(28).setCellValue(tempLoan.getAadhaarId());
				loanRow.createCell(29).setCellValue(tempLoan.getPancardId());
				loanRow.createCell(38).setCellValue(tempLoan.getMobileNo());
				loanRow.createCell(44).setCellValue(tempLoan.getAccHolderName());
				loanRow.createCell(45).setCellValue(tempLoan.getBankName());
				loanRow.createCell(46).setCellValue(tempLoan.getBankAccount());
				
				loanRow.createCell(47).setCellValue(tempLoan.getOccupation());
				if (tempLoan.getOccupation() != null) {
					loanRow.createCell(48).setCellValue(tempLoan.getMonthlyincome());
					loanRow.createCell(49).setCellValue(tempLoan.getMonthlyexpenses());
				}
				
				if (tempLoan.getReligion() != null) {
					switch (tempLoan.getReligion()) {
					case "Hindu":
						loanRow.createCell(50).setCellValue("R01");
						break;
					case "Muslim":
						loanRow.createCell(50).setCellValue("R02");
						break;
					case "Christian":
						loanRow.createCell(50).setCellValue("R03");
						break;
					case "Sikh":
						loanRow.createCell(50).setCellValue("R04");
						break;
					case "Buddhist":
						loanRow.createCell(50).setCellValue("R05");
						break;
					case "Jain":
						loanRow.createCell(50).setCellValue("R06");
						break;
					case "Bahai":
						loanRow.createCell(50).setCellValue("R07");
						break;
					case "Others":
						loanRow.createCell(50).setCellValue("R08");
						break;
					case "Religion not stated":
						loanRow.createCell(50).setCellValue("R09");
						break;
					}
				}
				
				loanRow.createCell(51).setCellValue(tempLoan.getCaste());
				loanRow.createCell(55).setCellValue("ADRCRD");
				loanRow.createCell(56)
				.setCellValue(tempLoan.getAddressLine1() + tempLoan.getAddressLine2()
						+ tempLoan.getAddressLine3() + tempLoan.getStreet()
						+ tempLoan.getTownVillage() + tempLoan.getCity());
				
				String stateCodeValue = "";
				if (tempLoan.getStateName() != null
						&& stateCodeAndName.containsKey(tempLoan.getStateName())) {
					stateCodeValue = stateCodeAndName.get(tempLoan.getStateName());
				}
				loanRow.createCell(57).setCellValue(stateCodeValue);
				loanRow.createCell(58).setCellValue(tempLoan.getPostalCode());
				loanRow.createCell(59)
						.setCellValue(tempLoan.getAddressLine1() + tempLoan.getAddressLine2()
								+ tempLoan.getAddressLine3() + tempLoan.getStreet()
								+ tempLoan.getTownVillage() + tempLoan.getCity());
				loanRow.createCell(60).setCellValue(stateCodeValue);
				loanRow.createCell(61).setCellValue(tempLoan.getPostalCode());
				
				loanRow.createCell(63).setCellValue("ACTCRD");
				loanRow.createCell(64).setCellValue(tempLoan.getLoanAcc());
				loanRow.createCell(65).setCellValue(tempLoan.getLoanAcc());
				loanRow.createCell(66).setCellValue(tempLoan.getOfficeExtrenal());
				loanRow.createCell(67).setCellValue(tempLoan.getOfficeExtrenal());
				loanRow.createCell(68).setCellValue(tempLoan.getLoanOfficer());

				Cell dateCell1 = loanRow.createCell(69);
				dateCell1.setCellValue(asDate);
				dateCell1.setCellStyle(style1);
				
				if (tempLoan.getLoanType() != null) {
					if (tempLoan.getLoanType().equals("GROUP")) {
						loanRow.createCell(70).setCellValue("T01");
					} else if (tempLoan.getLoanType().equals("JLG")) {
						loanRow.createCell(70).setCellValue("T02");
					} else {
						loanRow.createCell(70).setCellValue("T03");
					}
				}
				
				if (tempLoan.getGroupExternal() != null) {
					loanRow.createCell(71).setCellValue(tempLoan.getGroupExternal());
				} else {
					loanRow.createCell(71).setCellValue(tempLoan.getClientExternal());
				}
				String str = tempLoan.getLoanExternal();
				
				loanRow.createCell(72).setCellValue(tempLoan.getLoanCycle());

				if (tempLoan.getClosedDate() != null) {
					Date cdate = tempLoan.getClosedDate();
					if (cdate.after(asDate)) {
						cdate = tempLoan.setClosedDate(null);
					}

				}

				if (tempLoan.getOverDueAmount() != null && tempLoan.getOverDueAmount().floatValue() > 0) {
					loanRow.createCell(74).setCellValue("S05");
				} else {
					if (tempLoan.getClosedDate() != null) {
						loanRow.createCell(74).setCellValue("S07");
					} else {
						loanRow.createCell(74).setCellValue("S04");
					}
				}
				Cell dateCell = loanRow.createCell(75);
				dateCell.setCellValue(tempLoan.getSubmittedDate());
				dateCell.setCellStyle(style1);
				
				
				
				//        	DateFormat dateFormat2 = new SimpleDateFormat("yyyy-MM-dd");
//            String strDate2= dateFormat2.format(tempLoan.getDisbursedDate());
				Cell cell1 = loanRow.createCell(76);
				cell1.setCellValue(tempLoan.getDisbursedDate());
				cell1.setCellStyle(style1);
//        	DateFormat dateFormat3 = new SimpleDateFormat("yyyy-MM-dd");
//            String strDate3 = dateFormat3.format(tempLoan.getDisbursedDate());
				Cell cell = loanRow.createCell(77);
				cell.setCellValue(tempLoan.getDisbursedDate());
				cell.setCellStyle(style1);

				if (tempLoan.getTransactionDate() != null) {
					Cell Cell17 = loanRow.createCell(79);
					Cell17.setCellValue(tempLoan.getTransactionDate());
					Cell17.setCellStyle(style1);
				}

				if (tempLoan.getClosedDate() != null) {


					Cell cell16 = loanRow.createCell(78);
					cell16.setCellValue(tempLoan.getClosedDate());
					cell16.setCellStyle(style1);

				}
				loanRow.createCell(73).setCellValue(tempLoan.getLoanPurpose());
				loanRow.createCell(80).setCellValue(tempLoan.getAppliedAmount().floatValue());
				loanRow.createCell(81).setCellValue(tempLoan.getApprovedAmount().floatValue());
				loanRow.createCell(82).setCellValue(tempLoan.getDisbAmount().floatValue());
				loanRow.createCell(83).setCellValue(tempLoan.getInstallmentNumber());

				if (tempLoan.getTermfrequency().contains("Months")) {
					loanRow.createCell(84).setCellValue("F03");
				} else if (tempLoan.getTermfrequency().contains("Weeks")) {
					loanRow.createCell(84).setCellValue("F01");
				}
				loanRow.createCell(85).setCellValue(tempLoan.getInstallmentAmount().floatValue());
				loanRow.createCell(86).setCellValue(tempLoan.getOutBalance().floatValue());
				if (tempLoan.getOverDueAmount() != null)
					loanRow.createCell(87).setCellValue(tempLoan.getOverDueAmount().floatValue());
				else
					loanRow.createCell(87).setCellValue(0);
				loanRow.createCell(88).setCellValue(tempLoan.getDaysOverDUe());

				loanRow.createCell(89).setCellValue(tempLoan.getWriteoffAmount());
				if (tempLoan.getWriteoffAmount() != 0) {
					loanRow.createCell(90).setCellValue(tempLoan.getWriteoffDate());
					loanRow.createCell(91).setCellValue(tempLoan.getWriteoffReason());
				}

								

			} catch (Exception e) {
				loanRow.createCell(6).setCellValue("Error Occured" + e.getMessage());
			}
		}
		accountSheet.autoSizeColumn(0);
		accountSheet.autoSizeColumn(1);
		accountSheet.autoSizeColumn(2);
		accountSheet.autoSizeColumn(3);
		accountSheet.autoSizeColumn(4);
		accountSheet.autoSizeColumn(5);
		accountSheet.autoSizeColumn(6);
		accountSheet.autoSizeColumn(7);
		accountSheet.autoSizeColumn(8);
		accountSheet.autoSizeColumn(9);
		accountSheet.autoSizeColumn(10);
		accountSheet.autoSizeColumn(11);
		accountSheet.autoSizeColumn(12);
		accountSheet.autoSizeColumn(13);
		accountSheet.autoSizeColumn(14);
		accountSheet.autoSizeColumn(15);
		accountSheet.autoSizeColumn(16);
		accountSheet.autoSizeColumn(17);
		accountSheet.autoSizeColumn(18);
		accountSheet.autoSizeColumn(19);
		accountSheet.autoSizeColumn(20);
		accountSheet.autoSizeColumn(21);
		accountSheet.autoSizeColumn(22);
		accountSheet.autoSizeColumn(23);
		accountSheet.autoSizeColumn(24);
		accountSheet.autoSizeColumn(25);
		accountSheet.autoSizeColumn(26);
		accountSheet.autoSizeColumn(27);
		accountSheet.autoSizeColumn(28);
		accountSheet.autoSizeColumn(29);
		accountSheet.autoSizeColumn(30);
		accountSheet.autoSizeColumn(31);
		accountSheet.autoSizeColumn(32);
		accountSheet.autoSizeColumn(33);
		accountSheet.autoSizeColumn(34);
		accountSheet.autoSizeColumn(35);
		accountSheet.autoSizeColumn(36);
		accountSheet.autoSizeColumn(37);
		accountSheet.autoSizeColumn(38);
		accountSheet.autoSizeColumn(39);
		accountSheet.autoSizeColumn(40);
		accountSheet.autoSizeColumn(41);
		accountSheet.autoSizeColumn(42);
		accountSheet.autoSizeColumn(43);
		accountSheet.autoSizeColumn(44);
		accountSheet.autoSizeColumn(45);
		accountSheet.autoSizeColumn(46);
		accountSheet.autoSizeColumn(47);
		accountSheet.autoSizeColumn(48);
		accountSheet.autoSizeColumn(49);
		
		accountSheet.autoSizeColumn(50);
		accountSheet.autoSizeColumn(51);
		accountSheet.autoSizeColumn(52);
		accountSheet.autoSizeColumn(53);
		accountSheet.autoSizeColumn(54);
		accountSheet.autoSizeColumn(55);
		accountSheet.autoSizeColumn(56);
		accountSheet.autoSizeColumn(57);
		accountSheet.autoSizeColumn(58);
		accountSheet.autoSizeColumn(59);
		accountSheet.autoSizeColumn(60);
		accountSheet.autoSizeColumn(61);
		accountSheet.autoSizeColumn(62);
		accountSheet.autoSizeColumn(63);
		accountSheet.autoSizeColumn(64);
		accountSheet.autoSizeColumn(65);
		accountSheet.autoSizeColumn(66);
		accountSheet.autoSizeColumn(67);
		accountSheet.autoSizeColumn(68);
		accountSheet.autoSizeColumn(69);
		accountSheet.autoSizeColumn(70);
		accountSheet.autoSizeColumn(71);
		accountSheet.autoSizeColumn(72);
		accountSheet.autoSizeColumn(73);
		accountSheet.autoSizeColumn(74);
		accountSheet.autoSizeColumn(75);
		accountSheet.autoSizeColumn(76);
		accountSheet.autoSizeColumn(77);
		accountSheet.autoSizeColumn(78);
		accountSheet.autoSizeColumn(79);
		accountSheet.autoSizeColumn(80);
		accountSheet.autoSizeColumn(81);
		accountSheet.autoSizeColumn(82);
		accountSheet.autoSizeColumn(83);
		accountSheet.autoSizeColumn(84);
		accountSheet.autoSizeColumn(85);
		accountSheet.autoSizeColumn(86);
		accountSheet.autoSizeColumn(87);
		accountSheet.autoSizeColumn(88);
		accountSheet.autoSizeColumn(89);
		accountSheet.autoSizeColumn(90);
		accountSheet.autoSizeColumn(91);
		accountSheet.autoSizeColumn(92);
		accountSheet.autoSizeColumn(93);
		accountSheet.autoSizeColumn(94);
		accountSheet.autoSizeColumn(95);
		accountSheet.autoSizeColumn(96);
		accountSheet.autoSizeColumn(97);
		accountSheet.autoSizeColumn(98);
		accountSheet.autoSizeColumn(99);


		return workbook;
	}
	
	
	private static final class HighmarkDetailsMapper implements RowMapper<HighmarkDetailData> {

		public String schema() {
			return "select cva2.code_value as addressType,ca.client_id as client_id,addr.id as id,addr.street as street,"
					+ "addr.address_line_1 as address_line_1,addr.address_line_2 as address_line_2,addr.address_line_3 as address_line_3,"
					+ "addr.town_village as town_village,addr.city as city,addr.county_district as county_district,cva.code_value as "
					+ "state_name,cva1.code_value as country_name,addr.postal_code as postal_code,c.account_no as clientExternal , "
					+ "c.id as clientID, l.account_no as loanAcc, ifnull( ifnull( l.loan_counter, l.loan_product_counter ), 1 ) as"
					+ " loanCycle, o.external_id as officeExternal, re.enum_value as loanType, g.account_no as groupExternal,"
					+ "l.external_id as loanExternal, mcv.code_value as loanPurpose, l.submittedon_date as submittedDate,"
					+ "ifnull(max(date(ldd.disbursedon_date)),date(l.disbursedon_date)) as disbursedDate, l.closedon_date as closedDate,"
					+ "l.principal_amount_proposed as appliedAmount, l.approved_principal as approvedAmount, SUM(if( l.product_id in (39),"
					+ "ldd.principal, l.`principal_amount`)) as disbAmount, l.number_of_repayments as installmentNumber,"
					+ "re1.enum_value as termfrequency, ( SUM(if( l.product_id in (39), ldd.principal, l.`principal_amount`)) - "
					+ "`getPrincipalReceived`(l.id, ?) ) as outBalance, c.account_no as clientExternal, `getPrincipalOverdue`(l.id, ?)"
					+ " as overDue, `getNoOfDaysOverDue`(l.id, ?) as noOfDaysDue, (ifnull(mlr.principal_amount,0)+ "
					+ "ifnull(mlr.interest_amount,0)) as installmentAmount, getTransactionDate(l.id, ?) as transactionDate, "
					+ "(l.principal_writtenoff_derived + l.interest_writtenoff_derived) as writeoff_amount, cv.code_value as"
					+ " writeoff_reason, l.writtenoffon_date as writeoff_date,getLoanOfficerName(l.loan_officer_id) as loanOfficer ,ROUND( getInsurance(l.id), 2 ) as Insurance ,"
					+ "c.account_no as external, getAge(c.submittedon_date,c.date_of_birth) as age, "
					+ "o.external_id as officeExternal,c.activation_date as activateDate,c.display_name as name,c.mobile_no as mobileNo,"
					+ "c.date_of_birth as dob,mcvg.code_value as gender, mcs.code_value as marital_status ,mg.account_no as groupExternal,"
					+ "nd.`Name` as nomineeName, nd.`Age` as nomineeAge,mcv1.id as nomineeRelation,bd.`Bank Name` as bankName,"
					+ "bd.`Account Number` as bankAccount, bd.`Account Holder Name` as accHolderName,`getVoterId`(c.id) as voterId,"
					+ "`getAadhaarId`(c.id) as aadhaarId,`getPanCardId`(c.id) as pancardId ,mcvo.code_value as 'Occupation',"
					+ "ifnull(ROUND(iae.`Income of the Family`,2),0) as 'monthlyincome', ifnull(ROUND(iae.`Expenditure of the Family`,2),0) as 'monthlyexpenses',"
					+ " mcvr.code_value as 'Religion',mcvc.code_value as 'Caste'"
					
					+ " from m_loan l join m_client c on c.id = l.client_id  "
					+ " left join m_office o on o.id = c.office_id  "
					+ " left join r_enum_value re on re.enum_id = l.loan_type_enum and re.enum_name = 'account_type_type_enum' "
					+ " left join m_group g on g.id = l.group_id  left join m_code_value mcv on mcv.id = l.loanpurpose_cv_id  "
					+ " left join r_enum_value re1 on re1.enum_id = l.repayment_period_frequency_enum and  re1.enum_name = 'repayment_period_frequency_enum' "
					+ " left join m_loan_repayment_schedule mlr on mlr.loan_id = l.id  and mlr.installment = 1 "
					+ " left join m_loan_arrears_aging mla on mla.loan_id = l.id "
					+ " left join m_code_value cv  on cv.id = l.writeoff_reason_cv_id "
					+ " left join m_loan_disbursement_detail ldd on ldd.loan_id = l.id  and ldd.disbursedon_date is not null "
					+ " left join m_client_address ca  on ca.id =(select max(ca1.id) from m_client_address ca1  where ca1.client_id=c.id)"
					+ " left join m_address addr on addr.id= ca.address_id"
					+ " left join m_code_value cva on addr.state_province_id=cva.id  "
					+ " left join  m_code_value cva1 on addr.country_id=cva1.id  "
					+ " left join m_code_value cva2 on ca.address_type_id=cva2.id and ca.is_active=1 "
					+ " left join m_code_value mcvg on mcvg.id=c.gender_cv_id "
					+ " left join m_group_client mgc on mgc.client_id= c.id  "
					+ " left join m_group mg on mg.id= mgc.group_id  "
					+ " left join `bank detail` bd on bd.id=  (select max(bd1.id) from `bank detail` bd1  where bd1.client_id=c.id)"
					+ " left join `nominee detail` nd on nd.id =  (select max(nd1.id) from `nominee detail` nd1  where nd1.client_id=c.id) "
					+ " left join m_code_value mcv1 on mcv1.id=nd.`Relationship Type` "
					+ " left join `social status` ss  on ss.id =  (select max(ssj.id) from `social status` ssj  where ssj.client_id=c.id)  "
					+ " left join m_code_value mcs on mcs.id = ss.`Marital Status`  "
					+ " left join `income and expense` iae on iae.id=(select max(iae1.id) from `income and expense` iae1  where iae1.client_id=c.id) "
					+ " left join m_code_value mcvo on mcvo.id=iae.Occupation  "
					+ " left join `social status` ss12 on ss12.id=(select max(ss1.id) from `social status` ss1  where ss1.client_id=c.id) "
					+ " left join m_code_value mcvr on ss12.Religion=mcvr.id  "
					+ " left join `social status` ss11 on ss11.id=(select max(ss2.id) from `social status` ss2  where ss2.client_id=c.id) "
					+ " left join m_code_value mcvc on ss11.Caste=mcvc.id "
					+ " where l.loan_status_id not in(100, 200, 500)  and "
					+ " ifnull(date(ldd.disbursedon_date),date(l.disbursedon_date)) <= ? AND ( ( ifnull(l.closedon_date, ?) "
					+ " between ? and ? ) or ( if( l.closedon_date > ?, ?, l.closedon_date ) between ? and ? ) ) group by l.id";
		}

		@Override
		public HighmarkDetailData mapRow(final ResultSet rs, @SuppressWarnings("unused") final int rowNum)
				throws SQLException {

			final long clientID = rs.getInt("clientID");
			final String loanAcc = rs.getString("loanAcc");
			final String officeExtrenal = rs.getString("officeExternal");
			final String loanType = rs.getString("loanType");
			final String loanExternal = rs.getString("loanExternal");
			final String loanPurpose = rs.getString("loanPurpose");
			final String groupExternal = rs.getString("groupExternal");
			final Date submittedDate = rs.getDate("submittedDate");
			final Date disbursedDate = rs.getDate("disbursedDate");
			final Date closedDate = rs.getDate("closedDate");
			final BigDecimal appliedAmount = rs.getBigDecimal("appliedAmount");
			final BigDecimal approvedAmount = rs.getBigDecimal("approvedAmount");
			final BigDecimal disbAmount = rs.getBigDecimal("disbAmount");
			final int installmentNumber = rs.getInt("installmentNumber");
			final String termfrequency = rs.getString("termfrequency");
			final BigDecimal outBalance = rs.getBigDecimal("outBalance");
			final String clientExternal = rs.getString("clientExternal");
			final BigDecimal installmentAmount = rs.getBigDecimal("installmentAmount");
			final BigDecimal overDueAmount = rs.getBigDecimal("overDue");
			final int daysOverDUe = rs.getInt("noOfDaysDue");
			final long loanCycle = rs.getLong("loanCycle");
			final Date transactionDate = rs.getDate("transactionDate");

			final long writeoffAmount = rs.getLong("writeoff_amount");
			final String writeoffReason = rs.getString("writeoff_reason");
			final Date writeoffDate = rs.getDate("writeoff_date");
			
			
			final long client_id = rs.getLong("clientId");
			
			final String client_name = rs.getString("name");
			final String mobileNo = rs.getString("mobileNo");
			final String gender = rs.getString("gender");
			final String maritalStatus = rs.getString("marital_status");
			
			final String nomineeName = rs.getString("nomineeName");
			final int nomineeAge = rs.getInt("nomineeAge");
			final int nomineeRelation = rs.getInt("nomineeRelation");
			final String bankName = rs.getString("bankName");
			final String bankAccount = rs.getString("bankAccount");
			final String accHolderName = rs.getString("accHolderName");
			final String voterId = rs.getString("voterId");
			final String aadhaarId = rs.getString("aadhaarId");
			final String pancardId = rs.getString("pancardId");
			final Date activateDate = rs.getDate("activateDate");
			final Date dob = rs.getDate("dob");
			final int age = rs.getInt("age");

			final String occupation = rs.getString("Occupation");
			final long monthlyincome = rs.getLong("monthlyincome");
			final long monthlyexpenses = rs.getLong("monthlyexpenses");
			final String religion = rs.getString("Religion");
			final String caste = rs.getString("Caste");
			
			final String addressType = rs.getString("addressType");
			final long addressId = rs.getLong("id");

			

			final String street = rs.getString("street");

			final String address_line_1 = rs.getString("address_line_1");

			final String address_line_2 = rs.getString("address_line_2");

			final String address_line_3 = rs.getString("address_line_3");

			final String town_village = rs.getString("town_village");

			final String city = rs.getString("city");

			final String county_district = rs.getString("county_district");

			final String country_name = rs.getString("country_name");

			final String state_name = rs.getString("state_name");

			final String postal_code = rs.getString("postal_code");

			final String external = rs.getString("clientExternal");
			
			final String loanOfficer = rs.getString("loanOfficer");

			return new HighmarkDetailData(clientID, loanAcc, officeExtrenal, loanType, loanExternal, loanPurpose,
					groupExternal, submittedDate, disbursedDate, closedDate, appliedAmount, approvedAmount, disbAmount,
					installmentNumber, termfrequency, outBalance, clientExternal, installmentAmount, overDueAmount,
					daysOverDUe, loanCycle, transactionDate, writeoffAmount, writeoffReason, writeoffDate,
					nomineeAge,client_name,mobileNo,gender,maritalStatus,nomineeName,nomineeRelation,bankName,
					bankAccount,accHolderName,voterId,aadhaarId,pancardId,activateDate,dob,age,occupation,monthlyincome,
					monthlyexpenses,religion,caste,addressType,addressId,street,address_line_1,address_line_2,
					address_line_3,town_village,city,county_district,country_name,state_name,postal_code,loanOfficer);

		}
	}
}