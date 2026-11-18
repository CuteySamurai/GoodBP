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

/**
 * Feature 6: one big button, no menus. Family settings are behind a long-press on
 * the small corner area so she can't wander into them.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(onStart: () -> Unit, onHistory: () -> Unit, onFamilyLongPress: () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        BigButtonScreen(
            label = stringResource(R.string.home_button), onPress = onStart,
            secondaryLabel = "📋  " + stringResource(R.string.history), onSecondary = onHistory,
        )
        val context = androidx.compose.ui.platform.LocalContext.current
        val hint = stringResource(R.string.hold_for_family)
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .size(64.dp)
                .combinedClickable(
                    onClick = { android.widget.Toast.makeText(context, hint, android.widget.Toast.LENGTH_SHORT).show() },
                    onLongClick = onFamilyLongPress,
                ),
            contentAlignment = Alignment.Center,
        ) {
            // Small, grey gear: visible to family, but a tap alone does nothing for her.
            Text("⚙", fontSize = 28.sp, color = Color.LightGray)
        }
    }
}

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
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5F6368)),
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                ) {
                    Text(secondaryLabel, style = MaterialTheme.typography.headlineLarge)
                }
            }
        }
    }
}
