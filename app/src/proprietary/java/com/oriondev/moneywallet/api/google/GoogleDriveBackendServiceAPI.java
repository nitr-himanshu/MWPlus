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

import android.content.Context;
import androidx.annotation.NonNull;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.FileList;
import com.oriondev.moneywallet.api.AbstractBackendServiceAPI;
import com.oriondev.moneywallet.api.BackendException;
import com.oriondev.moneywallet.model.GoogleDriveFile;
import com.oriondev.moneywallet.model.IFile;
import com.oriondev.moneywallet.utils.ProgressInputStream;
import com.oriondev.moneywallet.utils.ProgressOutputStream;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by andrea on 24/11/18.
 * Updated to use Google Drive REST API v3
 */
public class GoogleDriveBackendServiceAPI extends AbstractBackendServiceAPI<GoogleDriveFile> {

    private static final String MIME_TYPE_FOLDER = "application/vnd.google-apps.folder";
    private static final String APP_FOLDER_NAME = "MoneyWallet";

    private final Drive mDriveService;
    private final String mAppFolderId;

    public GoogleDriveBackendServiceAPI(Context context) throws BackendException {
        super(GoogleDriveFile.class);
        GoogleSignInAccount signInAccount = GoogleSignIn.getLastSignedInAccount(context);
        if (signInAccount != null) {
            try {
                GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(
                        context, Collections.singletonList(DriveScopes.DRIVE_FILE));
                credential.setSelectedAccountName(signInAccount.getEmail());
                
                JsonFactory jsonFactory = GsonFactory.getDefaultInstance();
                mDriveService = new Drive.Builder(
                        new NetHttpTransport(),
                        jsonFactory,
                        credential)
                        .setApplicationName("MoneyWallet")
                        .build();
                
                mAppFolderId = getOrCreateAppFolder();
            } catch (IOException e) {
                throw new BackendException("Failed to initialize Google Drive: " + e.getMessage(), true);
            } catch (Exception e) {
                String errorMessage = e.getMessage();
                if (errorMessage == null || errorMessage.trim().isEmpty()) {
                    errorMessage = "Unknown initialization error";
                }
                throw new BackendException("Google Drive initialization error: " + errorMessage, false);
            }
        } else {
            throw new BackendException("Google Drive account not signed in", false);
        }
    }

    @Override
    public GoogleDriveFile upload(GoogleDriveFile folder, File file, ProgressInputStream.UploadProgressListener listener) throws BackendException {
        try (ProgressInputStream inputStream = new ProgressInputStream(file, listener)) {
            String parentFolderId = (folder != null) ? folder.getId() : mAppFolderId;
            
            com.google.api.services.drive.model.File fileMetadata = new com.google.api.services.drive.model.File();
            fileMetadata.setName(file.getName());
            fileMetadata.setParents(Collections.singletonList(parentFolderId));
            
            com.google.api.client.http.InputStreamContent mediaContent = 
                new com.google.api.client.http.InputStreamContent("application/octet-stream", inputStream);
            mediaContent.setLength(file.length());
            
            com.google.api.services.drive.model.File uploadedFile = mDriveService.files()
                    .create(fileMetadata, mediaContent)
                    .setFields("id, name, mimeType, size, modifiedTime, parents")
                    .execute();
            
            return new GoogleDriveFile(uploadedFile);
        } catch (IOException e) {
            throw new BackendException("Failed to upload file: " + e.getMessage(), true);
        } catch (Exception e) {
            throw new BackendException("Upload error: " + e.getMessage(), false);
        }
    }

    @Override
    public File download(File folder, @NonNull GoogleDriveFile file, ProgressOutputStream.DownloadProgressListener listener) throws BackendException {
        File destination = new File(folder, file.getName());
        try (OutputStream outputStream = new ProgressOutputStream(destination, file.getSize(), listener)) {
            mDriveService.files().get(file.getId())
                    .executeMediaAndDownloadTo(outputStream);
        } catch (IOException e) {
            throw new BackendException("Failed to download file: " + e.getMessage(), true);
        } catch (Exception e) {
            throw new BackendException("Download error: " + e.getMessage(), false);
        }
        return destination;
    }

    @Override
    public List<IFile> list(GoogleDriveFile folder) throws BackendException {
        List<IFile> fileList = new ArrayList<>();
        try {
            String parentFolderId = (folder != null) ? folder.getId() : mAppFolderId;
            String query = "'" + parentFolderId + "' in parents and trashed=false";
            
            FileList result = mDriveService.files().list()
                    .setQ(query)
                    .setFields("files(id, name, mimeType, size, modifiedTime, parents)")
                    .setSpaces("drive")
                    .execute();
            
            if (result.getFiles() != null) {
                for (com.google.api.services.drive.model.File driveFile : result.getFiles()) {
                    fileList.add(new GoogleDriveFile(driveFile));
                }
            }
        } catch (IOException e) {
            throw new BackendException("Failed to list files: " + e.getMessage(), true);
        } catch (Exception e) {
            throw new BackendException("List error: " + e.getMessage(), false);
        }
        return fileList;
    }

    @Override
    protected GoogleDriveFile newFolder(GoogleDriveFile parent, String name) throws BackendException {
        try {
            String parentFolderId = (parent != null) ? parent.getId() : mAppFolderId;
            
            com.google.api.services.drive.model.File fileMetadata = new com.google.api.services.drive.model.File();
            fileMetadata.setName(name);
            fileMetadata.setMimeType(MIME_TYPE_FOLDER);
            fileMetadata.setParents(Collections.singletonList(parentFolderId));
            
            com.google.api.services.drive.model.File createdFolder = mDriveService.files()
                    .create(fileMetadata)
                    .setFields("id, name, mimeType, modifiedTime, parents")
                    .execute();
            
            return new GoogleDriveFile(createdFolder);
        } catch (IOException e) {
            throw new BackendException("Failed to create folder: " + e.getMessage(), true);
        } catch (Exception e) {
            throw new BackendException("Folder creation error: " + e.getMessage(), false);
        }
    }

    private String getOrCreateAppFolder() throws IOException, BackendException {
        try {
            // First, try to find existing app folder in root
            String query = "name='" + APP_FOLDER_NAME + "' and mimeType='" + MIME_TYPE_FOLDER + 
                          "' and trashed=false and 'root' in parents";
            
            FileList result = mDriveService.files().list()
                    .setQ(query)
                    .setSpaces("drive")
                    .setFields("files(id, name)")
                    .execute();
            
            if (result.getFiles() != null && !result.getFiles().isEmpty()) {
                return result.getFiles().get(0).getId();
            }
            
            // Create new app folder if not found
            com.google.api.services.drive.model.File fileMetadata = new com.google.api.services.drive.model.File();
            fileMetadata.setName(APP_FOLDER_NAME);
            fileMetadata.setMimeType(MIME_TYPE_FOLDER);
            
            com.google.api.services.drive.model.File createdFolder = mDriveService.files()
                    .create(fileMetadata)
                    .setFields("id, name, mimeType, modifiedTime, parents")
                    .execute();
            
            if (createdFolder != null && createdFolder.getId() != null) {
                return createdFolder.getId();
            } else {
                throw new BackendException("Failed to create app folder: API returned null response", true);
            }
        } catch (IOException e) {
            throw new BackendException("Network error accessing Google Drive: " + e.getMessage(), true);
        } catch (Exception e) {
            String errorMessage = e.getMessage();
            if (errorMessage == null || errorMessage.trim().isEmpty()) {
                errorMessage = "Unknown error occurred";
            }
            throw new BackendException("Failed to get/create app folder: " + errorMessage, true);
        }
    }
}
