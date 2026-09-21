package com.test.propsid4sakura.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.test.propsid4sakura.R

@Composable
fun CategoryChips(
    categories: List<String>,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val allLabel = stringResource(R.string.tab_all)
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState())
    ) {
        FilterChip(
            selected = selectedCategory == "",
            onClick = { onCategorySelected("") },
            label = { Text(allLabel) },
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        categories.forEach { category ->
            FilterChip(
                selected = selectedCategory == category,
                onClick = { onCategorySelected(category) },
                label = { Text(category) },
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}
