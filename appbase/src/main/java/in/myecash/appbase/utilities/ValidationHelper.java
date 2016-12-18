package in.myecash.appbase.utilities;

import android.util.Patterns;

import java.util.Calendar;

import in.myecash.common.constants.CommonConstants;
import in.myecash.common.constants.ErrorCodes;

/**
 * Created by adgangwa on 06-02-2016.
 */
public final class ValidationHelper{

    /*
    public static int validatePin(String value) {
        if (value.isEmpty()) {
            return ErrorCodes.EMPTY_VALUE;
        } else if (value.length() != CommonConstants.PIN_LEN) {
            return ErrorCodes.WRONG_SECRET_PIN;
        } else {
            return ErrorCodes.NO_ERROR;
        }
    }*/

    public static int validatePin(String value) {
        if (value==null || value.isEmpty()) {
            return ErrorCodes.EMPTY_VALUE;
        } else if (value.length() != CommonConstants.PIN_LEN) {
            return ErrorCodes.INVALID_LENGTH;
        } else {
            return ErrorCodes.NO_ERROR;
        }
    }

    public static int validateOtp(String value) {
        if (value==null || value.isEmpty()) {
            return ErrorCodes.EMPTY_VALUE;
        } else if (value.length() != CommonConstants.OTP_LEN) {
            return ErrorCodes.INVALID_LENGTH;
        } else {
            return ErrorCodes.NO_ERROR;
        }
    }

    public static int validateMemberCard(String value) {
        if (value==null || value.isEmpty()) {
            return ErrorCodes.EMPTY_VALUE;
        } else if (value.length() != CommonConstants.CUSTOMER_CARDID_LEN) {
            return ErrorCodes.INVALID_LENGTH;
        }
        return ErrorCodes.NO_ERROR;
    }

    public static int validateMobileNo(String value) {
        if (value==null || value.isEmpty()) {
            return ErrorCodes.EMPTY_VALUE;
        } else if(value.length() != CommonConstants.MOBILE_NUM_LENGTH) {
            return ErrorCodes.INVALID_LENGTH;
        } else if (!Patterns.PHONE.matcher(value).matches()) {
            return ErrorCodes.INVALID_FORMAT;
        } else {
            return ErrorCodes.NO_ERROR;
        }
    }

    public static int validateEmail(String value) {
        if (value==null || value.isEmpty()) {
            return ErrorCodes.EMPTY_VALUE;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(value).matches()) {
            return ErrorCodes.INVALID_FORMAT;
        } else {
            return ErrorCodes.NO_ERROR;
        }
    }

    public static int validateAddress(String value) {
        if (value==null || value.isEmpty()) {
            return ErrorCodes.EMPTY_VALUE;
        } else {
            return ErrorCodes.NO_ERROR;
        }
    }

    public static int validateBrandName(String value) {
        if (value==null || value.isEmpty()) {
            return ErrorCodes.EMPTY_VALUE;
        } else {
            return ErrorCodes.NO_ERROR;
        }
    }

    public static int validateCustName(String value) {
        if (value==null || value.isEmpty()) {
            return ErrorCodes.EMPTY_VALUE;
        } else {
            return ErrorCodes.NO_ERROR;
        }
    }

    public static int validatePassword(String value) {
        if (value==null || value.isEmpty()) {
            return ErrorCodes.EMPTY_VALUE;
        } else {
            return ErrorCodes.NO_ERROR;
        }
    }

    public static int validateTicketNum(String value) {
        if (value==null || value.isEmpty()) {
            return ErrorCodes.EMPTY_VALUE;
        } else {
            return ErrorCodes.NO_ERROR;
        }
    }

    public static int validateMerchantId(String value) {
        if (value==null || value.isEmpty() ) {
            return ErrorCodes.EMPTY_VALUE;
        } else if (value.length() != CommonConstants.MERCHANT_ID_LEN) {
            return ErrorCodes.INVALID_LENGTH;
        } else {
            return ErrorCodes.NO_ERROR;
        }
    }

    public static int validateCustInternalId(String value) {
        if (value==null || value.isEmpty() ) {
            return ErrorCodes.EMPTY_VALUE;
        } else if (value.length() != CommonConstants.CUSTOMER_INTERNAL_ID_LEN) {
            return ErrorCodes.INVALID_LENGTH;
        } else {
            return ErrorCodes.NO_ERROR;
        }
    }

    public static int validateInternalUserId(String value) {
        if (value==null || value.isEmpty() ) {
            return ErrorCodes.EMPTY_VALUE;
        } else if (value.length() != CommonConstants.INTERNAL_USER_ID_LEN) {
            return ErrorCodes.INVALID_LENGTH;
        } else {
            return ErrorCodes.NO_ERROR;
        }
    }

    public static int validateAgentId(String value) {
        if (value==null || value.isEmpty() ) {
            return ErrorCodes.EMPTY_VALUE;
        } else if (value.length() != CommonConstants.INTERNAL_USER_ID_LEN) {
            return ErrorCodes.INVALID_LENGTH;
        } else if (!value.startsWith(CommonConstants.PREFIX_AGENT_ID)) {
            return ErrorCodes.INVALID_FORMAT;
        } else {
            return ErrorCodes.NO_ERROR;
        }
    }

    public static int validateCcId(String value) {
        if (value==null || value.isEmpty() ) {
            return ErrorCodes.EMPTY_VALUE;
        } else if (value.length() != CommonConstants.INTERNAL_USER_ID_LEN) {
            return ErrorCodes.INVALID_LENGTH;
        } else if (!value.startsWith(CommonConstants.PREFIX_CC_ID)) {
            return ErrorCodes.INVALID_FORMAT;
        } else {
            return ErrorCodes.NO_ERROR;
        }
    }

    public static int validateCcntId(String value) {
        if (value==null || value.isEmpty() ) {
            return ErrorCodes.EMPTY_VALUE;
        } else if (value.length() != CommonConstants.INTERNAL_USER_ID_LEN) {
            return ErrorCodes.INVALID_LENGTH;
        } else if (!value.startsWith(CommonConstants.PREFIX_CCNT_ID)) {
            return ErrorCodes.INVALID_FORMAT;
        } else {
            return ErrorCodes.NO_ERROR;
        }
    }

    public static int validateCardId(String value) {
        if (value==null || value.isEmpty() ) {
            return ErrorCodes.EMPTY_VALUE;
        } else if (value.length() != CommonConstants.CUSTOMER_CARDID_LEN) {
            return ErrorCodes.INVALID_LENGTH;
        } else {
            return ErrorCodes.NO_ERROR;
        }
    }

    public static int validateCbRate(String value) {
        if (value==null || value.isEmpty() ) {
            return ErrorCodes.EMPTY_VALUE;
        } else {
            float rate;
            try {
                rate = Float.parseFloat(value);
            } catch (NumberFormatException e) {
                return ErrorCodes.INVALID_FORMAT;
            }
            if(rate>100 || rate<0) {
                return ErrorCodes.INVALID_VALUE;
            }
            return ErrorCodes.NO_ERROR;
        }
    }

    public static int validateDob(String value) {
        if (value==null || value.isEmpty() ) {
            return ErrorCodes.EMPTY_VALUE;
        } else if (value.length() != CommonConstants.DOB_LEN) {
            return ErrorCodes.INVALID_LENGTH;
        } else {
            int currYear = Calendar.getInstance().get(Calendar.YEAR);

            if( Integer.parseInt(value.substring(0,2)) > 31 ||
                    Integer.parseInt(value.substring(2,4)) > 12 ||
                    Integer.parseInt(value.substring(4)) > currYear ) {
                return ErrorCodes.INVALID_FORMAT;
            }
            return ErrorCodes.NO_ERROR;
        }
    }


}

