package in.myecash.merchantbase.entities;

/**
 * Created by adgangwa on 04-03-2016.
 */
public class OrderItem {
    private int mQuantity;
    private int mUnitPrice;
    private int mPrice;
    private String mQuantityStr;
    private String mUnitPriceStr;
    private String mPriceStr;
    private boolean mCashbackExcluded;

    public OrderItem(int quantity, int unitPrice, boolean cashbackExcluded) {
        mQuantity = quantity;
        mQuantityStr = String.valueOf(quantity);
        mUnitPrice = unitPrice;
        mUnitPriceStr = String.valueOf(unitPrice);
        updatePrice();
        mCashbackExcluded = cashbackExcluded;
    }

    public void setQuantityStr(String quantityStr) {
        mQuantityStr = quantityStr;
        mQuantity = Integer.parseInt(quantityStr);
        updatePrice();
    }

    public void setUnitPriceStr(String unitPriceStr) {
        mUnitPriceStr = unitPriceStr;
        mUnitPrice = Integer.parseInt(unitPriceStr);
        updatePrice();
    }

    private void updatePrice() {
        mPrice = mQuantity*mUnitPrice;
        mPriceStr = String.valueOf(mPrice);
    }

    public void setCashbackExcluded(boolean cashbackExcluded) {
        mCashbackExcluded = cashbackExcluded;
    }

    public int getQuantity() {
        return mQuantity;
    }

    public int getUnitPrice() {
        return mUnitPrice;
    }

    public int getPrice() {
        return mPrice;
    }

    public String getQuantityStr() {
        return mQuantityStr;
    }

    public String getUnitPriceStr() {
        return mUnitPriceStr;
    }

    public String getPriceStr() {
        return mPriceStr;
    }

    public boolean isCashbackExcluded() {
        return mCashbackExcluded;
    }
}
