package com.lifx.chat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Green = Color(0xFF075E54)
private val BrightGreen = Color(0xFF128C7E)
private val Page = Color(0xFFF5F6F6)
private val Ink = Color(0xFF172B2A)
private val Muted = Color(0xFF74817F)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try { FirebaseApp.initializeApp(this) } catch (_: Exception) {}
        setContent { MaterialTheme(colorScheme = lightColorScheme(primary = Green, secondary = BrightGreen, background = Page)) { LifxApp() } }
    }
}

data class Person(val id: String, val name: String, val about: String = "Hai! Saya menggunakan LIFX Chat", val initials: String = name.take(1).uppercase())
data class ChatItem(val id: String, val name: String, val message: String, val time: String, val unread: Int = 0, val self: Boolean = false)

@Composable
fun LifxApp() {
    var tab by remember { mutableStateOf("Chat") }
    var screen by remember { mutableStateOf("main") }
    var selected by remember { mutableStateOf(ChatItem("self", "Pesan ke diri sendiri", "Catatan pribadi", "Sekarang", self = true)) }
    var profileName by remember { mutableStateOf("Pengguna LIFX") }
    var about by remember { mutableStateOf("Hai! Saya menggunakan LIFX Chat") }
    var email by remember { mutableStateOf("") }
    var messageInput by remember { mutableStateOf("") }
    var messages by remember { mutableStateOf(listOf<Pair<String, Boolean>>("Selamat datang di LIFX Chat 👋" to false)) }
    var contacts by remember { mutableStateOf(listOf(Person("self", "Pesan ke diri sendiri", "Catatan pribadi", "✎"))) }
    var chats by remember { mutableStateOf(listOf(ChatItem("self", "Pesan ke diri sendiri", "Catatan pribadi", "Sekarang", self = true))) }
    var search by remember { mutableStateOf("") }
    val auth = remember { runCatching { FirebaseAuth.getInstance() }.getOrNull() }
    val db = remember { runCatching { FirebaseFirestore.getInstance() }.getOrNull() }

    if (screen == "conversation") {
        ConversationScreen(selected, messages, messageInput, { messageInput = it }, {
            if (messageInput.isNotBlank()) {
                val text = messageInput.trim()
                messages = messages + (text to true)
                chats = listOf(ChatItem(selected.id, selected.name, text, "Sekarang", self = selected.self)) + chats.filterNot { it.id == selected.id }
                if (selected.id == "self") {
                    runCatching { db?.collection("users")?.document(auth?.currentUser?.uid ?: "local")?.collection("selfMessages")?.add(mapOf("text" to text, "createdAt" to System.currentTimeMillis())) }
                }
                messageInput = ""
            }
        }, { screen = "main" })
        return
    }
    if (screen == "profile") {
        ProfileScreen(profileName, { profileName = it }, about, { about = it }, email, { email = it }, { screen = "main" }, {
            runCatching { auth?.signOut() }
            screen = "main"
        })
        return
    }

    Scaffold(
        containerColor = Page,
        topBar = {
            Column {
                Row(Modifier.fillMaxWidth().background(Green).padding(start = 18.dp, end = 10.dp, top = 18.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("LIFX Chat", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                        Text(if (auth?.currentUser != null) "Terhubung ke akun" else "Pesan yang dekat, kapan saja", color = Color.White.copy(alpha = .78f), fontSize = 12.sp)
                    }
                    IconButton(onClick = { screen = "profile" }) { Icon(Icons.Default.AccountCircle, "Profil", tint = Color.White, modifier = Modifier.size(29.dp)) }
                    IconButton(onClick = { search = if (search.isEmpty()) " " else "" }) { Icon(Icons.Default.Search, "Cari", tint = Color.White) }
                    var menu by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, "Menu", tint = Color.White) }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            DropdownMenuItem(text = { Text("Profil") }, leadingIcon = { Icon(Icons.Default.Person, null) }, onClick = { menu = false; screen = "profile" })
                            DropdownMenuItem(text = { Text("Kontak") }, leadingIcon = { Icon(Icons.Default.Contacts, null) }, onClick = { menu = false; tab = "Kontak" })
                            DropdownMenuItem(text = { Text("Pengaturan") }, leadingIcon = { Icon(Icons.Default.Settings, null) }, onClick = { menu = false; tab = "Pengaturan" })
                        }
                    }
                }
                if (search.isNotEmpty()) {
                    OutlinedTextField(value = search, onValueChange = { search = it }, modifier = Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 12.dp, vertical = 4.dp), placeholder = { Text("Cari chat atau kontak") }, singleLine = true, trailingIcon = { IconButton(onClick = { search = "" }) { Icon(Icons.Default.Close, null) } })
                }
                Row(Modifier.fillMaxWidth().background(Green), horizontalArrangement = Arrangement.SpaceEvenly) {
                    listOf("Chat", "Status", "Panggilan", "Kontak").forEach { label ->
                        Column(Modifier.weight(1f).clickable { tab = label }.padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(label.uppercase(), color = if (tab == label) Color.White else Color.White.copy(alpha = .65f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(7.dp))
                            Box(Modifier.fillMaxWidth().height(3.dp).background(if (tab == label) Color.White else Color.Transparent))
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            if (tab == "Chat") FloatingActionButton(onClick = { tab = "Kontak" }, containerColor = BrightGreen, contentColor = Color.White) { Icon(Icons.Default.Chat, "Chat baru") }
            else if (tab == "Kontak") FloatingActionButton(onClick = { tab = "Tambah kontak" }, containerColor = BrightGreen, contentColor = Color.White) { Icon(Icons.Default.PersonAdd, "Tambah kontak") }
        },
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                listOf(Triple("Chat", Icons.Default.Chat, "Chat"), Triple("Kontak", Icons.Default.Contacts, "Kontak"), Triple("Pengaturan", Icons.Default.Settings, "Setelan")).forEach { (label, icon, title) ->
                    NavigationBarItem(selected = (tab == label), onClick = { tab = label }, icon = { Icon(icon, null) }, label = { Text(title) })
                }
            }
        }
    ) { padding ->
        when (tab) {
            "Chat" -> ChatList(chats.filter { it.name.contains(search.trim(), true) || it.message.contains(search.trim(), true) }, padding) { item ->
                selected = item
                messages = if (item.self) listOf("Ini ruang pesan pribadi kamu. Simpan catatan atau kirim pesan untuk diri sendiri." to false) else listOf("Mulai percakapan dengan ${item.name}" to false)
                screen = "conversation"
            }
            "Kontak" -> ContactsScreen(contacts.filter { it.name.contains(search.trim(), true) }, padding, onOpen = { person ->
                selected = ChatItem(person.id, person.name, person.about, "Sekarang", self = person.id == "self")
                messages = listOf("Mulai percakapan dengan ${person.name}" to false)
                screen = "conversation"
            }, onAdd = { name ->
                val person = Person("local_${System.currentTimeMillis()}", name, initials = name.take(1).uppercase())
                contacts = contacts + person
                chats = listOf(ChatItem(person.id, person.name, "Mulai percakapan", "Baru")) + chats
                runCatching { db?.collection("users")?.document(person.id)?.set(mapOf("name" to name, "about" to "Hai! Saya menggunakan LIFX Chat")) }
            })
            "Status" -> StatusScreen(profileName)
            "Panggilan" -> CallsScreen()
            "Tambah kontak" -> AddContactScreen { name ->
                if (name.isNotBlank()) {
                    val person = Person("local_${System.currentTimeMillis()}", name.trim(), initials = name.trim().take(1).uppercase())
                    contacts = contacts + person
                    tab = "Kontak"
                }
            }
            else -> SettingsScreen(onProfile = { screen = "profile" }, onSelfChat = {
                selected = ChatItem("self", "Pesan ke diri sendiri", "Catatan pribadi", "Sekarang", self = true)
                messages = listOf("Ini ruang pesan pribadi kamu. Simpan catatan atau kirim pesan untuk diri sendiri." to false)
                screen = "conversation"
            })
        }
    }
}

@Composable
fun ChatList(items: List<ChatItem>, padding: PaddingValues, onClick: (ChatItem) -> Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(padding)) {
        item {
            Row(Modifier.fillMaxWidth().clickable { onClick(ChatItem("self", "Pesan ke diri sendiri", "Catatan pribadi", "Sekarang", self = true)) }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Avatar("✎", BrightGreen)
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text("Pesan ke diri sendiri", fontWeight = FontWeight.SemiBold, color = Ink)
                    Text("Simpan catatan pribadi", color = Muted, fontSize = 13.sp)
                }
                Icon(Icons.Default.Bookmark, null, tint = BrightGreen)
            }
            HorizontalDivider(color = Color(0xFFE6EBE9))
        }
        items(items.filterNot { it.self }) { item ->
            Row(Modifier.fillMaxWidth().clickable { onClick(item) }.padding(horizontal = 16.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
                Avatar(item.name.take(1).uppercase(), avatarColor(item.id))
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(item.name, fontWeight = FontWeight.SemiBold, color = Ink, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(item.time, color = Muted, fontSize = 11.sp)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(item.message, color = Muted, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            HorizontalDivider(color = Color(0xFFE6EBE9), modifier = Modifier.padding(start = 82.dp))
        }
    }
}

@Composable
fun Avatar(text: String, color: Color) {
    Box(Modifier.size(52.dp).clip(CircleShape).background(color), contentAlignment = Alignment.Center) {
        Text(text.take(2), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
    }
}

@Composable
fun ConversationScreen(item: ChatItem, messages: List<Pair<String, Boolean>>, input: String, onInput: (String) -> Unit, onSend: () -> Unit, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Color(0xFFECE5DD))) {
        Row(Modifier.fillMaxWidth().background(Green).padding(horizontal = 6.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = Color.White) }
            Avatar(item.name.take(1).uppercase(), BrightGreen)
            Column(Modifier.weight(1f).padding(start = 10.dp)) {
                Text(item.name, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
                Text(if (item.self) "Ruang pribadi" else "LIFX Chat", color = Color.White.copy(alpha = .75f), fontSize = 12.sp)
            }
            IconButton(onClick = {}) { Icon(Icons.Default.Call, null, tint = Color.White) }
            IconButton(onClick = {}) { Icon(Icons.Default.MoreVert, null, tint = Color.White) }
        }
        LazyColumn(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), reverseLayout = false, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(messages) { msg ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = if (msg.second) Arrangement.End else Arrangement.Start) {
                    Column(Modifier.widthIn(max = 300.dp).clip(RoundedCornerShape(12.dp)).background(if (msg.second) Color(0xFFD9FDD3) else Color.White).padding(horizontal = 12.dp, vertical = 8.dp)) {
                        Text(msg.first, color = Ink, fontSize = 15.sp)
                        Row(Modifier.align(Alignment.End).padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()), color = Muted, fontSize = 10.sp)
                            if (msg.second) { Spacer(Modifier.width(4.dp)); Icon(Icons.Default.DoneAll, null, tint = Color(0xFF53A6D8), modifier = Modifier.size(14.dp)) }
                        }
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().background(Color(0xFFECE5DD)).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(value = input, onValueChange = onInput, modifier = Modifier.weight(1f), placeholder = { Text("Ketik pesan") }, shape = RoundedCornerShape(28.dp), maxLines = 4, colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = Color.White, focusedContainerColor = Color.White))
            Spacer(Modifier.width(7.dp))
            IconButton(onClick = onSend, modifier = Modifier.size(50.dp).background(BrightGreen, CircleShape)) { Icon(Icons.Default.Send, null, tint = Color.White) }
        }
    }
}

@Composable
fun ContactsScreen(contacts: List<Person>, padding: PaddingValues, onOpen: (Person) -> Unit, onAdd: (String) -> Unit) {
    var show by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(padding)) {
        Row(Modifier.fillMaxWidth().clickable { show = true }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Avatar("+", BrightGreen)
            Column(Modifier.padding(start = 12.dp)) { Text("Kontak baru", fontWeight = FontWeight.Bold); Text("Tambahkan kontak ke daftar", color = Muted, fontSize = 13.sp) }
        }
        HorizontalDivider()
        LazyColumn {
            items(contacts) { person ->
                Row(Modifier.fillMaxWidth().clickable { onOpen(person) }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Avatar(person.initials, avatarColor(person.id))
                    Column(Modifier.padding(start = 12.dp)) {
                        Text(person.name, color = Ink, fontWeight = FontWeight.SemiBold)
                        Text(person.about, color = Muted, fontSize = 13.sp)
                    }
                }
                HorizontalDivider(color = Color(0xFFE6EBE9), modifier = Modifier.padding(start = 82.dp))
            }
        }
    }
    if (show) AlertDialog(onDismissRequest = { show = false }, title = { Text("Tambah kontak") }, text = { OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nama kontak") }, singleLine = true) }, confirmButton = { TextButton(onClick = { if (name.isNotBlank()) onAdd(name.trim()); name = ""; show = false }) { Text("Simpan") } }, dismissButton = { TextButton(onClick = { show = false }) { Text("Batal") } })
}

@Composable
fun AddContactScreen(onAdd: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Tambah kontak", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text("Buat kontak lokal untuk memulai percakapan.", color = Muted)
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nama kontak") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        Button(onClick = { onAdd(name) }, modifier = Modifier.fillMaxWidth()) { Text("Simpan kontak") }
    }
}

@Composable
fun ProfileScreen(name: String, onName: (String) -> Unit, about: String, onAbout: (String) -> Unit, email: String, onEmail: (String) -> Unit, onBack: () -> Unit, onSignOut: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Page)) {
        Row(Modifier.fillMaxWidth().background(Green).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = Color.White) }
            Text("Profil", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold)
        }
        Column(Modifier.fillMaxWidth().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(104.dp).clip(CircleShape).background(BrightGreen), contentAlignment = Alignment.Center) { Text(name.take(1).uppercase(), fontSize = 40.sp, color = Color.White, fontWeight = FontWeight.Bold) }
            Text("Ketuk kolom untuk mengedit", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 10.dp, bottom = 20.dp))
            OutlinedTextField(value = name, onValueChange = onName, modifier = Modifier.fillMaxWidth(), label = { Text("Nama") }, leadingIcon = { Icon(Icons.Default.Person, null) }, singleLine = true)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(value = about, onValueChange = onAbout, modifier = Modifier.fillMaxWidth(), label = { Text("Info") }, leadingIcon = { Icon(Icons.Default.Info, null) }, minLines = 2)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(value = email, onValueChange = onEmail, modifier = Modifier.fillMaxWidth(), label = { Text("Email (opsional)") }, leadingIcon = { Icon(Icons.Default.Email, null) }, singleLine = true)
            Spacer(Modifier.height(18.dp))
            Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Simpan profil") }
            TextButton(onClick = onSignOut) { Text("Keluar dari akun") }
        }
    }
}

@Composable
fun StatusScreen(name: String) {
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Text("Status", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text("Bagikan kabar singkat kepada kontakmu.", color = Muted, modifier = Modifier.padding(top = 4.dp, bottom = 18.dp))
        Row(verticalAlignment = Alignment.CenterVertically) { Avatar(name.take(1), BrightGreen); Column(Modifier.padding(start = 12.dp)) { Text("Status saya", fontWeight = FontWeight.SemiBold); Text("Ketuk untuk menambahkan status", color = Muted, fontSize = 13.sp) } }
        Spacer(Modifier.height(24.dp))
        Text("Pembaruan terbaru", fontWeight = FontWeight.Bold)
        Text("Belum ada status dari kontak.", color = Muted, modifier = Modifier.padding(top = 12.dp))
    }
}

@Composable
fun CallsScreen() {
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Text("Panggilan", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text("Riwayat panggilan akan muncul di sini.", color = Muted, modifier = Modifier.padding(top = 6.dp))
        Spacer(Modifier.height(20.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(52.dp).clip(CircleShape).background(Color(0xFFDCEFEA)), contentAlignment = Alignment.Center) { Icon(Icons.Default.Link, null, tint = Green) }
            Column(Modifier.padding(start = 12.dp)) { Text("Tautan panggilan", fontWeight = FontWeight.SemiBold); Text("Buat tautan untuk panggilan (belum aktif)", color = Muted, fontSize = 12.sp) }
        }
        Spacer(Modifier.height(16.dp))
        Text("Catatan: panggilan suara/video membutuhkan integrasi layanan tambahan.", color = Muted, fontSize = 12.sp)
    }
}

@Composable
fun SettingsScreen(onProfile: () -> Unit, onSelfChat: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(18.dp)) {
        Text("Pengaturan", fontSize = 25.sp, fontWeight = FontWeight.Bold, color = Ink)
        Text("Atur akun dan pengalaman chat kamu.", color = Muted, modifier = Modifier.padding(top = 4.dp, bottom = 20.dp))
        SettingRow(Icons.Default.Person, "Profil", "Nama dan info akun", onProfile)
        SettingRow(Icons.Default.Lock, "Privasi", "Kontrol privasi (dasar)", {})
        SettingRow(Icons.Default.Notifications, "Notifikasi", "Pengaturan pemberitahuan", {})
        SettingRow(Icons.Default.Chat, "Chat pribadi", "Buka pesan ke diri sendiri", onSelfChat)
        SettingRow(Icons.Default.Storage, "Penyimpanan dan data", "Penggunaan data (belum aktif)", {})
        Spacer(Modifier.height(18.dp))
        Text("LIFX Chat · 1.1.0", color = Muted, fontSize = 12.sp)
    }
}
@Composable
fun SettingRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, desc: String, action: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { action() }.padding(vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xFFE2F1ED)), contentAlignment = Alignment.Center) { Icon(icon, null, tint = Green) }
        Column(Modifier.weight(1f).padding(start = 13.dp)) { Text(title, fontWeight = FontWeight.SemiBold, color = Ink); Text(desc, color = Muted, fontSize = 12.sp) }
        Icon(Icons.Default.ChevronRight, null, tint = Muted)
    }
    HorizontalDivider(color = Color(0xFFE6EBE9))
}
fun avatarColor(id: String): Color = when (id.hashCode().absoluteValue % 5) {
    0 -> Color(0xFF128C7E); 1 -> Color(0xFF5877A8); 2 -> Color(0xFF9A6AB0); 3 -> Color(0xFFB27A42); else -> Color(0xFF518B68)
}
private val Int.absoluteValue: Int get() = if (this == Int.MIN_VALUE) 0 else kotlin.math.abs(this)
