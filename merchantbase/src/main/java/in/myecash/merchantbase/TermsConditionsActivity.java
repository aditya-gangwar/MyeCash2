package in.myecash.merchantbase;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.widget.TextView;

/**
 * Created by adgangwa on 21-02-2016.
 */
public class TermsConditionsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_terms_conditions);

        TextView view = (TextView) findViewById(R.id.terms_text_view);
        view.setText(Html.fromHtml(getResources().getString(R.string.terms_conditions)));
    }
}
