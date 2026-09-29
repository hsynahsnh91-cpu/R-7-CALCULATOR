package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gemini.*
import com.example.gemini.database.SelfImprovementRepository
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun R7GeminiChatScreen(
    onBack: () -> Unit,
    onInsertIntoCalculator: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repository = remember { SelfImprovementRepository(context) }
    val backendClient = remember { R7BackendClient(context) }
    val engine = remember { SelfCorrectionEngine(context, backendClient, repository) }

    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val clipboardManager = LocalClipboardManager.current

    var selectedRole by remember { mutableStateOf(ChatbotRole.GENERAL_FLASH) }
    var inputText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var toastMessage by remember { mutableStateOf<String?>(null) }

    // Multi-turn conversation history
    var messages by remember {
        mutableStateOf(
            listOf(
                ChatMessage(
                    id = "init_0",
                    role = "model",
                    text = "مرحبًا بك في مساعد R-7 الحسابي والهندسي.\nيمكنني مساعدتك في حل المسائل الرياضية، العمليات الهندسية، التحويلات، وتوضيح خطوات الحساب بدقة وسرعة."
                )
            )
        )
    }

    // Scroll to bottom on new message
    LaunchedEffect(messages.size, isLoading) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    fun sendMessage(textToSend: String, overrideRole: ChatbotRole? = null) {
        val trimmed = textToSend.trim()
        if (trimmed.isEmpty() || isLoading) return

        val activeRole = overrideRole ?: selectedRole
        if (overrideRole != null) {
            selectedRole = overrideRole
        }

        val userMsg = ChatMessage(
            id = System.currentTimeMillis().toString(),
            role = "user",
            text = trimmed
        )
        val updatedHistory = messages + userMsg
        messages = updatedHistory
        inputText = ""
        isLoading = true

        scope.launch {
            val result = engine.processUserMessage(trimmed, updatedHistory, activeRole)
            isLoading = false
            result.onSuccess { res ->
                messages = messages + ChatMessage(
                    id = (System.currentTimeMillis() + 1).toString(),
                    role = "model",
                    text = res.finalText
                )
            }.onFailure { err ->
                val isOffline = (err as? R7BackendException)?.isOffline == true
                messages = messages + ChatMessage(
                    id = (System.currentTimeMillis() + 1).toString(),
                    role = "model",
                    text = err.message ?: "تعذر استلام رد حاليًا. يُرجى التحقق من اتصال الإنترنت.",
                    isError = true,
                    isOffline = isOffline,
                    failedPrompt = trimmed
                )
            }
        }
    }

    val quickPrompts = listOf(
        "احسب 25 × 25",
        "احسب مقاومة مكافئة لدائرتين على التوازي بقيمة 10 و 20 أوم",
        "احسب مشتقة f(x) = x^3 - 5x + 2",
        "كيف أحسب النسبة المئوية لـ 250 + 15%؟"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "مساعد R-7 الذكي",
                            color = R7TextPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "مساعد الحسابات والعلوم",
                            color = R7TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("btn_back_to_calculator")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "الرجوع للآلة الحاسبة",
                            tint = R7TextPrimary
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            messages = listOf(
                                ChatMessage(
                                    id = System.currentTimeMillis().toString(),
                                    role = "model",
                                    text = "تم بدء جلسة محادثة جديدة. يمكنك طرح أي سؤال أو مسألة حسابية."
                                )
                            )
                            toastMessage = "تم مسح المحادثة"
                        },
                        modifier = Modifier.testTag("btn_clear_chat")
                    ) {
                        Icon(
                            Icons.Default.DeleteOutline,
                            contentDescription = "مسح المحادثة",
                            tint = R7TextSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = R7ChassisDark)
            )
        },
        containerColor = R7ChassisDark
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Mode / Persona Selector Row
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(R7ChassisDark)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(ChatbotRole.values()) { role ->
                    val isSelected = (role == selectedRole)
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedRole = role },
                        label = {
                            Text(
                                text = role.titleAr,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = R7Orange,
                            selectedLabelColor = Color.White,
                            containerColor = R7PanelDark,
                            labelColor = R7TextSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = if (isSelected) R7Orange else Color(0xFF333842)
                        ),
                        modifier = Modifier.testTag("chip_role_${role.id}")
                    )
                }
            }

            HorizontalDivider(color = Color(0xFF282D38), thickness = 1.dp)

            // Conversation Messages List
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(messages, key = { it.id }) { msg ->
                    ChatMessageItem(
                        message = msg,
                        onCopy = {
                            clipboardManager.setText(AnnotatedString(msg.text))
                            toastMessage = "تم نسخ الرد إلى الحافظة"
                        },
                        onInsert = {
                            val candidate = extractMathCandidate(msg.text)
                            onInsertIntoCalculator(candidate)
                            toastMessage = "تم إدراج «$candidate» في الآلة الحاسبة"
                            onBack()
                        },
                        onRetry = { failedPrompt ->
                            sendMessage(failedPrompt)
                        },
                        onRestoreInput = { prompt ->
                            inputText = prompt
                        },
                        onFeedback = { signal ->
                            scope.launch {
                                val lastUserQuery = messages.takeLastWhile { it.id != msg.id }
                                    .lastOrNull { it.role == "user" }?.text ?: ""
                                repository.recordFeedback(
                                    messageId = msg.id,
                                    userQuery = lastUserQuery,
                                    aiAnswer = msg.text,
                                    feedbackType = signal
                                )
                            }
                        }
                    )
                }

                if (isLoading) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(R7PanelDark)
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = R7Orange,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "جاري الحساب وصياغة الرد...",
                                color = R7TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // Quick Prompts Row (shown if few messages)
            if (messages.size <= 2 && !isLoading) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(quickPrompts) { prompt ->
                        SuggestionChip(
                            onClick = { sendMessage(prompt) },
                            label = { Text(prompt, fontSize = 11.sp, color = R7TextPrimary) },
                            colors = SuggestionChipDefaults.suggestionChipColors(containerColor = R7PanelDark),
                            border = SuggestionChipDefaults.suggestionChipBorder(
                                enabled = true,
                                borderColor = Color(0xFF3A3F4B)
                            )
                        )
                    }
                }
            }

            // Floating Toast notification
            AnimatedVisibility(
                visible = toastMessage != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                toastMessage?.let { msg ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            color = Color(0xFF1E293B),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
                        ) {
                            Text(
                                text = msg,
                                color = Color.White,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }
                    }
                    LaunchedEffect(msg) {
                        kotlinx.coroutines.delay(2200L)
                        toastMessage = null
                    }
                }
            }

            // Input Bar
            Surface(
                color = R7ChassisDark,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_gemini_chat"),
                        placeholder = {
                            Text(
                                "اطرح سؤالك أو مسألتك الحسابية...",
                                color = R7TextSecondary,
                                fontSize = 13.sp
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = R7Orange,
                            unfocusedBorderColor = Color(0xFF333842),
                            focusedTextColor = R7TextPrimary,
                            unfocusedTextColor = R7TextPrimary,
                            cursorColor = R7Orange
                        ),
                        shape = RoundedCornerShape(20.dp),
                        maxLines = 3,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = { sendMessage(inputText) })
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = { sendMessage(inputText) },
                        enabled = inputText.isNotBlank() && !isLoading,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(if (inputText.isNotBlank() && !isLoading) R7Orange else Color(0xFF2E333D))
                            .testTag("btn_send_gemini_chat")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = "إرسال",
                            tint = if (inputText.isNotBlank() && !isLoading) Color.White else Color(0xFF6E7480),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChatMessageItem(
    message: ChatMessage,
    onCopy: () -> Unit,
    onInsert: () -> Unit,
    onRetry: (String) -> Unit,
    onRestoreInput: (String) -> Unit,
    onFeedback: (String) -> Unit
) {
    val isUser = (message.role == "user")
    var feedbackState by remember { mutableStateOf<String?>(null) }

    val bubbleColor = when {
        message.isError -> Color(0xFF3A1A1A)
        isUser -> Color(0xFF232A35)
        else -> R7PanelDark
    }

    val borderColor = when {
        message.isError -> Color(0xFF8B2B2B)
        isUser -> Color(0xFF3A475A)
        else -> Color(0xFF2F3540)
    }

    val textColor = when {
        message.isError -> Color(0xFFFFB4AB)
        isUser -> Color(0xFFE2E8F0)
        else -> R7TextPrimary
    }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Surface(
            color = bubbleColor,
            shape = RoundedCornerShape(
                topStart = 14.dp,
                topEnd = 14.dp,
                bottomStart = if (isUser) 14.dp else 2.dp,
                bottomEnd = if (isUser) 2.dp else 14.dp
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
            modifier = Modifier
                .widthIn(max = 340.dp)
                .testTag("chat_bubble_${message.id}")
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                SelectionContainer {
                    Text(
                        text = message.text,
                        color = textColor,
                        fontSize = 14.sp,
                        lineHeight = 21.sp
                    )
                }

                // If error bubble, offer retry and restore input actions (NO settings exposed)
                if (message.isError && message.failedPrompt != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = Color(0xFF5A2A2A), thickness = 0.8.dp)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onRetry(message.failedPrompt) },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFFD8D3)),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(34.dp)
                                .testTag("btn_retry_chat")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("إعادة المحاولة", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        TextButton(
                            onClick = { onRestoreInput(message.failedPrompt) },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            modifier = Modifier.weight(1f).height(34.dp)
                        ) {
                            Text(
                                text = "استرجاع السؤال ⏎",
                                color = R7TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                // Normal response action buttons (Copy / Insert / feedback)
                if (!isUser && !message.isError) {
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = Color(0xFF282D38), thickness = 0.6.dp)
                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Feedback signals: thumbs up / down
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    feedbackState = "THUMBS_UP"
                                    onFeedback("THUMBS_UP")
                                },
                                modifier = Modifier
                                    .size(24.dp)
                                    .testTag("btn_feedback_up_${message.id}")
                            ) {
                                Icon(
                                    Icons.Default.ThumbUp,
                                    contentDescription = "مفيد",
                                    tint = if (feedbackState == "THUMBS_UP") R7Orange else R7TextSecondary,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(2.dp))
                            IconButton(
                                onClick = {
                                    feedbackState = "THUMBS_DOWN"
                                    onFeedback("THUMBS_DOWN")
                                },
                                modifier = Modifier
                                    .size(24.dp)
                                    .testTag("btn_feedback_down_${message.id}")
                            ) {
                                Icon(
                                    Icons.Default.ThumbDown,
                                    contentDescription = "غير مفيد",
                                    tint = if (feedbackState == "THUMBS_DOWN") Color(0xFFFF8A65) else R7TextSecondary,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }

                        // Copy & Insert to calculator
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(
                                onClick = onCopy,
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 1.dp)
                            ) {
                                Text("نسخ", color = R7TextSecondary, fontSize = 11.sp)
                            }
                            TextButton(
                                onClick = onInsert,
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 1.dp)
                            ) {
                                Text("إدراج بالآلة", color = R7Orange, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Extracts candidate math numbers or formulas from an AI response to insert into the calculator.
 */
private fun extractMathCandidate(text: String): String {
    val eqMatch = Regex("""=\s*([+-]?\d+(?:\.\d+)?(?:[eE][+-]?\d+)?)""").find(text)
    if (eqMatch != null) {
        return eqMatch.groupValues[1].trim()
    }
    val numMatch = Regex("""\b([+-]?\d+(?:\.\d+)?)\b""").findAll(text).map { it.value }.lastOrNull()
    if (numMatch != null) {
        return numMatch
    }
    return text.take(20).trim()
}
