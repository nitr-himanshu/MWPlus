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

package com.oriondev.moneywallet.ui.view.text;

import android.content.Context;
import android.graphics.Rect;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.ListPopupWindow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.oriondev.moneywallet.R;
import com.oriondev.moneywallet.model.SuggestionItem;

import java.util.ArrayList;
import java.util.List;

/**
 * Extension of MaterialEditText that provides autocomplete functionality
 * with a dropdown list of suggestions.
 */
public class MaterialAutoCompleteEditText extends MaterialEditText {

    private static final int MIN_CHARS_FOR_AUTOCOMPLETE = 3;
    private static final int AUTOCOMPLETE_DELAY_MS = 300;

    private ListPopupWindow mPopup;
    private SuggestionAdapter mAdapter;
    private AutoCompleteListener mAutoCompleteListener;
    private Runnable mPendingAutocomplete;
    private boolean mBlockCompletion;

    public MaterialAutoCompleteEditText(Context context) {
        super(context);
        initialize();
    }

    public MaterialAutoCompleteEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize();
    }

    public MaterialAutoCompleteEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize();
    }

    private void initialize() {
        mAdapter = new SuggestionAdapter(getContext(), new ArrayList<SuggestionItem>());
        
        mPopup = new ListPopupWindow(getContext());
        mPopup.setAnchorView(this);
        mPopup.setAdapter(mAdapter);
        
        mPopup.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                SuggestionItem suggestion = mAdapter.getItem(position);
                if (suggestion != null) {
                    mBlockCompletion = true;
                    String description = suggestion.getDescription();
                    setText(description);
                    setSelection(description.length());
                    mBlockCompletion = false;
                    dismissDropDown();
                    
                    if (mAutoCompleteListener != null) {
                        mAutoCompleteListener.onSuggestionSelected(suggestion);
                    }
                }
            }
        });

        addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (mBlockCompletion) {
                    return;
                }
                
                // Cancel any pending autocomplete request
                if (mPendingAutocomplete != null) {
                    removeCallbacks(mPendingAutocomplete);
                }
                
                String text = s.toString();
                
                if (text.length() >= MIN_CHARS_FOR_AUTOCOMPLETE) {
                    // Delay the autocomplete request to avoid too many queries
                    mPendingAutocomplete = new Runnable() {
                        @Override
                        public void run() {
                            if (mAutoCompleteListener != null) {
                                mAutoCompleteListener.onTextChanged(text);
                            }
                        }
                    };
                    postDelayed(mPendingAutocomplete, AUTOCOMPLETE_DELAY_MS);
                } else {
                    dismissDropDown();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    /**
     * Update the list of suggestions shown in the dropdown.
     *
     * @param suggestions List of SuggestionItem objects
     */
    public void setSuggestions(List<SuggestionItem> suggestions) {
        if (suggestions != null && !suggestions.isEmpty()) {
            mAdapter.clear();
            mAdapter.addAll(suggestions);
            mAdapter.notifyDataSetChanged();
            
            if (!mPopup.isShowing() && hasFocus()) {
                showDropDown();
            }
        } else {
            dismissDropDown();
        }
    }

    /**
     * Show the dropdown with suggestions.
     */
    private void showDropDown() {
        if (mAdapter.getCount() > 0) {
            // Calculate width to match the edit text
            mPopup.setWidth(getWidth());
            
            // Calculate anchor position
            Rect rect = new Rect();
            getGlobalVisibleRect(rect);
            mPopup.setVerticalOffset(getHeight());
            
            if (!mPopup.isShowing()) {
                mPopup.show();
            }
        }
    }

    /**
     * Dismiss the dropdown.
     */
    private void dismissDropDown() {
        if (mPopup.isShowing()) {
            mPopup.dismiss();
        }
    }

    /**
     * Set a listener for autocomplete events.
     *
     * @param listener The listener to be notified of autocomplete events
     */
    public void setAutoCompleteListener(AutoCompleteListener listener) {
        mAutoCompleteListener = listener;
    }

    @Override
    protected void onFocusChanged(boolean focused, int direction, Rect previouslyFocusedRect) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect);
        if (!focused) {
            dismissDropDown();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        dismissDropDown();
    }

    /**
     * Listener interface for autocomplete events.
     */
    public interface AutoCompleteListener {
        /**
         * Called when the text changes and has at least MIN_CHARS_FOR_AUTOCOMPLETE characters.
         *
         * @param text The current text in the field
         */
        void onTextChanged(String text);

        /**
         * Called when a suggestion is selected from the dropdown.
         *
         * @param suggestion The selected SuggestionItem containing description and category
         */
        void onSuggestionSelected(SuggestionItem suggestion);
    }

    /**
     * Adapter for displaying suggestions with description and category in the dropdown.
     */
    private static class SuggestionAdapter extends ArrayAdapter<SuggestionItem> {
        
        public SuggestionAdapter(@NonNull Context context, @NonNull List<SuggestionItem> objects) {
            super(context, R.layout.item_suggestion_dropdown, objects);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                LayoutInflater inflater = LayoutInflater.from(getContext());
                view = inflater.inflate(R.layout.item_suggestion_dropdown, parent, false);
            }
            
            SuggestionItem item = getItem(position);
            if (item != null) {
                TextView descriptionText = view.findViewById(R.id.description_text);
                TextView categoryText = view.findViewById(R.id.category_text);
                
                descriptionText.setText(item.getDescription());
                
                if (item.getCategoryName() != null && !item.getCategoryName().isEmpty()) {
                    categoryText.setText(item.getCategoryName());
                    categoryText.setVisibility(View.VISIBLE);
                } else {
                    categoryText.setVisibility(View.GONE);
                }
            }
            
            return view;
        }

        @NonNull
        @Override
        public Filter getFilter() {
            return new Filter() {
                @Override
                protected FilterResults performFiltering(CharSequence constraint) {
                    // We handle filtering manually, so just return all items
                    FilterResults results = new FilterResults();
                    results.values = null;
                    results.count = getCount();
                    return results;
                }

                @Override
                protected void publishResults(CharSequence constraint, FilterResults results) {
                    // No-op, we update the adapter manually
                }
            };
        }
    }
}

