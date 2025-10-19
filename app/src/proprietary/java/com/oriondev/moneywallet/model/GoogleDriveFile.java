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

package com.oriondev.moneywallet.model;

import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by andre on 21/03/2018.
 * Updated to use Google Drive REST API v3
 */
public class GoogleDriveFile implements IFile, Parcelable {

    private static final String ID = "id";
    private static final String NAME = "name";
    private static final String SIZE = "size";
    private static final String DIRECTORY = "directory";
    private static final String MIME_TYPE_FOLDER = "application/vnd.google-apps.folder";

    private final String mId;
    private final String mName;
    private final long mSize;
    private final boolean mIsDirectory;

    private GoogleDriveFile(Parcel in) {
        mId = in.readString();
        mName = in.readString();
        mSize = in.readLong();
        mIsDirectory = in.readByte() != 0;
    }

    /**
     * Constructor for REST API v3 File object
     */
    public GoogleDriveFile(com.google.api.services.drive.model.File file) {
        mId = file.getId();
        mName = file.getName();
        mIsDirectory = MIME_TYPE_FOLDER.equals(file.getMimeType());
        
        // Size might be null for folders or Google Docs
        Long size = file.getSize();
        mSize = (size != null) ? size : 0L;
    }

    public GoogleDriveFile(String encoded) {
        try {
            JSONObject object = new JSONObject(encoded);
            mId = object.getString(ID);
            mName = object.optString(NAME);
            mSize = object.optLong(SIZE);
            mIsDirectory = object.getBoolean(DIRECTORY);
        } catch (JSONException e) {
            throw new RuntimeException("Cannot decode file from string: " + e.getMessage());
        }
    }

    public static final Creator<GoogleDriveFile> CREATOR = new Creator<GoogleDriveFile>() {

        @Override
        public GoogleDriveFile createFromParcel(Parcel in) {
            return new GoogleDriveFile(in);
        }

        @Override
        public GoogleDriveFile[] newArray(int size) {
            return new GoogleDriveFile[size];
        }

    };

    @Override
    public String getName() {
        return mName;
    }

    @Override
    public String getExtension() {
        int index = mName.lastIndexOf(".");
        return index >= 0 ? mName.substring(index) : null;
    }

    @Override
    public boolean isDirectory() {
        return mIsDirectory;
    }

    @Override
    public long getSize() {
        return mSize;
    }

    @Override
    public String encodeToString() {
        try {
            JSONObject object = new JSONObject();
            object.put(ID, mId);
            object.put(NAME, mName);
            object.put(SIZE, mSize);
            object.put(DIRECTORY, mIsDirectory);
            return object.toString();
        } catch (JSONException e) {
            throw new RuntimeException("Cannot encode file to string: " + e.getMessage());
        }
    }

    /**
     * Get the file ID for REST API v3
     */
    public String getId() {
        return mId;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mId);
        dest.writeString(mName);
        dest.writeLong(mSize);
        dest.writeByte((byte) (mIsDirectory ? 1 : 0));
    }
}
