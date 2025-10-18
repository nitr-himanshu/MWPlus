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

package com.oriondev.moneywallet.utils;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;

import com.oriondev.moneywallet.model.Category;
import com.oriondev.moneywallet.model.SuggestionItem;
import com.oriondev.moneywallet.storage.database.Contract;
import com.oriondev.moneywallet.storage.database.DataContentProvider;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Helper class to provide transaction description suggestions and category recommendations
 * based on previously entered transactions.
 */
public class TransactionSuggestionHelper {

    private static final int MIN_QUERY_LENGTH = 3;
    private static final int MAX_SUGGESTIONS = 5;

    /**
     * Get description suggestions that match the given query string.
     * Only returns suggestions if query is at least MIN_QUERY_LENGTH characters.
     *
     * @param contentResolver The content resolver to query the database
     * @param query The partial description to search for
     * @return List of SuggestionItem objects containing description and full category info
     */
    public static List<SuggestionItem> getDescriptionSuggestions(ContentResolver contentResolver, String query) {
        List<SuggestionItem> suggestions = new ArrayList<>();
        
        if (query == null || query.trim().length() < MIN_QUERY_LENGTH) {
            return suggestions;
        }

        String trimmedQuery = query.trim();
        
        Uri uri = DataContentProvider.CONTENT_TRANSACTIONS;
        String[] projection = new String[] {
                Contract.Transaction.DESCRIPTION,
                Contract.Transaction.CATEGORY_ID,
                Contract.Transaction.CATEGORY_NAME,
                Contract.Transaction.CATEGORY_ICON,
                Contract.Transaction.CATEGORY_PARENT_ID,
                Contract.Transaction.CATEGORY_TYPE,
                Contract.Transaction.CATEGORY_TAG,
                Contract.Transaction.CATEGORY_SHOW_REPORT
        };
        
        // Query for transactions with descriptions that start with or contain the query
        String selection = Contract.Transaction.DESCRIPTION + " LIKE ?";
        String[] selectionArgs = new String[] { trimmedQuery + "%" };
        String sortOrder = Contract.Transaction.DATE + " DESC";
        
        Cursor cursor = contentResolver.query(uri, projection, selection, selectionArgs, sortOrder);
        
        if (cursor != null) {
            try {
                // Use LinkedHashSet to maintain order while removing duplicates
                // We'll track unique description+category combinations
                Set<String> uniqueKeys = new LinkedHashSet<>();
                
                int descriptionIndex = cursor.getColumnIndex(Contract.Transaction.DESCRIPTION);
                int categoryIdIndex = cursor.getColumnIndex(Contract.Transaction.CATEGORY_ID);
                int categoryNameIndex = cursor.getColumnIndex(Contract.Transaction.CATEGORY_NAME);
                int categoryIconIndex = cursor.getColumnIndex(Contract.Transaction.CATEGORY_ICON);
                int categoryTypeIndex = cursor.getColumnIndex(Contract.Transaction.CATEGORY_TYPE);
                int categoryTagIndex = cursor.getColumnIndex(Contract.Transaction.CATEGORY_TAG);
                
                while (cursor.moveToNext() && suggestions.size() < MAX_SUGGESTIONS) {
                    String description = cursor.getString(descriptionIndex);
                    long categoryId = cursor.getLong(categoryIdIndex);
                    String categoryName = cursor.getString(categoryNameIndex);
                    String categoryIcon = cursor.getString(categoryIconIndex);
                    int categoryType = cursor.getInt(categoryTypeIndex);
                    String categoryTag = cursor.getString(categoryTagIndex);
                    
                    if (description != null && !description.trim().isEmpty()) {
                        description = description.trim();
                        // Create a unique key to avoid duplicates
                        String uniqueKey = description + "|" + categoryId;
                        
                        if (!uniqueKeys.contains(uniqueKey)) {
                            uniqueKeys.add(uniqueKey);
                            
                            // Create full Category object
                            Category category = new Category(
                                    categoryId,
                                    categoryName,
                                    IconLoader.parse(categoryIcon),
                                    Contract.CategoryType.fromValue(categoryType),
                                    categoryTag
                            );
                            
                            suggestions.add(new SuggestionItem(description, categoryName, category));
                        }
                    }
                }
            } finally {
                cursor.close();
            }
        }
        
        return suggestions;
    }

    /**
     * Get the most recently used category for a given description.
     * This allows automatic category suggestion when user enters a description they've used before.
     * This properly handles both parent categories and subcategories.
     *
     * @param contentResolver The content resolver to query the database
     * @param description The exact description to search for
     * @return Category object if found, null otherwise
     */
    public static Category getCategoryForDescription(ContentResolver contentResolver, String description) {
        if (description == null || description.trim().isEmpty()) {
            return null;
        }

        String trimmedDescription = description.trim();
        
        Uri uri = DataContentProvider.CONTENT_TRANSACTIONS;
        String[] projection = new String[] {
                Contract.Transaction.CATEGORY_ID,
                Contract.Transaction.CATEGORY_NAME,
                Contract.Transaction.CATEGORY_ICON,
                Contract.Transaction.CATEGORY_PARENT_ID,
                Contract.Transaction.CATEGORY_TYPE,
                Contract.Transaction.CATEGORY_TAG,
                Contract.Transaction.CATEGORY_SHOW_REPORT
        };
        
        // Look for exact match of description
        String selection = Contract.Transaction.DESCRIPTION + " = ?";
        String[] selectionArgs = new String[] { trimmedDescription };
        // Order by date descending to get the most recent transaction with this description
        String sortOrder = Contract.Transaction.DATE + " DESC LIMIT 1";
        
        Cursor cursor = contentResolver.query(uri, projection, selection, selectionArgs, sortOrder);
        
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    long categoryId = cursor.getLong(cursor.getColumnIndex(Contract.Transaction.CATEGORY_ID));
                    String categoryName = cursor.getString(cursor.getColumnIndex(Contract.Transaction.CATEGORY_NAME));
                    String categoryIcon = cursor.getString(cursor.getColumnIndex(Contract.Transaction.CATEGORY_ICON));
                    int categoryType = cursor.getInt(cursor.getColumnIndex(Contract.Transaction.CATEGORY_TYPE));
                    String categoryTag = cursor.getString(cursor.getColumnIndex(Contract.Transaction.CATEGORY_TAG));
                    
                    // Create category with full information including tag for proper identification
                    // The category ID is already the correct one (parent or subcategory)
                    return new Category(
                            categoryId,
                            categoryName,
                            IconLoader.parse(categoryIcon),
                            Contract.CategoryType.fromValue(categoryType),
                            categoryTag
                    );
                }
            } finally {
                cursor.close();
            }
        }
        
        return null;
    }

    /**
     * Get the most recently used category for a given partial description.
     * This is useful for suggesting categories as the user types.
     * This properly handles both parent categories and subcategories.
     *
     * @param contentResolver The content resolver to query the database
     * @param partialDescription The partial description to search for
     * @return Category object if found, null otherwise
     */
    public static Category getCategoryForPartialDescription(ContentResolver contentResolver, String partialDescription) {
        if (partialDescription == null || partialDescription.trim().length() < MIN_QUERY_LENGTH) {
            return null;
        }

        String trimmedQuery = partialDescription.trim();
        
        Uri uri = DataContentProvider.CONTENT_TRANSACTIONS;
        String[] projection = new String[] {
                Contract.Transaction.CATEGORY_ID,
                Contract.Transaction.CATEGORY_NAME,
                Contract.Transaction.CATEGORY_ICON,
                Contract.Transaction.CATEGORY_PARENT_ID,
                Contract.Transaction.CATEGORY_TYPE,
                Contract.Transaction.CATEGORY_TAG,
                Contract.Transaction.CATEGORY_SHOW_REPORT,
                Contract.Transaction.DESCRIPTION
        };
        
        // Look for descriptions that start with the partial description
        String selection = Contract.Transaction.DESCRIPTION + " LIKE ?";
        String[] selectionArgs = new String[] { trimmedQuery + "%" };
        // Order by date descending to get the most recent match
        String sortOrder = Contract.Transaction.DATE + " DESC LIMIT 1";
        
        Cursor cursor = contentResolver.query(uri, projection, selection, selectionArgs, sortOrder);
        
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    long categoryId = cursor.getLong(cursor.getColumnIndex(Contract.Transaction.CATEGORY_ID));
                    String categoryName = cursor.getString(cursor.getColumnIndex(Contract.Transaction.CATEGORY_NAME));
                    String categoryIcon = cursor.getString(cursor.getColumnIndex(Contract.Transaction.CATEGORY_ICON));
                    int categoryType = cursor.getInt(cursor.getColumnIndex(Contract.Transaction.CATEGORY_TYPE));
                    String categoryTag = cursor.getString(cursor.getColumnIndex(Contract.Transaction.CATEGORY_TAG));
                    
                    // Create category with full information including tag for proper identification
                    // The category ID is already the correct one (parent or subcategory)
                    return new Category(
                            categoryId,
                            categoryName,
                            IconLoader.parse(categoryIcon),
                            Contract.CategoryType.fromValue(categoryType),
                            categoryTag
                    );
                }
            } finally {
                cursor.close();
            }
        }
        
        return null;
    }
}

