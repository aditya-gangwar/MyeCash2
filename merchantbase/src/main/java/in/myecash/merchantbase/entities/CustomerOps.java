package in.myecash.merchantbase.entities;

import com.crashlytics.android.Crashlytics;
import in.myecash.commonbase.constants.AppConstants;

/**
 * Created by adgangwa on 28-08-2016.
 */
public class CustomerOps {
    public static String CUSTOMER_OP_STATUS_OTP_GENERATED = "OtpGenerated";

    private String extra_op_params;
    private String mobile_num;
    private String qr_card;
    private String op_code;
    private String op_status;
    private String otp;
    private String pin;

    public String getExtra_op_params() {
        return extra_op_params;
    }

    public void setExtra_op_params(String extra_op_params) {
        this.extra_op_params = extra_op_params;
    }

    public String getMobile_num() {
        return mobile_num;
    }

    public void setMobile_num(String mobile_num) {
        this.mobile_num = mobile_num;
        Crashlytics.setString(AppConstants.CLTS_INPUT_CUST_MOBILE, mobile_num);
    }

    public String getQr_card() {
        return qr_card;
    }

    public void setQr_card(String qr_card) {
        this.qr_card = qr_card;
        Crashlytics.setString(AppConstants.CLTS_INPUT_CUST_CARD, qr_card);
    }

    public String getOp_code() {
        return op_code;
    }

    public void setOp_code(String op_code) {
        this.op_code = op_code;
    }

    public String getOp_status() {
        return op_status;
    }

    public void setOp_status(String op_status) {
        this.op_status = op_status;
    }

    public String getOtp() {
        return otp;
    }

    public void setOtp(String otp) {
        this.otp = otp;
    }

    public String getPin() {
        return pin;
    }

    public void setPin(String pin) {
        this.pin = pin;
    }
}
