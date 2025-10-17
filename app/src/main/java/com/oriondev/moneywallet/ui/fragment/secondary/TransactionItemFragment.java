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

package com.oriondev.moneywallet.ui.fragment.secondary;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.oriondev.moneywallet.R;
import com.oriondev.moneywallet.model.Attachment;
import com.oriondev.moneywallet.model.CurrencyUnit;
import com.oriondev.moneywallet.storage.database.Contract;
import com.oriondev.moneywallet.storage.database.DataContentProvider;
import com.oriondev.moneywallet.storage.database.SQLiteDataException;
import com.oriondev.moneywallet.ui.activity.NewEditItemActivity;
import com.oriondev.moneywallet.ui.activity.NewEditTransactionActivity;
import com.oriondev.moneywallet.ui.fragment.base.SecondaryPanelFragment;
import com.oriondev.moneywallet.ui.view.AttachmentView;
import com.oriondev.moneywallet.ui.view.theme.ThemedDialog;
import com.oriondev.moneywallet.utils.CurrencyManager;
import com.oriondev.moneywallet.utils.DateFormatter;
import com.oriondev.moneywallet.utils.DateUtils;
import com.oriondev.moneywallet.utils.MoneyFormatter;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by andrea on 03/04/18.
 */
public class TransactionItemFragment extends SecondaryPanelFragment implements LoaderManager.LoaderCallbacks<Cursor>,AttachmentView.Controller {

    private static final int TRANSACTION_LOADER_ID = 54347;
    private static final int PEOPLE_LOADER_ID = 54348;
    private static final int ATTACHMENTS_LOADER_ID = 54349;

    private View mProgressLayout;
    private View mMainLayout;

    private TextView mCurrencyTextView;
    private TextView mMoneyTextView;

    private TextView mDescriptionTextView;
    private TextView mCategoryTextView;
    private TextView mDateTimeTextView;
    private TextView mWalletTextView;
    private TextView mEventTextView;
    private TextView mPeopleTextView;
    private TextView mPlaceTextView;
    private TextView mNoteTextView;
    private CheckBox mConfirmedCheckBox;
    private CheckBox mCountInTotalCheckBox;
    private AttachmentView mAttachmentView;

    private MoneyFormatter mMoneyFormatter = MoneyFormatter.getInstance();

    @Override
    protected void onCreateHeaderView(LayoutInflater inflater, @NonNull ViewGroup parent, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.layout_header_show_money_item, parent, true);
        mCurrencyTextView = view.findViewById(R.id.currency_text_view);
        mMoneyTextView = view.findViewById(R.id.money_text_view);
    }

    @Override
    protected void onCreateBodyView(LayoutInflater inflater, @NonNull ViewGroup parent, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.layout_panel_show_transaction_item, parent, true);
        mProgressLayout = view.findViewById(R.id.secondary_panel_progress_wheel);
        mMainLayout = view.findViewById(R.id.secondary_panel_layout);
        mDescriptionTextView = view.findViewById(R.id.description_text_view);
        mCategoryTextView = view.findViewById(R.id.category_text_view);
        mDateTimeTextView = view.findViewById(R.id.date_time_text_view);
        mWalletTextView = view.findViewById(R.id.wallet_text_view);
        mEventTextView = view.findViewById(R.id.event_text_view);
        mPeopleTextView = view.findViewById(R.id.people_text_view);
        mPlaceTextView = view.findViewById(R.id.place_text_view);
        mNoteTextView = view.findViewById(R.id.note_text_view);
        mConfirmedCheckBox = view.findViewById(R.id.confirmed_checkbox);
        mCountInTotalCheckBox = view.findViewById(R.id.count_in_total_checkbox);
        mAttachmentView = view.findViewById(R.id.attachment_view);
        mAttachmentView.setAllowRemove(false);
        mAttachmentView.setController(this);
    }

    @Override
    protected String getTitle() {
        return getString(R.string.title_fragment_item_transaction);
    }

    @Override
    protected int onInflateMenu() {
        return R.menu.menu_clone_edit_delete_transaction;
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_clone_transaction) {
            cloneTransaction();
        } else if (itemId == R.id.action_edit_item) {
            Intent intent = new Intent(getActivity(), NewEditTransactionActivity.class);
            intent.putExtra(NewEditItemActivity.MODE, NewEditItemActivity.Mode.EDIT_ITEM);
            intent.putExtra(NewEditItemActivity.ID, getItemId());
            startActivity(intent);
        } else if (itemId == R.id.action_delete_item) {
            showDeleteDialog(getActivity());
        }
        return false;
    }

    private void cloneTransaction() {
        Activity activity = getActivity();
        if (activity != null) {
            Uri uri = ContentUris.withAppendedId(DataContentProvider.CONTENT_TRANSACTIONS, getItemId());
            ContentResolver contentResolver = activity.getContentResolver();
            
            // Query the transaction data
            String[] projection = new String[] {
                    Contract.Transaction.MONEY,
                    Contract.Transaction.DESCRIPTION,
                    Contract.Transaction.NOTE,
                    Contract.Transaction.CONFIRMED,
                    Contract.Transaction.COUNT_IN_TOTAL,
                    Contract.Transaction.CATEGORY_ID,
                    Contract.Transaction.CATEGORY_NAME,
                    Contract.Transaction.CATEGORY_ICON,
                    Contract.Transaction.CATEGORY_TYPE,
                    Contract.Transaction.CATEGORY_TAG,
                    Contract.Transaction.WALLET_ID,
                    Contract.Transaction.WALLET_NAME,
                    Contract.Transaction.WALLET_ICON,
                    Contract.Transaction.WALLET_CURRENCY,
                    Contract.Transaction.EVENT_ID,
                    Contract.Transaction.EVENT_NAME,
                    Contract.Transaction.EVENT_ICON,
                    Contract.Transaction.EVENT_START_DATE,
                    Contract.Transaction.EVENT_END_DATE,
                    Contract.Transaction.PLACE_ID,
                    Contract.Transaction.PLACE_NAME,
                    Contract.Transaction.PLACE_ICON,
                    Contract.Transaction.PLACE_ADDRESS,
                    Contract.Transaction.PLACE_LATITUDE,
                    Contract.Transaction.PLACE_LONGITUDE,
                    Contract.Transaction.TYPE
            };
            
            Cursor cursor = contentResolver.query(uri, projection, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                Intent intent = new Intent(activity, NewEditTransactionActivity.class);
                intent.putExtra(NewEditItemActivity.MODE, NewEditItemActivity.Mode.NEW_ITEM);
                intent.putExtra(NewEditTransactionActivity.TYPE, cursor.getInt(cursor.getColumnIndex(Contract.Transaction.TYPE)));
                
                // Pass transaction data as extras
                intent.putExtra("clone_money", cursor.getLong(cursor.getColumnIndex(Contract.Transaction.MONEY)));
                
                String description = cursor.getString(cursor.getColumnIndex(Contract.Transaction.DESCRIPTION));
                if (description != null) {
                    intent.putExtra("clone_description", description);
                }
                
                String note = cursor.getString(cursor.getColumnIndex(Contract.Transaction.NOTE));
                if (note != null) {
                    intent.putExtra("clone_note", note);
                }
                
                intent.putExtra("clone_confirmed", cursor.getInt(cursor.getColumnIndex(Contract.Transaction.CONFIRMED)) == 1);
                intent.putExtra("clone_count_in_total", cursor.getInt(cursor.getColumnIndex(Contract.Transaction.COUNT_IN_TOTAL)) == 1);
                
                // Create and pass category
                if (!cursor.isNull(cursor.getColumnIndex(Contract.Transaction.CATEGORY_ID))) {
                    com.oriondev.moneywallet.model.Category category = new com.oriondev.moneywallet.model.Category(
                            cursor.getLong(cursor.getColumnIndex(Contract.Transaction.CATEGORY_ID)),
                            cursor.getString(cursor.getColumnIndex(Contract.Transaction.CATEGORY_NAME)),
                            com.oriondev.moneywallet.utils.IconLoader.parse(cursor.getString(cursor.getColumnIndex(Contract.Transaction.CATEGORY_ICON))),
                            Contract.CategoryType.fromValue(cursor.getInt(cursor.getColumnIndex(Contract.Transaction.CATEGORY_TYPE))),
                            cursor.getString(cursor.getColumnIndex(Contract.Transaction.CATEGORY_TAG))
                    );
                    intent.putExtra("clone_category", category);
                }
                
                // Create and pass wallet
                if (!cursor.isNull(cursor.getColumnIndex(Contract.Transaction.WALLET_ID))) {
                    com.oriondev.moneywallet.model.Wallet wallet = new com.oriondev.moneywallet.model.Wallet(
                            cursor.getLong(cursor.getColumnIndex(Contract.Transaction.WALLET_ID)),
                            cursor.getString(cursor.getColumnIndex(Contract.Transaction.WALLET_NAME)),
                            com.oriondev.moneywallet.utils.IconLoader.parse(cursor.getString(cursor.getColumnIndex(Contract.Transaction.WALLET_ICON))),
                            CurrencyManager.getCurrency(cursor.getString(cursor.getColumnIndex(Contract.Transaction.WALLET_CURRENCY))),
                            0L, 0L
                    );
                    intent.putExtra("clone_wallet", wallet);
                }
                
                // Create and pass event
                if (!cursor.isNull(cursor.getColumnIndex(Contract.Transaction.EVENT_ID))) {
                    com.oriondev.moneywallet.model.Event event = new com.oriondev.moneywallet.model.Event(
                            cursor.getLong(cursor.getColumnIndex(Contract.Transaction.EVENT_ID)),
                            cursor.getString(cursor.getColumnIndex(Contract.Transaction.EVENT_NAME)),
                            com.oriondev.moneywallet.utils.IconLoader.parse(cursor.getString(cursor.getColumnIndex(Contract.Transaction.EVENT_ICON))),
                            DateUtils.getDateFromSQLDateString(cursor.getString(cursor.getColumnIndex(Contract.Transaction.EVENT_START_DATE))),
                            DateUtils.getDateFromSQLDateString(cursor.getString(cursor.getColumnIndex(Contract.Transaction.EVENT_END_DATE)))
                    );
                    intent.putExtra("clone_event", event);
                }
                
                // Create and pass place
                if (!cursor.isNull(cursor.getColumnIndex(Contract.Transaction.PLACE_ID))) {
                    com.oriondev.moneywallet.model.Place place = new com.oriondev.moneywallet.model.Place(
                            cursor.getLong(cursor.getColumnIndex(Contract.Transaction.PLACE_ID)),
                            cursor.getString(cursor.getColumnIndex(Contract.Transaction.PLACE_NAME)),
                            com.oriondev.moneywallet.utils.IconLoader.parse(cursor.getString(cursor.getColumnIndex(Contract.Transaction.PLACE_ICON))),
                            cursor.getString(cursor.getColumnIndex(Contract.Transaction.PLACE_ADDRESS)),
                            cursor.isNull(cursor.getColumnIndex(Contract.Transaction.PLACE_LATITUDE)) ? null : cursor.getDouble(cursor.getColumnIndex(Contract.Transaction.PLACE_LATITUDE)),
                            cursor.isNull(cursor.getColumnIndex(Contract.Transaction.PLACE_LONGITUDE)) ? null : cursor.getDouble(cursor.getColumnIndex(Contract.Transaction.PLACE_LONGITUDE))
                    );
                    intent.putExtra("clone_place", place);
                }
                
                cursor.close();
                
                // Query and pass people
                Uri peopleUri = Uri.withAppendedPath(uri, "people");
                String[] peopleProjection = new String[] {
                        Contract.Person.ID,
                        Contract.Person.NAME,
                        Contract.Person.ICON
                };
                Cursor peopleCursor = contentResolver.query(peopleUri, peopleProjection, null, null, null);
                if (peopleCursor != null && peopleCursor.moveToFirst()) {
                    com.oriondev.moneywallet.model.Person[] people = new com.oriondev.moneywallet.model.Person[peopleCursor.getCount()];
                    for (int i = 0; peopleCursor.moveToPosition(i) && i < peopleCursor.getCount(); i++) {
                        people[i] = new com.oriondev.moneywallet.model.Person(
                                peopleCursor.getLong(peopleCursor.getColumnIndex(Contract.Person.ID)),
                                peopleCursor.getString(peopleCursor.getColumnIndex(Contract.Person.NAME)),
                                com.oriondev.moneywallet.utils.IconLoader.parse(peopleCursor.getString(peopleCursor.getColumnIndex(Contract.Person.ICON)))
                        );
                    }
                    intent.putExtra("clone_people", people);
                    peopleCursor.close();
                }
                
                // Start the activity
                startActivity(intent);
            }
            
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private void showDeleteDialog(Context context) {
        ThemedDialog.buildMaterialDialog(context)
                .title(R.string.title_warning)
                .content(R.string.message_delete_transaction)
                .positiveText(android.R.string.ok)
                .negativeText(android.R.string.cancel)
                .onPositive(new MaterialDialog.SingleButtonCallback() {

                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        Activity activity = getActivity();
                        if (activity != null) {
                            try {
                                Uri uri = ContentUris.withAppendedId(DataContentProvider.CONTENT_TRANSACTIONS, getItemId());
                                ContentResolver contentResolver = activity.getContentResolver();
                                contentResolver.delete(uri, null, null);
                                navigateBackSafely();
                                showItemId(0L);
                            } catch (SQLiteDataException e) {
                                if (e.getErrorCode() == Contract.ErrorCode.TRANSACTION_USED_IN_TRANSFER) {
                                    ThemedDialog.buildMaterialDialog(activity)
                                            .title(R.string.title_error)
                                            .content(R.string.message_error_delete_transaction_of_transfer)
                                            .positiveText(android.R.string.ok)
                                            .show();
                                }
                            }
                        }
                    }

                })
                .show();
    }

    @Override
    protected void onShowItemId(long itemId) {
        setLoadingScreen(true);
        getLoaderManager().restartLoader(TRANSACTION_LOADER_ID, null, this);
        getLoaderManager().restartLoader(PEOPLE_LOADER_ID, null, this);
        getLoaderManager().restartLoader(ATTACHMENTS_LOADER_ID, null, this);
    }

    private void setLoadingScreen(boolean loading) {
        if (loading) {
            mMoneyTextView.setText(null);
            mCurrencyTextView.setText(null);
            mDescriptionTextView.setText(null);
            mCategoryTextView.setText(null);
            mDateTimeTextView.setText(null);
            mWalletTextView.setText(null);
            mEventTextView.setText(null);
            mPeopleTextView.setText(null);
            mPlaceTextView.setText(null);
            mNoteTextView.setText(null);
            mConfirmedCheckBox.setText(null);
            mCountInTotalCheckBox.setText(null);
            mAttachmentView.setAttachments(null);
            mProgressLayout.setVisibility(View.VISIBLE);
            mMainLayout.setVisibility(View.GONE);
        } else {
            mProgressLayout.setVisibility(View.GONE);
            mMainLayout.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Activity activity = getActivity();
        if (activity != null) {
            Uri uri = ContentUris.withAppendedId(DataContentProvider.CONTENT_TRANSACTIONS, getItemId());
            if (id == TRANSACTION_LOADER_ID) {
                String[] projection = new String[] {
                        "*"
                };
                return new CursorLoader(getActivity(), uri, projection, null, null, null);
            } else if (id == PEOPLE_LOADER_ID) {
                String[] projection = new String[] {
                        "*"
                };
                return new CursorLoader(getActivity(), Uri.withAppendedPath(uri, "people"), projection, null, null, null);
            } else if (id == ATTACHMENTS_LOADER_ID) {
                String[] projection = new String[] {
                        Contract.Attachment.ID,
                        Contract.Attachment.FILE,
                        Contract.Attachment.NAME,
                        Contract.Attachment.TYPE,
                        Contract.Attachment.SIZE
                };
                return new CursorLoader(getActivity(), Uri.withAppendedPath(uri, "attachments"), projection, null, null, null);
            }
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (loader.getId() == TRANSACTION_LOADER_ID) {
            if (cursor != null && cursor.moveToFirst()) {
                String iso = cursor.getString(cursor.getColumnIndex(Contract.Transaction.WALLET_CURRENCY));
                CurrencyUnit currency = CurrencyManager.getCurrency(iso);
                if (currency != null) {
                    mCurrencyTextView.setText(currency.getSymbol());
                } else {
                    mCurrencyTextView.setText("?");
                }
                long money = cursor.getLong(cursor.getColumnIndex(Contract.Transaction.MONEY));
                mMoneyTextView.setText(mMoneyFormatter.getNotTintedString(currency, money, MoneyFormatter.CurrencyMode.ALWAYS_HIDDEN));
                String description = cursor.getString(cursor.getColumnIndex(Contract.Transaction.DESCRIPTION));
                if (!TextUtils.isEmpty(description)) {
                    mDescriptionTextView.setText(description);
                    mDescriptionTextView.setVisibility(View.VISIBLE);
                } else {
                    mDescriptionTextView.setVisibility(View.GONE);
                }
                String category = cursor.getString(cursor.getColumnIndex(Contract.Transaction.CATEGORY_NAME));
                mCategoryTextView.setText(category);
                Date datetime = DateUtils.getDateFromSQLDateTimeString(cursor.getString(cursor.getColumnIndex(Contract.Transaction.DATE)));
                DateFormatter.applyDateTime(mDateTimeTextView, datetime);
                String wallet = cursor.getString(cursor.getColumnIndex(Contract.Transaction.WALLET_NAME));
                mWalletTextView.setText(wallet);
                String event = cursor.getString(cursor.getColumnIndex(Contract.Transaction.EVENT_NAME));
                if (!TextUtils.isEmpty(event)) {
                    mEventTextView.setText(event);
                    mEventTextView.setVisibility(View.VISIBLE);
                } else {
                    mEventTextView.setVisibility(View.GONE);
                }
                String place = cursor.getString(cursor.getColumnIndex(Contract.Transaction.PLACE_NAME));
                if (!TextUtils.isEmpty(place)) {
                    mPlaceTextView.setText(place);
                    mPlaceTextView.setVisibility(View.VISIBLE);
                } else {
                    mPlaceTextView.setVisibility(View.GONE);
                }
                String note = cursor.getString(cursor.getColumnIndex(Contract.Transaction.NOTE));
                if (!TextUtils.isEmpty(note)) {
                    mNoteTextView.setText(note);
                    mNoteTextView.setVisibility(View.VISIBLE);
                } else {
                    mNoteTextView.setVisibility(View.GONE);
                }
                mConfirmedCheckBox.setChecked(cursor.getInt(cursor.getColumnIndex(Contract.Transaction.CONFIRMED)) == 1);
                mCountInTotalCheckBox.setChecked(cursor.getInt(cursor.getColumnIndex(Contract.Transaction.COUNT_IN_TOTAL)) == 1);
            } else {
                showItemId(0L);
            }
            setLoadingScreen(false);
        } else if (loader.getId() == PEOPLE_LOADER_ID) {
            if (cursor != null && cursor.moveToFirst()) {
                StringBuilder builder = new StringBuilder();
                for (int i = 0; cursor.moveToPosition(i) && i < cursor.getCount(); i++) {
                    if (i != 0) {
                        builder.append(", ");
                    }
                    String name = cursor.getString(cursor.getColumnIndex(Contract.Person.NAME));
                    builder.append(name);
                }
                mPeopleTextView.setText(builder);
                mPeopleTextView.setVisibility(View.VISIBLE);
            } else {
                mPeopleTextView.setVisibility(View.GONE);
            }
        } else if (loader.getId() == ATTACHMENTS_LOADER_ID) {
            if (cursor != null && cursor.moveToFirst()) {
                List<Attachment> attachments = new ArrayList<>();
                for (int i = 0; cursor.moveToPosition(i) && i < cursor.getCount(); i++) {
                    attachments.add(new Attachment(
                            cursor.getLong(cursor.getColumnIndex(Contract.Attachment.ID)),
                            cursor.getString(cursor.getColumnIndex(Contract.Attachment.FILE)),
                            cursor.getString(cursor.getColumnIndex(Contract.Attachment.NAME)),
                            cursor.getString(cursor.getColumnIndex(Contract.Attachment.TYPE)),
                            cursor.getLong(cursor.getColumnIndex(Contract.Attachment.SIZE))
                    ));
                }
                mAttachmentView.setAttachments(attachments);
                mAttachmentView.setVisibility(View.VISIBLE);
            } else {
                mAttachmentView.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // nothing to release
    }

    @Override
    public void onAttachmentClick(Attachment attachment) {
        Attachment.openAttachment(getActivity(), attachment);
    }

    @Override
    public void onAttachmentDelete(Attachment attachment) {
        // attachment removal is not allowed here
    }
}