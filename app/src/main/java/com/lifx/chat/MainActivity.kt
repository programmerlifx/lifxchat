package com.lifx.chat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import java.security.MessageDigest

private val Bg = Color(0xFF090D0B)
private val Panel = Color(0xFF121A16)
private val Panel2 = Color(0xFF1A251F)
private val Green = Color(0xFF25D366)
private val TextMain = Color(0xFFF1F7F3)
private val TextMuted = Color(0xFF9AA9A0)
private val Danger = Color(0xFFFF6B6B)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        setContent { LifxApp() }
    }
}

data class Person(val uid: String, val name: String, val email: String)
data class ChatMessage(val id: String, val senderId: String, val senderName: String, val text: String, val sentAt: Long)

@Composable
fun LifxApp() {
    val auth = remember { FirebaseAuth.getInstance() }
    val db = remember { FirebaseFirestore.getInstance() }
    var user by remember { mutableStateOf(auth.currentUser) }
    var selected by remember { mutableStateOf<Person?>(null) }
    var people by remember { mutableStateOf<List<Person>>(emptyList()) }
    var messages by remember { mutableStateOf<List<ChatMessage>>(emptyList()) }
    var error by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var authMode by remember { mutableStateOf(false) }
    var displayName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var draft by remember { mutableStateOf("") }

    MaterialTheme(colorScheme = darkColorScheme(
        primary = Green, onPrimary = Color.Black, background = Bg, surface = Panel,
        onBackground = TextMain, onSurface = TextMain, secondary = Green
    )) {
        Surface(Modifier.fillMaxSize(), color = Bg) {
            LaunchedEffect(user?.uid) {
                val current = user ?: return@LaunchedEffect
                db.collection("users").document(current.uid).set(
                    mapOf("uid" to current.uid, "name" to (current.displayName ?: current.email?.substringBefore("@") ?: "Pengguna"),
                        "email" to (current.email ?: ""), "updatedAt" to FieldValue.serverTimestamp()),
                    com.google.firebase.firestore.SetOptions.merge()
                )
                db.collection("users").addSnapshotListener { snap, ex ->
                    if (ex != null) { error = ex.localizedMessage ?: "Gagal memuat pengguna"; return@addSnapshotListener }
                    people = snap?.documents.orEmpty().mapNotNull { d ->
                        val uid = d.getString("uid") ?: d.id
                        if (uid == current.uid) null else Person(uid, d.getString("name") ?: "Pengguna", d.getString("email") ?: "")
                    }.sortedBy { it.name.lowercase() }
                }
            }

            if (user == null) {
                AuthScreen(
                    register = authMode, name = displayName, onName = { displayName = it },
                    email = email, onEmail = { email = it }, password = password, onPassword = { password = it },
                    error = error, busy = busy, onToggle = { authMode = !authMode; error = "" },
                    onSubmit = {
                        if (email.isBlank() || password.length < 6 || (authMode && displayName.isBlank())) {
                            error = "Isi semua kolom. Kata sandi minimal 6 karakter."; return@AuthScreen
                        }
                        busy = true; error = ""
                        val task = if (authMode) auth.createUserWithEmailAndPassword(email.trim(), password)
                            else auth.signInWithEmailAndPassword(email.trim(), password)
                        task.addOnCompleteListener { result ->
                            busy = false
                            if (result.isSuccessful) {
                                user = auth.currentUser
                                if (authMode && user != null) {
                                    user!!.updateProfile(com.google.firebase.auth.UserProfileChangeRequest.Builder().setDisplayName(displayName.trim()).build())
                                        .addOnCompleteListener { /* profile name saved on next app launch too */ }
                                    db.collection("users").document(user!!.uid).set(mapOf("uid" to user!!.uid, "name" to displayName.trim(), "email" to email.trim()), com.google.firebase.firestore.SetOptions.merge())
                                }
                            } else error = result.exception?.localizedMessage ?: "Login gagal"
                        }
                    }
                )
            } else if (selected == null) {
                HomeScreen(
                    currentName = user?.displayName ?: user?.email?.substringBefore("@") ?: "Pengguna",
                    currentEmail = user?.email ?: "", people = people, error = error,
                    onSelect = { selected = it; error = "" },
                    onLogout = { auth.signOut(); user = null; selected = null; people = emptyList() }
                )
            } else {
                ChatScreen(
                    me = user!!, person = selected!!, messages = messages, draft = draft,
                    onDraft = { draft = it }, error = error,
                    onBack = { selected = null; messages = emptyList(); draft = ""; error = "" },
                    onSend = {
                        val text = draft.trim()
                        if (text.isNotEmpty()) {
                            val current = user!!
                            val peer = selected!!
                            val chatId = chatId(current.uid, peer.uid)
                            db.collection("chats").document(chatId).collection("messages").add(
                                mapOf("senderId" to current.uid, "senderName" to (current.displayName ?: current.email?.substringBefore("@") ?: "Pengguna"),
                                    "text" to text, "sentAt" to FieldValue.serverTimestamp())
                            ).addOnSuccessListener { draft = "" }
                                .addOnFailureListener { error = it.localizedMessage ?: "Pesan gagal dikirim" }
                        }
                    }
                )
                DisposableChatListener(selected!!.uid, user!!.uid, db) { messages = it }
            }
        }
    }
}

@Composable
private fun AuthScreen(
    register: Boolean, name: String, onName: (String) -> Unit, email: String, onEmail: (String) -> Unit,
    password: String, onPassword: (String) -> Unit, error: String, busy: Boolean, onToggle: () -> Unit, onSubmit: () -> Unit
) {
    Column(Modifier.fillMaxSize().background(Bg).padding(horizontal = 26.dp), verticalArrangement = Arrangement.Center) {
        Box(Modifier.size(68.dp).background(Green, CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Chat, null, tint = Color.Black, modifier = Modifier.size(34.dp))
        }
        Spacer(Modifier.height(22.dp))
        Text("LIFX Chat", fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, color = TextMain)
        Text(if (register) "Buat akun untuk mulai terhubung." else "Pesanmu, koneksimu.", color = TextMuted, fontSize = 15.sp)
        Spacer(Modifier.height(28.dp))
        if (register) {
            LifxField(name, onName, "Nama tampilan")
            Spacer(Modifier.height(10.dp))
        }
        LifxField(email, onEmail, "Email", keyboard = KeyboardType.Email)
        Spacer(Modifier.height(10.dp))
        LifxField(password, onPassword, "Kata sandi", password = true)
        if (error.isNotBlank()) { Spacer(Modifier.height(10.dp)); Text(error, color = Danger, fontSize = 12.sp) }
        Spacer(Modifier.height(20.dp))
        Button(onClick = onSubmit, enabled = !busy, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp)) {
            if (busy) CircularProgressIndicator(Modifier.size(20.dp), color = Color.Black, strokeWidth = 2.dp)
            else Text(if (register) "Buat akun" else "Masuk", fontWeight = FontWeight.Bold)
        }
        TextButton(onClick = onToggle, modifier = Modifier.fillMaxWidth()) {
            Text(if (register) "Sudah punya akun? Masuk" else "Belum punya akun? Daftar", color = Green)
        }
    }
}

@Composable
private fun LifxField(value: String, onValue: (String) -> Unit, label: String, keyboard: KeyboardType = KeyboardType.Text, password: Boolean = false) {
    OutlinedTextField(
        value = value, onValueChange = onValue, label = { Text(label) }, singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        keyboardOptions = KeyboardOptions(keyboardType = keyboard),
        visualTransformation = if (password) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Green, unfocusedBorderColor = Panel2,
            focusedLabelColor = Green, cursorColor = Green,
            focusedTextColor = TextMain, unfocusedTextColor = TextMain
        )
    )
}

@Composable
private fun HomeScreen(currentName: String, currentEmail: String, people: List<Person>, error: String, onSelect: (Person) -> Unit, onLogout: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Bg)) {
        Row(Modifier.fillMaxWidth().background(Panel).padding(horizontal = 20.dp, vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(46.dp).background(Green, CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Chat, null, tint = Color.Black)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("LIFX Chat", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
                Text("Terhubung sebagai $currentName", color = TextMuted, fontSize = 12.sp)
            }
            IconButton(onClick = onLogout) { Icon(Icons.Default.Logout, "Keluar", tint = TextMuted) }
        }
        Column(Modifier.padding(20.dp)) {
            Text("PESAN", color = Green, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Spacer(Modifier.height(5.dp))
            Text("Mulai percakapan", fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Text("Pilih pengguna untuk mengirim pesan pribadi.", color = TextMuted, fontSize = 13.sp)
            Spacer(Modifier.height(20.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.People, null, tint = Green)
                Spacer(Modifier.width(8.dp))
                Text("Pengguna", fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Text("${people.size} akun", color = TextMuted, fontSize = 12.sp)
            }
        }
        if (error.isNotBlank()) Text(error, color = Danger, modifier = Modifier.padding(horizontal = 20.dp))
        if (people.isEmpty()) {
            Column(Modifier.fillMaxWidth().weight(1f).padding(30.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Box(Modifier.size(76.dp).background(Panel2, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.People, null, tint = Green, modifier = Modifier.size(34.dp))
                }
                Spacer(Modifier.height(16.dp))
                Text("Belum ada pengguna lain", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text("Minta teman membuat akun LIFX Chat agar muncul di sini.", color = TextMuted, fontSize = 13.sp)
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(people, key = { it.uid }) { p ->
                    Row(Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(18.dp)).clickable { onSelect(p) }.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(48.dp).background(Panel2, CircleShape), contentAlignment = Alignment.Center) {
                            Text(p.name.take(1).uppercase(), color = Green, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(p.name, fontWeight = FontWeight.SemiBold)
                            Text(p.email, color = TextMuted, fontSize = 12.sp)
                        }
                        Icon(Icons.Default.Chat, null, tint = Green)
                    }
                }
            }
        }
        Text("LIFX CHAT  •  PRIVATE MESSAGING", color = TextMuted, fontSize = 10.sp, letterSpacing = 1.5.sp, modifier = Modifier.align(Alignment.CenterHorizontally).padding(16.dp))
    }
}

@Composable
private fun ChatScreen(me: com.google.firebase.auth.FirebaseUser, person: Person, messages: List<ChatMessage>, draft: String, onDraft: (String) -> Unit, error: String, onBack: () -> Unit, onSend: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Bg)) {
        Row(Modifier.fillMaxWidth().background(Panel).padding(horizontal = 8.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Kembali", tint = TextMain) }
            Box(Modifier.size(40.dp).background(Panel2, CircleShape), contentAlignment = Alignment.Center) {
                Text(person.name.take(1).uppercase(), color = Green, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text(person.name, fontWeight = FontWeight.Bold)
                Text("Chat pribadi", color = TextMuted, fontSize = 12.sp)
            }
        }
        LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp), reverseLayout = false) {
            items(messages, key = { it.id }) { msg ->
                val mine = msg.senderId == me.uid
                Row(Modifier.fillMaxWidth(), horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start) {
                    Column(Modifier.widthIn(max = 300.dp).background(if (mine) Color(0xFF174D31) else Panel, RoundedCornerShape(16.dp)).padding(horizontal = 14.dp, vertical = 10.dp)) {
                        if (!mine) Text(msg.senderName, color = Green, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(msg.text, color = TextMain, fontSize = 15.sp)
                    }
                }
            }
        }
        if (error.isNotBlank()) Text(error, color = Danger, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 12.dp))
        Row(Modifier.fillMaxWidth().background(Panel).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(value = draft, onValueChange = onDraft, modifier = Modifier.weight(1f), placeholder = { Text("Tulis pesan...", color = TextMuted) }, maxLines = 4, shape = RoundedCornerShape(24.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Green, unfocusedBorderColor = Panel2, cursorColor = Green))
            Spacer(Modifier.width(8.dp))
            FilledIconButton(onClick = onSend, enabled = draft.isNotBlank(), colors = IconButtonDefaults.filledIconButtonColors(containerColor = Green, contentColor = Color.Black)) {
                Icon(Icons.Default.Send, "Kirim")
            }
        }
    }
}

@Composable
private fun DisposableChatListener(peerUid: String, myUid: String, db: FirebaseFirestore, onMessages: (List<ChatMessage>) -> Unit) {
    DisposableEffect(peerUid, myUid) {
        val id = chatId(myUid, peerUid)
        val registration: ListenerRegistration = db.collection("chats").document(id).collection("messages")
            .orderBy("sentAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snap, _ ->
                onMessages(snap?.documents.orEmpty().map { d ->
                    ChatMessage(d.id, d.getString("senderId") ?: "", d.getString("senderName") ?: "Pengguna",
                        d.getString("text") ?: "", d.getTimestamp("sentAt")?.toDate()?.time ?: 0L)
                })
            }
        onDispose { registration.remove() }
    }
}

private fun chatId(a: String, b: String): String {
    val pair = listOf(a, b).sorted().joinToString("_")
    val bytes = MessageDigest.getInstance("SHA-256").digest(pair.toByteArray())
    return bytes.joinToString("") { "%02x".format(it) }
}
