package com.goodbp.ui.home

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goodbp.R

/** Single big button screen, used by the full-screen reminder. */
@Composable
fun BigButtonScreen(
    label: String,
    onPress: () -> Unit,
    color: Color = MaterialTheme.colorScheme.primary,
    secondaryLabel: String? = null,
    onSecondary: () -> Unit = {},
) {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Button(
                onClick = onPress,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = color),
                modifier = Modifier.fillMaxWidth().aspectRatio(1f),
            ) {
                Text(label, style = MaterialTheme.typography.displayLarge, textAlign = TextAlign.Center)
            }
            if (secondaryLabel != null) {
                androidx.compose.foundation.layout.Spacer(Modifier.size(32.dp))
                // Second, clearly smaller button so the main action stays obvious.
                Button(
                    onClick = onSecondary,
                    colors = ButtonDefaults.buttonColors(containerColor = com.goodbp.ui.theme.Palette.BlueDeep),
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                ) {
                    Text(secondaryLabel, style = MaterialTheme.typography.headlineLarge)
                }
            }
        }
    }
}
