package in.myecash.appbase.barcodeReader;

import android.util.SparseArray;

import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.barcode.Barcode;
import in.myecash.appbase.utilities.LogMy;

/**
 * Created by adgangwa on 02-05-2016.
 */
public class MyBarcodeDetector extends Detector<Barcode> {
    private static final String TAG = "BaseApp-MyBarcodeDetector";
    private Detector<Barcode> mDelegate;
    private Callback mCallback;
    //private boolean mFirstImageProcessed = false;

    public interface Callback {
        void onDetection(Frame frame);
    }

    MyBarcodeDetector(Detector<Barcode> delegate, Callback callback) {
        mDelegate = delegate;
        mCallback = callback;
    }

    public SparseArray<Barcode> detect(Frame frame) {
        //LogMy.d(TAG, "In detect");
        SparseArray<Barcode> faces = mDelegate.detect(frame);
        if (faces.size() > 0) {
            mCallback.onDetection(frame);
            /*
            // save the frame to the file system
            ByteBuffer byteBuffer = frame.getGrayscaleImageData();
            int w = frame.getMetadata().getWidth();
            int h = frame.getMetadata().getHeight();

            YuvImage yuvimage = new YuvImage(byteBuffer.array(), ImageFormat.NV21, w, h, null);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            yuvimage.compressToJpeg(new Rect(0, 0, w, h), 80, baos); // Where 80 is the quality of the generated jpeg
            byte[] jpegArray = baos.toByteArray();
            Bitmap bitmap = BitmapFactory.decodeByteArray(jpegArray, 0, jpegArray.length);
            */

            //mFirstImageProcessed = true;
        }
        return faces;
    }

    public boolean isOperational() {
        return mDelegate.isOperational();
    }

    public boolean setFocus(int id) {
        return mDelegate.setFocus(id);
    }
}
