package com.example.to_do_list

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.CalendarContract
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.to_do_list.ui.theme.To_do_listTheme
import com.example.to_do_list.ui.theme.*
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            To_do_listTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = background
                ) {
                    CalendarWrapper()
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun CalendarWrapper() {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED)
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        hasPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasPermission) launcher.launch(Manifest.permission.READ_CALENDAR)
    }

    if (hasPermission) {
        SimpleCustomCalendar(context)
    } else {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "Vui lòng cấp quyền truy cập lịch để xem danh sách ngày.")
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun SimpleCustomCalendar(context: Context) {
    val currentDate = remember { LocalDate.now() }
    var selectedDate by remember { mutableStateOf(currentDate) }

    var isExpanded by remember { mutableStateOf(false) }

    // Thay thế format ngày cũ thành chữ "Tháng X"
    val monthText = "Tháng ${selectedDate.monthValue}"

    val weekDays = remember(selectedDate) {
        val firstDayOfWeek = selectedDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        (0..6).map { firstDayOfWeek.plusDays(it.toLong()) }
    }

    val monthDays = remember(selectedDate) {
        val firstDayOfMonth = selectedDate.withDayOfMonth(1)
        val firstDayOfGrid = firstDayOfMonth.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        (0..41).map { firstDayOfGrid.plusDays(it.toLong()) }
    }

    val events = remember { getCalendarEvents(context) }

    Column(modifier = Modifier.padding(top = 40.dp, start = 16.dp, end = 16.dp)) {

        // --- THANH ĐIỀU HƯỚNG TRÊN CÙNG GIAO DIỆN iOS ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Nút bên trái (Hình tròn)
            IconButton(
                onClick = { /* TODO: Xử lý sự kiện click */ },
                modifier = Modifier
                    .background(Color.LightGray.copy(alpha = 0.3f), shape = CircleShape)
                    .size(40.dp)
            ) {
                Icon(imageVector = Icons.Default.List, contentDescription = "Menu")
            }

            // 2. Chữ "Tháng 5" in đậm ở giữa
            Text(
                text = monthText,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )

            // 3. Cụm 2 nút bên phải (Hình viên thuốc)
            Row(
                modifier = Modifier
                    .background(Color.LightGray.copy(alpha = 0.3f), shape = RoundedCornerShape(50))
                    .height(40.dp)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { /* TODO */ }, modifier = Modifier.size(36.dp)) {
                    Icon(imageVector = Icons.Default.Settings, contentDescription = "Tùy chỉnh", modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = { /* TODO */ }, modifier = Modifier.size(36.dp)) {
                    Icon(imageVector = Icons.Default.MoreVert, contentDescription = "Thêm", modifier = Modifier.size(20.dp))
                }
            }
        }
        // ------------------------------------------------

        Spacer(modifier = Modifier.height(24.dp))

        // KHUNG LỊCH CHÍNH
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize()
                .background(Color.Transparent)
                .pointerInput(Unit) {
                    detectVerticalDragGestures { change, dragAmount ->
                        change.consume()
                        if (dragAmount > 8) isExpanded = true
                        else if (dragAmount < -8) isExpanded = false
                    }
                }
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                weekDays.forEach { date ->
                    Text(
                        text = date.format(DateTimeFormatter.ofPattern("EEE", Locale.ENGLISH)),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        fontWeight = FontWeight.Bold, // THÊM IN ĐẬM Ở ĐÂY
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (isExpanded) {
                val rows = monthDays.chunked(7)
                rows.forEach { week ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        week.forEach { date ->
                            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                DayCell(
                                    date = date,
                                    currentDate = currentDate,
                                    selectedDate = selectedDate,
                                    isCurrentMonth = date.month == selectedDate.month,
                                    onClick = { selectedDate = date }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            } else {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    weekDays.forEach { date ->
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            DayCell(
                                date = date,
                                currentDate = currentDate,
                                selectedDate = selectedDate,
                                isCurrentMonth = true,
                                onClick = { selectedDate = date }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(events.size) { index ->
                Text(
                    text = "• ${events[index]}",
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun DayCell(
    date: LocalDate,
    currentDate: LocalDate,
    selectedDate: LocalDate,
    isCurrentMonth: Boolean,
    onClick: () -> Unit
) {
    val isToday = date == currentDate
    val isSelected = date == selectedDate

    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .clickable { onClick() }
            .background(
                color = if (isSelected) Color(0xFF87B5E2) else Color.Transparent,
                shape = CircleShape
            )
            .border(
                width = 1.dp,
                color = if (isToday && !isSelected) Color(0xFF87B5E2) else Color.Transparent,
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "${date.dayOfMonth}",
            color = if (isSelected) Color.White
            else if (isToday) Color(0xFF87B5E2)
            else if (!isCurrentMonth) Color.Gray
            else Color.Black,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

fun getCalendarEvents(context: Context): List<String> {
    val events = mutableListOf<String>()
    val projection = arrayOf(CalendarContract.Events.TITLE)
    val cursor = context.contentResolver.query(CalendarContract.Events.CONTENT_URI, projection, null, null, null)

    cursor?.use {
        val titleIdx = it.getColumnIndexOrThrow(CalendarContract.Events.TITLE)
        while (it.moveToNext()) {
            events.add(it.getString(titleIdx))
        }
    }
    return events
}