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

package com.oriondev.moneywallet.api.google;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.api.services.drive.DriveScopes;
import com.oriondev.moneywallet.R;
import com.oriondev.moneywallet.api.AbstractBackendServiceDelegate;
import com.oriondev.moneywallet.api.BackendException;
import com.oriondev.moneywallet.api.BackendServiceFactory;
import com.oriondev.moneywallet.ui.view.theme.ThemedDialog;

import java.util.HashSet;
import java.util.Set;

import androidx.activity.ComponentActivity;
import androidx.annotation.NonNull;

/**
 * Created by andrea on 21/11/18.
 * Updated to use Google Drive REST API v3
 */
public class GoogleDriveBackendService extends AbstractBackendServiceDelegate {

    private static final int REQUEST_CODE_SIGN_IN = 8393;

    public GoogleDriveBackendService(BackendServiceStatusListener listener) {
        super(listener);
    }

    @Override
    public String getId() {
        return BackendServiceFactory.SERVICE_ID_GOOGLE_DRIVE;
    }

    @Override
    public int getName() {
        return R.string.service_backup_google_drive;
    }

    @Override
    public int getBackupCoverMessage() {
        return R.string.cover_message_backup_google_drive_title;
    }

    @Override
    public int getBackupCoverAction() {
        return R.string.cover_message_backup_google_drive_button;
    }

    @Override
    public boolean isServiceEnabled(Context context) {
        Set<Scope> requiredScopes = new HashSet<>(1);
        requiredScopes.add(new Scope(DriveScopes.DRIVE_FILE));
        GoogleSignInAccount signInAccount = GoogleSignIn.getLastSignedInAccount(context);
        return signInAccount != null && signInAccount.getGrantedScopes().containsAll(requiredScopes);
    }

    @Override
    public void setup(ComponentActivity activity) throws BackendException {
        GoogleSignInOptions signInOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestScopes(new Scope(DriveScopes.DRIVE_FILE))
                .requestEmail()
                .build();
        GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(activity, signInOptions);
        Intent signInIntent = googleSignInClient.getSignInIntent();
        activity.startActivityForResult(signInIntent, REQUEST_CODE_SIGN_IN);
    }

    @Override
    public void teardown(final ComponentActivity activity) throws BackendException {
        ThemedDialog.buildMaterialDialog(activity)
                .title(R.string.title_warning)
                .content(R.string.message_backup_service_google_drive_disconnect)
                .positiveText(android.R.string.yes)
                .negativeText(android.R.string.no)
                .onPositive(new MaterialDialog.SingleButtonCallback() {

                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        signOutFromGoogle(activity);
                    }

                })
                .show();
    }

    private void signOutFromGoogle(Activity activity) {
        GoogleSignInOptions signInOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestScopes(new Scope(DriveScopes.DRIVE_FILE))
                .build();
        GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(activity, signInOptions);
        Task<Void> signOutTask = googleSignInClient.signOut();
        signOutTask.addOnCompleteListener(new OnCompleteListener<Void>() {

            @Override
            public void onComplete(@NonNull Task<Void> task) {
                setBackendServiceEnabled(false);
            }

        });
    }

    @Override
    public boolean handleActivityResult(Context context, int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            if (task.isSuccessful()) {
                setBackendServiceEnabled(true);
            } else {
                setBackendServiceEnabled(false);
            }
            return true;
        }
        return false;
    }
}
