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

/**
 * Model class for transaction description suggestions with full category information.
 */
public class SuggestionItem {
    
    private final String description;
    private final String categoryName;
    private final Category category;
    
    public SuggestionItem(String description, String categoryName, Category category) {
        this.description = description;
        this.categoryName = categoryName;
        this.category = category;
    }
    
    public String getDescription() {
        return description;
    }
    
    public String getCategoryName() {
        return categoryName;
    }
    
    public Category getCategory() {
        return category;
    }
    
    @Override
    public String toString() {
        if (categoryName != null && !categoryName.isEmpty()) {
            return description + " (" + categoryName + ")";
        }
        return description;
    }
}

