package com.sillygirl.client.ui.screens.fenyong

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.sillygirl.client.ui.components.*
import com.sillygirl.client.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

// Helper to avoid internal weight val conflict in Compose compiler
private fun Modifier._weight(p: Float) = androidx.compose.foundation.layout.weight(this, p)

private val RedColor = Color(0xFFE60012)
private val OrangeColor = Color(0xFFFF5000)
private val PddColor = Color(0xFFE02A24)
private val GrayText = Color(0xFF999999)
