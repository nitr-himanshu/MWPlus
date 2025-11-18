/*
 * Copyright (c) 2018.
 *
 * This file is part of MoneyWallet.
 *
 * MoneyWallet is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MoneyWallet is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MoneyWallet.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.oriondev.moneywallet.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import com.oriondev.moneywallet.R;
import com.oriondev.moneywallet.model.LockMode;
import com.oriondev.moneywallet.storage.preference.PreferenceManager;
import com.oriondev.moneywallet.ui.activity.base.ThemedActivity;

import java.util.concurrent.Executor;

/**
 * Created by andrea on 24/07/18.
 */
public class LockActivity extends ThemedActivity {

    public static final String MODE = "LockActivity::Arguments::Mode";
    public static final String ACTION = "LockActivity::Arguments::Action";

    /**
     * Default action if not specified, it will prompt the user to insert the key
     * and return an intent with RESULT_OK if the user entered the correct secret.
     */
    public static final int ACTION_UNLOCK = 0;

    /**
     * This action will prompt the user to insert the current key to verify his
     * identity, then if the correct secret has been provided, the lock mode will
     * be set to DISABLED state and an intent with RESULT_OK will be returned.
     */
    public static final int ACTION_DISABLE = 1;

    /**
     * This action will prompt the user to create a key using the specified MODE
     * and will verify it before setting the current lock mode to the specified
     * mode. An intent with RESULT_OK will be returned if the operation have
     * success.
     */
    public static final int ACTION_ENABLE = 2;

    /**
     * This action will prompt the user to insert the current key to verify his
     * identity, then if the correct secret has been provided, it will be prompted
     * to create (and verify) a new key keeping the same lock mode. An intent with
     * RESULT_OK will be returned if the operation have success.
     */
    public static final int ACTION_CHANGE_KEY = 3;

    /**
     * This action will prompt the user to insert the current key to verify his
     * identity, then if the correct secret has been provided, it will be prompted
     * to create (and verify) a new key using the provided MODE. An intent with
     * RESULT_OK will be returned if the operation have success.
     */
    public static final int ACTION_CHANGE_MODE = 4;

    public static Intent unlock(Activity activity) {
        Intent intent = new Intent(activity, LockActivity.class);
        intent.putExtra(ACTION, ACTION_UNLOCK);
        return intent;
    }

    public static Intent enableLock(Activity activity, LockMode lockMode) {
        Intent intent = new Intent(activity, LockActivity.class);
        intent.putExtra(MODE, lockMode);
        intent.putExtra(ACTION, ACTION_ENABLE);
        return intent;
    }

    public static Intent disableLock(Activity activity) {
        Intent intent = new Intent(activity, LockActivity.class);
        intent.putExtra(ACTION, ACTION_DISABLE);
        return intent;
    }

    public static Intent changeKey(Activity activity) {
        Intent intent = new Intent(activity, LockActivity.class);
        intent.putExtra(ACTION, ACTION_CHANGE_KEY);
        return intent;
    }

    public static Intent changeMode(Activity activity, LockMode lockMode) {
        Intent intent = new Intent(activity, LockActivity.class);
        intent.putExtra(MODE, lockMode);
        intent.putExtra(ACTION, ACTION_CHANGE_MODE);
        return intent;
    }

    private int mAction;
    private LockMode mTargetLockMode;
    private LockMode mCurrentLockMode;

    private TextView mHelpTextView;
    private Button mFingerprintButton;
    private TextView mCancelButton;

    private Executor executor;
    private BiometricPrompt biometricPrompt;
    private BiometricPrompt.PromptInfo promptInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lock_biometric);

        // Get intent parameters
        mAction = getIntent().getIntExtra(ACTION, ACTION_UNLOCK);
        mTargetLockMode = (LockMode) getIntent().getSerializableExtra(MODE);
        mCurrentLockMode = PreferenceManager.getCurrentLockMode();

        // Initialize views
        mHelpTextView = findViewById(R.id.help_text_view);
        mFingerprintButton = findViewById(R.id.fingerprint_button);
        mCancelButton = findViewById(R.id.cancel_button);

        // Initialize biometric authentication
        executor = ContextCompat.getMainExecutor(this);
        biometricPrompt = new BiometricPrompt(this, executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                handleAuthenticationError(errorCode, errString);
            }

            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                handleAuthenticationSuccess();
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                Toast.makeText(LockActivity.this, R.string.fingerprint_not_recognized, Toast.LENGTH_SHORT).show();
            }
        });

        // Set up button click listeners
        mFingerprintButton.setOnClickListener(v -> startBiometricAuthentication());
        mCancelButton.setOnClickListener(v -> finishWithResult(RESULT_CANCELED));

        // Update UI based on action
        updateUI();
    }

    private void updateUI() {
        switch (mAction) {
            case ACTION_UNLOCK:
                mHelpTextView.setText(R.string.fingerprint_unlock_message);
                mFingerprintButton.setText(R.string.fingerprint_unlock);
                break;
            case ACTION_DISABLE:
                mHelpTextView.setText(R.string.fingerprint_disable_message);
                mFingerprintButton.setText(R.string.fingerprint_disable);
                break;
            case ACTION_ENABLE:
                mHelpTextView.setText(R.string.fingerprint_enable_message);
                mFingerprintButton.setText(R.string.fingerprint_enable);
                break;
            case ACTION_CHANGE_KEY:
                mHelpTextView.setText(R.string.fingerprint_change_key_message);
                mFingerprintButton.setText(R.string.fingerprint_change_key);
                break;
            case ACTION_CHANGE_MODE:
                mHelpTextView.setText(R.string.fingerprint_change_mode_message);
                mFingerprintButton.setText(R.string.fingerprint_change_mode);
                break;
        }
    }

    private void startBiometricAuthentication() {
        // Check if biometric authentication is available
        BiometricManager biometricManager = BiometricManager.from(this);
        switch (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)) {
            case BiometricManager.BIOMETRIC_SUCCESS:
                // Biometric authentication is available
                break;
            case BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE:
                Toast.makeText(this, R.string.fingerprint_error_no_hardware, Toast.LENGTH_LONG).show();
                return;
            case BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE:
                Toast.makeText(this, R.string.fingerprint_error_hw_unavailable, Toast.LENGTH_LONG).show();
                return;
            case BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED:
                Toast.makeText(this, R.string.fingerprint_error_none_enrolled, Toast.LENGTH_LONG).show();
                return;
            default:
                Toast.makeText(this, R.string.fingerprint_error_unknown, Toast.LENGTH_LONG).show();
                return;
        }

        // Create prompt info
        promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle(getString(R.string.fingerprint_auth_title))
                .setSubtitle(getString(R.string.fingerprint_auth_subtitle))
                .setNegativeButtonText(getString(R.string.cancel))
                .build();

        // Start authentication
        biometricPrompt.authenticate(promptInfo);
    }

    private void handleAuthenticationError(int errorCode, CharSequence errString) {
        switch (errorCode) {
            case BiometricPrompt.ERROR_USER_CANCELED:
            case BiometricPrompt.ERROR_NEGATIVE_BUTTON:
                // User canceled, do nothing
                break;
            case BiometricPrompt.ERROR_LOCKOUT:
            case BiometricPrompt.ERROR_LOCKOUT_PERMANENT:
                Toast.makeText(this, R.string.fingerprint_error_lockout, Toast.LENGTH_LONG).show();
                break;
            default:
                Toast.makeText(this, errString, Toast.LENGTH_LONG).show();
                break;
        }
    }

    private void handleAuthenticationSuccess() {
        switch (mAction) {
            case ACTION_UNLOCK:
                // Authentication successful, unlock the app
                finishWithResult(RESULT_OK);
                break;
            case ACTION_DISABLE:
                // Disable lock mode
                PreferenceManager.setCurrentLockMode(LockMode.DISABLED);
                finishWithResult(RESULT_OK);
                break;
            case ACTION_ENABLE:
                // Enable biometric lock mode
                PreferenceManager.setCurrentLockMode(LockMode.BIOMETRIC);
                finishWithResult(RESULT_OK);
                break;
            case ACTION_CHANGE_KEY:
                // Change key (for biometric, this is just a re-authentication)
                finishWithResult(RESULT_OK);
                break;
            case ACTION_CHANGE_MODE:
                // Change to target lock mode
                if (mTargetLockMode != null) {
                    PreferenceManager.setCurrentLockMode(mTargetLockMode);
                }
                finishWithResult(RESULT_OK);
                break;
        }
    }

    private void finishWithResult(int resultCode) {
        setResult(resultCode);
        finish();
    }

    @Override
    public void onBackPressed() {
        // Prevent back button from bypassing authentication
        finishWithResult(RESULT_CANCELED);
    }
}