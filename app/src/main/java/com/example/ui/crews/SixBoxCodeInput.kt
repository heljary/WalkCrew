package com.example.ui.crews

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.PlusJakartaSansFamily
import com.example.ui.theme.WalkCrewTheme

@Composable
fun SixBoxCodeInput(
    code: String,
    onCodeChange: (String) -> Unit,
    onComplete: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val focusRequesters = remember { List(6) { FocusRequester() } }

    LaunchedEffect(Unit) {
        focusRequesters.firstOrNull()?.requestFocus()
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until 6) {
            val char = if (i < code.length) code[i].toString() else ""
            val isFocused = code.length == i || (i == 5 && code.length == 6)

            Box(
                modifier = Modifier
                    .size(width = 48.dp, height = 54.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(14.dp)
                    )
                    .border(
                        width = if (isFocused) 2.dp else 1.dp,
                        color = if (isFocused) MaterialTheme.colorScheme.primary else WalkCrewTheme.extendedColors.divider,
                        shape = RoundedCornerShape(14.dp)
                    )
                    .clickable {
                        val targetIndex = code.length.coerceAtMost(5)
                        focusRequesters[targetIndex].requestFocus()
                    }
                    .testTag("code_box_$i"),
                contentAlignment = Alignment.Center
            ) {
                // Individual invisible text field managing focus & typing
                BasicTextField(
                    value = TextFieldValue(
                        text = char,
                        selection = TextRange(char.length)
                    ),
                    onValueChange = { newValue ->
                        val input = newValue.text.uppercase()
                        if (input.isNotEmpty()) {
                            val newChar = input.last()
                            val sb = StringBuilder(code)
                            if (i < sb.length) {
                                sb.setCharAt(i, newChar)
                            } else {
                                sb.append(newChar)
                            }
                            val updated = sb.toString().take(6)
                            onCodeChange(updated)

                            if (i < 5 && updated.length > i) {
                                focusRequesters[i + 1].requestFocus()
                            }
                            if (updated.length == 6) {
                                onComplete(updated)
                            }
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .focusRequester(focusRequesters[i])
                        .onKeyEvent { keyEvent ->
                            if (keyEvent.key == Key.Backspace) {
                                if (char.isEmpty() && i > 0) {
                                    val updated = code.dropLast(1)
                                    onCodeChange(updated)
                                    focusRequesters[i - 1].requestFocus()
                                    true
                                } else if (char.isNotEmpty()) {
                                    val updated = if (i < code.length) {
                                        code.removeRange(i, i + 1)
                                    } else {
                                        code.dropLast(1)
                                    }
                                    onCodeChange(updated)
                                    true
                                } else false
                            } else false
                        },
                    singleLine = true,
                    textStyle = TextStyle(
                        fontFamily = PlusJakartaSansFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = WalkCrewTheme.extendedColors.textPrimary,
                        textAlign = TextAlign.Center
                    ),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters,
                        autoCorrectEnabled = false,
                        keyboardType = KeyboardType.Ascii,
                        imeAction = if (i == 5) ImeAction.Done else ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (code.length == 6) onComplete(code)
                        }
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    decorationBox = { innerTextField ->
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(48.dp)
                        ) {
                            if (char.isEmpty()) {
                                Text(
                                    text = "-",
                                    style = TextStyle(
                                        fontFamily = PlusJakartaSansFamily,
                                        fontWeight = FontWeight.Normal,
                                        fontSize = 18.sp,
                                        color = WalkCrewTheme.extendedColors.textSecondary.copy(alpha = 0.4f),
                                        textAlign = TextAlign.Center
                                    )
                                )
                            }
                            innerTextField()
                        }
                    }
                )
            }
        }
    }
}
