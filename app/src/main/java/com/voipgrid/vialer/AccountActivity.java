package com.voipgrid.vialer;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;

import com.voipgrid.vialer.api.Api;
import com.voipgrid.vialer.api.PreviousRequestNotFinishedException;
import com.voipgrid.vialer.api.ServiceGenerator;
import com.voipgrid.vialer.api.models.MobileNumber;
import com.voipgrid.vialer.api.models.PhoneAccount;
import com.voipgrid.vialer.api.models.SystemUser;
import com.voipgrid.vialer.util.DialogHelper;
import com.voipgrid.vialer.util.PhoneNumberUtils;
import com.voipgrid.vialer.util.JsonStorage;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AccountActivity extends AppCompatActivity implements
        Switch.OnCheckedChangeListener,
        Callback {

    private CompoundButton mSwitch;
    private EditText mSipIdEditText;

    private ServiceGenerator mServiceGen;

    private PhoneAccount mPhoneAccount;
    private Preferences mPreferences;
    private JsonStorage mJsonStorage;
    private SystemUser mSystemUser;

    private boolean mEditMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account);

        mJsonStorage = new JsonStorage(this);
        mSystemUser = (SystemUser) mJsonStorage.get(SystemUser.class);
        mPhoneAccount = (PhoneAccount) mJsonStorage.get(PhoneAccount.class);

        mPreferences = new Preferences(this);

        /* set the Toolbar to use as ActionBar */
        setSupportActionBar((Toolbar) findViewById(R.id.action_bar));

        /* enabled home as up for the Toolbar */
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        /* enabled home button for the Toolbar */
        getSupportActionBar().setHomeButtonEnabled(true);
        
        mSwitch = (CompoundButton) findViewById(R.id.account_sip_switch);
        mSwitch.setOnCheckedChangeListener(this);

        mSipIdEditText = ((EditText) findViewById(R.id.account_sip_id_edit_text));

        populate();
    }

    private void populate() {
        if(mPreferences.hasSipPermission()) {
            mSwitch.setChecked(mPreferences.hasSipEnabled());
            if(!mSwitch.isChecked()) {
                mSipIdEditText.setVisibility(View.GONE);
            }
            if(mPhoneAccount != null) {
                mSipIdEditText.setText(mPhoneAccount.getAccountId());
            }
        } else {
            mSwitch.setVisibility(View.GONE);
            mSipIdEditText.setVisibility(View.GONE);
        }
        ((EditText) findViewById(R.id.account_mobile_number_edit_text))
                .setText(mSystemUser.getMobileNumber());
        ((EditText) findViewById(R.id.account_outgoing_number_edit_text))
                .setText(mSystemUser.getOutgoingCli());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_account, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.action_edit).setVisible(!mEditMode);
        menu.findItem(R.id.action_done).setVisible(mEditMode);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_edit || id == R.id.action_done) {
            if (id == R.id.action_edit) {
                mEditMode = true;
            }
            if (id == R.id.action_done) {

                if (isValidNumber()) {
                    mEditMode = false;
                    save();
                } else {
                    DialogHelper.displayAlert(
                            this,
                            getString(R.string.invalid_mobile_number_title),
                            getString(R.string.invalid_mobile_number_message)
                    );
                }
            }
            invalidateOptionsMenu();
            invalidateEditText();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * isValidNumber returns true if the number currently entered is a valid phone number.
     */
    private boolean isValidNumber() {
        String mobileNumber = ((EditText) findViewById(
                R.id.account_mobile_number_edit_text)).getText().toString();

        return PhoneNumberUtils.isValidMobileNumber(PhoneNumberUtils.formatMobileNumber(mobileNumber));
    }

    private void save() {
        findViewById(R.id.container).setFocusableInTouchMode(true);

        String number = ((EditText) findViewById(
                R.id.account_mobile_number_edit_text)).getText().toString();
        number = PhoneNumberUtils.formatMobileNumber(number);

        try {
            mServiceGen = ServiceGenerator.getInstance();
        } catch(PreviousRequestNotFinishedException e) {
            return;
        }
        Api api = mServiceGen.createService(
                this,
                Api.class,
                getString(R.string.api_url),
                mSystemUser.getEmail(),
                mSystemUser.getPassword()
        );
        Call<MobileNumber> call = api.mobileNumber(new MobileNumber(number));
        call.enqueue(this);

        mSystemUser.setMobileNumber(number);

        populate();
    }

    private void invalidateEditText() {
        findViewById(R.id.account_mobile_number_edit_text).setEnabled(mEditMode);
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        /* First, view updates */

        mSipIdEditText.setVisibility(isChecked ? View.VISIBLE : View.GONE);

        if (mPreferences.hasSipEnabled() == isChecked) {
            /* nothing changed, so return */
            return;
        }
        mPreferences.setSipEnabled(isChecked);

        if (!isChecked) {
            // Unregister at middleware.

            // Blocking for now, quickfix for beta testers.
            // TODO VIALA 366 temporary disabled.
//            Middleware.unregister(this);

        } else {
            // Register. Fix this later in SIP vialer version.
            // TODO: VIALA-364.
        }
    }

    @Override
    public void onResponse(Call call, Response response) {
        mServiceGen.release();
        if (response.isSuccess()) {
            // Success callback for updating mobile number.
            // Update the systemuser.
            mJsonStorage.save(mSystemUser);
        } else {
            failedFeedback();
        }
    }

    @Override
    public void onFailure(Call call, Throwable t) {
        mServiceGen.release();
        failedFeedback();
    }

    /**
     * Function to inform the user of a failed requests.
     */
    private void failedFeedback() {
        DialogHelper.displayAlert(
                this,
                getString(R.string.onboarding_account_configure_failed_title),
                getString(R.string.onboarding_account_configure_invalid_phone_number)
        );
    }
}
