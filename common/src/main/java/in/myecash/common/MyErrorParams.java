package in.myecash.common;

import in.myecash.common.constants.CommonConstants;

/**
 * Created by adgangwa on 04-02-2017.
 */
public class MyErrorParams {

    // Class contains some parameters returned by Backend in some cases
    // as error message of exception thrown
    public int errorCode;
    public int attemptAvailable;
    public int opScheduledMins;
    public String imgFileName;

    public MyErrorParams() {}
    public MyErrorParams(int errorCode, int attemptAvailable, int opScheduledMins, String imgFileName) {
        this.errorCode = errorCode;
        this.attemptAvailable = attemptAvailable;
        this.opScheduledMins = opScheduledMins;
        this.imgFileName = imgFileName;
    }

    public static final String ERROR_PARAMS_IN_MSG_MARKER = "ZZ";

    // Index of above parameters in CSV record
    private static int ERPM_CSV_MARKER = 0;
    private static int ERPM_CSV_ERROR_CODE = 1;
    private static int ERPM_CSV_ATTEMPTS = 2;
    private static int ERPM_CSV_OP_SCH = 3;
    private static int ERPM_CSV_IMG = 4;
    private static int ERPM_CSV_TOTAL_FIELDS = 5;

    // Total size of above fields
    private static final int CUST_CSV_MAX_SIZE = 64;

    public String toCsvString() {
        String[] csvFields = new String[CUST_CSV_MAX_SIZE];

        csvFields[ERPM_CSV_MARKER] = ERROR_PARAMS_IN_MSG_MARKER;
        csvFields[ERPM_CSV_ERROR_CODE] = String.valueOf(errorCode);

        if(attemptAvailable>=0) {
            csvFields[ERPM_CSV_ATTEMPTS] = String.valueOf(attemptAvailable);
        } else {
            csvFields[ERPM_CSV_ATTEMPTS] = "";
        }

        if(opScheduledMins>=0) {
            csvFields[ERPM_CSV_OP_SCH] = String.valueOf(opScheduledMins);
        } else {
            csvFields[ERPM_CSV_OP_SCH] = "";
        }
        csvFields[ERPM_CSV_IMG] = imgFileName;

        // join the fields in single CSV string
        StringBuilder sb = new StringBuilder(CUST_CSV_MAX_SIZE);
        for(int i=0; i<ERPM_CSV_TOTAL_FIELDS; i++) {
            sb.append(csvFields[i]).append(CommonConstants.CSV_DELIMETER);
        }
        return sb.toString();
    }

    public boolean init(String csvStr) {
        if(csvStr== null || csvStr.isEmpty() || !csvStr.startsWith(ERROR_PARAMS_IN_MSG_MARKER)) {
            errorCode = -1;
            attemptAvailable = 0;
            opScheduledMins = 0;
            imgFileName = "";
            return false;
        }
        String[] csvFields = csvStr.split(CommonConstants.CSV_DELIMETER, -1);

        errorCode = Integer.parseInt(csvFields[ERPM_CSV_ERROR_CODE]);

        if(!csvFields[ERPM_CSV_ATTEMPTS].isEmpty()) {
            attemptAvailable = Integer.parseInt(csvFields[ERPM_CSV_ATTEMPTS]);
        } else {
            attemptAvailable = -1;
        }

        if(!csvFields[ERPM_CSV_OP_SCH].isEmpty()) {
            opScheduledMins = Integer.parseInt(csvFields[ERPM_CSV_OP_SCH]);
        } else {
            opScheduledMins = -1;
        }

        imgFileName = csvFields[ERPM_CSV_IMG];
        return true;
    }
}
