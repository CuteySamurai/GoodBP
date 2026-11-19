package com.goodbp.ui.info

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goodbp.R

/** Placeholder: will hold articles from reliable hospital sources, chosen by the family. */
@Composable
fun InfoTab() = Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Text(stringResource(R.string.tab_info), style = MaterialTheme.typography.headlineLarge)
    Text(stringResource(R.string.info_coming), fontSize = 22.sp, color = Color.Gray)
}
