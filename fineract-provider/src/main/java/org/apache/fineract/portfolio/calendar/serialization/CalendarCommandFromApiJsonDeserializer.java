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
package org.apache.fineract.portfolio.calendar.serialization;

import java.lang.reflect.Type;
import java.sql.Time;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.fineract.infrastructure.core.data.ApiParameterError;
import org.apache.fineract.infrastructure.core.data.DataValidatorBuilder;
import org.apache.fineract.infrastructure.core.exception.InvalidJsonException;
import org.apache.fineract.infrastructure.core.exception.PlatformApiDataValidationException;
import org.apache.fineract.infrastructure.core.serialization.AbstractFromApiJsonDeserializer;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.apache.fineract.portfolio.calendar.CalendarConstants.CALENDAR_SUPPORTED_PARAMETERS;
import org.apache.fineract.portfolio.calendar.command.CalendarCommand;
import org.apache.fineract.portfolio.calendar.domain.CalendarEntityType;
import org.apache.fineract.portfolio.calendar.domain.CalendarFrequencyType;
import org.apache.fineract.portfolio.calendar.domain.CalendarRemindBy;
import org.apache.fineract.portfolio.calendar.domain.CalendarWeekDaysType;
import org.apache.fineract.portfolio.calendar.service.CalendarUtils;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
//import org.joda.time.LocalTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;

@Component
public class CalendarCommandFromApiJsonDeserializer extends AbstractFromApiJsonDeserializer<CalendarCommand> {

    /**
     * The parameters supported for this command.
     */
    private final Set<String> supportedParameters = CALENDAR_SUPPORTED_PARAMETERS.getAllValues();

    private final FromJsonHelper fromApiJsonHelper;

    @Autowired
    public CalendarCommandFromApiJsonDeserializer(final FromJsonHelper fromApiJsonHelper) {
        this.fromApiJsonHelper = fromApiJsonHelper;
    }

    @Override
    public CalendarCommand commandFromApiJson(final String json) {
        if (StringUtils.isBlank(json)) { throw new InvalidJsonException(); }

        final Type typeOfMap = new TypeToken<Map<String, Object>>() {}.getType();
        this.fromApiJsonHelper.checkForUnsupportedParameters(typeOfMap, json, this.supportedParameters);

        final JsonElement element = this.fromApiJsonHelper.parse(json);
        final String title = this.fromApiJsonHelper.extractStringNamed(CALENDAR_SUPPORTED_PARAMETERS.TITLE.getValue(), element);
        final String description = this.fromApiJsonHelper.extractStringNamed(CALENDAR_SUPPORTED_PARAMETERS.DESCRIPTION.getValue(), element);
        final String location = this.fromApiJsonHelper.extractStringNamed(CALENDAR_SUPPORTED_PARAMETERS.LOCATION.getValue(), element);
        final LocalDate startDate = this.fromApiJsonHelper.extractLocalDateNamed(CALENDAR_SUPPORTED_PARAMETERS.START_DATE.getValue(),
                element);
        final LocalDate endDate = this.fromApiJsonHelper.extractLocalDateNamed(CALENDAR_SUPPORTED_PARAMETERS.END_DATE.getValue(), element);
        final LocalDate createdDate = this.fromApiJsonHelper.extractLocalDateNamed(CALENDAR_SUPPORTED_PARAMETERS.CREATED_DATE.getValue(),
                element);
        final Integer duration = this.fromApiJsonHelper.extractIntegerSansLocaleNamed(CALENDAR_SUPPORTED_PARAMETERS.DURATION.getValue(),
                element);
        final Integer typeId = this.fromApiJsonHelper.extractIntegerSansLocaleNamed(CALENDAR_SUPPORTED_PARAMETERS.TYPE_ID.getValue(),
                element);
        final boolean repeating = this.fromApiJsonHelper.extractBooleanNamed(CALENDAR_SUPPORTED_PARAMETERS.REPEATING.getValue(), element);
        final Integer remindById = this.fromApiJsonHelper.extractIntegerSansLocaleNamed(
                CALENDAR_SUPPORTED_PARAMETERS.REMIND_BY_ID.getValue(), element);
        final Integer firstReminder = this.fromApiJsonHelper.extractIntegerSansLocaleNamed(
                CALENDAR_SUPPORTED_PARAMETERS.FIRST_REMINDER.getValue(), element);
        final Integer secondReminder = this.fromApiJsonHelper.extractIntegerSansLocaleNamed(
                CALENDAR_SUPPORTED_PARAMETERS.SECOND_REMINDER.getValue(), element);

        return new CalendarCommand(title, description, location, startDate, endDate, createdDate, duration, typeId, repeating, remindById,
                firstReminder, secondReminder);
    }

    public void validateForCreate(final String json) {

        if (StringUtils.isBlank(json)) { throw new InvalidJsonException(); }

        final Type typeOfMap = new TypeToken<Map<String, Object>>() {}.getType();
        this.fromApiJsonHelper.checkForUnsupportedParameters(typeOfMap, json, this.supportedParameters);
        final JsonElement element = this.fromApiJsonHelper.parse(json);

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();

        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors).resource("calendar");

        final String title = this.fromApiJsonHelper.extractStringNamed(CALENDAR_SUPPORTED_PARAMETERS.TITLE.getValue(), element);
        baseDataValidator.reset().parameter(CALENDAR_SUPPORTED_PARAMETERS.TITLE.getValue()).value(title).notBlank()
                .notExceedingLengthOf(50);

        if (this.fromApiJsonHelper.parameterExists(CALENDAR_SUPPORTED_PARAMETERS.DESCRIPTION.getValue(), element)) {
            final String description = this.fromApiJsonHelper.extractStringNamed(CALENDAR_SUPPORTED_PARAMETERS.DESCRIPTION.getValue(),
                    element);
            baseDataValidator.reset().parameter(CALENDAR_SUPPORTED_PARAMETERS.DESCRIPTION.getValue()).value(description).ignoreIfNull()
                    .notExceedingLengthOf(100);
        }

        if (this.fromApiJsonHelper.parameterExists(CALENDAR_SUPPORTED_PARAMETERS.LOCATION.getValue(), element)) {
            final String location = this.fromApiJsonHelper.extractStringNamed(CALENDAR_SUPPORTED_PARAMETERS.LOCATION.getValue(), element);
            baseDataValidator.reset().parameter(CALENDAR_SUPPORTED_PARAMETERS.LOCATION.getValue()).value(location).ignoreIfNull()
                    .notExceedingLengthOf(50);
        }

        final String startDateStr = this.fromApiJsonHelper.extractStringNamed(CALENDAR_SUPPORTED_PARAMETERS.START_DATE.getValue(), element);
        baseDataValidator.reset().parameter(CALENDAR_SUPPORTED_PARAMETERS.START_DATE.getValue()).value(startDateStr).notBlank();

        if (!StringUtils.isBlank(startDateStr)) {
            final LocalDate startDate = this.fromApiJsonHelper.extractLocalDateNamed(CALENDAR_SUPPORTED_PARAMETERS.START_DATE.getValue(),
                    element);
            baseDataValidator.reset().parameter(CALENDAR_SUPPORTED_PARAMETERS.START_DATE.getValue()).value(startDate).notNull();
        }

        if (this.fromApiJsonHelper.parameterExists(CALENDAR_SUPPORTED_PARAMETERS.END_DATE.getValue(), element)) {
            final String endDateStr = this.fromApiJsonHelper.extractStringNamed(CALENDAR_SUPPORTED_PARAMETERS.END_DATE.getValue(), element);
            baseDataValidator.reset().parameter(CALENDAR_SUPPORTED_PARAMETERS.END_DATE.getValue()).value(endDateStr).notBlank();

            final LocalDate endDate = this.fromApiJsonHelper.extractLocalDateNamed(CALENDAR_SUPPORTED_PARAMETERS.END_DATE.getValue(),
                    element);
            baseDataValidator.reset().parameter(CALENDAR_SUPPORTED_PARAMETERS.END_DATE.getValue()).value(endDate).notNull();
        }

        if (this.fromApiJsonHelper.parameterExists(CALENDAR_SUPPORTED_PARAMETERS.DURATION.getValue(), element)) {
            final Integer duration = this.fromApiJsonHelper.extractIntegerSansLocaleNamed(
                    CALENDAR_SUPPORTED_PARAMETERS.DURATION.getValue(), element);
            baseDataValidator.reset().parameter(CALENDAR_SUPPORTED_PARAMETERS.DURATION.getValue()).value(duration).ignoreIfNull();
        }

        final Integer typeId = this.fromApiJsonHelper.extractIntegerSansLocaleNamed(CALENDAR_SUPPORTED_PARAMETERS.TYPE_ID.getValue(),
                element);
        baseDataValidator.reset().parameter(CALENDAR_SUPPORTED_PARAMETERS.TYPE_ID.getValue()).value(typeId).notNull()
                .inMinMaxRange(CalendarEntityType.getMinValue(), CalendarEntityType.getMaxValue());

        if (this.fromApiJsonHelper.parameterExists(CALENDAR_SUPPORTED_PARAMETERS.REPEATING.getValue(), element)) {
            // FIXME - Throws NullPointerException when boolean value is null
            final boolean repeating = this.fromApiJsonHelper.extractBooleanNamed(CALENDAR_SUPPORTED_PARAMETERS.REPEATING.getValue(),
                    element);
            baseDataValidator.reset().parameter(CALENDAR_SUPPORTED_PARAMETERS.REPEATING.getValue()).value(repeating).notNull();

            if (repeating) {
                final Integer frequency = this.fromApiJsonHelper.extractIntegerSansLocaleNamed(
                        CALENDAR_SUPPORTED_PARAMETERS.FREQUENCY.getValue(), element);
                baseDataValidator.reset().parameter(CALENDAR_SUPPORTED_PARAMETERS.FREQUENCY.getValue()).value(frequency).notBlank()
                        .inMinMaxRange(CalendarFrequencyType.getMinValue(), CalendarFrequencyType.getMaxValue());

                if (this.fromApiJsonHelper.parameterExists(CALENDAR_SUPPORTED_PARAMETERS.INTERVAL.getValue(), element)) {
                    final Integer interval = this.fromApiJsonHelper.extractIntegerSansLocaleNamed(
                            CALENDAR_SUPPORTED_PARAMETERS.INTERVAL.getValue(), element);
                    baseDataValidator.reset().parameter(CALENDAR_SUPPORTED_PARAMETERS.INTERVAL.getValue()).value(interval).notNull()
                            .integerGreaterThanZero();
                }
                if (CalendarFrequencyType.fromInt(frequency).isWeekly()) {
                    final Integer repeatsOnDay = this.fromApiJsonHelper.extractIntegerSansLocaleNamed(
                            CALENDAR_SUPPORTED_PARAMETERS.REPEATS_ON_DAY.getValue(), element);
                    baseDataValidator.reset().parameter(CALENDAR_SUPPORTED_PARAMETERS.REPEATS_ON_DAY.getValue()).value(repeatsOnDay)
                            .notBlank().inMinMaxRange(CalendarWeekDaysType.getMinValue(), CalendarWeekDaysType.getMaxValue());
                } else if (CalendarFrequencyType.fromInt(frequency).isMonthly()) {
                    CalendarUtils.validateNthDayOfMonthFrequency(baseDataValidator,
                            CALENDAR_SUPPORTED_PARAMETERS.REPEATS_ON_NTH_DAY_OF_MONTH.getValue(),
                            CALENDAR_SUPPORTED_PARAMETERS.REPEATS_ON_LAST_WEEKDAY_OF_MONTH.getValue(), element, this.fromApiJsonHelper);
                }
            }
        }

        if (this.fromApiJsonHelper.parameterExists(CALENDAR_SUPPORTED_PARAMETERS.REMIND_BY_ID.getValue(), element)) {
            final Integer remindById = this.fromApiJsonHelper.extractIntegerSansLocaleNamed(
                    CALENDAR_SUPPORTED_PARAMETERS.REMIND_BY_ID.getValue(), element);
            baseDataValidator.reset().parameter(CALENDAR_SUPPORTED_PARAMETERS.REMIND_BY_ID.getValue()).value(remindById).ignoreIfNull()
                    .inMinMaxRange(CalendarRemindBy.getMinValue(), CalendarRemindBy.getMaxValue());
        }

        if (this.fromApiJsonHelper.parameterExists(CALENDAR_SUPPORTED_PARAMETERS.FIRST_REMINDER.getValue(), element)) {
            final Integer firstReminder = this.fromApiJsonHelper.extractIntegerSansLocaleNamed(
                    CALENDAR_SUPPORTED_PARAMETERS.FIRST_REMINDER.getValue(), element);
            baseDataValidator.reset().parameter(CALENDAR_SUPPORTED_PARAMETERS.FIRST_REMINDER.getValue()).value(firstReminder)
                    .ignoreIfNull().integerGreaterThanZero();
        }

        if (this.fromApiJsonHelper.parameterExists(CALENDAR_SUPPORTED_PARAMETERS.SECOND_REMINDER.getValue(), element)) {
            final Integer secondReminder = this.fromApiJsonHelper.extractIntegerSansLocaleNamed(
                    CALENDAR_SUPPORTED_PARAMETERS.SECOND_REMINDER.getValue(), element);
            baseDataValidator.reset().parameter(CALENDAR_SUPPORTED_PARAMETERS.SECOND_REMINDER.getValue()).value(secondReminder)
                    .ignoreIfNull().integerGreaterThanZero();
        }
        if (this.fromApiJsonHelper.parameterExists(CALENDAR_SUPPORTED_PARAMETERS.MEETING_TIME.getValue(), element)) {
            final Time meetingTime = this.fromApiJsonHelper.extractLocalTime(CALENDAR_SUPPORTED_PARAMETERS.MEETING_TIME.getValue(),
                    element);
            baseDataValidator.reset().parameter(CALENDAR_SUPPORTED_PARAMETERS.MEETING_TIME.getValue()).value(meetingTime).ignoreIfNull();
        }
                          

        if (!dataValidationErrors.isEmpty()) { throw new PlatformApiDataValidationException("validation.msg.validation.errors.exist",
                "Validation errors exist.", dataValidationErrors); }
    }

    public void validateForUpdate(final String json) {

        if (StringUtils.isBlank(json)) { throw new InvalidJsonException(); }

        final Type typeOfMap = new TypeToken<Map<String, Object>>() {}.getType();
        this.fromApiJsonHelper.checkForUnsupportedParameters(typeOfMap, json, this.supportedParameters);
        final JsonElement element = this.fromApiJsonHelper.parse(json);

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();

        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors).resource("calendar");

        if (this.fromApiJsonHelper.parameterExists(CALENDAR_SUPPORTED_PARAMETERS.RESCHEDULE_BASED_ON_MEETING_DATES.getValue(), element)) {
            final Boolean rescheduleBasedOnMeetingDates = this.fromApiJsonHelper.extractBooleanNamed(
                    CALENDAR_SUPPORTED_PARAMETERS.RESCHEDULE_BASED_ON_MEETING_DATES.getValue(), element);
            baseDataValidator.reset().parameter(CALENDAR_SUPPORTED_PARAMETERS.RESCHEDULE_BASED_ON_MEETING_DATES.getValue())
                    .value(rescheduleBasedOnMeetingDates).validateForBooleanValue();
        }

        if (this.fromApiJsonHelper.parameterExists(CALENDAR_SUPPORTED_PARAMETERS.PRESENT_MEETING_DATE.getValue(), element)) {
            final String presentMeetingDate = this.fromApiJsonHelper.extractStringNamed(
                    CALENDAR_SUPPORTED_PARAMETERS.PRESENT_MEETING_DATE.getValue(), element);
            baseDataValidator.reset().parameter(CALENDAR_SUPPORTED_PARAMETERS.PRESENT_MEETING_DATE.getValue()).value(presentMeetingDate)
                    .notNull();
        }

        if (this.fromApiJsonHelper.parameterExists(CALENDAR_SUPPORTED_PARAMETERS.NEW_MEETING_DATE.getValue(), element)) {
            final String newMeetingDate = this.fromApiJsonHelper.extractStringNamed(
                    CALENDAR_SUPPORTED_PARAMETERS.NEW_MEETING_DATE.getValue(), element);
            baseDataValidator.reset().parameter(CALENDAR_SUPPORTED_PARAMETERS.NEW_MEETING_DATE.getValue()).value(newMeetingDate).notNull();
        }

        if (this.fromApiJsonHelper.parameterExists(CALENDAR_SUPPORTED_PARAMETERS.TITLE.getValue(), element)) {
            final String title = this.fromApiJsonHelper.extractStringNamed(CALENDAR_SUPPORTED_PARAMETERS.TITLE.getValue(), element);
            baseDataValidator.reset().parameter(CALENDAR_SUPPORTED_PARAMETERS.TITLE.getValue()).value(title).notBlank()
                    .notExceedingLengthOf(50);
        }

        if (this.fromApiJsonHelper.parameterExists(CALENDAR_SUPPORTED_PARAMETERS.DESCRIPTION.getValue(), element)) {
            final String description = this.fromApiJsonHelper.extractStringNamed(CALENDAR_SUPPORTED_PARAMETERS.DESCRIPTION.getValue(),
                    element);
            baseDataValidator.reset().parameter(CALENDAR_SUPPORTED_PARAMETERS.DESCRIPTION.getValue()).value(description).ignoreIfNull()
                    .notExceedingLengthOf(100);
        }

        if (this.fromApiJsonHelper.parameterExists(CALENDAR_SUPPORTED_PARAMETERS.LOCATION.getValue(), element)) {
            final String location = this.fromApiJsonHelper.extractStringNamed(CALENDAR_SUPPORTED_PARAMETERS.LOCATION.getValue(), element);
            baseDataValidator.reset().parameter(CALENDAR_SUPPORTED_PARAMETERS.LOCATION.getValue()).value(location).ignoreIfNull()
                    .notExceedingLengthOf(50);
        }

        if (this.fromApiJsonHelper.parameterExists(CALENDAR_SUPPORTED_PARAMETERS.START_DATE.getValue(), element)) {
            final String startDateStr = this.fromApiJsonHelper.extractStringNamed(CALENDAR_SUPPORTED_PARAMETERS.START_DATE.getValue(),
                    element);
            baseDataValidator.reset().parameter(CALENDAR_SUPPORTED_PARAMETERS.START_DATE.getValue()).value(startDateStr).notNull();

            final LocalDate startDate = this.fromApiJsonHelper.extractLocalDateNamed(CALENDAR_SUPPORTED_PARAMETERS.START_DATE.getValue(),
                    element);
            baseDataValidator.reset().parameter(CALENDAR_SUPPORTED_PARAMETERS.START_DATE.getValue()).value(startDate).notNull();
        }
        if (this.fromApiJsonHelper.parameterExists(CALENDAR_SUPPORTED_PARAMETERS.END_DATE.getValue(), element)) {
            final LocalDate endDate = this.fromApiJsonHelper.extractLocalDateNamed(CALENDAR_SUPPORTED_PARAMETERS.END_DATE.getValue(),
                    element);
            baseDataValidator.reset().parameter(CALENDAR_SUPPORTED_PARAMETERS.END_DATE.getValue()).value(endDate).notNull();
        }

        if (this.fromApiJsonHelper.parameterExists(CALENDAR_SUPPORTED_PARAMETERS.DURATION.getValue(), element)) {
            final Integer duration = this.fromApiJsonHelper.extractIntegerSansLocaleNamed(
                    CALENDAR_SUPPORTED_PARAMETERS.DURATION.getValue(), element);
            baseDataValidator.reset().parameter(CALENDAR_SUPPORTED_PARAMETERS.DURATION.getValue()).value(duration).ignoreIfNull();
        }
        // TODO: AA do not allow to change calendar type.
        if (this.fromApiJsonHelper.parameterExists(CALENDAR_SUPPORTED_PARAMETERS.TYPE_ID.getValue(), element)) {
            final Integer typeId = this.fromApiJsonHelper.extractIntegerSansLocaleNamed(CALENDAR_SUPPORTED_PARAMETERS.TYPE_ID.getValue(),
                    element);
            baseDataValidator.reset().parameter(CALENDAR_SUPPORTED_PARAMETERS.TYPE_ID.getValue()).value(typeId).notNull()
                    .inMinMaxRange(CalendarEntityType.getMinValue(), CalendarEntityType.getMaxValue());
        }
        if (this.fromApiJsonHelper.parameterExists(CALENDAR_SUPPORTED_PARAMETERS.REPEATING.getValue(), element)) {
            // FIXME - Throws NullPointerException when boolean value is null
            final boolean repeating = this.fromApiJsonHelper.extractBooleanNamed(CALENDAR_SUPPORTED_PARAMETERS.REPEATING.getValue(),
                    element);
            baseDataValidator.reset().parameter(CALENDAR_SUPPORTED_PARAMETERS.REPEATING.getValue()).value(repeating).notNull();

            if (repeating) {
                final Integer frequency = this.fromApiJsonHelper.extractIntegerSansLocaleNamed(
                        CALENDAR_SUPPORTED_PARAMETERS.FREQUENCY.getValue(), element);
                baseDataValidator.reset().parameter(CALENDAR_SUPPORTED_PARAMETERS.FREQUENCY.getValue()).value(frequency).notBlank()
                        .inMinMaxRange(CalendarFrequencyType.getMinValue(), CalendarFrequencyType.getMaxValue());

                if (this.fromApiJsonHelper.parameterExists(CALENDAR_SUPPORTED_PARAMETERS.INTERVAL.getValue(), element)) {
                    final Integer interval = this.fromApiJsonHelper.extractIntegerSansLocaleNamed(
                            CALENDAR_SUPPORTED_PARAMETERS.INTERVAL.getValue(), element);
                    baseDataValidator.reset().parameter(CALENDAR_SUPPORTED_PARAMETERS.INTERVAL.getValue()).value(interval).notNull()
                            .integerGreaterThanZero();
                }

                if (CalendarFrequencyType.fromInt(frequency).isWeekly()) {
                if (this.fromApiJsonHelper.parameterExists(CALENDAR_SUPPORTED_PARAMETERS.REPEATS_ON_DAY.getValue(), element)) {
                    final Integer repeatsOnDay = this.fromApiJsonHelper.extractIntegerSansLocaleNamed(
                            CALENDAR_SUPPORTED_PARAMETERS.REPEATS_ON_DAY.getValue(), element);
                    baseDataValidator.reset().parameter(CALENDAR_SUPPORTED_PARAMETERS.REPEATS_ON_DAY.getValue()).value(repeatsOnDay)
                            .notBlank().inMinMaxRange(CalendarWeekDaysType.getMinValue(), CalendarWeekDaysType.getMaxValue());
                    }
                } else if (CalendarFrequencyType.fromInt(frequency).isMonthly()) {
                    CalendarUtils.validateNthDayOfMonthFrequency(baseDataValidator,
                            CALENDAR_SUPPORTED_PARAMETERS.REPEATS_ON_NTH_DAY_OF_MONTH.getValue(),
                            CALENDAR_SUPPORTED_PARAMETERS.REPEATS_ON_LAST_WEEKDAY_OF_MONTH.getValue(), element, this.fromApiJsonHelper);
                }
            }
        }

        if (this.fromApiJsonHelper.parameterExists(CALENDAR_SUPPORTED_PARAMETERS.REMIND_BY_ID.getValue(), element)) {
            final Integer remindById = this.fromApiJsonHelper.extractIntegerSansLocaleNamed(
                    CALENDAR_SUPPORTED_PARAMETERS.REMIND_BY_ID.getValue(), element);
            baseDataValidator.reset().parameter(CALENDAR_SUPPORTED_PARAMETERS.REMIND_BY_ID.getValue()).value(remindById).ignoreIfNull()
                    .inMinMaxRange(CalendarRemindBy.getMinValue(), CalendarRemindBy.getMaxValue());
        }

        if (this.fromApiJsonHelper.parameterExists(CALENDAR_SUPPORTED_PARAMETERS.FIRST_REMINDER.getValue(), element)) {
            final Integer firstReminder = this.fromApiJsonHelper.extractIntegerSansLocaleNamed(
                    CALENDAR_SUPPORTED_PARAMETERS.FIRST_REMINDER.getValue(), element);
            baseDataValidator.reset().parameter(CALENDAR_SUPPORTED_PARAMETERS.FIRST_REMINDER.getValue()).value(firstReminder)
                    .ignoreIfNull();
        }

        if (this.fromApiJsonHelper.parameterExists(CALENDAR_SUPPORTED_PARAMETERS.SECOND_REMINDER.getValue(), element)) {
            final Integer secondReminder = this.fromApiJsonHelper.extractIntegerSansLocaleNamed(
                    CALENDAR_SUPPORTED_PARAMETERS.SECOND_REMINDER.getValue(), element);
            baseDataValidator.reset().parameter(CALENDAR_SUPPORTED_PARAMETERS.SECOND_REMINDER.getValue()).value(secondReminder)
                    .ignoreIfNull();
        }
        if (this.fromApiJsonHelper.parameterExists(CALENDAR_SUPPORTED_PARAMETERS.MEETING_TIME.getValue(), element)) {
            final Time startDate = this.fromApiJsonHelper.extractLocalTime(CALENDAR_SUPPORTED_PARAMETERS.MEETING_TIME.getValue(),
                    element);
            baseDataValidator.reset().parameter(CALENDAR_SUPPORTED_PARAMETERS.MEETING_TIME.getValue()).value(startDate).ignoreIfNull();
        }
       
        if (!dataValidationErrors.isEmpty()) { throw new PlatformApiDataValidationException("validation.msg.validation.errors.exist",
                "Validation errors exist.", dataValidationErrors); }
    }
        
}
